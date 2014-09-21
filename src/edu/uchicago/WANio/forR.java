package edu.uchicago.WANio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class forR extends HttpServlet {
	private static final Logger log = Logger.getLogger(forR.class.getName());

	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.info("something from R - Got a GET...");
		
		if (req.getParameter("preload") != null) {
			List<String> sites=new ArrayList<String>();
			List<String> users=new ArrayList<String>();
			List<String> projects=new ArrayList<String>();
			sites.add("site1");
			sites.add("site2");
			sites.add("site3");
			sites.add("site4");
			users.add("udfgsite1");
			users.add("sitsdfge2");
			users.add("sitesdfg3");
			users.add("sitesdfg4");
			projects.add("projects1");
			projects.add("projects");
			projects.add("projcts");
			projects.add("proje");
			JSONObject o=new JSONObject();
			o.append("sites", new JSONArray(sites));
			o.append("users", new JSONArray(sites));
			o.append("projects", new JSONArray(sites));
			
			resp.getWriter().print(o);
			return;
		}

		if (req.getParameter("selsite") != null && req.getParameter("seluser") != null && req.getParameter("selproject") != null){

			log.warning("collecting data...");
			Random rnd = new Random();
			JSONArray data = new JSONArray();
			for (int i=0;i<1000;i++){
				JSONArray p = new JSONArray();
				p.put(rnd.nextInt(4));
				p.put(rnd.nextInt(4));
				p.put(rnd.nextInt(4));
				p.put(1407862804+i);
				data.put(p);
			}
			resp.getWriter().print(data);
		}
	}
}
