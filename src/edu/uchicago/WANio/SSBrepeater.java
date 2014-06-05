package edu.uchicago.WANio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class SSBrepeater extends HttpServlet {
	
	private static final Logger log = Logger.getLogger(SSBrepeater.class.getName());
	
	private Date lastReload=new Date();
	private JSONObject res=new JSONObject() ;
	
	public void init(){
		reload();
	}
	
	
	private void reload(){
		JSONObject SSB=new JSONObject() ;
		String link="http://dashb-atlas-ssb.cern.ch/dashboard/request.py/getplotdata?time=custom&batch=1&columnid=10083";
		String dateFormat = "yyyy-MM-dd";
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, new Locale("en_US"));
		String fDate = sdf.format(new Date());
		String tDate = sdf.format(new Date(new Date().getTime()+86400000));
		link+="&dateFrom="+fDate;
		link+="&dateTo="+tDate;
		try {
			log.warning("loading: "+link);
			SSB=readJsonFromUrl(link);
		} catch (JSONException | IOException e) {
			log.severe("could not reload SSB site info");
			e.printStackTrace();
			return;
		}
		SSB.remove("version");
		SSB.remove("adminRightModifyColumn");
		SSB.remove("vo_info");
		SSB.remove("meta");
		SSB.remove("modifyMetricHistoryData");
		SSB.remove("access");
		for (int i=0;i<SSB.getJSONArray("csvdata").length();i++){
			JSONObject s=SSB.getJSONArray("csvdata").getJSONObject(i);
			s.remove("URL");
			s.remove("Time");
			s.remove("EndTime");
			s.remove("Value");
			s.remove("COLORNAME");
			s.remove("SiteId");
		}
		

		// leaving only the last returned value 
		JSONArray orig=SSB.getJSONArray("csvdata");
		JSONArray modi=new JSONArray();
		
		for (int i=orig.length()-1;i>=0;i--){
			JSONObject s=orig.getJSONObject(i);
			String tc=s.getString("VOName");
//			log.warning("checking: "+tc);
			int found=0;
			for (int j=0;j<modi.length();j++){
				if (modi.getJSONObject(j).getString("VOName").equals(tc) ) {
					found=1;
					break;
					}
			}
			if (found==0) 
				modi.put(s);
		}
		res.put("csvdata", modi);
		
	}
	
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		
		if (new Date().getTime()-lastReload.getTime()>1800*1000) reload();
		resp.setContentType("application/json");
		
		log.info("A new SSB request.");
		resp.getWriter().print(res);

	}
}
