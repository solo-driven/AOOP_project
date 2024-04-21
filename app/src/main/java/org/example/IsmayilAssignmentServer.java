package org.example;


import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;


public class IsmayilAssignmentServer extends Server {
    private String destinationsPath = "/destinations.csv";
    private List<Destination> destinations;
    private List<Student> students = Collections.synchronizedList(new ArrayList<>());
    private Population population;
    private Map<Student, String> assignments = new ConcurrentHashMap<>();
    private Map<Student, PrintWriter> studentConnections = new ConcurrentHashMap<>();

    public IsmayilAssignmentServer(int port) throws IOException {
        super(port);

        // Fill destinations from file
        fillDestinations(this.destinationsPath);

        // runAlgorithm();

        setupHandlers();
    }


    private void runAlgorithm() {
        // Initialize Population
        int populationSize = 50;
        double mutationProb = 0.10; // Choose any mutation rate
        double crossoverProb = 0.90; // Choose any crossover rate
        int maxGenerations = 10000;
        int maxNoImprovementCount = 750;
        this.population = new Population(this.destinations, this.students, populationSize, maxGenerations, maxNoImprovementCount, mutationProb, crossoverProb);
        this.population.initialize();
    }

    private Map<Student, String> getSolution() {
        // Initialize Population
        int populationSize = 50;
        double mutationProb = 0.10; // Choose any mutation rate
        double crossoverProb = 0.90; // Choose any crossover rate
        int maxGenerations = 10000;
        int maxNoImprovementCount = 750;
        this.population = new Population(this.destinations, this.students, populationSize, maxGenerations, maxNoImprovementCount, mutationProb, crossoverProb);
        this.population.initialize();
        Gene solution = this.population.evolve();
        return solution.getAssignment();
    }

