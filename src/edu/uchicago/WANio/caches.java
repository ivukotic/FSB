package edu.uchicago.WANio;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.Query;

public class caches {


	private static final Logger log = Logger.getLogger(caches.class.getName());
	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	
	public static List<String> lSources = new ArrayList<String>();
	public static List<String> lDestinations = new ArrayList<String>();

	private static Date refresh=new Date();
	
	private static void loadSites() {
		lSources.clear();
		lDestinations.clear();
		Query q = new Query("FAXcost6h").setDistinct(true);
		q.addProjection(new PropertyProjection("source", String.class));
		List<Entity> lRes = datastore.prepare(q).asList(FetchOptions.Builder.withDefaults());
		for (Entity result : lRes) {
			String res = (String) result.getProperty("source");
			if (res != null)
				lSources.add(res);
		}
		q = new Query("FAXcost6h").setDistinct(true);
		q.addProjection(new PropertyProjection("destination", String.class));
		lRes = datastore.prepare(q).asList(FetchOptions.Builder.withDefaults());
		for (Entity result : lRes) {
			String res = (String) result.getProperty("destination");
			if (res != null)
				lDestinations.add(res);
		}

		log.warning("links sources: " + caches.lSources.size() + "  destinations: " + caches.lDestinations.size());
	}
	
	public static void reload(){
		if (new Date().getTime()-refresh.getTime() > 24*3600*1000 || lSources.isEmpty() || lDestinations.isEmpty() ) loadSites();
	}
	
	public static void rewrite(){
		List<Entity> sou=new ArrayList<Entity>();
		for (String s:lSources){
			sou.add(new Entity("source",s));
		}
		datastore.put(sou);
		
		List<Entity> dest=new ArrayList<Entity>();
		for (String d:lDestinations){
			dest.add(new Entity("destination",d));
		}
		datastore.put(dest);
	}
	
}
