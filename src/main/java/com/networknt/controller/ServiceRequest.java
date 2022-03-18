package com.networknt.controller;

import com.networknt.client.Http2Client;
import com.networknt.client.oauth.Jwt;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.monad.Result;
import com.networknt.status.HttpStatus;
import com.networknt.utility.StringUtils;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServiceRequest {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRequest.class);
    private static final Http2Client client = Http2Client.getInstance();
    private static final OptionMap optionMap = OptionMap.create(UndertowOptions.ENABLE_HTTP2, true);
    private static final ControllerConfig config = (ControllerConfig) Config.getInstance().getJsonObjectConfig(ControllerConfig.CONFIG_NAME, ControllerConfig.class);
    private static final int UNUSUAL_STATUS_CODE = 300;

    private String protocol;
    private String address;
    private String port;
    private String path;
    private int statusCode;
    private HttpString method;
    private ClientConnection connection;
    private String requestBody;
    private Map<String, String> pathParams;
    private Map<String, String> queryParams;
    private AtomicReference<ClientResponse> responseReference;
    private long requestTime;
    private long responseTime;

    public static class Builder {

        private final String protocol;
        private final String address;
        private final String port;
        private String path;
        private String requestBody;
        private final HttpString method;
        private final ClientConnection connection;
        private final Map<String, String> pathParams = new HashMap<>();
        private final Map<String, String> queryParams = new HashMap<>();

        private final static String PATH_PARAM_PATTERN = "[{]+[a-zA-Z-0-9]+[}]";

        public Builder(String protocol, String address, String port, HttpString method) {
            this.protocol = protocol;
            this.method = method;
            this.port = port;
            this.address = address;
            this.connection = establishBaseConnection(protocol, address, port);
        }

        /**
         * Add a path param to our request.
         *
         * @param k - the key name of the param.
         * @param v - the value of the param.
         * @return - this.
         */
        public Builder addPathParam(String k, Object v) {
            if (v != null) {
                this.pathParams.put(k, v.toString());
            }
            return this;
        }

        /**
         * Add a query param to our request.
         *
         * @param k - the key name of the param.
         * @param v - the value of the param
         * @return - this.
         */
        public Builder addQueryParam(String k, Object v) {
            if (v != null) {
                this.queryParams.put(k, v.toString());
            }
            return this;
        }

        /**
         * Bring all parts of our request uri together.
         * solve our base path first, then append our query params.
         *
         * @param inPath - The endpoint given to the builder.
         * @return - this.
         */
        public Builder buildFullPath(String inPath) {
            String solvedBasePath = this.getSolvedBasePath(inPath);
            String solvedQueryParams = this.getSolvedQueryParams();
            this.path = solvedBasePath + solvedQueryParams;
            return this;
        }

        /**
         * Get the base url of our request (protocol + address + port).
         *
         * @param inProtocol - given protocol.
         * @param inAddress  - given address.
         * @param inPort     - given port.
         * @return - string base path.
         */
        private static String getBaseUrl(String inProtocol, String inAddress, String inPort) {
            StringBuilder url = new StringBuilder();

            if (inProtocol != null) {
                url.append(inProtocol);
                url.append("://");
            }

            url.append(inAddress);

            if (inPort != null) {
                url.append(":");
                url.append(inPort);
            }
            return url.toString();
        }

        private static ClientConnection establishBaseConnection(String inProtocol, String inAddress, String inPort) {
            if(logger.isTraceEnabled()) logger.trace("inProtocol = " + inProtocol + " inAddress = " + inAddress + " inPort = " + inPort);
            ClientConnection connection = null;
            try {
                URI uri = new URI(getBaseUrl(inProtocol, inAddress, inPort));

                if ("https".equals(inProtocol)) {
                    connection = client.borrowConnection(uri, Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, optionMap).get();
                } else {
                    connection = client.borrowConnection(uri, Http2Client.WORKER, Http2Client.BUFFER_POOL, OptionMap.EMPTY).get();
                }

            } catch (URISyntaxException e) {
                logger.error("URI Exception occurred", e);
            } catch (IOException e) {
                logger.error("Exception occurred while creating a client connection", e);
            }
            return connection;
        }

        private String getSolvedBasePath(String inPath) {
            String rawPath = inPath;

            if (!this.pathParams.isEmpty() && rawPath.contains("{")) {
                Matcher m = Pattern.compile(PATH_PARAM_PATTERN).matcher(rawPath);

                while (m.find()) {
                    String match = m.group();
                    String pathParamValue = this.pathParams.get(match);
                    if (pathParamValue != null) {
                        rawPath = rawPath.replace(match, pathParamValue);
                    }
                }
            }
            return rawPath;
        }

        /**
         * Build the query param part of our request.
         * For each query param, add ?keyname + = + value to our uri.
         *
         * @return - query string for uri.
         */
        private String getSolvedQueryParams() {
            StringBuilder queryParamBuilder = new StringBuilder();
            if (!this.queryParams.isEmpty()) {
                AtomicInteger i = new AtomicInteger(0);
                this.queryParams.forEach((k, v) -> {
                    if (i.get() == 0) {
                        queryParamBuilder.append("?");
                    } else {
                        queryParamBuilder.append("&");
                    }
                    queryParamBuilder.append(k);
                    queryParamBuilder.append("=");
                    queryParamBuilder.append(v);
                    i.getAndIncrement();
                });
            }
            return queryParamBuilder.toString();
        }

        public Builder withRequestBody(Object requestBody) {
            if (requestBody != null) {
                this.requestBody = JsonMapper.toJson(requestBody);
            } else {
                this.requestBody = null;
            }
            return this;
        }

        public ServiceRequest build() {
            ServiceRequest serviceRequest = new ServiceRequest();
            serviceRequest.address = this.address;
            serviceRequest.port = this.port;
            serviceRequest.path = this.path;
            serviceRequest.protocol = this.protocol;
            serviceRequest.requestBody = this.requestBody;
            serviceRequest.queryParams = this.queryParams;
            serviceRequest.pathParams = this.pathParams;
            serviceRequest.method = this.method;
            serviceRequest.connection = this.connection;
            return serviceRequest;
        }
    }

    /**
     * Send our built request. We do not confirm results here.
     */
    public void sendRequest() {
        AtomicReference<ClientResponse> reference = null;
        this.requestTime = System.currentTimeMillis();

        try {
            reference = send(this.connection, this.method, this.path, config.getBootstrapToken(), this.requestBody);

            if (reference.get() != null) {
                int statusCode = reference.get().getResponseCode();
                if (statusCode >= UNUSUAL_STATUS_CODE) {
                    logger.error("Error: {} : {}", statusCode, reference.get().getAttachment(Http2Client.RESPONSE_BODY));
                }
            }
        } catch (Exception e) {
            logger.error("Request exception", e);
        } finally {

            if (reference != null) {
                this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
                client.returnConnection(connection);
            }
        }
        this.responseTime = System.currentTimeMillis();
        this.responseReference = reference;
    }

    /**
     * send to service from controller
     *
     * @param connection client connection
     * @param path       path to send to controller
     * @param token      token to put in header
     * @param method     method of the request
     * @param json       json body
     * @return AtomicReference{ClientResponse} response
     * @throws InterruptedException int. exception
     */
    @SuppressWarnings("rawtypes")
    private static AtomicReference<ClientResponse> send(ClientConnection connection, HttpString method, String path, String token, String json) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        ClientRequest request = new ClientRequest().setMethod(method).setPath(path);

        // add host header for HTTP/1.1 server when HTTP is used.
        request.getRequestHeaders().put(Headers.HOST, "localhost");

        // TODO: Does this save dynamic token? where does it go?
        if (config.isDynamicToken()) {
            Result result = client.addCcToken(request);

            if (result.isFailure()) {
                logger.error(result.getError().toString());
            } else {

                if (logger.isTraceEnabled())
                    logger.trace("Dynamic token  = " + ((Jwt) result.getResult()).getJwt());
            }
        } else {

            if (logger.isTraceEnabled())
                logger.trace("Static token = " + token);

            if (token != null)
                request.getRequestHeaders().put(Headers.AUTHORIZATION, "Bearer " + token);

        }

        if (StringUtils.isBlank(json)) {
            connection.sendRequest(request, client.createClientCallback(reference, latch));
        } else {
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, json));
        }
        latch.await(config.getClientTimeout(), TimeUnit.MILLISECONDS);
        return reference;
    }

    public String getResponseBody() {
        String res = "{}";

        if (this.responseReference != null && this.responseReference.get() != null) {
            res = responseReference.get().getAttachment(Http2Client.RESPONSE_BODY);
            logger.info("{}", this.getResponseTime());
        }
        return res;
    }

    public int getStatusCode() {
        if (this.responseReference != null && this.responseReference.get() != null) {
            this.statusCode = responseReference.get().getResponseCode();
        }
        return this.statusCode;
    }

    public long getResponseTime() {
        long totalTime = 0L;

        if (this.responseReference != null && this.responseReference.get() != null) {
            totalTime = this.responseTime - this.requestTime;
        }
        return totalTime;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getAddress() {
        return address;
    }

    public String getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public HttpString getMethod() {
        return method;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public Map<String, String> getPathParams() {
        return pathParams;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public AtomicReference<ClientResponse> getResponseReference() {
        return responseReference;
    }
}
