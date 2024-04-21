package org.example;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;


public class AssignmentServer extends Server {
    private String destinations_path = "/destinations.csv";
    private Set<Destination> destinations;
    private GeneticAlgorithm geneticAlgorithm;
    private Set<Student> students = Collections.synchronizedSet(new LinkedHashSet<>());
    private Map<Student, String> assignments = new ConcurrentHashMap<>();
    private Map<Student, PrintWriter> studentConnections = new ConcurrentHashMap<>();

    public AssignmentServer(int port) throws IOException {
        super(port);
        this.geneticAlgorithm = new GeneticAlgorithm();

        // Get destinations from file
        InputStream in = getClass().getResourceAsStream(this.destinations_path);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        this.destinations = new HashSet<>();
        List<String> lines = reader.lines().collect(Collectors.toList());
        for (String line : lines) {
            String[] parts = line.split(", ");

            String city = parts[1];
            int max_students = Integer.parseInt(parts[0]);
            this.destinations.add(new Destination(city, max_students));
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
        registerRoute(new Route("GET", "/assignments"), this::handleGetAssignments);

    }

    private Map<Student, String> getSolution() {
        // Initialize Population
        int populationSize = 50;
        double mutationProb = 0.10; // Choose any mutation rate
        double crossoverProb = 0.90; // Choose any crossover rate
        int maxGenerations = 10000;
        int maxNoImprovementCount = 750;
        Population population = new Population(new ArrayList<>(destinations), new ArrayList<>(students), populationSize, maxGenerations, maxNoImprovementCount, mutationProb, crossoverProb);
        population.initialize();
        Gene solution = population.evolve();
        return solution.getAssignment();
    }


    private Response handleGetAssignments(Request req) {
        Gson gson = new Gson();
        String body = gson.toJson(assignments);

        return new RESTResponse(200, "OK", body);
    }

    private Response handleAssignmentStream(Request req) {
        String client_email = req.queryParams.get("clientId");
        if (client_email == null) {
            return new RESTResponse(400, "Bad Request", new Message("clientId query parameter not provided."));
        }

        Student student;

        synchronized (students) {
            student = students.stream().filter(s -> s.email.equals(client_email)).findFirst().orElse(null);
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
        Student student;
        try{
            student = getStudentFromBody(req.body);
        } catch (JsonSyntaxException e) {
            return new RESTResponse(400, "Bad Request", new Message("Invalid JSON format in request body"));
        }

        // Validate student
        Response validationResponse = validateStudent(student);
        if (validationResponse != null) {
            return validationResponse;
        }

        // if student is not in the list
        if (!students.contains(student)) {
            return new RESTResponse(404, "Not Found", new Message("Student with email " + student.email
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
   
                        SSEEvent assignment = new SSEEvent("assignment", newAssignment + " to " + currentStudent.email + " by " + student.email);
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
        List<String> cities = this.destinations.stream().map(Destination::getName).collect(Collectors.toList());

        Gson gson = new Gson();
        String body = gson.toJson(cities);

        return new RESTResponse(200, "OK", body);
    }

    private Student getStudentFromBody(String body) throws JsonSyntaxException{
        Gson gson = new Gson();
        JsonStudent jsonStudent = gson.fromJson(body, JsonStudent.class);
    
        // Convert List<String> to Map<Integer, Destination>
        Map<Integer, Destination> preferences = new HashMap<>();
        for (int i = 0; i < jsonStudent.preferences.size(); i++) {
            String cityName = jsonStudent.preferences.get(i);
            Destination destination = destinations.stream()
                .filter(d -> d.getName().equals(cityName))
                .findFirst()
                .orElse(null);
            if (destination != null) {
                preferences.put(i, destination);
            } else {
                throw new JsonSyntaxException("Invalid destination: " + cityName);
            }
        }
    
        return new Student(jsonStudent.email, preferences);
    }


    private Response handlePostPreferences(Request req) {
        Student student;

        try {
            System.out.println();
            student = getStudentFromBody(req.body);
        } catch (JsonSyntaxException e) {
            return new RESTResponse(404, "Bad Request", new Message("Invalid JSON format in request body"));
        }
        
        // Validate student
        Response validationResponse = validateStudent(student); 
        if (validationResponse != null) {
            return validationResponse;
        }

        if (students.contains(student)) {
            return new RESTResponse(400, "Bad Request",
                new Message("Student with email " + student.email + " already exists"));
        }

        // Add student to list
        students.add(student);
        System.out.println("Student added: " + student.email + " " + student.preferences);
        System.out.println("Students: " + students);
        // Handle POST /preferences
        return new RESTResponse(201, "Created", new Message("Student with email " + student.email + " created"));
    }

    private Response validateStudent(Student student) {
        if (student == null) {
            return new RESTResponse(400, "Bad Request", new Message("Missing body"));
        }
        if (student.preferences == null) {
            return new RESTResponse(400, "Bad Request", new Message("Missing preferences"));
        }

        if (student.email == null) {
            return new RESTResponse(400, "Bad Request", new Message("Missing email"));
        }

        // no repeated preferences
        Set<Destination> uniquePreferences = new HashSet<>(student.getPreferences().values());
        if (uniquePreferences.size() != student.getPreferences().size()) {
            return new RESTResponse(400, "Bad Request", new Message("Repeated preferences"));
        }
        
        // preferences must be between 0 and 5
        if (student.preferences.size() > 5 || student.preferences.size() < 1){
            return new RESTResponse(400, "Bad Request", new Message("Too many preferences, maximum is 5"));
        }

        // validate destination
        for (Destination destination : student.getPreferences().values()) {
            boolean destinationExists = destinations.contains(destination);
            if (!destinationExists) {
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
                if (student.email.equals(updatedStudent.email)) {
                    student.preferences = updatedStudent.preferences;
                    return new RESTResponse(200, "OK",
                            new Message("Student with email " + student.email + " updated"));
                }
            }
        }

        // If student not found in list
        return new RESTResponse(404, "Not Found",
                new Message("Student with email " + updatedStudent.email + " not found"));
    }

}