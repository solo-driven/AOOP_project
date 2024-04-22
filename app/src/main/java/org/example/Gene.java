package org.example;

import java.util.*;

public class Gene {
    private int[] gene;
    private List<Student> students;
    private List<Destination> destinations;
    private final int length;

    public Gene(int length) {
        this.length = length;
        this.gene = new int[length];
    }

    public Gene(List<Student> students, List<Destination> destinations) {
        this.students = students;
        this.destinations = destinations;
        this.length = this.students.size() * this.destinations.size();
        this.gene = new int[length];
    }

    public int[] getGene() {
        return this.gene;
    }
    public int getLength() {
        return this.length;
    }

    public int chooseDestinationFromPreference(Student student) {
        Map<Integer, Destination> preferences = student.getPreferences();
        Random random = new Random();
        int currentStudents, maxStudents, destinationIndex;
        Destination destination;

        // List to keep track of visited destinations
        List<Destination> visitedDestinations = new ArrayList<>();
        do {
            int preferenceIndex = random.nextInt(preferences.size());
            destination = preferences.get(preferenceIndex);
            destinationIndex = this.destinations.indexOf(destination);

            // Check if the destination has already been visited
            if (visitedDestinations.contains(destination)) {
                continue; // Skip if already visited
            }

            // Update the list of visited destinations
            visitedDestinations.add(destination);

            // Get max and current students for the destination
            maxStudents = destination.getMaxStudents();
            currentStudents = countStudentsForDestination(destinationIndex);

            // If the destination is not full, return its index
            if (currentStudents < maxStudents) {
                return destinationIndex;
            }
        } while (visitedDestinations.size() < preferences.size()); // Continue until all preferences are visited

        // If all preferences are visited and none of them are available, assign to a random available destination
        return chooseAvailableDestination();
    }

    private int findAssignedDestination(int studentIndex) {
        for (int i = 0; i < this.destinations.size(); i++) {
            int startIndex = i * this.students.size();
            if (this.gene[startIndex + studentIndex] == 1) {
                return i;
            }
        }
        return -1;
    }

    private List<Student> findAssignedStudents(int destinationIndex) {
        List<Student> assignedStudents = new ArrayList<>();
        int startIndex = destinationIndex * this.students.size();
        int endIndex = startIndex + this.students.size();
        for (int index = startIndex; index < endIndex; index++) {
            if (this.gene[index] == 1) {
               int studentIndex = index % this.students.size();
               Student student = this.students.get(studentIndex);
               assignedStudents.add(student);
            }
        }
        return assignedStudents;
    }

    private int chooseAvailableDestination() {
        Random random = new Random();
        int destinationIndex;
        List<Destination> availableDestinations = getAvailableDestinations();
        if (!availableDestinations.isEmpty()) {
            int randomIndex = random.nextInt(availableDestinations.size());
            Destination destination = availableDestinations.get(randomIndex);
            destinationIndex = this.destinations.indexOf(destination);
            return destinationIndex;
        }
        return -1;
    }

    private List<Destination> getAvailableDestinations() {
        List<Destination> availableDestinations = new ArrayList<>();

        for (Destination destination : this.destinations) {
            int destinationIndex = this.destinations.indexOf(destination);
            int currentStudents = countStudentsForDestination(destinationIndex);
            int maxStudents = destination.getMaxStudents();

            if (currentStudents < maxStudents) {
                availableDestinations.add(destination);
            }
        }

        return availableDestinations;
    }

    public Map<Destination, Integer> countStudentsForAllDestinations() {
        Map<Destination, Integer> destinationStudentsMap = new HashMap<>();
        for (int destinationIndex = 0; destinationIndex < this.destinations.size(); destinationIndex++) {
            Destination destination = this.destinations.get(destinationIndex);
            int numOfStudents = countStudentsForDestination(destinationIndex);
            destinationStudentsMap.put(destination, numOfStudents);
        }
        return destinationStudentsMap;
    }

    private int countStudentsForDestination(int destinationIndex) {
        int count = 0;
        int startIndex = destinationIndex * this.students.size();
        int endIndex = startIndex + this.students.size();

        for (int i = startIndex; i < endIndex; i++) {
            if (this.gene[i] == 1) {
                count++;
            }
        }
        return count;
    }

    public void showAssignment() {
        Map<Student, String> assignmentMap = getAssignment();
        for (Map.Entry<Student, String> assignment : assignmentMap.entrySet()) {
            Student student = assignment.getKey();
            String city = assignment.getValue();
            System.out.println(student.getEmail() + " is assigned to destination " + city);
        }
    }

