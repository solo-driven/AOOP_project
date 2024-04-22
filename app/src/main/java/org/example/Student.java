package org.example;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

public class Student implements Serializable {
    public String email;
    public Map<Integer, Destination> preferences;

    public Student(String email, Map<Integer, Destination> preferences) {
        if (!isValidEmail(email)) {
            throw new InvalidEmailException("Invalid email: " + email);
        }

        this.email = email;
        this.preferences = preferences;
    }

    public String getEmail() {
        return this.email;
    }

    
    public static boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        Pattern pat = Pattern.compile(emailRegex);
        if (email == null)
            return false;
        return pat.matcher(email).matches();
    }

    public Map<Integer, Destination> getPreferences() {
        return this.preferences;
    }

    public void setPreferences(Map<Integer, Destination> preferences) {
        this.preferences = preferences;
    }

    public int getRankFromDestination(Destination destination) {
        for (Map.Entry<Integer, Destination> preference : this.preferences.entrySet()) {
            // If the destination matches the current destination index
            if (preference.getValue().equals(destination)) {
                // Get the preference index
                return preference.getKey(); // No need to continue searching
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        return this.email + ": " + this.preferences;
    }
}
