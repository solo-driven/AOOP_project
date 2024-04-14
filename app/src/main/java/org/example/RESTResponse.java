package org.example;

public class RESTResponse extends Response {
    
    public RESTResponse(int statusCode, String statusMessage, String body) {
        super(statusCode, statusMessage, body);
        initRESTHeaders();
    }

    public RESTResponse(int statusCode, String statusMessage, Message msg) {
        super(statusCode, statusMessage, msg);
        initRESTHeaders();
    }

    private void initRESTHeaders() {
        this.headers.put("Content-Type", "application/json");
        this.headers.put("Content-Length", Integer.toString(this.body.length()));
    }
}
