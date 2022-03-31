package edu.upenn.cis.cis455.crawler.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;

public class Robots {
    public int crawlDelaySeconds = 0;
    public long timeLastCrawled;
    public List<String> disallowPaths;

    final static String USER_AGENT = "User-agent:";
    final static String DISALLOW = "Disallow:";
    final static String CRAWL_DELAY = "Crawl-delay:";

    static String getValue(String line, String key) {
        // Eg. "User-agent: cis455crawler #455 crawler" -> "cis455crawler"
        String rightSide = line.substring(key.length()).trim();
        return rightSide.substring(0, rightSide.indexOf('#'));
    }

    void readGroup(BufferedReader reader) throws IOException {
        List<String> groupDisallow = new LinkedList<String>();
        String line;

        while ((line = reader.readLine()) != null && !line.equals("")) {
            if (line.startsWith("#")) {
                continue; // ignore comments
            }
            if (line.startsWith(DISALLOW)) {
                String disallowRule = getValue(line, DISALLOW);
                if (!disallowRule.equals("")) { // Crawlers ignore directives without a [path].
                    groupDisallow.add(disallowRule);
                }
            }
            if (line.startsWith(CRAWL_DELAY)) {
                // Could be overwritten if we find more specific group, aka. cis455crawler group
                // that matches
                this.crawlDelaySeconds = NumberUtils.toInt(getValue(line, CRAWL_DELAY));
            }
        }
        // Also could be overwritten
        this.disallowPaths = groupDisallow;
    }

    public Robots(InputStream responseBody) throws ParseException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#")) {
                continue; // ignore comments
            }
            if (line.startsWith(USER_AGENT)) {
                String userAgent = getValue(line, USER_AGENT);
                if (userAgent.equals("*")) {
                    readGroup(reader);
                } else if (userAgent.equals("cis455crawler")) {
                    // This is most specific group that can be, so can ignore all other groups'
                    // rules
                    readGroup(reader);
                    break;
                }
            }
        }
        this.timeLastCrawled = System.currentTimeMillis();
    }

}
