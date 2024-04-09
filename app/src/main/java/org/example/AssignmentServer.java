package org.example;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;


public class AssignmentServer extends Server{
    private String destinations_path = "/destinations.csv";
    private Map<String, Integer> destinations;
    private GeneticAlgorithm geneticAlgorithm;
    private List<Student> students = new ArrayList<>();


    public AssignmentServer(int port) throws IOException {
        super(port);
        this.geneticAlgorithm = new GeneticAlgorithm();

        // Get destinations from file
        InputStream in = getClass().getResourceAsStream(this.destinations_path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        this.destinations = new HashMap<>();
        List<String> lines = reader.lines().collect(Collectors.toList());
        for (String line : lines) {
            String[] parts = line.split(", ");

            String city = parts[1];
            int students = Integer.parseInt(parts[0]);
            this.destinations.put(city, students);

        }

        setupHandlers();
    }

    protected void setupHandlers() {
        registerRoute(new Route("GET", "/destinations"), this::handleGetDestinations);
        registerRoute(new Route("POST", "/assign-student"), this::handleGetAssignment);
        registerRoute(new Route("POST", "/preferences"), this::handlePostPreferences);
        registerRoute(new Route("PUT", "/preferences"), this::handlePutPreferences);
        registerRoute(new Route("GET", "/students"), this::handleGetStudents);
    }

    private Response handleGetStudents(String requestBody) {
        Gson gson = new Gson();
        String body = gson.toJson(students);
    
        return new Response(200, "OK", body);
    }

    private Response handleGetDestinations(String requestBody) {
        Set<String> cities = this.destinations.keySet();

        Gson gson = new Gson();
        String body = gson.toJson(cities);


        return new Response(200, "OK",  body);
    }

    private Response handleGetAssignment(String requestBody) {
        return new Response(200, "OK", new Message("Response for GET /assignment"));
    }

    private Response handlePostPreferences(String requestBody) {
        Gson gson = new Gson();
        System.err.println(requestBody);
        Student student = gson.fromJson(requestBody, Student.class);

        // Validate student
        Response validationResponse = validateStudent(student);
        if (validationResponse != null) {
            return validationResponse;
        }

        // Student already exists
        for (Student s : students) {
            if (s.email.equals(student.email)) {
                return new Response(400, "Bad Request", new Message("Student with email " + student.email + " already exists"));
            }
        }

        // Add student to list
        students.add(student);

        // Handle POST /preferences
        return new Response(201, "Created", new Message("Student with email " + student.email + " created"));
    }


    private Response validateStudent(Student student) {
        if (student == null) {
            return new Response(400, "Bad Request", new Message("Missing body"));
        }
        if (student.preferences == null) {
            return new Response(400, "Bad Request",new Message("Missing preferences"));
        }
    
        if (student.email == null) {
            return new Response(400, "Bad Request", new Message("Missing email"));
        }
    
        // no repeated preferences
        Set<String> uniquePreferences = new HashSet<>(student.preferences);
        if (uniquePreferences.size() != student.preferences.size()) {
            return new Response(400, "Bad Request", new Message("Repeated preferences"));
        }
    
        // preferences must be between 0 and 5
        if (student.preferences.size() > 5) {
            return new Response(400, "Bad Request", new Message("Too many preferences, maximum is 5"));
        }
    
        // validate destination
        for (String destination : student.preferences) {
            if (!destinations.containsKey(destination)) {
                return new Response(400, "Bad Request", new Message("Invalid destination: '" + destination + "'"));
            }
        }
    
        return null;
    }

    private Response handlePutPreferences(String requestBody) {
        Gson gson = new Gson();
        Student updatedStudent = gson.fromJson(requestBody, Student.class);

        Response validationResponse = validateStudent(updatedStudent);
        if (validationResponse != null) {
            return validationResponse;
        }

        // Find and update student in list
        for (Student student : students) {
            if (student.email.equals(updatedStudent.email)) {
                student.preferences = updatedStudent.preferences;
                return new Response(200, "OK", 
                        "Student with email " + student.email + " updated");
            }
        }

        // If student not found in list
        return new Response(404, "Not Found",
                "Student with email " + updatedStudent.email + " not found");
    }

}