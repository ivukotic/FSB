package edu.uchicago.WANio;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

@SuppressWarnings("serial")
public class WANcostServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(WANcostServlet.class.getName());
	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	private List<Entity> lCost = new ArrayList<Entity>();
	private Date lastPush = new Date();

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		Date currTime = new Date();
		Entity result = new Entity("FAXcost");
		result.setProperty("timestamp", currTime);
		result.setProperty("source", req.getParameter("source"));
		result.setProperty("destination", req.getParameter("destination"));
		result.setUnindexedProperty("rate", Float.parseFloat(req.getParameter("rate")));
		lCost.add(result);

		if (currTime.getTime() - lastPush.getTime() > 60000) { // every minute
			lastPush = new Date();
			datastore.put(lCost);
			log.warning("added " + lCost.size() + " rows.");
			lCost.clear();
		}
		// log.info(source + "->" + destination + "\t" + rate);
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.warning("WANcostServlet Got a GET...");

		caches.reload();

		resp.setContentType("text/plain");

		if (req.getParameter("preload") != null) {
			log.info("data for the source and destination...");
			JSONObject res = new JSONObject();
			res.put("sources", caches.lSources);
			res.put("destinations", caches.lDestinations);
			resp.getWriter().print(res);
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

			link[][] linkArr = new link[caches.lDestinations.size()][caches.lSources.size()];

			for (int d = 0; d < caches.lDestinations.size(); d++)
				for (int s = 0; s < caches.lSources.size(); s++)
					linkArr[d][s] = new link();

			log.warning("links..." + caches.lSources.size() + " " + caches.lDestinations.size());
			Integer interval = Integer.parseInt(req.getParameter("costmatrix"));
			Date currTime = new Date(new Date().getTime() - interval * 3600l * 1000);
			Filter f = new Query.FilterPredicate("timestamp", FilterOperator.GREATER_THAN, currTime);
			Query q = new Query("FAXcost1h").setFilter(f);

			List<Entity> lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(50000).chunkSize(10000));

			log.warning("lRes obtained... " + lRes.size());
			for (Entity result : lRes) {
				String so = (String) result.getProperty("source");
				String de = (String) result.getProperty("destination");
				int s = caches.lSources.indexOf(so);
				int d = caches.lDestinations.indexOf(de);
				if (s < 0 || d < 0) {
					log.warning(so + " " + de + " " + s + " " + d);
					continue;
				}
				linkArr[d][s].measurements++;
				if (result.getProperty("rate") == null)
					log.severe("rate is null");
				else
					linkArr[d][s].sum += ((Double) result.getProperty("rate")).floatValue();
			}

			JSONObject res = new JSONObject();
			JSONArray links = new JSONArray();
			res.put("sources", caches.lSources);
			res.put("destinations", caches.lDestinations);

			for (int d = 0; d < caches.lDestinations.size(); d++) {
				for (int s = 0; s < caches.lSources.size(); s++) {
					links.put(new Float(new DecimalFormat("#.##").format(linkArr[d][s].getAvg())));
				}
			}
			res.put("links", links);
			resp.getWriter().print(res);
			return;
		}

		if (req.getParameter("central") != null) {
			log.warning("data for map...");
			String asa = req.getParameter("as");
			String oth = "source";
			if (asa.equals("source"))
				oth = "destination";

			String res = "";
			String central = req.getParameter("central");
			Filter f1 = new FilterPredicate(asa, FilterOperator.EQUAL, central);

			Query q = new Query("FAXcost24h").setFilter(f1);// .addSort("timestamp",
															// SortDirection.DESCENDING);
			List<Entity> lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(80));
			log.warning("results found:" + lRes.size());
			Map<String, Double> mySortedMap = new TreeMap<String, Double>();
			for (Entity result : lRes) {
				mySortedMap.put(result.getProperty(oth).toString(), (Double) result.getProperty("rate"));
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

	}
}
