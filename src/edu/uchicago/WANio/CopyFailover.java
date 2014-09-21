package edu.uchicago.WANio;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SuppressWarnings("serial")
public class CopyFailover extends HttpServlet {
	private static final Logger log = Logger.getLogger(CopyFailover.class.getName());

	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	private JsonElement getJsonFromUrl(String theURL){
		log.info("getting data from:"+theURL);
		URL url;
		HttpURLConnection request;
		JsonParser jp = new JsonParser();
		JsonElement root = null;
		try {
			url = new URL(theURL);
			request = (HttpURLConnection) url.openConnection();
		    request.setRequestMethod("POST");
		    request.setRequestProperty("Accept", "application/json");
			request.connect();
			root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
			request.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return root;
	}
	
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.warning("CopyFromPandaMonServlet Got a POST...");
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.info("CopyFromPandaMonServlet Got a GET...");
		
		if (req.getParameter("preload") != null) {
			List<String> lSites = new ArrayList<String>();
			Query q = new Query("failoverSummary").setDistinct(true);
			q.addProjection(new PropertyProjection("site", String.class));
			List<Entity> lRes = datastore.prepare(q).asList(FetchOptions.Builder.withDefaults());
			for (Entity result : lRes) {
				String res = (String) result.getProperty("site");
				if (res != null)
					lSites.add(res);
			}
			Gson gson = new Gson();
			resp.getWriter().println(gson.toJson(lSites));
			return;
		}

		if (req.getParameter("failovertime") != null && req.getParameter("failoverplot") != null && req.getParameter("failoversite") != null){

			log.warning("collecting data...");
			Filter f1 = new FilterPredicate("site", FilterOperator.EQUAL, req.getParameter("failoversite"));
			Integer interval = Integer.parseInt(req.getParameter("failovertime"));
			Date currTime = new Date(new Date().getTime() - interval * 3600l * 1000);
			Filter f2 = new FilterPredicate("timestamp", FilterOperator.GREATER_THAN, currTime);
			Filter cF = CompositeFilterOperator.and(f1, f2);

			Query q = new Query("failoverSummary").setFilter(cF);
			List<Entity> lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(1000));
			log.warning("lRes obtained... " + lRes.size());

			JSONObject res = new JSONObject();
			JSONArray plot = new JSONArray();

			if (req.getParameter("failoverplot").equals("jobs")) {
				JSONObject succ = new JSONObject();
				JSONObject fail = new JSONObject();
				JSONArray dsucc = new JSONArray();
				JSONArray dfail = new JSONArray();
				succ.put("name", "success");
				fail.put("name", "failure");
				for (Entity result : lRes) {
					JSONArray su = new JSONArray();
					JSONArray fa = new JSONArray();
					su.put(((Date) result.getProperty("timestamp")).getTime());
					su.put((Long) result.getProperty("successes"));
					fa.put(((Date) result.getProperty("timestamp")).getTime());
					fa.put((Long) result.getProperty("failures"));
					dsucc.put(su);
					dfail.put(fa);
				}
				succ.put("data", dsucc);
				fail.put("data", dfail);
				plot.put(succ);
				plot.put(fail);
			}

			if (req.getParameter("failoverplot").equals("Files")) {
				JSONObject ser1 = new JSONObject();
				JSONObject ser2 = new JSONObject();
				JSONArray wF  = new JSONArray();
				JSONArray woF = new JSONArray();
				ser1.put("name", "Files obtained using FAX");
				ser2.put("name", "Files obtained directly");
				for (Entity result : lRes) {
					JSONArray w = new JSONArray();
					JSONArray wo = new JSONArray();
					w.put(((Date) result.getProperty("timestamp")).getTime());
					w.put((Long) result.getProperty("WithFAX"));
					wo.put(((Date) result.getProperty("timestamp")).getTime());
					wo.put((Long) result.getProperty("WithoutFAX"));
					wF.put(w);
					woF.put(wo);
				}
				ser1.put("data", wF);
				ser2.put("data", woF);
				plot.put(ser1);
				plot.put(ser2);
			}

			if (req.getParameter("failoverplot").equals("Data size")) {
				JSONObject ser1 = new JSONObject();
				JSONObject ser2 = new JSONObject();
				JSONArray wF  = new JSONArray();
				JSONArray woF = new JSONArray();
				ser1.put("name", "Data obtained using FAX");
				ser2.put("name", "Data obtained directly");
				for (Entity result : lRes) {
					JSONArray w = new JSONArray();
					JSONArray wo = new JSONArray();
					w.put(((Date) result.getProperty("timestamp")).getTime());
					w.put(((Long) result.getProperty("bytesWithFAX"))/1024./1024./1024.);
					wo.put(((Date) result.getProperty("timestamp")).getTime());
					wo.put(((Long) result.getProperty("bytesWithoutFAX"))/1024./1024./1024.);
					wF.put(w);
					woF.put(wo);
				}
				ser1.put("data", wF);
				ser2.put("data", woF);
				plot.put(ser1);
				plot.put(ser2);
			}			
			
			if (req.getParameter("failoverplot").equals("Transfer duration")) {
				JSONObject ser1 = new JSONObject();
				ser1.put("name", "duration");
				JSONArray data = new JSONArray();
				for (Entity result : lRes) {
					JSONArray point = new JSONArray();
					point.put(((Date) result.getProperty("timestamp")).getTime());
					point.put((Double) result.getProperty("timeToCopy"));
					data.put(point);
				}
				ser1.put("data", data);
				plot.put(ser1);
			}

			res.put("plot", plot);

			// for the table
			JSONObject tableData = new JSONObject();
			JSONArray data = new JSONArray();
			JSONArray headers = new JSONArray();
			headers.put(new JSONObject().put("title", "Site"));
			headers.put(new JSONObject().put("title", "Time"));
			headers.put(new JSONObject().put("title", "Successes"));
			headers.put(new JSONObject().put("title", "Failures"));
			headers.put(new JSONObject().put("title", "files with"));
			headers.put(new JSONObject().put("title", "files without"));
			headers.put(new JSONObject().put("title", "GBs with"));
			headers.put(new JSONObject().put("title", "GBs without"));
			headers.put(new JSONObject().put("title", "duration"));
			for (Entity result : lRes) {
				JSONArray su = new JSONArray();
				su.put((String) result.getProperty("site"));
				su.put(((Date) result.getProperty("timestamp")).getTime());
				su.put((Long) result.getProperty("successes"));
				su.put((Long) result.getProperty("failures"));
				su.put((Long) result.getProperty("WithFAX"));
				su.put((Long) result.getProperty("WithoutFAX"));
				su.put(((Long) result.getProperty("bytesWithFAX"))/1024./1024./1024.);
				su.put(((Long) result.getProperty("bytesWithoutFAX"))/1024./1024./1024.);
				su.put((Double) result.getProperty("timeToCopy"));
				data.put(su);
			}
			tableData.put("data", data);
			tableData.put("headers", headers);
			res.put("tableData", tableData);
			resp.getWriter().print(res);
			return;
		}
		
		
		
		
		
