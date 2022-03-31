package edu.upenn.cis.cis455;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.junit.Test;

import edu.upenn.cis.cis455.crawler.utils.Robots;
import junit.framework.TestCase;

public class CrawlerTests extends TestCase {

    InputStream streamOf(String file) throws FileNotFoundException {
        File initialFile = new File(file);
        return new FileInputStream(initialFile);
    }

    @Test
    void testRobotsParser() throws FileNotFoundException, ParseException, IOException {
        Robots robotsInfo = new Robots(streamOf("resources/trickyrobots.txt"));
        assertEquals(robotsInfo.crawlDelaySeconds, 10);
    }

}
