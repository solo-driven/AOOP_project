package org.geneticalgorithm;

import java.util.*;

public class Gene {
    private int[] gene;
    private List<Student> students;
    private List<Destination> destinations;
    private HelperMethods HelperMethods; // Helper methods for gene manipulation
    private final int length;

    public Gene(List<Student> students, List<Destination> destinations) {
        this.students = students;
        this.destinations = destinations;
        this.length = this.students.size() * this.destinations.size();
        this.gene = new int[length];
        this.HelperMethods = new HelperMethods(this.gene, this.students, this.destinations);
    }

    public int[] getGene() {
        return this.gene;
    }

    public HelperMethods getHelperMethods() {
        return this.HelperMethods;
    }

    // Method to display the assignment of students to destinations in server for debugging purposes
    public void showAssignment() {
        // Retrieve assignment map
        Map<Student, String> assignmentMap = getAssignment();
        // Iterate over assignment map and display assignments
        for (Map.Entry<Student, String> assignment : assignmentMap.entrySet()) {
            Student student = assignment.getKey();
            String destination = assignment.getValue();
            System.out.println(student.getEmail() + " is assigned to destination " + destination);
        }
    }

    // Method to get the assignment map of students to destinations to use in server
    public Map<Student, String> getAssignment() {
        Map<Student,String> assignmentMap = new HashMap<>();
        // Iterate over students to find their assigned destinations
        for (int studentIndex=0; studentIndex < this.students.size(); studentIndex++) {
            Student student = this.students.get(studentIndex);
            int destinationIndex = this.HelperMethods.findAssignedDestination(studentIndex);
            Destination destination = null;
            // If destination is assigned, add to assignment map
            if (destinationIndex != -1) {
                destination = this.destinations.get(destinationIndex);
                assignmentMap.put(student, destination.getName());
            }
        }
        return assignmentMap;
    }

    // Method to calculate the fitness of the gene
    public int calculateFitness() {
        int totalCost = 0;

        // Iterate over the gene array
        for (int i = 0; i < this.gene.length; i++) {
            if (this.gene[i] == 1) {
                // Calculate the student index and destination index
                int index = i % this.students.size();
                int destinationIndex = i / this.students.size();

                // Get the corresponding student and destination
                Student student = this.students.get(index);
                Destination destination = this.destinations.get(destinationIndex);
                // Get the student's preferences
                Map<Integer, Destination> preferences = student.getPreferences();

                // Calculate the cost based on the preferences
                if (preferences.containsValue(destination)) {
                    // Cost for the i-th choice
                    int choice = student.getRankFromDestination(destination); // preference index
                    totalCost += choice * choice;
                } else {
                    // Cost for not chosen destination
                    int Nd = preferences.size(); // Nd <= 5, number of preferences
                    totalCost += 10 * Nd * Nd;
                }
            }
        }
        return totalCost;
    }

    // Method to perform mutation on the gene based on mutation probability
    public void mutate(double mutationProb) {
        Random random = new Random();
        // Check if mutation should occur based on mutation probability
        if (random.nextDouble() <= mutationProb) {
            // Randomly select mutation type
            if (random.nextDouble() >= 0.5)
                swapMutate();
            else
                bitFlipMutate();
        }
    }

    // Method to perform crossover with another gene
    public Gene crossover(Gene partner) {
        Gene child = new Gene(this.students, this.destinations);
        Random random = new Random();
        // Randomly select crossover point
        int crossoverPoint = random.nextInt(this.students.size());
        // Perform crossover
        for (int i = 0; i < this.length; i++) {
            int studentIndex = i % this.students.size();
            // Choose gene from parent based on crossover point
            if (studentIndex > crossoverPoint)
                child.getGene()[i] = this.gene[i];
            else
                child.getGene()[i] = partner.getGene()[i];
        }
        // Remove exceeding destination capacity from child gene
        child.getHelperMethods().removeExceedingDestinationCapacity();
        return child;
    }

    // Method for swap mutation
    private void swapMutate() {
        Pair twoStudentIndices = this.HelperMethods.getUniqueStudentIndices();
        int firstStudentIndex = twoStudentIndices.getFirst();
        int secondStudentIndex = twoStudentIndices.getSecond();
        // Ensure distinct student indices
        while (firstStudentIndex == secondStudentIndex)
            secondStudentIndex = this.HelperMethods.getUniqueStudentIndices().getSecond();
        // Perform swap mutation
        if (secondStudentIndex != -1)
            this.HelperMethods.swapStudents(firstStudentIndex, secondStudentIndex);
    }

    // Method for bit flip mutation
    private void bitFlipMutate() {
        Random random = new Random();
        // Randomly select student index
        int studentIndex = random.nextInt(this.students.size());
        int currentDestinationIndex = this.HelperMethods.findAssignedDestination(studentIndex);
        int currentIndex = this.HelperMethods.calculateIndex(studentIndex, currentDestinationIndex);
        // Find another available destination for the selected student
        int newDestinationIndex = this.HelperMethods.chooseAvailableDestination();
        // Update gene to assign student to new destination
        if (newDestinationIndex != -1) {
            int newIndex = this.HelperMethods.calculateIndex(studentIndex, newDestinationIndex);
            this.gene[currentIndex] = 0;
            this.gene[newIndex] = 1;
        }
    }
}