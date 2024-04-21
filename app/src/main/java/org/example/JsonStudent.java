package org.example;

import java.util.List;

public class JsonStudent {
    String email;
    List<String> preferences;

    public JsonStudent(String email, List<String> preferences) {
        this.email = email;
        this.preferences = preferences;
    }
}