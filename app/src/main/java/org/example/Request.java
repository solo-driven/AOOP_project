package org.example;

import java.io.IOException;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class Request {
    Socket clientSocket;
    Map<String, String> headers;
    Map<String, String> queryParams;
    String body;
    String method;
    String path;

    public Request(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        
        HttpParser parser = new HttpParser();
        parser.parseRequest(clientSocket.getInputStream());
        this.method = parser.method;
        this.path = extractPath(parser.URI);
        this.headers = parser.headers;
        this.body = parser.body;
        this.queryParams = parseQueryParams(parser.URI);
    }

    private String extractPath(String fullPath) {
        if (fullPath.contains("?")) {
            return fullPath.split("\\?")[0];
        }
        return fullPath;
    }

    private Map<String, String> parseQueryParams(String fullPath) throws IOException {
        Map<String, String> queryParams = new HashMap<>();
        if (fullPath.contains("?")) {
            String queryString = fullPath.split("\\?")[1];
            String[] pairs = queryString.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                queryParams.put(key, value);
            }
        }
        return queryParams;
    }
}