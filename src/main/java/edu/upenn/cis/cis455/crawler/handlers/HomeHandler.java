package edu.upenn.cis.cis455.crawler.handlers;

import spark.Request;
import spark.Response;
import spark.Route;

public class HomeHandler implements Route {

    @Override
    public Object handle(Request req, Response res) throws Exception {
        String username = req.attribute("user");
        res.type("text/html");
        return "<html><h1>Welcome " + username + "</h1></html>";
    }

}
