package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GeneticAlgorithm {
    
    public Map<Student, String> assignStudent(Student student, Map<String, Integer> destinations, Map<Student, String> assignments) {
        Map<Student, String> newAssignments = new HashMap<>(assignments);
        List<String> destinationKeys = new ArrayList<>(destinations.keySet());
        Random random = new Random();

        // Assign a random destination to the new student
        String randomDestination = destinationKeys.get(random.nextInt(destinationKeys.size()));
        newAssignments.put(student, randomDestination);

        // Assign a random destination to each existing student
        for (Student existingStudent : assignments.keySet()) {
            randomDestination = destinationKeys.get(random.nextInt(destinationKeys.size()));
            newAssignments.put(existingStudent, randomDestination);
        }

        return newAssignments;
    }
}