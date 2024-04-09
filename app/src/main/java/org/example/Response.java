package org.example;

import java.util.HashMap;
import java.util.Map;

public class Response {
    int statusCode;
    String statusMessage;
    Map<String, String> headers;
    String body;

    Response(int statusCode, String statusMessage, String body) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.body = body;

        initHeaders();
    }

    Response(int statusCode, String statusMessage, Message msg) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.body = msg.toString();

        initHeaders();
    }


    private void initHeaders() {
        this.headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Content-Length", Integer.toString(this.body.length()));
    }



    @Override
    public String toString() {
        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage)
                .append("\r\n");

        for (Map.Entry<String, String> header : headers.entrySet()) {
            responseBuilder.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        responseBuilder.append("\r\n").append(body);

        return responseBuilder.toString();
    }
}