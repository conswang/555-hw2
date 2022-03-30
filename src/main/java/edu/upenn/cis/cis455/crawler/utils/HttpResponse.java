package edu.upenn.cis.cis455.crawler.utils;

public class HttpResponse {
    public final int status;
    public final long contentLength;
    public final String contentType;
    public final byte[] body;
    
    public HttpResponse(int status, long contentLength, String contentType, byte[] body) {
        this.status = status;
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.body = body;
    }
}
