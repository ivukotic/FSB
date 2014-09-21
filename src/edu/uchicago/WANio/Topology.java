package edu.uchicago.WANio;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;

@SuppressWarnings("serial")
public class Topology extends HttpServlet {
	private static final Logger log = Logger.getLogger(Topology.class.getName());

	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.warning("Topology Got a POST...");

	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.info("Topology Got a GET...");

		resp.setContentType("application/json");
		JSONObject data = new JSONObject();
		JSONArray nodes = new JSONArray();
		JSONArray edges = new JSONArray();
		class node{
			String name;
			String address;
			public  node(String name, String address){
				this.name=name;
				this.address=address;
			}
		}
		HashMap<Integer,node> mNodes=new HashMap<Integer, node>();
		
		int idCounter=0;
		Query q = new Query("endpoint");
		List<Entity> lNodes = datastore.prepare(q).asList(FetchOptions.Builder.withDefaults());
		for (Entity result : lNodes) {
			String name=result.getKey().getName();
			String red = (String) result.getProperty("redirector");
			mNodes.put(idCounter, new node(name,red));
			JSONObject n=new JSONObject();
			n.put("id", idCounter);
			n.put("label", name);
			nodes.put(n);
			idCounter++;
		}
		q = new Query("redirector");
		List<Entity> lRed = datastore.prepare(q).asList(FetchOptions.Builder.withDefaults());
		for (Entity result : lRed) {
			String name=result.getKey().getName();
			String red="XROOTD_atlas-xrd-eu";
			if (name.equals("XROOTD-atlas-xrd-central") || name.equals("XROOTD-atlas-xrd-east") || name.equals("XROOTD-atlas-xrd-west")) red="XROOTD-atlas-xrd-us";
			if (name.equals("XROOTD-atlas-xrd-asia-tw")) red="XROOTD-atlas-xrd-us";
			if (name.equals("XROOTD_atlas-xrd-eu")) red="XROOTD-atlas-xrd-us";
			log.warning(name+"  "+red);
			mNodes.put(idCounter, new node(name,red));
			JSONObject n=new JSONObject();
			n.put("id", idCounter);
			n.put("label", name);
			n.put("shape","box");
			n.put("color", "black");
			n.put("fontColor", "white");
			n.put("fontSize", 20);
			n.put("mass", 2);
			nodes.put(n);
			idCounter++;
		}
		
		
		for (Entry<Integer,node> e:mNodes.entrySet()){
			Integer found=-1;

			for (Entry<Integer,node> j:mNodes.entrySet()){
				if (e.getValue().address.equals(j.getValue().name) ){
					found=j.getKey();
					break;
				}
			}
			JSONObject edge=new JSONObject();
			edge.put("from", e.getKey());
			edge.put("to", found );
			edges.put(edge);
		}
		
		data.put("nodes", nodes);
		data.put("edges", edges);

		resp.getWriter().print(data);

	}
}
