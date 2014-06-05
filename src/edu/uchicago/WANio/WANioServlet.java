package edu.uchicago.WANio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;


@SuppressWarnings("serial")
public class WANioServlet extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(WANioServlet.class.getName());
	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	private List<Entity> lEPs, lREs;
	private long lastEPUpdate=0;
	private long lastREUpdate=0;
	
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	private void updateEndpoints(){
		// don't refresh if done less then 5 min ago
		if (new Date().getTime()-lastEPUpdate < 5*60*1000) return;
		lastEPUpdate=new Date().getTime();
		// not using any endpoints last updated more then 10 hours ago. (some could change name or got disabled.)
		Date currTime = new Date(new Date().getTime() - 10 * 3600 * 1000);
		Filter onRecentEPs = new Query.FilterPredicate("timestamp", FilterOperator.GREATER_THAN, currTime);
		Query q = new Query("endpoint").setFilter(onRecentEPs);
		PreparedQuery pq = datastore.prepare(q);
		lEPs=pq.asList(FetchOptions.Builder.withDefaults());
	}

	private void updateRedirectors(){
		// don't refresh if done less then 5 min ago
		if (new Date().getTime()-lastREUpdate < 5*60*1000) return;
		lastREUpdate=new Date().getTime();
		// not using any endpoints last updated more then 10 hours ago. (some could change name or got disabled.)
		Date currTime = new Date(new Date().getTime() - 10 * 3600 * 1000);
		Filter onRecentREs = new Query.FilterPredicate("timestamp", FilterOperator.GREATER_THAN, currTime);
		Query q = new Query("redirector").setFilter(onRecentREs);
		PreparedQuery pq = datastore.prepare(q);
		lREs=pq.asList(FetchOptions.Builder.withDefaults());
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
//		log.warning("Got a POST...");
		
		String endpointName = req.getParameter("epName");
		if (endpointName!=null){
			int endpointStatus = Integer.parseInt(req.getParameter("epStatus"));
			updateEndpoints();
			
			for (Entity endpoint:lEPs){
				if (endpointName.equals(endpoint.getKey().getName() )){
				
					Entity result = new Entity("result", endpoint.getKey());

					Date currTime = new Date();
					result.setProperty("timestamp", currTime);
					if ((endpointStatus & 1) == 1)
						result.setProperty("direct", true);
					else
						result.setProperty("direct", false);
					if ((endpointStatus & 2) == 2)
						result.setProperty("upstream", true);
					else
						result.setProperty("upstream", false);
					if ((endpointStatus & 4) == 4)
						result.setProperty("downstream", true);
					else
						result.setProperty("downstream", false);
					if ((endpointStatus & 8) == 8)
						result.setProperty("x509", true);
					else
						result.setProperty("x509", false);
					
					datastore.put(result);

					// log.info(endpointName + '\n' + endpointStatus + "\n" + endpointAddress + '\n');
				}
			}
		}		
		
		String redirectorName = req.getParameter("reName");
		if (redirectorName!=null){
			int redirectorStatus = Integer.parseInt(req.getParameter("reStatus"));
			updateRedirectors();
			
			for (Entity redirector:lREs){
				if (endpointName.equals(redirector.getKey().getName() )){
				
					Entity result = new Entity("result_redirector", redirector.getKey());

					Date currTime = new Date();
					result.setProperty("timestamp", currTime);
					if ((redirectorStatus & 1) == 1)
						result.setProperty("can not check", true);
					else
						result.setProperty("can not check", false);
					if ((redirectorStatus & 4) == 4)
						result.setProperty("upstream", true);
					else
						result.setProperty("upstream", false);
					if ((redirectorStatus & 2) == 2)
						result.setProperty("downstream", true);
					else
						result.setProperty("downstream", false);
					
					datastore.put(result);

					// log.info(endpointName + '\n' + endpointStatus + "\n" + endpointAddress + '\n');
				}
			}
		}		


	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.warning("Got a GET...");

		resp.setContentType("text/plain");
		
		if (req.getParameter("map")!=null){
			log.info("data for the map...");
			String res="";
			for(Entity ep:lEPs){
				String na=ep.getKey().getName();
				res+=na+"\t"+ep.getProperty("latitude")+"\t"+ep.getProperty("longitude")+"\n";
			}
			resp.getWriter().println(res);
			log.info(res);
			return;
		}

		
		updateEndpoints();
		int sites=lEPs.size(); 
		
		String res="";
		Query q = new Query("result").addSort("timestamp", SortDirection.DESCENDING);
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> lRes=pq.asList(FetchOptions.Builder.withLimit(sites));
		
		for(Entity result:lRes){
			String na=result.getParent().getName();
			res+=na+"\t"+result.getProperty("direct")+"\t"+result.getProperty("upstream")+"\t"+result.getProperty("downstream")+"\t"+result.getProperty("x509")+"\n";
		}
		

		resp.getWriter().println(res);
		
		
		

		
		// Properties p = System.getProperties();
		// p.list(resp.getWriter());
		//
		// SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd");
		// dateFormatGmt.setTimeZone(TimeZone.getTimeZone("UCT"));
		// String today = dateFormatGmt.format(new Date());
		// String tomorrow = dateFormatGmt.format(new Date(new Date().getTime()
		// + 24 * 3600 * 1000));
		//
		// String directURL =
		// "http://dashb-atlas-ssb.cern.ch/dashboard/request.py/getplotdata?time=custom&dateFrom="
		// + today + "&dateTo=" + tomorrow
		// + "&batch=1&columnid=10083";
		// resp.getWriter().println(directURL);
		// JSONArray jo=readJsonFromUrl(directURL).getJSONArray("csvdata");
		// resp.getWriter().println(jo.length());
		// for (int i = 0; i < jo.length(); ++i) {
		// JSONObject rec = jo.getJSONObject(i);
		// resp.getWriter().println(rec.toString());
		// }

		// UserService userService = UserServiceFactory.getUserService();
		// User currentUser = userService.getCurrentUser();
		//
		// if (currentUser != null) {
		// resp.getWriter().println("Hello, " + currentUser.getNickname());
		// }
	}
}
