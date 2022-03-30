package edu.upenn.cis.cis455.crawler.utils;

public class HttpUtils {
    public static boolean isAcceptedType(String contentType) {
        return contentType != null
                && (contentType.equals("text/html") || contentType.equals("application/xml")
                        || contentType.equals("text/xml") || contentType.endsWith("+xml"));
    }
}
