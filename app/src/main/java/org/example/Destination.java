package org.example;

import java.io.Serializable;

public class Destination implements Serializable {
    private String name;
    private int maxStudents;

    public Destination(String name, int maxStudents) {
        this.name = name;
        this.maxStudents = maxStudents;
    }

    public String getName() {
        return this.name;
    }

    public int getMaxStudents() {
        return this.maxStudents;
    }

    @Override
    public String toString() {
        return this.getName() + " " + this.getMaxStudents();
    }
}