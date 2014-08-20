package edu.uchicago.WANio;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SuppressWarnings("serial")
public class CopyFromAGISServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(CopyFromAGISServlet.class.getName());

	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	private JsonElement getJsonFromUrl(String theURL) {
		log.info("getting data from:" + theURL);
		URL url;
		HttpURLConnection request;
		JsonParser jp = new JsonParser();
		JsonElement root = null;
		try {
			url = new URL(theURL);
			request = (HttpURLConnection) url.openConnection();
			request.connect();
			root = jp.parse(new InputStreamReader((InputStream) request.getContent()));
			request.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return root;
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.warning("CopyFromAGISServlet Got a POST...");

	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.info("CopyFromAGISServlet Got a GET...");

		class si {
			String name;
			String rname;
			String address;
			String raddress;
			// List<String> ddmendpoints;
			float lat;
			float lon;
			int tier;
		}

		List<si> sis = new ArrayList<si>();

		log.info("get EP info: site name, site address, redirector, redirector address");
		String theURL = "http://atlas-agis-api.cern.ch/request/service/query/get_se_services/?json&state=ACTIVE&flavour=XROOTD";
		JsonArray jo = getJsonFromUrl(theURL).getAsJsonArray();

		for (int i = 0; i < jo.size(); ++i) {
			JsonObject rec = jo.get(i).getAsJsonObject();

			si s = new si();
			s.name = rec.get("rc_site").getAsString();
			s.address = rec.get("endpoint").getAsString();
			s.rname = rec.getAsJsonObject("redirector").get("name").getAsString();
			s.raddress = rec.getAsJsonObject("redirector").get("endpoint").getAsString();
			sis.add(s);

			// resp.getWriter().println("added " + s.name+"\n");
		}

		log.info("get EP info:  lat,long, tier.");
		theURL = "http://atlas-agis-api.cern.ch/request/site/query/list/?json&vo_name=atlas&state=ACTIVE";
		jo = getJsonFromUrl(theURL).getAsJsonArray();

		// resp.getWriter().println(jo.size()+"\n");

		for (int i = 0; i < jo.size(); ++i) {
			JsonObject rec = jo.get(i).getAsJsonObject();
			String name = rec.get("rc_site").getAsString();
			for (si s : sis) {
				if (s.name.equals(name)) {
					if (name.equals("GRIF"))
						s.name = rec.get("name").getAsString();
					s.lat = (float) rec.get("latitude").getAsDouble();
					s.lon = (float) rec.get("longitude").getAsDouble();
					s.tier = rec.get("tier_level").getAsInt();

					// resp.getWriter().println("info added " + s.name+"\n");
					break;
				}
			}
		}

		Date currTime = new Date();

		for (si s : sis) {
			Entity ep = new Entity("endpoint", s.name);
			ep.setProperty("timestamp", currTime);
			ep.setProperty("address", s.address);
			ep.setProperty("redirector", s.rname);
			ep.setProperty("redirectorAddress", s.raddress);
			ep.setProperty("latitude", s.lat);
			ep.setProperty("longitude", s.lon);
			ep.setProperty("tier_level", s.tier);
			datastore.put(ep);
		}

		// Getting redirectors
		theURL = "http://atlas-agis-api.cern.ch/request/service/query/get_redirector_services/?json&state=ACTIVE";
		jo = getJsonFromUrl(theURL).getAsJsonArray();

		// resp.getWriter().println(jo.size());

		for (int i = 0; i < jo.size(); ++i) {
			JsonObject rec = jo.get(i).getAsJsonObject();

			String Rname = rec.get("name").getAsString();
			String Raddress = rec.get("endpoint").getAsString();

			Entity ep = new Entity("redirector", Rname);
			ep.setProperty("timestamp", currTime);
			ep.setProperty("address", Raddress);
			datastore.put(ep);

			// resp.getWriter().println("redirector" + Rname+"\n");
		}

	}
}
