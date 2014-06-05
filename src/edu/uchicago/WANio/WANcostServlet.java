package edu.uchicago.WANio;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

@SuppressWarnings("serial")
public class WANcostServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(WANcostServlet.class.getName());
	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	// private List<Entity> lEPs;
	// private long lastEPUpdate=0;
	//
	// private static String readAll(Reader rd) throws IOException {
	// StringBuilder sb = new StringBuilder();
	// int cp;
	// while ((cp = rd.read()) != -1) {
	// sb.append((char) cp);
	// }
	// return sb.toString();
	// }
	//
	// public static JSONObject readJsonFromUrl(String url) throws IOException,
	// JSONException {
	// InputStream is = new URL(url).openStream();
	// try {
	// BufferedReader rd = new BufferedReader(new InputStreamReader(is,
	// Charset.forName("UTF-8")));
	// String jsonText = readAll(rd);
	// JSONObject json = new JSONObject(jsonText);
	// return json;
	// } finally {
	// is.close();
	// }
	// }
	

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		String source = req.getParameter("source");
		String destination = req.getParameter("destination");
		Float rate = Float.parseFloat(req.getParameter("rate"));

		Date currTime = new Date();
		Entity result = new Entity("FAXcost");
		result.setProperty("timestamp", currTime);
		result.setProperty("source", source);
		result.setProperty("destination", destination);
		result.setProperty("rate", rate);

		datastore.put(result);
		// log.info(source + "->" + destination + "\t" + rate);
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.warning("WANcostServlet Got a GET...");
		
		caches.reload();
		
		resp.setContentType("text/plain");

		if (req.getParameter("preload") != null) {
			log.info("data for the source and destination...");
			String res = "";

			for (String site : caches.lSources) {
				res += "source\t" + site + "\n";
			}
			for (String site : caches.lDestinations) {
				res += "destination\t" + site + "\n";
			}
			resp.getWriter().println(res);
			// log.debug(res);
			return;
		}

		if (req.getParameter("costmatrix") != null) {
			log.warning("data for costmatrix...");
			class link {
				float measurements;
				float sum;

				float getAvg() {
					if (measurements == 0)
						return -1f;
					return sum / measurements;
				}
			}

			link[][] linkArr = new link[caches.lSources.size()][caches.lDestinations.size()];

			for (int i = 0; i < caches.lSources.size(); i++)
				for (int j = 0; j < caches.lDestinations.size(); j++)
					linkArr[i][j] = new link();

			log.warning("links..." + caches.lSources.size() + " " + caches.lDestinations.size());
			Integer interval = Integer.parseInt(req.getParameter("costmatrix"));
			Date currTime = new Date(new Date().getTime() - interval * 3600l * 1000);
			Filter f = new Query.FilterPredicate("timestamp", FilterOperator.GREATER_THAN, currTime);
			Query q = new Query("FAXcost1h").setFilter(f);

			List<Entity> lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(50000).chunkSize(10000));

			log.warning("lRes obtained... "+lRes.size());
			for (Entity result : lRes) {
				String s = (String) result.getProperty("source");
				String d = (String) result.getProperty("destination");
				int i = caches.lSources.indexOf(s);
				int j = caches.lDestinations.indexOf(d);
				if(i<0 || j<0) {
					log.warning(s+" "+d+" "+i+" "+j);
					continue; 
				}
				linkArr[i][j].measurements++;
				if (result.getProperty("rate")==null) 
					log.severe("rate is null"); 
				else
					linkArr[i][j].sum += ((Double)result.getProperty("rate")).floatValue();
			}

			String res = "size"+'\t'+caches.lSources.size() + "\t" + caches.lDestinations.size()+"\n";
			for (int i = 0; i < caches.lSources.size(); i++)
				for (int j = 0; j < caches.lDestinations.size(); j++)
					res += caches.lSources.get(i) + "\t" + caches.lDestinations.get(j) + "\t" + new DecimalFormat("#.##").format(linkArr[i][j].getAvg()) + "\n";

			resp.getWriter().println(res);
			log.info(res);
			return;
		}

		if(req.getParameter("central") != null) {
			log.warning("data for map...");
			String asa=req.getParameter("as");
			String oth="source";
			if(asa.equals("source"))
				oth="destination";
			
			String res = "";
			String central = req.getParameter("central");
			Filter f1 = new FilterPredicate(asa, FilterOperator.EQUAL, central);

			Query q = new Query("FAXcost24h").setFilter(f1);//.addSort("timestamp", SortDirection.DESCENDING);
			List<Entity> lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(80));
			log.warning("results found:" + lRes.size());
			Map<String, Double> mySortedMap = new TreeMap<String, Double>();
			for (Entity result : lRes) {
				mySortedMap.put( result.getProperty(oth).toString(), (Double) result.getProperty("rate"));
			}
			for (Map.Entry<String, Double> entry : mySortedMap.entrySet()) {
				res += entry.getKey() + "\t" + new DecimalFormat("#.##").format(entry.getValue()) + "\n";
			}
			resp.getWriter().println(res);
			return;
		}
		
		
		log.info("data for plot...");

		String source = req.getParameter("source");
		String destination = req.getParameter("destination");
		String binning = req.getParameter("binning");
		String res = "";
		Filter f1 = new FilterPredicate("source", FilterOperator.EQUAL, source);
		Filter f2 = new FilterPredicate("destination", FilterOperator.EQUAL, destination);
		Filter cF = CompositeFilterOperator.and(f1, f2);

		Query q = new Query(binning).setFilter(cF);
		List<Entity> lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(2000).chunkSize(2000));
		log.warning("results found:" + lRes.size());
		Map<Long, Double> mySortedMap = new TreeMap<Long, Double>();
		for (Entity result : lRes) {
			mySortedMap.put(((Date) result.getProperty("timestamp")).getTime(), (Double) result.getProperty("rate"));
		}
		for (Map.Entry<Long, Double> entry : mySortedMap.entrySet()) {
			res += entry.getKey() + "\t" + new DecimalFormat("#.##").format(entry.getValue()) + "\n";
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
