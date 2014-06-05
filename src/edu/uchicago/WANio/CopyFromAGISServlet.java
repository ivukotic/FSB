package edu.uchicago.WANio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;

@SuppressWarnings("serial")
public class CopyFromAGISServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(CopyFromAGISServlet.class.getName());

	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONArray readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONArray json = new JSONArray(jsonText);
			return json;
		} finally {
			is.close();
		}
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
//			List<String> ddmendpoints;
			float lat;
			float lon;
			int tier;
		}

		List<si> sis = new ArrayList<si>();


		log.info("get EP info: site name, site address, redirector, redirector address");
		String directURL = "http://atlas-agis-api.cern.ch/request/service/query/get_se_services/?json&state=ACTIVE&flavour=XROOTD";
//		resp.getWriter().println(directURL);
		JSONArray jo = readJsonFromUrl(directURL);
//		resp.getWriter().println(jo.length());

		for (int i = 0; i < jo.length(); ++i) {
			JSONObject rec = jo.getJSONObject(i);

			si s = new si();
			s.name=rec.getString("rc_site");
			s.address = rec.getString("endpoint");
			s.rname = rec.getJSONObject("redirector").getString("name");
			s.raddress = rec.getJSONObject("redirector").getString("endpoint");
			sis.add(s);

//			log.warning("2 added " + s.name);
			
		}
		
		
		
		log.info("get EP info:  lat,long, tier.");
		String SiteInfoURL = "http://atlas-agis-api.cern.ch/request/site/query/list/?json&vo_name=atlas&state=ACTIVE";
//		resp.getWriter().println(SiteInfoURL);
		jo = readJsonFromUrl(SiteInfoURL);
//		resp.getWriter().println(jo.length());

		for (int i = 0; i < jo.length(); ++i) {
			JSONObject rec = jo.getJSONObject(i);
			String name=rec.getString("rc_site");
			for (si s : sis) {
				if (s.name.equals(name)) {
					if (name.equals("GRIF")) s.name=rec.getString("name");
					s.lat = (float) rec.getDouble("latitude");
					s.lon = (float) rec.getDouble("longitude");
					s.tier = rec.getInt("tier_level");

//					log.warning("1 added " + s.name);
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


	}
}
