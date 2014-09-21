package edu.uchicago.WANio;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.google.appengine.api.datastore.PropertyProjection;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.gson.Gson;

@SuppressWarnings("serial")
public class CopyOverflow extends HttpServlet {
	private static final Logger log = Logger.getLogger(CopyOverflow.class.getName());

	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	private List<Entity> lJobs = new ArrayList<Entity>();

	private int secondsFromDuration(String d) {
		SimpleDateFormat stf = new SimpleDateFormat("hh:mm:ss");
		Date date=null;
		try {
			String dt=d.substring(d.length()-7);
			date = stf.parse(dt);
			if (d.length()>9) {
				int days = Integer.parseInt(d.substring(0, 1));
				date.setTime(date.getTime() + days * 24 * 3600 * 1000);
			} 
		} catch (ParseException e) {
			log.severe("error in parsing duration: "+d);
			log.severe(e.getMessage());
			return 0;
		}
		int res = Math.round(date.getTime() / 1000);
		return res;
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.info("CopyOverflow Servlet Got a POST...");
		StringBuilder sb = new StringBuilder();
		BufferedReader br = req.getReader();
		String str;
		while ((str = br.readLine()) != null) {
			sb.append(str);
		}

		JSONArray jobs = null;
		try {
			jobs = new JSONArray(sb.toString());
		} catch (Exception e) {
			log.severe("could not parse to JSONArray");
			log.severe(sb.toString());
			log.severe(e.getMessage());
		}

		class SumUp {
			int successes = 0;
			int failures = 0;
			long waittimes = 0;
			long cputimes = 0;
			long durations = 0;
		}

		Map<String, SumUp> summary = new HashMap<String, SumUp>();
		SumUp all = new SumUp();
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss+00:00");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
		for (int i = 0; i < jobs.length(); i++) {
			JSONArray job = jobs.getJSONArray(i);
			Entity result = new Entity("overflowJobs", job.getLong(0));
			String js = job.getString(2);
			if (!(js.equals("finished") || js.equals("failed")))
				continue;
			String cs = job.getString(4);
			if (!summary.containsKey(cs))
				summary.put(cs, new SumUp());
			SumUp su = summary.get(cs);
			if (js.equals("failed")) {
				su.failures += 1;
				all.failures += 1;
			} else if (js.equals("finished")) {
				su.successes += 1;
				all.successes += 1;
			}

			result.setProperty("jeditaskid", job.getLong(1));
			result.setProperty("jobstatus", js);
			result.setProperty("currentpriority", job.getInt(3));
			result.setProperty("computingsite", cs);
			result.setProperty("produsername", job.getString(5));

			Date date = null;
			try {
				date = sdf.parse(job.getString(6));
				result.setProperty("creationtime", date);
			} catch (ParseException e) {
				log.severe("error in parsing creationtime." + job.getString(6));
				log.severe(e.getMessage());
			}

			int wt = secondsFromDuration(job.getString(7));
			su.waittimes += wt;
			all.waittimes += wt;
			result.setProperty("waittime", wt);

			try {
				date = sdf.parse(job.getString(8));
				result.setProperty("starttime", date);
			} catch (ParseException e) {
				log.severe("error in parsing the starttime date." + job.getString(8));
				log.severe(e.getMessage());
			}

			int du = secondsFromDuration(job.getString(9));
			su.durations += du;
			all.durations += du;
			result.setProperty("duration", du);

			try {
				int cp = job.getInt(10);
				su.cputimes += cp;
				all.cputimes += cp;
				result.setProperty("cpuconsumptiontime", cp);
			} catch (Exception e) {
				log.severe("error in getting cpuconsumptiontime." + job.getInt(10));
				log.severe(e.getMessage());
			}

			lJobs.add(result);
		}
		log.warning(jobs.toString());
		datastore.put(lJobs);
		log.warning("added " + lJobs.size() + " jobs.");
		if (lJobs.size() == 0)
			return;
		lJobs.clear();

		summary.put("all", all);
		for (Map.Entry<String, SumUp> s : summary.entrySet()) {
			Entity result = new Entity("overflowSummary");
			SumUp v = s.getValue();
			result.setProperty("timestamp", new Date());
			result.setProperty("site", s.getKey());
			result.setProperty("successes", v.successes);
			result.setProperty("failures", v.failures);
			result.setProperty("AvgCPUtime", Math.round(v.cputimes / (v.successes + v.failures)));
			result.setProperty("AvgDuration", Math.round(v.durations / (v.successes + v.failures)));
			result.setProperty("AvgWaiting", Math.round(v.waittimes / (v.successes + v.failures)));
			// log.warning(result.toString());
			datastore.put(result);
		}

	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.warning("CopyOverflow Servlet Got a GET...");

		if (req.getParameter("preload") != null) {
			List<String> lSites = new ArrayList<String>();
			Query q = new Query("overflowSummary").setDistinct(true);
			q.addProjection(new PropertyProjection("site", String.class));
			List<Entity> lRes = datastore.prepare(q).asList(FetchOptions.Builder.withDefaults());
			for (Entity result : lRes) {
				String res = (String) result.getProperty("site");
				if (res != null)
					lSites.add(res);
			}
			Gson gson = new Gson();
			resp.getWriter().println(gson.toJson(lSites));
			return;
		}

		if (req.getParameter("overflowtime") == null)
			return;
		if (req.getParameter("overflowplot") == null)
			return;
		if (req.getParameter("overflowdest") == null)
			return;

		log.warning("collecting data...");
		Filter f1 = new FilterPredicate("site", FilterOperator.EQUAL, req.getParameter("overflowdest"));
		Integer interval = Integer.parseInt(req.getParameter("overflowtime"));
		Date currTime = new Date(new Date().getTime() - interval * 3600l * 1000);
		Filter f2 = new FilterPredicate("timestamp", FilterOperator.GREATER_THAN, currTime);
		Filter cF = CompositeFilterOperator.and(f1, f2);

		Query q = new Query("overflowSummary").setFilter(cF);
		List<Entity> lRes = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(1000));
		log.warning("lRes obtained... " + lRes.size());

