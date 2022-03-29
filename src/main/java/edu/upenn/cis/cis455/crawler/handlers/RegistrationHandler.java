package edu.upenn.cis.cis455.crawler.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.HaltException;
import spark.Request;
import spark.Response;
import spark.Route;

public class RegistrationHandler implements Route {
    Logger logger = LogManager.getLogger(RegistrationHandler.class);
    StorageInterface db;

    public RegistrationHandler(StorageInterface db) {
        this.db = db;
    }

    @Override
    public Object handle(Request req, Response res) throws HaltException {
        String user = req.queryParams("username");
        String pass = req.queryParams("password");
        logger.debug("Registration request for " + user + " and " + pass);

        // TODO - error checking
        boolean addedSuccessfully = db.addUser(user, pass);
        // @878 register the status code with 200 success and return a page with body
        // contains <a href> link to the main page
        res.status(200);
        res.header("Content-Type", "text/html");
        return addedSuccessfully ? "<html><a href=\"/login-form.html\">Go to login page</a></html>"
                : "<html>Could not add, username already taken</html>";
    }

}
