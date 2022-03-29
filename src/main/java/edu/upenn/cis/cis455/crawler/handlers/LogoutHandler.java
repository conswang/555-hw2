package edu.upenn.cis.cis455.crawler.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

public class LogoutHandler implements Route {
    final static Logger logger = LogManager.getLogger(RegistrationHandler.class);

    @Override
    public Object handle(Request req, Response res) throws Exception {
        Session ses = req.session(false);
        if (ses != null) { // Session should never be null though because of login filter
            ses.invalidate();
            res.redirect("/login-form.html");
        }
        return null;
    }

}
