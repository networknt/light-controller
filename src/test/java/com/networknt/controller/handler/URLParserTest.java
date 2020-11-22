package com.networknt.controller.handler;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

public class URLParserTest {
    @Test
    public void testURLParser() {
        String s = "https://localhost:443/com.networknt.ac-1.0.0?environment=test1&";
        try {
            URL url = new URL(s);
            String serviceId = url.getPath().substring(1);
            Assert.assertEquals("com.networknt.ac-1.0.0", serviceId);
            String query = url.getQuery();
            String tag = null;
            if(query.indexOf("=") > 0) {
                tag = query.substring(query.indexOf("=") + 1, query.length() -1);
            }
            Assert.assertEquals("test1", tag);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }
}
