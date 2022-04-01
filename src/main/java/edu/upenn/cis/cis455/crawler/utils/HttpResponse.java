package edu.upenn.cis.cis455.crawler.utils;

public class HttpResponse {
    public final int status;
    public final long contentLength;
    public final String contentType;
    public final byte[] body;
    public final long lastModified;

    public HttpResponse(int status, long contentLength, String contentType, byte[] body,
            long lastModified) {
        this.status = status;
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.body = body;
        this.lastModified = lastModified;
    }
}
