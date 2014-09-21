package edu.uchicago.WANio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class MongoTraces extends HttpServlet {

	private static final Logger log = Logger.getLogger(MongoTraces.class.getName());

	private Date lastReload = new Date();
	private JSONArray nodes = new JSONArray();

	public void init() {
		reload();
	}

	private void reload() {

		try {
			nodes = getJSONArray("http://db.mwt2.org:8080/ips");
		} catch (JSONException e) {// | IOException e
			log.severe("could not reload nodes info: " + e.getMessage());
			return;
		}

		lastReload = new Date();
	}

	private static JSONArray getJSONArray(String surl) {
		try {
			URL url = new URL(surl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestMethod("POST");
			connection.connect();
			log.warning("response:" + connection.getResponseCode());

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line = "";
				String res="";
				while (line != null) {
					line = in.readLine();
					res+=line;
//					log.warning(line);
				}
				in.close();
//				log.warning(res);
				JSONArray json = new JSONArray(res);
				return json;
			} else {
				log.warning("Server returned HTTP error code.");
				return null;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	private static JSONObject getLinkNetwork(String surl, String source, String destination) {
		try {
			URL url = new URL(surl+"?source="+source+"&destination="+destination);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestProperty("Accept", "application/json");
//			connection.setRequestProperty("Content-Type","application/json");
//			connection.setRequestProperty("source", source);
//			connection.setRequestProperty("destination", destination);
			connection.setRequestMethod("POST");
			connection.connect();
			log.warning("response:" + connection.getResponseCode());

			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line = "";
				String res="";
				while (line != null) {
					line = in.readLine();
					res+=line;
//					log.warning(line);
				}
				in.close();
//				log.warning(res);
				JSONObject json = new JSONObject(res);
				return json;
			} else {
				log.warning("Server returned HTTP error code.");
				return null;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.warning("MongoTraces Got a POST...");

	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.warning("MongoTraces Got a GET...");

		resp.setContentType("application/json");
		
		if (req.getParameter("source")!=null &&  req.getParameter("destination")!=null){
			try {
				JSONObject network = getLinkNetwork("http://db.mwt2.org:8080/network",req.getParameter("source"), req.getParameter("destination"));
				resp.getWriter().print(network);
			} catch (JSONException e) {// | IOException e
				log.severe("could not reload nodes info: " + e.getMessage());
				return;
			}
			return;
		}
		
		if (new Date().getTime() - lastReload.getTime() > 1800 * 1000)
			reload();

		// String rsite=req.getParameter("rc_site");
		// if (rsite!=null){
		// log.info("A new service request."+rsite);
		// JSONArray res=new JSONArray();
		// for (int i=0;i<AGISservice.length();i++){
		// if (AGISservice.getJSONObject(i).getString("rc_site").equals(rsite)){
		// res.put(AGISservice.getJSONObject(i));
		// break;
		// }
		// }
		// resp.getWriter().print(res);
		// return;
		// }

//		log.info("A new site request.");
		resp.getWriter().print(nodes);

	}
}
