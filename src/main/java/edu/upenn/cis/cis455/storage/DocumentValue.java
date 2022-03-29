package edu.upenn.cis.cis455.storage;

import java.io.Serializable;

public class DocumentValue implements Serializable {
    private static final long serialVersionUID = 1L;
    
    long timeLastCrawled;
    // If alias not null, then it points to original URL with same content
    // Otherwise if alias is null, look at content
    String alias = null;
    byte[] content = null;
    
    public DocumentValue(long timeLastCrawled, String alias) {
        this.timeLastCrawled = timeLastCrawled;
        this.alias = alias;
    }
    
    public DocumentValue(long timeLastCrawled, byte[] content) {
        this.timeLastCrawled = timeLastCrawled;
        this.content = content;
    }
    
    public boolean isAlias() {
        return alias != null;
    }
}
