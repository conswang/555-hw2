package edu.upenn.cis.cis455.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.utils.HttpResponse;
import edu.upenn.cis.cis455.crawler.utils.HttpUtils;
import edu.upenn.cis.cis455.crawler.utils.Robots;
import edu.upenn.cis.cis455.crawler.utils.URLInfo;
import edu.upenn.cis.cis455.storage.DocumentValue;
import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class Crawler implements CrawlMaster {
    final static Logger logger = LogManager.getLogger(Crawler.class);

    ///// TODO: you'll need to flesh all of this out. You'll need to build a thread
    // pool of CrawlerWorkers etc.

    // url queue allows for duplicates, but we won't crawl again if document digest
    // is in content seen table
    Queue<URLInfo> urls;
    StorageInterface db;
    Map<String, Robots> robotsCache; // base url (not inclu. file path) -> robots info
    Set<String> noRobotsCache; // set of base urls

    long maxContentSizeBytes;
    // Stop crawling when count = maxDocSize
    int count = 0;
    int numToCrawl;

    static final int NUM_WORKERS = 10;

    public Crawler(String startUrl, StorageInterface db, int maxContentSizeMegabytes,
            int numToCrawl) {
        urls = new LinkedList<URLInfo>();
        urls.add(new URLInfo(startUrl));
        this.db = db;
        this.maxContentSizeBytes = 1024 * 1024 * maxContentSizeMegabytes;
        this.numToCrawl = numToCrawl;

        robotsCache = new HashMap<String, Robots>();
        noRobotsCache = new HashSet<String>();
    }

    boolean shouldCrawl(HttpResponse res) {
        if (!HttpUtils.isOk(res.status)) {
            logger.debug("Request had status {}", res.status);
            return false;
        }
        if (res.contentLength == -1 || res.contentLength > maxContentSizeBytes) {
            logger.debug("ContentLength was {}, max is {}", res.contentLength, maxContentSizeBytes);
            return false;
        }
        if (!HttpUtils.isAcceptedType(res.contentType)) {
            logger.debug("Content type was {}", res.contentType);
            return false;
        }
        return true;
    }

    boolean checkRobots(URLInfo nextUrl) {
        // @806 1. it should first check if it can be crawled by checking the robot.txt
        // file
        Robots robotsInfo = HttpUtils.getRobots(nextUrl, robotsCache, noRobotsCache);
        if (robotsInfo == null) {
            logger.debug("Robots.txt not found, will continue to fetch {}", nextUrl.toString());
            return true;
        } else {
            if (robotsInfo.isDisallowed(nextUrl)) {
                logger.debug(
                        "cis455crawler is disallowed from fetching {}. Not incrementing current document count, which is {}",
                        nextUrl.toString(), count);
                return false;
            }
            if (!robotsInfo.crawl()) {
                // @806 2. then it should check if it needs to delay crawl, if it needs to
                // delay crawl, add the URL back to the end of the queue
//                logger.debug(
//                        "Putting url {} back in the end of queue because of crawl delay",
//                        nextUrlString);
                // TODO: fix "busy wait" when no websites are crawlable
                urls.add(nextUrl);
                return false;
            } else {
                return true;
            }
        }
    }

    void extractLinks(byte[] html, String url) {
        List<URLInfo> links = HttpUtils.extractLinks(html, url);
        urls.addAll(links);
    }

    /**
     * Main thread
     */
    public void start() {
        while (!urls.isEmpty() && !isDone()) {
//            logger.debug("---------------begin crawl loop-----------------------");
            URLInfo nextUrl = urls.poll();

            try {
                String urlString = nextUrl.toString();
                URL url = new URL(urlString);

                if (!checkRobots(nextUrl)) {
                    continue;
                }

                HttpResponse headRes = HttpUtils.fetch(url, "HEAD");
                if (!shouldCrawl(headRes)) {
                    logger.debug("Skipping URL {} because of HEAD response", urlString);
                    continue;
                }

                incCount();

                DocumentValue storedDocument = db.getDocument(urlString);

                // @806 3.2 If it has been crawled but modified, it should get its content.
                // @806 3.3 If it has not been crawled before, then download content
                if (storedDocument == null
                        || storedDocument.timeLastCrawled < headRes.lastModified) {
                    HttpResponse getRes = HttpUtils.fetch(url, "GET");
                    if (!shouldCrawl(getRes)) {
                        logger.debug("Skipping URL {} because of GET response", urlString);
                        continue;
                    }
                    // @806 3.5 Will update the corpus if the content has not been seen
                    boolean isNewContent = db.addDocument(urlString, getRes.body,
                            getRes.contentType);
                    if (!isNewContent) {
                        // We've extracted same content before (not necessarily from same url)
                        // @806 3.4 If the MD5 hashed content is seen, do nothing.
                        logger.info("{}: not modified", urlString);
                        logger.debug("Content seen in table already, do NOT crawl again");
                        continue;
                    } else {
                        logger.info("{}: downloading", urlString);
                        if (HttpUtils.isHtml(getRes.contentType)) {
                            extractLinks(getRes.body, urlString);
                        }
                    }
                } else {
                    // @806 3.1 If it has been crawled before and not modified, check outgoing
                    // links.
                    logger.info("{}: not modified", urlString);
                    // Also check if content was seen in this crawler run
                    if (HttpUtils.isHtml(storedDocument.contentType)
                            && !db.contentSeen(storedDocument.content)) {
                        extractLinks(storedDocument.content, urlString);
                    }
                }
            } catch (MalformedURLException e) {
                logger.error("Skipping URL {}, failed to parse url string", e.toString());
                continue;
            } catch (Exception e) {
                logger.error("Skipping URL {}, other exception {}", e.toString());
            }
        }
    }

    /**
     * We've indexed another document
     */
    @Override
    public void incCount() {
        // TODO: when we make this multithreaded, will need to synchronize count updates
        count++;
    }

    /**
     * Workers can poll this to see if they should exit, ie the crawl is done
     */
    @Override
    public boolean isDone() {
        return count >= numToCrawl;
    }

    /**
     * Workers should notify when they are processing an URL
     */
    @Override
    public void setWorking(boolean working) {
    }

    /**
     * Workers should call this when they exit, so the master knows when it can shut
     * down
     */
    @Override
    public void notifyThreadExited() {
    }

    /**
     * Main program: init database, start crawler, wait for it to notify that it is
     * done, then close.
     */
    public static void main(String args[]) {
        if (args.length < 3 || args.length > 5) {
            System.out.println(
                    "Usage: Crawler {start URL} {database environment path} {max doc size in MB} {number of files to index}");
            System.exit(1);
        }

        System.out.println("Crawler starting");
        String startUrl = args[0];
        String envPath = args[1];
        Integer size = Integer.valueOf(args[2]);
        Integer count = args.length == 4 ? Integer.valueOf(args[3]) : 100;

        StorageInterface db = StorageFactory.getDatabaseInstance(envPath);

        Crawler crawler = new Crawler(startUrl, db, size, count);

        System.out.println("Starting crawl of " + count + " documents, starting at " + startUrl);
        crawler.start();

//        while (!crawler.isDone())
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }

        System.out.println("Done crawling!");

        // TODO: final shutdown
        // Clean up code before server exits
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            public void run() {
//                db.close();
//            }
//        });
        db.close();

    }

}
