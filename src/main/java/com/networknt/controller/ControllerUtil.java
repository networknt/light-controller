package com.networknt.controller;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ControllerUtil {
    public static List delService(List nodes, String address, int port) {
        if(nodes != null) {
            for(Iterator<Map<String, Object>> iter = nodes.iterator(); iter.hasNext(); ) {
                Map<String, Object> map = iter.next();
                String a = (String)map.get("address");
                int p = (Integer)map.get("port");
                if (address.equals(a) && port == p)
                    iter.remove();
            }
        }
        return nodes;
    }
}
