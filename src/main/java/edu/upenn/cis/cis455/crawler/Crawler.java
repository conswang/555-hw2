package edu.upenn.cis.cis455.crawler;

import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class Crawler implements CrawlMaster {
    ///// TODO: you'll need to flesh all of this out. You'll need to build a thread
    // pool of CrawlerWorkers etc.

    static final int NUM_WORKERS = 10;

    public Crawler(String startUrl, StorageInterface db, int size, int count) {
        // TODO: initialize
    }

    /**
     * Main thread
     */
    public void start() {
    }

    /**
     * We've indexed another document
     */
    @Override
    public void incCount() {
    }

    /**
     * Workers can poll this to see if they should exit, ie the crawl is done
     */
    @Override
    public boolean isDone() {
        return true;
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
            System.out.println("Usage: Crawler {start URL} {database environment path} {max doc size in MB} {number of files to index}");
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

        while (!crawler.isDone())
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        // TODO: final shutdown

        System.out.println("Done crawling!");
    }

}
