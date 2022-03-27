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
    public Object handle(Request request, Response response) throws HaltException {
        // TODO Auto-generated method stub
        return null;
    }

}
