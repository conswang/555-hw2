package edu.upenn.cis.cis455.crawler.handlers;

import spark.Request;
import spark.Route;
import spark.Response;
import spark.HaltException;
import spark.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.storage.StorageInterface;

public class LoginHandler implements Route {
    final static Logger logger = LogManager.getLogger(LoginHandler.class);
    
    StorageInterface db;

    public LoginHandler(StorageInterface db) {
        this.db = db;
    }

    @Override
    public String handle(Request req, Response resp) throws HaltException {
        String user = req.queryParams("username");
        String pass = req.queryParams("password");

        logger.debug("Login request for " + user + " and " + pass);
        if (db.getSessionForUser(user, pass)) {
            logger.debug("Logged in!");
            Session session = req.session();

            session.attribute("user", user);
            session.attribute("password", pass);
            session.maxInactiveInterval(5 * 60);
            resp.redirect("/index.html");
        } else {
            logger.debug("Invalid credentials");
            resp.redirect("/login-form.html");
        }

        return "";
    }
}
