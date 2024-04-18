package org.example;

import java.util.HashMap;

public class SSEResponse extends Response {
    public SSEResponse(int statusCode, String statusMessage) {
        super(statusCode, statusMessage, "");
        initHeaders();

    }
 
    private void initHeaders() {
        this.headers = new HashMap<>();
        this.headers.put("Content-Type", "text/event-stream");
        this.headers.put("Cache-Control", "no-cache");
        this.headers.put("Connection", "keep-alive");
    }

}