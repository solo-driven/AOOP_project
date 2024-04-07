package org.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;

public class AssignmentServer {
    private String destinations_path = "/destinations.csv";
    private Map<String, Integer> destinations;
    private Map<Route, Handler> handlers = new HashMap<>();
    private int port;
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private ExecutorService pool = Executors.newCachedThreadPool();
    private GeneticAlgorithm geneticAlgorithm;

    private List<Student> students = new ArrayList<>();

    @FunctionalInterface
    interface Handler {
        Response handle(String requestBody);
    }

    static class Response {
        int statusCode;
        String statusMessage;
        Map<String, String> headers;
        String body;

        Response(int statusCode, String statusMessage, Map<String, String> headers, String body) {
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
            this.headers = headers;
            this.body = body;
        }

        @Override
        public String toString() {
            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage)
                    .append("\r\n");
    
            for (Map.Entry<String, String> header : headers.entrySet()) {
                responseBuilder.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
            }
            responseBuilder.append("\r\n").append(body);
    
            return responseBuilder.toString();
        }
    }

    public AssignmentServer(int port) throws IOException {
        this.port = port;
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

    boolean isValidDestination(String destination) {
        return destinations.containsKey(destination);
    }

    private void registerRoute(Route route, Handler handler) {
        handlers.put(route, handler);
    }

    private void setupHandlers() {
        registerRoute(new Route("GET", "/destinations"), this::handleGetDestinations);
        registerRoute(new Route("GET", "/assignment"), this::handleGetAssignment);
        registerRoute(new Route("POST", "/preferences"), this::handlePostPreferences);
        registerRoute(new Route("PUT", "/preferences"), this::handlePutPreferences);
        // list all students
        registerRoute(new Route("GET", "/students"), this::handleGetStudents);
    }

    private Response handleGetStudents(String requestBody) {
        Gson gson = new Gson();
        String body = gson.toJson(students);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        return new Response(200, "OK", headers, body);
    }

    private Response handleGetDestinations(String requestBody) {
        Set<String> cities = this.destinations.keySet();

        Gson gson = new Gson();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String body = gson.toJson(cities);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        return new Response(200, "OK", headers, body);
    }

    private Response handleGetAssignment(String requestBody) {
        return new Response(200, "OK", Collections.emptyMap(), "Response for GET /assignment");
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
                return new Response(400, "Bad Request", Collections.emptyMap(),
                        "Student with email " + student.email + " already exists");
            }
        }

        // Add student to list
        students.add(student);

        // Handle POST /preferences
        return new Response(201, "Created", Collections.emptyMap(), "Student with email " + student.email + " created");
    }

    private Response validateStudent(Student student) {
        if (student.preferences == null) {
            return new Response(400, "Bad Request", Collections.emptyMap(), "Missing preferences");
        }

        if (student.email == null) {
            return new Response(400, "Bad Request", Collections.emptyMap(), "Missing email");
        }

        // no repeated preferences
        Set<String> uniquePreferences = new HashSet<>(student.preferences);
        if (uniquePreferences.size() != student.preferences.size()) {
            return new Response(400, "Bad Request", Collections.emptyMap(), "Repeated preferences");
        }

        // preferences must be between 0 and 5
        if (student.preferences.size() > 5) {
            return new Response(400, "Bad Request", Collections.emptyMap(), "Too many preferences, maximum is 5");
        }

        // validate destination
        for (String destination : student.preferences) {
            if (!destinations.containsKey(destination)) {
                return new Response(400, "Bad Request", Collections.emptyMap(),
                        "Invalid destination: \'" + destination + "\'");
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
                return new Response(200, "OK", Collections.emptyMap(),
                        "Student with email " + student.email + " updated");
            }
        }

        // If student not found in list
        return new Response(404, "Not Found", Collections.emptyMap(),
                "Student with email " + updatedStudent.email + " not found");
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(this.port);
        System.out.println("Server started on port " + this.port);

        while (running) {
            try {
                final Socket clientSocket = serverSocket.accept();
                pool.execute(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (!this.running)
                    break;
                e.printStackTrace();
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            HttpParser parser = new HttpParser();

            parser.parseRequest(clientSocket.getInputStream());

            System.out.println(parser);

            String method = parser.getMethod();
            String path = parser.getPath();
            String body = parser.getBody();

            System.out.println("Method: " + method);
            System.out.println("Path: " + path);
            System.out.println("Body: " + body);
            System.out.println("Headers: " + parser.getHeaders());

            Route route = new Route(method, path);

            Handler handler = handlers.get(route);

            Response response;

            if (handler != null) {
                response = handler.handle(body);
            } else {
                response = new Response(404, "Not Found", Collections.emptyMap(),
                        "The requested route does not exist.");
            }

            OutputStream outputStream = clientSocket.getOutputStream();
            outputStream.write(response.toString().getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() throws IOException {
        running = false;
        serverSocket.close();
        pool.shutdown();
    }

}