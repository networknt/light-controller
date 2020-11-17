package com.networknt.controller;

import com.networknt.controller.model.Check;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.TimerTask;

public class CheckTask extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(CheckTask.class);

    @Override
    public void run() {
        // iterate the checks map and run the check based on the parameters in check object
        try {
            if(ControllerStartupHook.checks != null) {
                for(Check check: ControllerStartupHook.checks.values()) {
                    if(logger.isTraceEnabled()) logger.trace("id = " + check.getId() + " lastFailed = " + check.getLastFailedTimestamp() + " lastExceute = " + check.getLastExecuteTimestamp() + " interval = " + check.getInterval() + " deregAfter = " + check.getDeregisterCriticalServiceAfter());
                    if(check.getLastExecuteTimestamp() == 0) {
                        if(logger.isTraceEnabled()) logger.trace("check " + check.getId() + " first time");
                        execute(check);
                    } else if(System.currentTimeMillis() - check.getInterval() > check.getLastExecuteTimestamp()){
                        if(logger.isTraceEnabled()) logger.trace("check " + check.getId() + " interval reached");
                        execute(check);
                    } else {
                        if(logger.isTraceEnabled()) logger.trace("check " + check.getId() + " not reach interval yet");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void execute(Check check) {
        if(check.getHttp() != null) {
            boolean res = ControllerClient.checkHealth(check.getProtocol(), check.getAddress(), check.getPort(), check.getServiceId());
            if(res) {
                check.setLastExecuteTimestamp(System.currentTimeMillis());
                // whenever this is success, reset the failure flag.
                check.setLastFailedTimestamp(0L);
            } else {
                // only set the failure flag if it is not set yet.
                if(check.getLastFailedTimestamp() == 0L) check.setLastFailedTimestamp(System.currentTimeMillis());
            }
            if(check.getLastFailedTimestamp() != 0L) {
                removeNode(check);
            }
        } else {
            // ttl check so we need to check the last failed time to decide if the node should be removed.
            if(check.getLastFailedTimestamp() != 0L) {
                removeNode(check);
            } else {
                // last failed time is 0, set it to current
                check.setLastFailedTimestamp(System.currentTimeMillis());
            }
        }
    }

    private void removeNode(Check check) {
        if(System.currentTimeMillis() - check.getDeregisterCriticalServiceAfter() > check.getLastFailedTimestamp()) {
            // remove the node as it passed the de-register after.
            String key = check.getTag() == null ?  check.getServiceId() : check.getServiceId() + "|" + check.getTag();
            List nodes = (List)ControllerStartupHook.services.get(key);
            nodes = ControllerUtil.delService(nodes, check.getAddress(), check.getPort());
            if(nodes != null && nodes.size() > 0) {
                ControllerStartupHook.services.put(key, nodes);
            } else {
                ControllerStartupHook.services.remove(key);
            }
        }
    }
}
