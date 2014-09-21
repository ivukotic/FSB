package edu.uchicago.WANio;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;

@SuppressWarnings("serial")
public class LogsServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(LogsServlet.class.getName());
    private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();

    
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(req);
        List<BlobKey> blobKeys = blobs.get("myFile");
        
        if (blobKeys.size()==0) {
        	log.severe("Log not received.");
        } else {
        	log.warning("Logs received: "+blobKeys.size());
            
        }

	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.warning("LogsServlet delivering a log ...");
		BlobKey blobKey = new BlobKey(req.getParameter("blob-key"));
        blobstoreService.serve(blobKey, resp);
	}
}
