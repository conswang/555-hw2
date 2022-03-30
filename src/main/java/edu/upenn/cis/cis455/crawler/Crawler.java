package edu.upenn.cis.cis455.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.utils.HttpResponse;
import edu.upenn.cis.cis455.crawler.utils.HttpUtils;
import edu.upenn.cis.cis455.crawler.utils.URLInfo;
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

    /**
     * Main thread
     */
    public void start() {
        while (!urls.isEmpty() && !isDone()) {
            URLInfo nextUrl = urls.poll();
            incCount();

            try {
                String nextUrlString = nextUrl.toString();
                URL url = new URL(nextUrlString);
                HttpResponse headRes = HttpUtils.fetch(url, "HEAD");
                // Check if modified since to see if we should even bother fetching?
                if (!shouldCrawl(headRes)) {
                    logger.debug("Skipping URL {} because of HEAD response", nextUrlString);
                    continue;
                }
                HttpResponse getRes = HttpUtils.fetch(url, "GET");
                // TODO: check whether should store in DB based on content seen table
                boolean isNewContent = db.addDocument(nextUrlString, getRes.body);
                if (!isNewContent) {
                    // TODO: differentiate between if we've extracted same URL before
                    // and if we've extracted same content before
                    logger.info("{}: not modified", nextUrlString);
                    continue;
                }
                logger.info("{}: downloading", nextUrlString);
                if (!shouldCrawl(getRes)) {
                    logger.debug("Skipping URL {} because of GET response", nextUrlString);
                    continue;
                }
                // Only extract links for unseen before html documents
                if (getRes.contentType != null
                        && getRes.contentType.equalsIgnoreCase("text/html")) {
                    List<URLInfo> links = HttpUtils.extractLinks(getRes.body, nextUrlString);
                    urls.addAll(links);
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
