package org.example;

public class SSEEvent {
    String eventType;
    String eventData;

    public SSEEvent(String eventType, String eventData) {
        this.eventType = eventType;
        this.eventData = eventData;
    }

    @Override
    public String toString() {
        return "event: " + eventType + "\n" + "data: " + eventData + "\n\n";
    }
}
