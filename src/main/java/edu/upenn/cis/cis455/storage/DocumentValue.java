package edu.upenn.cis.cis455.storage;

import java.io.Serializable;

public class DocumentValue implements Serializable {
    private static final long serialVersionUID = 1L;

    long timeLastCrawled;
    // If alias not null, then it points to original URL with same content
    // Otherwise if alias is null, look at content
    String alias = null;
    byte[] content = null;

    public DocumentValue(String alias) {
        // TODO: technically to be more accurate, we should pass in response date header
        // But seeing as the provided StorageInterface doesn't have a parameter for that
        // I doubt we're testing so thoroughly
        this.timeLastCrawled = System.currentTimeMillis();
        this.alias = alias;
    }

    public DocumentValue(byte[] content) {
        this.timeLastCrawled = System.currentTimeMillis();
        this.content = content;
    }

    public boolean isAlias() {
        return alias != null;
    }

    public String toString() {
        return "[timeLastCrawled=" + timeLastCrawled
                + (alias == null ? ", content num bytes=" + content.length : ", alias=" + alias) + "]";
    }
}