		class SumUp {
			int successes = 0;
			int failures = 0;
			int fileswith = 0;
			int fileswithout = 0;
			long byteswith = 0;
			long byteswithout = 0;
			float timeToCopy = 0;
		}

		Map<String, SumUp> summary = new HashMap<String, SumUp>();
		SumUp all = new SumUp();
		
		
		String directURL = "http://pandamon.cern.ch/fax/failover?hours=4";
	    JsonObject rootobj = getJsonFromUrl(directURL).getAsJsonObject(); 
	    
	    //log.warning(rootobj.toString());
	    JsonArray pm=rootobj.getAsJsonArray("pm");
	    JsonObject js=new JsonObject();
	    for (int i=0;i<pm.size();i++){
	    	js=pm.get(i).getAsJsonObject();
	    	if (js.has("json")){
	    		break;
	    	}
	    }
	    
	    
	    
	    js=js.get("json").getAsJsonObject();
	    JsonArray rows=js.getAsJsonArray("info");
		List<Entity> lRes=new ArrayList<Entity>();
		
		for (int i = 0; i < rows.size(); ++i) {
			JsonArray rec = rows.get(i).getAsJsonArray();

			Entity ep = new Entity("failover", rec.get(0).getAsLong());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			Date date = null;
		    try {
		        date = sdf.parse(rec.get(1).getAsString());
		    } catch (ParseException e) {
		        e.printStackTrace();
		    }
		    String cs=rec.get(2).getAsString().split(":")[1];
		    String st=rec.get(3).getAsString();
			ep.setProperty("time", date);
			ep.setProperty("computingSite", cs);
			ep.setProperty("jobStatus", st);
			ep.setProperty("prodUserName", rec.get(4).getAsString());
			int wfax=rec.get(5).getAsInt();
			ep.setProperty("WithFAX", wfax);
			int wofax=rec.get(6).getAsInt();
			ep.setProperty("WithoutFAX", wofax);
			long bwf=rec.get(7).getAsLong();
			ep.setProperty("bytesWithFAX", bwf);
			long bwof=rec.get(8).getAsLong();
			ep.setProperty("bytesWithoutFAX", bwof);
			int ttc=rec.get(9).getAsInt();
			ep.setProperty("timeToCopy", ttc);
			
			if (!summary.containsKey(cs))
				summary.put(cs, new SumUp());
			SumUp su = summary.get(cs);
			if (st.equals("failed")) {
				su.failures += 1;
				all.failures += 1;
			} else if (st.equals("finished")) {
				su.successes += 1;
				all.successes += 1;
			}
			
			su.fileswith+=wfax;
			su.fileswithout+=wofax;
			su.byteswith+=bwf;
			su.byteswithout+=bwof;
			su.timeToCopy+=ttc;
			
			all.fileswith+=wfax;
			all.fileswithout+=wofax;
			all.byteswith+=bwf;
			all.byteswithout+=bwof;
			all.timeToCopy+=ttc;			
			
			lRes.add(ep);

		}

		datastore.put(lRes);
		log.warning("added " + lRes.size() + " jobs.");
		lRes.clear();

		summary.put("all", all);
		for (Map.Entry<String, SumUp> s : summary.entrySet()) {
			Entity result = new Entity("failoverSummary");
			SumUp v = s.getValue();
			result.setProperty("timestamp", new Date());
			result.setProperty("site", s.getKey());
			result.setProperty("successes", v.successes);
			result.setProperty("failures", v.failures);
			result.setProperty("WithFAX", v.fileswith);
			result.setProperty("WithoutFAX", v.fileswithout);
			result.setProperty("bytesWithFAX", v.byteswith);
			result.setProperty("bytesWithoutFAX", v.byteswithout);
			result.setProperty("timeToCopy", v.timeToCopy);
			// log.warning(result.toString());
			datastore.put(result);
		}
		
	}
}
