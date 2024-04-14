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
    private Map<Student, String> assignments = new HashMap<>();
    private Map<Student, PrintWriter> studentConnections = new HashMap<>(); 
    


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
        registerRoute(new Route("POST", "/assign"), this::handleAssignment);
        registerRoute(new Route("POST", "/preferences"), this::handlePostPreferences);
        registerRoute(new Route("PUT", "/preferences"), this::handlePutPreferences);
        registerRoute(new Route("GET", "/students"), this::handleGetStudents);
        registerRoute(new Route("GET", "/assignment-stream"), this::handleAssignmentStream);
        
    }

    private Response handleAssignmentStream(Request req) {
        String client_email = req.queryParams.get("clientId");
        if (client_email == null) {
            return new RESTResponse(400, "Bad Request", new Message("clientId query parameter not provided."));
        }

        Student student = students.stream().filter(s -> s.email.equals(client_email)).findFirst().orElse(null);
        if (student == null) {
            return new RESTResponse(404, "Not Found", new Message("Student with email " + client_email + " not found"));
        }
    
        // Validate student
        Response validationResponse = validateStudent(student);
        if (validationResponse != null) {
            return validationResponse;
        }
    
        try {
            PrintWriter out = new PrintWriter(req.clientSocket.getOutputStream(), true);
            studentConnections.put(student, out);
        } catch (IOException e) {
            e.printStackTrace();
            return new RESTResponse(500, "Internal Server Error", new Message("Failed to get output stream"));
        }
    
        // Create a response with the appropriate headers for server-sent events
        Response response = new Response(200, "OK", "");
        response.headers.put("Content-Type", "text/event-stream");
        response.headers.put("Cache-Control", "no-cache");
        response.headers.put("Connection", "keep-alive");
    

        return response;
    }
    private Response handleGetStudents(Request req) {
        Gson gson = new Gson();
        String body = gson.toJson(students);
    
        return new RESTResponse(200, "OK", body);
    }

    private Response handleGetDestinations(Request req) {
        Set<String> cities = this.destinations.keySet();

        Gson gson = new Gson();
        String body = gson.toJson(cities);


        return new RESTResponse(200, "OK",  body);
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
            return new RESTResponse(404, "Not Found", new Message("Student with email " + student.email + " not found. Please add student to list of preferences first."));
        }

        Map<Student, String> newAssignments;
        synchronized(this) {
            // Assign students
            newAssignments = this.geneticAlgorithm.assignStudent(student, this.destinations, this.assignments);
        
            // Check if assignments have changed
            for (Map.Entry<Student, String> entry : newAssignments.entrySet()) {
                Student currentStudent = entry.getKey();
                String newAssignment = entry.getValue();
    
                String oldAssignment = assignments.get(currentStudent);
                if (oldAssignment == null || !oldAssignment.equals(newAssignment)) {
                    // Notify student of assignment change
                    PrintWriter out = studentConnections.get(currentStudent);
                    if (out != null) {
                        out.println("event: assignment\n");
                        out.println("data: " + newAssignment + "\n\n");
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

    private Response handlePostPreferences(Request req) {
        Gson gson = new Gson();
        Student student = gson.fromJson(req.body, Student.class);

        // Validate student
        Response validationResponse = validateStudent(student);
        if (validationResponse != null) {
            return validationResponse;
        }

        // Student already exists
        for (Student s : students) {
            if (s.email.equals(student.email)) {
                return new RESTResponse(400, "Bad Request", new Message("Student with email " + student.email + " already exists"));
            }
        }

        // Add student to list
        students.add(student);

        // Handle POST /preferences
        return new RESTResponse(201, "Created", new Message("Student with email " + student.email + " created"));
    }


    private Response validateStudent(Student student) {
        if (student == null) {
            return new RESTResponse(400, "Bad Request", new Message("Missing body"));
        }
        if (student.preferences == null) {
            return new RESTResponse(400, "Bad Request",new Message("Missing preferences"));
        }
    
        if (student.email == null) {
            return new RESTResponse(400, "Bad Request", new Message("Missing email"));
        }
    
        // no repeated preferences
        Set<String> uniquePreferences = new HashSet<>(student.preferences);
        if (uniquePreferences.size() != student.preferences.size()) {
            return new RESTResponse(400, "Bad Request", new Message("Repeated preferences"));
        }
    
        // preferences must be between 0 and 5
        if (student.preferences.size() > 5) {
            return new RESTResponse(400, "Bad Request", new Message("Too many preferences, maximum is 5"));
        }
    
        // validate destination
        for (String destination : student.preferences) {
            if (!destinations.containsKey(destination)) {
                return new RESTResponse(400, "Bad Request", new Message("Invalid destination: '" + destination + "'"));
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

        // Find and update student in list
        for (Student student : students) {
            if (student.email.equals(updatedStudent.email)) {
                student.preferences = updatedStudent.preferences;
                return new RESTResponse(200, "OK", 
                        new Message("Student with email " + student.email + " updated"));
            }
        }

        // If student not found in list
        return new RESTResponse(404, "Not Found",
                new Message("Student with email " + updatedStudent.email + " not found"));
    }

}