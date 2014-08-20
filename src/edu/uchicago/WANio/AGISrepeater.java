package edu.uchicago.WANio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
public class AGISrepeater extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(AGISrepeater.class.getName());
	
	private Date lastReload=new Date();
	private JSONArray AGISsites=new JSONArray() ;
	private JSONArray AGISservice=new JSONArray() ;
	
	
	public void init(){
		reload();
	}
	
	private void reload(){
		
		try {
			AGISsites=readJsonFromUrl("http://atlas-agis-api.cern.ch/request/site/query/list/?json");
		} catch (JSONException | IOException e) {
			log.severe("could not reload AGIS site info: "+ e.getMessage());
			return;
		}
		
		for (int i=0;i<AGISsites.length();i++){
			JSONObject s=AGISsites.getJSONObject(i);
			s.remove("ddmendpoints");
			s.remove("fsconf");
			s.remove("presources");
			s.remove("rcsite");
			s.remove("timezone");
			s.remove("last_modified");
			s.remove("psconf");
			s.remove("has_notification");
			s.remove("is_pledged");
			s.remove("infoURL");
			s.remove("emailContact");
			s.remove("email");
			s.remove("state_comment");
		}
		try {
			AGISservice=readJsonFromUrl("http://atlas-agis-api.cern.ch/request/service/query/get_se_services/?json&state=ACTIVE&flavour=XROOTD");
		} catch (JSONException | IOException e) {
			log.severe("could not reload AGIS service info: "+e.getMessage());
		}

		lastReload=new Date();
	}
	
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
		
		if (new Date().getTime()-lastReload.getTime()>1800*1000) reload();
		resp.setContentType("application/json");
		
		String rsite=req.getParameter("rc_site");
		if (rsite!=null){
			log.info("A new service request."+rsite);
			JSONArray res=new JSONArray();
			for (int i=0;i<AGISservice.length();i++){
				if (AGISservice.getJSONObject(i).getString("rc_site").equals(rsite)){
					res.put(AGISservice.getJSONObject(i));
					break;
				}
			}
			resp.getWriter().print(res);
			return;
		}
		
		log.info("A new site request.");
		resp.getWriter().print(AGISsites);

	}
}
