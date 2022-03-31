package edu.upenn.cis.cis455.crawler.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    static HttpURLConnection setUpConnection(URL url, String method) throws IOException {
        // Cast also works for Https urls, because HttpsURLConnection is a subclass
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("User-Agent", "cis455crawler");
        // TODO: set request timeout?
        return connection;
    }

    public static HttpResponse fetch(URL url, String method) {
        try {
            HttpURLConnection connection = setUpConnection(url, method);
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

    /**
     * 
     * @param urlInfo
     * @param robotsCache   Robots.txt info on sites that do have one
     * @param noRobotsCache Set of sites that don't have one
     * @return Robots info if there is, null if there isn't
     */
    public Robots getRobots(URLInfo urlInfo, Map<String, Robots> robotsCache,
            Set<String> noRobotsCache) {
        String robotsAddress = urlInfo.getRobotsTxt();
        if (robotsCache.containsKey(robotsAddress)) {
            return robotsCache.get(robotsAddress);
        }
        URL url;
        try {
            url = new URL(robotsAddress);
        } catch (MalformedURLException e) {
            logger.error(
                    "Could not construct url to get robots txt of website; very bad or href had malformed string{}",
                    e.toString());
            return null;
        }

        try {
            HttpURLConnection conn = setUpConnection(url, "GET");
            int status = conn.getResponseCode();
            // TODO: google follows redirects for robots.txt for up to 5 hops?
            if (!isOk(status)) {
                logger.debug("Could not find robots.txt file, will ignore and continue crawling");
                noRobotsCache.add(robotsAddress);
                return null;
            }
            try {
                Robots robots = new Robots(conn.getInputStream());
                robotsCache.put(robotsAddress, robots);
            } catch (ParseException e) {
                logger.debug("Could not parse robots.txt file, will ignore and continue crawling");
                noRobotsCache.add(robotsAddress);
                return null;
            }
        } catch (IOException e) {
            logger.error("Could not open connection {}", e.toString());
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
