package org.geneticalgorithm;

import java.util.*;

public class HelperMethods {
  private int[] gene;
  private List<Student> students;
  private List<Destination> destinations;

  public HelperMethods(int[] gene, List<Student> students, List<Destination> destinations) {
    this.gene = gene;
    this.students = students;
    this.destinations = destinations;
  }

  // Method to choose a random destination based on student preferences
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

  // Method to find the destination index assigned to a specific student
  public int findAssignedDestination(int studentIndex) {
      for (int i = 0; i < this.destinations.size(); i++) {
          int startIndex = i * this.students.size();
          if (this.gene[startIndex + studentIndex] == 1) {
              return i;
          }
      }
      return -1;
  }

  // Method to find all students assigned to a specific destination
  public List<Student> findAssignedStudents(int destinationIndex) {
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

  // Method to choose an available destination randomly from the available destinations list
  public int chooseAvailableDestination() {
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

  // Method to get available destinations based on number of assigned students
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

  // Method to count all students assigned to each destination
  public Map<Destination, Integer> countStudentsForAllDestinations() {
    Map<Destination, Integer> destinationStudentsMap = new HashMap<>();
    for (int destinationIndex = 0; destinationIndex < this.destinations.size(); destinationIndex++) {
        Destination destination = this.destinations.get(destinationIndex);
        int numOfStudents = countStudentsForDestination(destinationIndex);
        destinationStudentsMap.put(destination, numOfStudents);
    }
    return destinationStudentsMap;
  }

  // Method to get number of students assigned to a specific destination
  public int countStudentsForDestination(int destinationIndex) {
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

  // Method to remove students from destinations exceeding capacity resulting from crossover operation
  public void removeExceedingDestinationCapacity() {
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

  // Method to swap students between two destinations
  public void swapStudents(int studentIndexA, int studentIndexB) {
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

  // Method to calculate index of the given student in the given destination
  public int calculateIndex(int studentIndex, int destinationIndex) {
    return studentIndex + destinationIndex * this.students.size();
  }

  // Method to get unique student indices for mutation
  public Pair getUniqueStudentIndices() {
    Random random = new Random();
    int indexA = random.nextInt(this.students.size());
    int indexB = (this.students.size() == 1) ? -1 : random.nextInt(this.students.size());
    while (indexA == indexB)
      indexB = random.nextInt(this.students.size());
    return new Pair(indexA, indexB);
  }
}