package edu.upenn.cis.cis455.crawler;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.utils.HttpUtils;
import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class Crawler implements CrawlMaster {
    final static Logger logger = LogManager.getLogger(Crawler.class);

    ///// TODO: you'll need to flesh all of this out. You'll need to build a thread
    // pool of CrawlerWorkers etc.

    Queue<String> urls;
    StorageInterface db;
    long maxContentSizeBytes;
    // Stop crawling when count = maxDocSize
    int count = 0;
    int numToCrawl;

    static final int NUM_WORKERS = 10;

    public Crawler(String startUrl, StorageInterface db, int maxContentSizeMegabytes, int numToCrawl) {
        urls = new LinkedList<String>();
        urls.add(startUrl);
        this.db = db;
        this.maxContentSizeBytes = 1024 * 1024 * maxContentSizeMegabytes;
        this.numToCrawl = numToCrawl;
    }

    /**
     * Main thread
     */
    public void start() {
        while (!urls.isEmpty() && !isDone()) {
            String next = urls.poll();
            incCount();

            try {
                // TODO: use URLInfo to canonize all url names
                URL url = new URL(next);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setRequestProperty("User-Agent", "cis455crawler");
                int status = connection.getResponseCode();
                // TODO: check if there are any other ok statuses
                if (status != 200) {
                    logger.debug("Will not GET URL {}, HEAD request had status {}", next, status);
                    continue;
                }
                long contentLength = connection.getContentLengthLong();
                if (contentLength == -1 || contentLength > maxContentSizeBytes) {
                    logger.debug("Will not GET URL {}, contentLength was {}, max is {}", next,
                            contentLength, maxContentSizeBytes);
                    continue;
                }
                String contentType = connection.getContentType();
                if (!HttpUtils.isAcceptedType(contentType)) {
                    logger.debug("Will not GET URL {}, content type was {}", next, contentType);
                    continue;
                }

                // TODO: set request timeout?

            } catch (MalformedURLException e) {
                logger.error("Skipping URL {}, failed to parse url string", e.toString());
                continue;
            } catch (IOException e) {
                logger.error("Skipping URL {}, failed to open connection/connect {}", e.toString());
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

        // TODO: final shutdown
        // Clean up code before server exits
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                db.close();
            }
        });

        System.out.println("Done crawling!");
    }

}
