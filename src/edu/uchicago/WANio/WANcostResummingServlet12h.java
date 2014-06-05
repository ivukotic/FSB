package edu.uchicago.WANio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;

@SuppressWarnings("serial")
public class WANcostResummingServlet12h extends HttpServlet {

	private static final Logger log = Logger.getLogger(WANcostResummingServlet12h.class.getName());
	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	

	class link {
		float measurements;
		float sum;

		float getAvg() {
			if (measurements == 0)
				return -1f;
			return sum / measurements;
		}
	}
	

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.info("WANcostResummingServlet12h Got a GET...");
		
		caches.reload();
		
		link[][] linkArr = new link[caches.lSources.size()][caches.lDestinations.size()];

		for (int i = 0; i < caches.lSources.size(); i++)
			for (int j = 0; j < caches.lDestinations.size(); j++)
				linkArr[i][j] = new link();
		
		
		// load data younger than 12 hours
		Date currTime = new Date();
		Date cutoffTime = new Date(currTime.getTime() -  12 * 3600l * 1000);
		
		Filter f = new Query.FilterPredicate("timestamp", FilterOperator.GREATER_THAN, cutoffTime);
		Query q = new Query("FAXcost6h").setFilter(f);
		
		String res="";
		List<Entity> lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(10000).chunkSize(3000));
		res+="results in last 12 h: "+ lRes.size()+"\n";
		log.warning("results in last 3h : "+ lRes.size());
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

		log.info("putting new data");
		List<Entity> lIns =new ArrayList<Entity>();
		for (int i = 0; i < caches.lSources.size(); i++){
			for (int j = 0; j < caches.lDestinations.size(); j++){
				if (linkArr[i][j].getAvg()<0) continue;
				Entity result = new Entity("FAXcost12h");
				result.setProperty("timestamp", currTime);
				result.setProperty("source", caches.lSources.get(i));
				result.setProperty("destination", caches.lDestinations.get(j));
				result.setProperty("rate", linkArr[i][j].getAvg());
				lIns.add(result);
			}
		}
		datastore.put(lIns);
		resp.getWriter().println(res);

	}
}
