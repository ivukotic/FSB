package edu.uchicago.WANio;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.servlet.http.*;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;

@SuppressWarnings("serial")
public class LocalSetupFAX extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(LocalSetupFAX.class.getName());
	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	private static JSONObject preload() {

		JSONObject json = new JSONObject();
		return json;

	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.warning("LocalSetupFAX Got a POST...");

		resp.setContentType("application/json");

		StringBuilder sb = new StringBuilder();
		BufferedReader br = req.getReader();
		String str;
		while ((str = br.readLine()) != null) {
			sb.append(str);
		}
		log.warning("parameters: " + sb.toString());
		JSONObject pars = null;
		try {
			pars = new JSONObject(sb.toString());
		} catch (Exception e) {
			log.severe("could not parse parameters to JSONObject");
			log.severe(sb.toString());
			log.severe(e.getMessage());
			resp.getWriter().print(new JSONObject());
			return;
		}
		
		if (pars.has("preload")) {
			try {
				JSONObject dists = preload();
				resp.getWriter().print(dists);
				return;
			} catch (JSONException e) {// | IOException e
				log.severe("could not get preload data: " + e.getMessage());
				return;
			}
		}

		Entity result = new Entity("localsetupfax");
		result.setProperty("timestamp", new Date());
		Iterator<?> keys = pars.keys();

        while( keys.hasNext() ){
            String key = (String)keys.next();
            result.setProperty(key, pars.get(key));
        }
        
		log.warning(result.toString());
		datastore.put(result);

	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.warning("LocalSetupFAX Got a GET...");

	}

}
