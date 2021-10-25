package com.networknt.controller.handler;

import org.junit.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class URLEncodeTest {
    @Test
    public void testEncode() throws Exception {
        String pathParam = "com.networknt.petstore-3.0.1|dev:https:192.168.1.1:8443";
        String encoded = URLEncoder.encode(pathParam, StandardCharsets.UTF_8.toString());
        System.out.println(encoded);
    }

}
