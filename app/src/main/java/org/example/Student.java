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

    @Override
    public String toString() {
        return email;
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
}