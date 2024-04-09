package org.example;

import com.google.gson.JsonObject;

public class Message {
    String message;

    Message(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        JsonObject jsonMsg = new JsonObject();
        jsonMsg.addProperty("message", message);
        return jsonMsg.toString();
    }
}