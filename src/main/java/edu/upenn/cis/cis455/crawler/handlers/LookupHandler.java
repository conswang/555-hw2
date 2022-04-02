package edu.upenn.cis.cis455.crawler.handlers;

import edu.upenn.cis.cis455.crawler.utils.URLInfo;
import edu.upenn.cis.cis455.storage.DocumentValue;
import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.HaltException;
import spark.Request;
import spark.Response;
import spark.Route;

import static spark.Spark.*;

public class LookupHandler implements Route {
    StorageInterface db;

    public LookupHandler(StorageInterface db) {
        this.db = db;
    }

    @Override
    public Object handle(Request req, Response res) throws HaltException {
        String url = req.queryParams("url");
        if (url == null) {
            halt(400, "Missing url query parameter :<");
        }
        
        URLInfo urlInfo = new URLInfo(url);
        String canonicalUrl = urlInfo.toString();
        
        DocumentValue contents = db.getDocument(canonicalUrl);
        if (contents == null) {
            halt(404, "Document at " + canonicalUrl + " not found :<");
        }
        // No need to set content-type header, Spark/browser can probe it
        return contents.content;
    }

}
