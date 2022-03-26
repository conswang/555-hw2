package edu.upenn.cis.cis455.storage;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

public class Storage implements StorageInterface {
    final static Logger logger = LogManager.getLogger(Storage.class);

    Environment env;

    static final String JAVA_CATALOG = "java_catalog";
    static final String USER_STORE = "user_store";

    StoredClassCatalog javaCatalog;
    Database userStore;

    StoredSortedMap<String, String> userMap;

    public Storage(String directory) {
        try {
            EnvironmentConfig envConfig = new EnvironmentConfig();
            envConfig.setAllowCreate(true);
            env = new Environment(new File(directory), envConfig);

            // Create class catalog database and other databases
            // TODO: need to customize dbConfig more?
            DatabaseConfig dbConfig = new DatabaseConfig();
            Database catalogDb = env.openDatabase(null, JAVA_CATALOG, dbConfig);
            javaCatalog = new StoredClassCatalog(catalogDb);
            userStore = env.openDatabase(null, USER_STORE, dbConfig);

            // Bind stored sorted maps to the databases
            EntryBinding<String> userKeyBinding = new SerialBinding<String>(javaCatalog,
                    String.class);
            EntryBinding<String> userValueBinding = new SerialBinding<String>(javaCatalog,
                    String.class);
            userMap = new StoredSortedMap<String, String>(userStore, userKeyBinding,
                    userValueBinding, true);

        } catch (Exception e) {
            logger.fatal("Could not create database environment with directory {}: {}", directory,
                    e.toString());
            System.exit(1);
        }
    }

    @Override
    public int getCorpusSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int addDocument(String url, String documentContents) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getDocument(String url) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int addUser(String username, String password) {
        userMap.put(username, password);
        return 0;
    }

    @Override
    public boolean getSessionForUser(String username, String password) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void close() throws DatabaseException {
        javaCatalog.close();
        userStore.close();

        env.close();
    }

}