		JSONObject res = new JSONObject();
		JSONArray plot = new JSONArray();

		if (req.getParameter("overflowplot").equals("jobs")) {
			JSONObject succ = new JSONObject();
			JSONObject fail = new JSONObject();
			JSONArray dsucc = new JSONArray();
			JSONArray dfail = new JSONArray();
			succ.put("name", "success");
			fail.put("name", "failure");
			for (Entity result : lRes) {
				JSONArray su = new JSONArray();
				JSONArray fa = new JSONArray();
				su.put(((Date) result.getProperty("timestamp")).getTime());
				su.put((Long) result.getProperty("successes"));
				fa.put(((Date) result.getProperty("timestamp")).getTime());
				fa.put((Long) result.getProperty("failures"));
				dsucc.put(su);
				dfail.put(fa);
			}
			succ.put("data", dsucc);
			fail.put("data", dfail);
			plot.put(succ);
			plot.put(fail);
		}

		if (req.getParameter("overflowplot").equals("Avg. wait time")) {
			JSONObject ser1 = new JSONObject();
			ser1.put("name", "Avg. wait time");
			JSONArray data = new JSONArray();
			for (Entity result : lRes) {
				JSONArray point = new JSONArray();
				point.put(((Date) result.getProperty("timestamp")).getTime());
				point.put((Long) result.getProperty("AvgWaiting"));
				data.put(point);
			}
			ser1.put("data", data);
			plot.put(ser1);
		}
		if (req.getParameter("overflowplot").equals("Avg. duration")) {
			JSONObject ser1 = new JSONObject();
			ser1.put("name", "Avg. job duration");
			JSONArray data = new JSONArray();
			for (Entity result : lRes) {
				JSONArray point = new JSONArray();
				point.put(((Date) result.getProperty("timestamp")).getTime());
				point.put((Long) result.getProperty("AvgDuration"));
				data.put(point);
			}
			ser1.put("data", data);
			plot.put(ser1);
		}
		if (req.getParameter("overflowplot").equals("Avg. CPU time")) {
			JSONObject ser1 = new JSONObject();
			ser1.put("name", "Avg. CPU time");
			JSONArray data = new JSONArray();
			for (Entity result : lRes) {
				JSONArray point = new JSONArray();
				point.put(((Date) result.getProperty("timestamp")).getTime());
				point.put((Long) result.getProperty("AvgCPUtime"));
				data.put(point);
			}
			ser1.put("data", data);
			plot.put(ser1);
		}

		res.put("plot", plot);

		// for the table
		JSONObject tableData = new JSONObject();
		JSONArray data = new JSONArray();
		JSONArray headers = new JSONArray();
		headers.put(new JSONObject().put("title", "Site"));
		headers.put(new JSONObject().put("title", "Time"));
		headers.put(new JSONObject().put("title", "Successes"));
		headers.put(new JSONObject().put("title", "Failures"));
		headers.put(new JSONObject().put("title", "Avg CPU time"));
		headers.put(new JSONObject().put("title", "Avg Duration"));
		headers.put(new JSONObject().put("title", "Avg Waiting"));
		for (Entity result : lRes) {
			JSONArray su = new JSONArray();
			su.put((String) result.getProperty("site"));
			su.put(((Date) result.getProperty("timestamp")).getTime());
			su.put((Long) result.getProperty("successes"));
			su.put((Long) result.getProperty("failures"));
			su.put((Long) result.getProperty("AvgCPUtime"));
			su.put((Long) result.getProperty("AvgDuration"));
			su.put((Long) result.getProperty("AvgWaiting"));
			data.put(su);
		}
		tableData.put("data", data);
		tableData.put("headers", headers);
		res.put("tableData", tableData);
		resp.getWriter().print(res);
	}
}
