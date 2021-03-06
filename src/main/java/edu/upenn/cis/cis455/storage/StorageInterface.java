package edu.upenn.cis.cis455.storage;

public interface StorageInterface {

    /**
     * How many documents so far?
     */
    public int getCorpusSize();
    
    public boolean contentSeen(byte[] documentContents);

    /**
     * Add a new document, getting its ID
     * 
     * @return true if the document has new content and we should crawl its links
     *         (if it's html). False if document is in contentSeen table
     */
    public boolean addDocument(String url, byte[] documentContents, String contentType);

    /**
     * Retrieves a document's contents by URL
     */
    public DocumentValue getDocument(String url);

    /**
     * Adds a user and returns an ID
     */
    public boolean addUser(String username, String password);

    /**
     * Tries to log in the user, or else throws a HaltException
     */
    public boolean getSessionForUser(String username, String password);

    /**
     * Shuts down / flushes / closes the storage system
     */
    public void close();
}
