package org.example;

import java.util.List;


import java.io.Serializable;
import java.util.*;

public class Student implements Serializable, Comparable<Student> {
    public String email;
    public Map<Integer, Destination> preferences;

    public Student(String email, Map<Integer, Destination> preferences) {
        this.email = email;
        this.preferences = preferences;
    }

    public String getEmail() {
        return this.email;
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


    
    @Override
    public int hashCode() {
        return email != null ? email.hashCode() : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Student student = (Student) obj;
        return email != null ? email.equals(student.email) : student.email == null;
    }


    
    @Override
    public int compareTo(Student other) {
        return this.email.compareTo(other.email);
    }

}
