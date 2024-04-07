package org.example;

import java.util.List;

public class Student implements Comparable<Student> {
    String email;
    List<String> preferences;

    public Student(String email, List<String> preferences) {
        this.email = email;
        this.preferences = preferences;
    }

    @Override
    public int compareTo(Student other) {
        return this.email.compareTo(other.email);
    }
}