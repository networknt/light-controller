package com.networknt.controller;

public class ControllerConstants {
    public static int NONCE = 1;
    public static String HOST = "lightapi.net";
    public static String CHECK = "check";
    public static int CHECK_FREQUENCY = 10; // check the health every 10 seconds by default.

    /* Common Query Parameter Names */
    public static final String PORT = "port";
    public static final String PROTOCOL = "protocol";
    public static final String ADDRESS = "address";

    /* Client Controller Path Strings */
    public static final String SERVER_INFO_ENDPOINT = "/server/info";
    public static final String LOGGER_ENDPOINT = "/logger";
    public static final String LOGGER_CONTENT_ENDPOINT = "/logger/contents";
    public static final String CHAOS_MONKEY_ASSAULT_ENDPOINT = "/chaosmonkey/{assaultType}";
    public static final String CHAOS_MONKEY_ENDPOINT = "/chaosmonkey";

}
