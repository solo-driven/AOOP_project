package org.geneticalgorithm;
import java.io.Serializable;

// Destination class containing  the destination name and max number of students it can possibly have
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Destination destination = (Destination) obj;
        return this.getName().equals(destination.getName()) && this.getMaxStudents() == destination.getMaxStudents();
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode() + this.getMaxStudents();
    }
}