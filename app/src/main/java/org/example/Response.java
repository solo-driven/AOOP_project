package org.example;

import java.util.HashMap;
import java.util.Map;

public class Response {
    int statusCode;
    String statusMessage;
    Map<String, String> headers = new HashMap<>();
    String body;
    String separator = "\r\n";

    Response(int statusCode, String statusMessage, String body) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.body = body;
    }

    Response(int statusCode, String statusMessage, Message msg) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.body = msg.toString();
    }

    @Override
    public String toString() {
        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage)
                .append(separator);

        for (Map.Entry<String, String> header : headers.entrySet()) {
            responseBuilder.append(header.getKey()).append(": ").append(header.getValue()).append(separator);
        }
        responseBuilder.append(separator).append(body);

        return responseBuilder.toString();
    }
}