    private void fillDestinations(String destinationsPath) {
        InputStream in = getClass().getResourceAsStream(this.destinationsPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        this.destinations = new ArrayList<>();
        List<String> lines = reader.lines().collect(Collectors.toList());
        for (String line : lines) {
            String[] parts = line.split(", ");
            String city = parts[1];
            int maxStudents = Integer.parseInt(parts[0]);
            Destination destination = new Destination(city, maxStudents);
            this.destinations.add(destination);
        }
    }

    protected void setupHandlers() {
        registerRoute(new Route("GET", "/destinations"), this::handleGetDestinations);
        registerRoute(new Route("POST", "/assign"), this::handleAssignment);
        registerRoute(new Route("POST", "/preferences"), this::handlePostPreferences);
        registerRoute(new Route("PUT", "/preferences"), this::handlePutPreferences);
        registerRoute(new Route("GET", "/students"), this::handleGetStudents);
        registerRoute(new Route("GET", "/assignment-stream"), this::handleAssignmentStream);
        registerRoute(new Route("GET", "/assignments"), this::handleGetAssignments);

    }

    private Response handleGetAssignments(Request req) {
        Gson gson = new Gson();
        String body = gson.toJson(assignments);

        return new RESTResponse(200, "OK", body);
    }

    private Response handleAssignmentStream(Request req) {
        System.out.println("#########################################");
        String client_email = req.queryParams.get("clientId");
        if (client_email == null) {
            return new RESTResponse(400, "Bad Request", new Message("clientId query parameter not provided."));
        }

        Student student;
        System.out.println("Students list: " + students);
        synchronized (students) {
            student = students.stream().filter(s -> s.getEmail().equals(client_email)).findFirst().orElse(null);
        }

        if (student == null) {
            return new RESTResponse(404, "Not Found", new Message("Student with email " + client_email + " not found"));
        }

        // Validate student
        Response validationResponse = validateStudent(student);
        if (validationResponse != null) {
            return validationResponse;
        }

        synchronized (studentConnections) {

            try {
                PrintWriter out = new PrintWriter(req.clientSocket.getOutputStream(), true);
                //System.out.println("handleAssignmentStream Out for " + student + " is " + out);
                studentConnections.put(student, out);
            } catch (IOException e) {
                e.printStackTrace();
                return new RESTResponse(500, "Internal Server Error", new Message("Failed to get output stream"));
            }
        }

        // Create a response with the appropriate headers for server-sent events
        SSEResponse response = new SSEResponse(200, "OK");

        return response;
    }

    private Response handleAssignment(Request req) {
        Gson gson = new Gson();
        Student student = gson.fromJson(req.body, Student.class);

        // Validate student
        Response validationResponse = validateStudent(student);
        if (validationResponse != null) {
            return validationResponse;
        }

        // if student is not in the list
        if (!students.contains(student)) {
            return new RESTResponse(404, "Not Found", new Message("Student with email " + student.getEmail()
                    + " not found. Please add student to list of preferences first."));
        }
        Map<Student, String> newAssignments;
        synchronized (this) {
            // Assign students
            newAssignments = getSolution();

            // Check if assignments have changed
            for (Map.Entry<Student, String> entry : newAssignments.entrySet()) {
                Student currentStudent = entry.getKey();
                String newAssignment = entry.getValue();
                //System.out.println( "handleAssignment Current student: " + currentStudent + " new assignment: " + newAssignment);

                String oldAssignment = assignments.get(currentStudent);
                if (oldAssignment == null || !oldAssignment.equals(newAssignment)) {
                    // Notify student of assignment change
                    PrintWriter out = studentConnections.get(currentStudent);
                    //System.out.println("handleAssignment Out for " + currentStudent + " is " + out);
                    if (out != null) {
                        //System.out.println("handleAssignment out is not null");
   
                        SSEEvent assignment = new SSEEvent("assignment", newAssignment + " to " + currentStudent.getEmail() + " by " + student.getEmail());
                        out.write(assignment.toString());
                        out.flush();


                    }

                    // Update assignment in map
                    assignments.put(currentStudent, newAssignment);
                }
            }
        }

        // Get the new assignment for the student who requested it
        String newAssignment = newAssignments.get(student);

        // Create a map with the new assignment and convert it to JSON
        Map<String, String> assignmentMap = new HashMap<>();
        assignmentMap.put("assignment", newAssignment);
        String json = gson.toJson(assignmentMap);

        return new RESTResponse(200, "OK", json);
    }

    private Response handleGetStudents(Request req) {
        Gson gson = new Gson();
        String body = gson.toJson(students);

        return new RESTResponse(200, "OK", body);
    }

    private Response handleGetDestinations(Request req) {
        Set<String> cities = new  HashSet<>();
        for (Destination destination : this.destinations) {
            cities.add(destination.getName());
        }
        System.out.println("Cities: " + cities);

        Gson gson = new Gson();
        String body = gson.toJson(cities);

        return new RESTResponse(200, "OK", body);
    }

    private Response handlePostPreferences(Request req) {
        Gson gson = new Gson();
        Student student;

        try {
            student = gson.fromJson(req.body, Student.class);
        } catch (JsonSyntaxException e) {
            return new RESTResponse(404, "Bad Request", new Message("Invalid JSON format in request body"));
        }
        // Validate student
        Response validationResponse = validateStudent(student);
        if (validationResponse != null) {
            return validationResponse;
        }
        System.out.println("Student in POST:" + student);
        synchronized (students) {
            // Student already exists
            for (Student s : students) {
                if (s.getEmail().equals(student.getEmail())) {
                    return new RESTResponse(400, "Bad Request",
                            new Message("Student with email " + student.getEmail() + " already exists"));
                }
            }

        }
        // Add student to list
        students.add(student);
        System.out.println("Students list in POST: " + students);
        // Handle POST /preferences
        return new RESTResponse(201, "Created", new Message("Student with email " + student.getEmail() + " created"));
    }

    private Response validateStudent(Student student) {
        if (student == null) {
            return new RESTResponse(400, "Bad Request", new Message("Missing body"));
        }
        if (student.getPreferences() == null) {
            return new RESTResponse(400, "Bad Request", new Message("Missing preferences"));
        }

        if (student.getEmail() == null) {
            return new RESTResponse(400, "Bad Request", new Message("Missing email"));
        }

        // no repeated preferences
        // Set<String> uniquePreferences = new HashSet<>(student.getPreferences());
        // if (uniquePreferences.size() != student.getPreferences().size()) {
        //     return new RESTResponse(400, "Bad Request", new Message("Repeated preferences"));
        // }

        // preferences must be between 0 and 5
        if (student.getPreferences().size() > 5) {
            return new RESTResponse(400, "Bad Request", new Message("Too many preferences, maximum is 5"));
        }

        // validate destination
        for (Destination destination : student.getPreferences().values()) {
            if (!destinations.contains(destination)) {
                return new RESTResponse(400, "Bad Request", new Message("Invalid destination: '" + destination.getName() + "'"));
            }
        }

        return null;
    }

    private Response handlePutPreferences(Request req) {
        Gson gson = new Gson();
        Student updatedStudent = gson.fromJson(req.body, Student.class);

        Response validationResponse = validateStudent(updatedStudent);
        if (validationResponse != null) {
            return validationResponse;
        }

        synchronized (students) {
            // Find and update student in list
            for (Student student : students) {
                if (student.getEmail().equals(updatedStudent.getEmail())) {
                    student.setPreferences(updatedStudent.getPreferences());
                    return new RESTResponse(200, "OK",
                            new Message("Student with email " + student.getEmail() + " updated"));
                }
            }
        }

        // If student not found in list
        return new RESTResponse(404, "Not Found",
                new Message("Student with email " + updatedStudent.getEmail() + " not found"));
    }

}