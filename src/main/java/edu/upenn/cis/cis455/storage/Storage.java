package edu.upenn.cis.cis455.storage;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

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
            dbConfig.setAllowCreate(true);

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

    // @768 can also use URL as the key for document content
    @Override
    public boolean addDocument(String url, String documentContents) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getDocument(String url) {
        // TODO Auto-generated method stub
        return null;
    }

    // @768 We also allow you to change the interface to not use ids if you don’t
    // want to
    // create ids (so you can choose to look up users by username only). It’s just
    // more efficient to lookup keys based on an integer instead of a string.
    // TODO: switch to int id and add username as secondary key
    @Override
    public boolean addUser(String username, String password) {
        // TODO: encrypt password with SHA-256
        userMap.put(username, password);
        logger.debug(mapToString("Users", userMap.entrySet().iterator()));
        return true;
    }

    @Override
    public boolean getSessionForUser(String username, String password) {
        // TODO: encrypt password with SHA-256
        if (userMap.containsKey(username)) {
            return userMap.get(username).equals(password);
        }
        return false;
    }

    @Override
    public void close() throws DatabaseException {
        javaCatalog.close();
        userStore.close();

        env.close();
    }

    static <K, V> String mapToString(String label, Iterator<Map.Entry<K, V>> iterator) {
        StringBuilder res = new StringBuilder("\n--- " + label + " ---\n");
        while (iterator.hasNext()) {
            Map.Entry<K, V> entry = iterator.next();
            res.append("Key: ");
            res.append(entry.getKey().toString());
            res.append("\nValue: ");
            res.append(entry.getValue().toString());
            res.append("\n");
        }
        return res.toString();
    }

}