    public Map<Student, String> getAssignment() {
        Map<Student,String> assignmentMap = new HashMap<>();
        for (int studentIndex=0; studentIndex < this.students.size(); studentIndex++) {
            Student student = this.students.get(studentIndex);
            int destinationIndex = findAssignedDestination(studentIndex);
            Destination destination = null;
            if (destinationIndex != -1) {
                destination = this.destinations.get(destinationIndex);
                assignmentMap.put(student, destination.getName());
            }
        }
        return  assignmentMap;
    }

    private void removeExceedingDestinationCapacity() {
        Map<Destination, Integer> destinationStudentsMap = countStudentsForAllDestinations();
        for (Map.Entry<Destination, Integer> entry : destinationStudentsMap.entrySet()) {
            Destination destination = entry.getKey();
            int currentDestinationIndex = this.destinations.indexOf(destination);
            int currentStudents = entry.getValue();
            int maxStudents = destination.getMaxStudents();
            while (currentStudents > maxStudents) {
                Random random = new Random();
                List<Student> assignedStudents = findAssignedStudents(currentDestinationIndex);
                Student randomAssignedStudent = assignedStudents.get(random.nextInt(assignedStudents.size()));
                int studentIndex = this.students.indexOf(randomAssignedStudent);
                int newDestinationIndex = chooseAvailableDestination();
                int currentIndex = calculateIndex(studentIndex, currentDestinationIndex);
                int newIndex = calculateIndex(studentIndex, newDestinationIndex);
                this.gene[currentIndex] = 0;
                this.gene[newIndex] = 1;
                currentStudents = countStudentsForDestination(currentDestinationIndex);
            }
        }
    }

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

    public void mutate(double mutationProb) {
        Random random = new Random();
        if (random.nextDouble() <= mutationProb) {
            if (random.nextDouble() >= 0.5)
                swapMutate();
            else
                bitFlipMutate();
        }
    }

    public Gene crossover(Gene partner) {
        Gene child = new Gene(this.students, this.destinations);
        Random random = new Random();
        int crossoverPoint = random.nextInt(this.students.size());
        for (int i = 0; i < this.length; i++) {
            int studentIndex = i % this.students.size();
            if (studentIndex > crossoverPoint)
                child.getGene()[i] = this.gene[i];
            else
                child.getGene()[i] = partner.getGene()[i];
        }
        child.removeExceedingDestinationCapacity();
        return child;
    }

    private void swapMutate() {
        Pair twoStudentIndices = getUniqueStudentIndices();
        int firstStudentIndex = twoStudentIndices.getFirst();
        int secondStudentIndex = twoStudentIndices.getSecond();
        while (firstStudentIndex == secondStudentIndex)
            secondStudentIndex = getUniqueStudentIndices().getSecond();
        if (secondStudentIndex != -1)
            swapStudents(firstStudentIndex, secondStudentIndex);
    }

    private void bitFlipMutate() {
        Random random = new Random();
        int studentIndex = random.nextInt(this.students.size());
        int currentDestinationIndex = findAssignedDestination(studentIndex);
        int currentIndex = calculateIndex(studentIndex, currentDestinationIndex);
        // Find another available destination for the selected student
        int newDestinationIndex = chooseAvailableDestination();
        if (newDestinationIndex != -1) {
            // Update the gene to assign the student to the new destination
            int newIndex = calculateIndex(studentIndex, newDestinationIndex);
            this.gene[currentIndex] = 0;
            this.gene[newIndex] = 1;
        }
    }

    private void swapStudents(int studentIndexA, int studentIndexB) {
        int destinationIndexA = findAssignedDestination(studentIndexA);
        int destinationIndexB = findAssignedDestination(studentIndexB);

        int currentIndexA = calculateIndex(studentIndexA, destinationIndexA);
        int currentIndexB = calculateIndex(studentIndexB, destinationIndexB);

        int newIndexA = calculateIndex(studentIndexA, destinationIndexB);
        this.gene[currentIndexA] = 0;
        this.gene[newIndexA] = 1;

        int newIndexB = calculateIndex(studentIndexB, destinationIndexA);
        this.gene[currentIndexB] = 0;
        this.gene[newIndexB] = 1;
    }

    private int calculateIndex(int studentIndex, int destinationIndex) {
        return studentIndex + destinationIndex * this.students.size();
    }

    private Pair getUniqueStudentIndices() {
        Random random = new Random();
        int indexA = random.nextInt(this.students.size());
        int indexB = (this.students.size() == 1) ? -1 : random.nextInt(this.students.size());
        while (indexA == indexB)
            indexB = random.nextInt(this.students.size());
        return new Pair(indexA, indexB);
    }
}