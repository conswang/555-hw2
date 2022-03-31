package edu.upenn.cis.cis455;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.junit.Test;

import edu.upenn.cis.cis455.crawler.utils.Robots;

public class CrawlerTest {

    InputStream streamOf(String file) throws FileNotFoundException {
        File initialFile = new File(file);
        return new FileInputStream(initialFile);
    }

    @Test
    public void testRobotsParser() throws FileNotFoundException, ParseException, IOException {
        Robots robotsInfo = new Robots(streamOf("./trickyrobots.txt"));
        String[] expectedDisallow = new String[] { "/a", "/mail", "/tasks/fluffypuffy" };
        
        assertArrayEquals(expectedDisallow, robotsInfo.disallowPaths().toArray());
        assertEquals(10, robotsInfo.crawlDelaySeconds());
        assertFalse(robotsInfo.timeLastCrawled() == 0);
    }

}
