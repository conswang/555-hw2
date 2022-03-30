package edu.upenn.cis.cis455.storage;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
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
    static final String DOCUMENT_STORE = "document_store";
    static final String CONTENT_SEEN_STORE = "content_seen_store";

    StoredClassCatalog javaCatalog;
    Database userStore;
    Database documentStore;
    Database contentSeenStore;

    // username -> SHA 256 password
    StoredSortedMap<String, String> userMap;
    // url -> DocumentValue
    StoredSortedMap<String, DocumentValue> documentMap;
    // MD5 hash of document -> first url it came from
    StoredSortedMap<String, String> contentSeenMap;

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
            documentStore = env.openDatabase(null, DOCUMENT_STORE, dbConfig);
            // Content seen store is temporary and requires its own dbConfig
            // env.removeDatabase(null, CONTENT_SEEN_STORE);
            DatabaseConfig tempDbConfig = new DatabaseConfig();
            tempDbConfig.setAllowCreate(true);
            tempDbConfig.setTemporary(true);
            contentSeenStore = env.openDatabase(null, CONTENT_SEEN_STORE, tempDbConfig);

            // Bind stored sorted maps to the databases
            EntryBinding<String> userKeyBinding = new SerialBinding<String>(javaCatalog,
                    String.class);
            EntryBinding<String> userValueBinding = new SerialBinding<String>(javaCatalog,
                    String.class);
            userMap = new StoredSortedMap<String, String>(userStore, userKeyBinding,
                    userValueBinding, true);
            EntryBinding<String> documentKeyBinding = new SerialBinding<String>(javaCatalog,
                    String.class);
            EntryBinding<DocumentValue> documentValueBinding = new SerialBinding<DocumentValue>(
                    javaCatalog, DocumentValue.class);
            documentMap = new StoredSortedMap<String, DocumentValue>(documentStore,
                    documentKeyBinding, documentValueBinding, true);
            EntryBinding<String> contentSeenKeyBinding = new SerialBinding<String>(javaCatalog,
                    String.class);
            EntryBinding<String> contentSeenValueBinding = new SerialBinding<String>(javaCatalog,
                    String.class);
            contentSeenMap = new StoredSortedMap<String, String>(contentSeenStore,
                    contentSeenKeyBinding, contentSeenValueBinding, true);

        } catch (Exception e) {
            logger.fatal("Could not create database environment with directory {}: {}", directory,
                    e.toString());
            System.exit(1);
        }
        
        
        logger.debug("State of DB at start of application------");
        logger.debug(mapToString("Documents", documentMap.entrySet().iterator()));
        logger.debug(mapToString("Users", userMap.entrySet().iterator()));
        logger.debug(mapToString("Content seen (should be empty)", contentSeenMap.entrySet().iterator()));
    }

    @Override
    public int getCorpusSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    // @768 can also use URL as the key for document content
    @Override
    public boolean addDocument(String url, byte[] documentContents) {
        if (documentContents == null) {
            logger.debug("No content found at {}, not adding url to db", url);
            return false;
        }
        String digest = DigestUtils.md5Hex(documentContents);
        if (contentSeenMap.containsKey(digest)) {
            // We will NOT want to crawl it nor do we want to store an extra copy in DB
            // Instead store an association to the URL
            String urlWhereSeen = contentSeenMap.get(digest);
            DocumentValue value = new DocumentValue(urlWhereSeen);
            documentMap.put(url, value);
            return false;
        }
        // For NEW content, we will store the document in DB and may crawl it if it's html
        DocumentValue value = new DocumentValue(documentContents);
        if (documentMap.containsKey(url)) {
            // TODO: need special behaviour here?
            logger.debug("Updating document at {} which we crawled before", url);
        }
        contentSeenMap.put(url, digest);
        documentMap.put(url, value);
        
        return true;
    }

    @Override
    public String getDocument(String url) {
        DocumentValue value = documentMap.get(url);
        if (!value.isAlias()) {
            if (value.content != null) {
                return new String(value.content);
            }
        }
        // TODO: add content seen table and deal with aliases
        return "";
    }

    // @768 We also allow you to change the interface to not use ids if you don’t
    // want to
    // create ids (so you can choose to look up users by username only). It’s just
    // more efficient to lookup keys based on an integer instead of a string.
    // TODO: switch to int id and add username as secondary key
    @Override
    public boolean addUser(String username, String password) {
        String passwordDigest = DigestUtils.sha256Hex(password);
        if (userMap.containsKey(username)) {
            return false;
        }
        userMap.put(username, passwordDigest);
        logger.debug(mapToString("Users", userMap.entrySet().iterator()));
        return true;
    }

    @Override
    public boolean getSessionForUser(String username, String password) {
        String passwordDigest = DigestUtils.sha256Hex(password);
        if (userMap.containsKey(username)) {
            return userMap.get(username).equals(passwordDigest);
        }
        return false;
    }

    @Override
    public void close() throws DatabaseException {
        javaCatalog.close();
        userStore.close();
        documentStore.close();
        contentSeenStore.close();

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
