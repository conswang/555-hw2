package edu.upenn.cis.cis455.crawler.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HttpUtils {
    final static Logger logger = LogManager.getLogger(HttpUtils.class);

    public static boolean isAcceptedType(String contentType) {
        return contentType != null && (contentType.equalsIgnoreCase("text/html")
                || contentType.equalsIgnoreCase("application/xml")
                || contentType.equalsIgnoreCase("text/xml")
                || contentType.toLowerCase().endsWith("+xml"));
    }

    public static boolean isOk(int status) {
        return 100 <= status && status < 400;
    }

    public static HttpResponse fetch(URL url, String method) {
        try {
            // Cast also works for Https urls, because HttpsURLConnection is a subclass
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("User-Agent", "cis455crawler");
            // TODO: set request timeout?

            int status = connection.getResponseCode();
            long contentLength = connection.getContentLengthLong();
            String contentType = connection.getContentType();
            // TODO: put a cap on how many bytes we're reading?
            byte[] body;
            if (isOk(status)) {
                body = connection.getInputStream().readAllBytes();
            } else {
                body = connection.getErrorStream().readAllBytes();
            }
            return new HttpResponse(status, contentLength, contentType, body);
        } catch (IOException e) {
            logger.error("Did not {} URL {}, failed to open connection/connect {}", method,
                    url.toString(), e.toString());
        } catch (Exception e) {
            logger.error("Did not {} URL {}, other exception: ", method, url.toString(),
                    e.toString());
        }
        return null;
    }

    public static List<URLInfo> extractLinks(byte[] body, String baseUri) {
        List<URLInfo> res = new LinkedList<URLInfo>();
        String html = new String(body);
        Document doc = Jsoup.parse(html, baseUri);
        // @890 extract all hrefs from all tags and crawl them
        Elements allLinks = doc.select("*[href]");
        for (Element link : allLinks) {
            String linkString = link.attr("abs:href");
            URLInfo urlInfo = new URLInfo(linkString);
            res.add(urlInfo);
            logger.debug("JSoup parsed link {} from website {}", linkString, baseUri);
        }
        return res;
    }
}
