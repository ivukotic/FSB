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
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;

@SuppressWarnings("serial")
public class WANcostDeleteServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(WANcostDeleteServlet.class.getName());
	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.info("WANcostDeleteServlet Got a GET...");

//		if(false){
//			Query q = new Query("FAXcost30min").setKeysOnly();
//		
//			List<Entity> lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(100));
//			for (Entity result : lRes) {
//				datastore.delete(result.getKey());
//			}
//
//			log.warning("deleted old ones.");
//		}
		

		

		Date cutoffTime = new Date(new Date().getTime() - 7 * 86400l * 1000);
		Filter f = new Query.FilterPredicate("timestamp", FilterOperator.LESS_THAN, cutoffTime);
		Query q = new Query("FAXcost").setFilter(f).setKeysOnly();
		List<Entity> lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(20000).chunkSize(20000));
		List<Key> kTOd= new ArrayList<Key>();
		for (Entity result : lRes) {
			kTOd.add(result.getKey());
		}
		log.warning("deleting "+kTOd.size()+" rows from FAXcost");
		datastore.delete(kTOd);
		
		
		cutoffTime = new Date(new Date().getTime() - 15 * 86400l * 1000);
		f = new Query.FilterPredicate("timestamp", FilterOperator.LESS_THAN, cutoffTime);
		q = new Query("FAXcost30min").setFilter(f).setKeysOnly();
		lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(5000).chunkSize(5000));
		kTOd.clear();
		for (Entity result : lRes) {
			kTOd.add(result.getKey());
		}
		log.warning("deleting "+kTOd.size()+" rows from FAXcost30min");
		datastore.delete(kTOd);
		
		
		cutoffTime = new Date(new Date().getTime() - 30 * 86400l * 1000);
		f = new Query.FilterPredicate("timestamp", FilterOperator.LESS_THAN, cutoffTime);
		q = new Query("FAXcost1h").setFilter(f).setKeysOnly();
		lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(1000).chunkSize(1000));
		kTOd.clear();
		for (Entity result : lRes) {
			kTOd.add(result.getKey());
		}
		log.warning("deleting "+kTOd.size()+" rows from FAXcost1h");
		datastore.delete(kTOd);
		
		
		cutoffTime = new Date(new Date().getTime() - 60 * 86400l * 1000);
		f = new Query.FilterPredicate("timestamp", FilterOperator.LESS_THAN, cutoffTime);
		q = new Query("FAXcost3h").setFilter(f).setKeysOnly();
		lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(1000).chunkSize(1000));
		kTOd.clear();
		for (Entity result : lRes) {
			kTOd.add(result.getKey());
		}
		log.warning("deleting "+kTOd.size()+" rows from FAXcost3h");
		datastore.delete(kTOd);
		
		
		cutoffTime = new Date(new Date().getTime() - 120 * 86400l * 1000);
		f = new Query.FilterPredicate("timestamp", FilterOperator.LESS_THAN, cutoffTime);
		q = new Query("FAXcost6h").setFilter(f).setKeysOnly();
		lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(1000));
		kTOd.clear();
		for (Entity result : lRes) {
			kTOd.add(result.getKey());
		}
		log.warning("deleting "+kTOd.size()+" rows from FAXcost6h");
		datastore.delete(kTOd);

		
		cutoffTime = new Date(new Date().getTime() - 240 * 86400l * 1000);
		f = new Query.FilterPredicate("timestamp", FilterOperator.LESS_THAN, cutoffTime);
		q = new Query("FAXcost12h").setFilter(f).setKeysOnly();
		lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(1000));
		kTOd.clear();
		for (Entity result : lRes) {
			kTOd.add(result.getKey());
		}
		log.warning("deleting "+kTOd.size()+" rows from FAXcost12h");
		datastore.delete(kTOd);
		
	}
}
