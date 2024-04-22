# Authors

- Yusif Askari: Server code implementation
- Ismayil Abdullazada: Genetic algorithm development
- Murad Ganbarli: Client-side coding
- Saleh Alizada: GUI design

```mermaid
classDiagram

    class Route {
        String method
        String path
    }
    class Request {
        Socket clientSocket
        Map<String, String> headers
        Map<String, String> queryParams
        String body
        String method
        String path
    }
    class Response {
        int statusCode
        String statusMessage
        Map<String, String> headers
        String body
        String separator
    }
    class Destination {
        String name
        int maxStudents
    }
    class HttpParser {
        String method
        String URI
        String version
        Map<String, String> headers
        String body
        + void parseRequest(InputStream inputStream)
    }

    class RequestHandler {
        Response handle(Request requestBody)
    }
    class ExceptionHandler {
        Response handle(Exception e)
    }

    class Student {
        String name
        int age
        boolean isValidEmail()
        int getRankFromDestination(Destination destination)
    }



    class Server {
        -int port
        -ServerSocket serverSocket
        -boolean running
        -ExecutorService pool
        -Map<Route, RequestHandler> handlers
        -Map<Class<? extends Exception>, ExceptionHandler> exceptionHandlers
        +Server(int port)
        +void addExceptionHandler(Class<? extends Exception> exceptionClass, ExceptionHandler handler)
        -Response handleRequest(Request req)
        -void registerRequestHandler(Route route, RequestHandler handler)
        +void start()
        -void handleClient(Socket clientSocket)
        +void stop()
    }

     class AssignmentServer {
        -String destinations_path
        -Set<Destination> destinations
        -Set<Student> students
        -Map<Student, String> assignments
        -Map<Student, PrintWriter> studentConnections
        +AssignmentServer(int port)
        -void initDestinations()
        -Map<Student, String> getSolution()
        -Response handleGetAssignments(Request req)
        -Response handleAssignmentStream(Request req)
        -Response handleAssignment(Request req)
        -Response handleGetStudents(Request req)
        -Response handleGetDestinations(Request req)
        -Student getStudentFromBody(String body)
        -Response handlePostPreferences(Request req)
        -Response validateStudent(Student student)
        -Response handlePutPreferences(Request req)
    }

    class Gene {
        - int[] gene
        - List<Student> students
        - List<Destination> destinations
        - HelperMethods HelperMethods
        - int length
        + Gene( List<Student> students, List<Destination> destinations)
        + void showAssignment()
        + Map<Student, String> getAssignment()
        + int calculateFitness()
        + void mutate(mutationProb: double)
        + Gene crossover(partner: Gene)
        - void swapMutate()
        - void bitFlipMutate()
    }

    class Population {
        -List<Gene> population
        -List<Destination> destinations
        -List<Student> students
        -int size
        -int maxGenerations
        -int maxNoImprovementCount
        -double mutationProb
        -double crossoverProb
        +Population(List<Destination> destinations, List<Student> students, int size, int maxGenerations, int maxNoImprovementCount, double mutationProb, double crossoverProb)
        +List<Gene> getPopulation()
        +void initialize()
        -Gene generateSolution()
        +Map<Gene, Integer> calculateFitness()
        +Gene select()
        +Gene evolve()
        -void mutate(Gene gene)
    }

    Server "1" -- "*" Route : uses
    Server "1" -- "*" RequestHandler : uses
    Server "1" -- "*" ExceptionHandler : uses

    RequestHandler -- Request : handles
    HttpParser -- Request : parses
    ExceptionHandler -- Exception : handles
    RequestHandler -- Response : returns
    ExceptionHandler -- Response : returns
    AssignmentServer --|> Server
    Gene "*" o-- "*" Student : has
    Gene "*" o-- "*" Destination : has
    Population "*" o-- "*" Student : has
    Population "*" o-- "*" Destination : has
    Population "*" o-- "*" Gene : has
    Gene -- Pair : uses
    Gene -- HelperMethods : uses
    AssignmentServer "1" -- "*" Population : uses
    AssignmentServer -- "*" Student : interacts
    AssignmentServer -- "*" Destination : has
```

# Assignment Server

The **Assignment Server** is a robust server application designed to manage student assignments. It provides a variety of endpoints to handle tasks such as retrieving a list of destinations, fetching assignments, managing student preferences, and subscribing to real-time updates.

## Architecture

The server is built on a multi-threaded architecture, utilizing a `ServerSocket` to listen for incoming connections. Each connection is handled by a separate thread from a thread pool, ensuring efficient resource utilization.

The server uses an `HttpParser` to parse incoming HTTP requests and a map of routes to handlers to process the requests. The assignment of students to destinations is carried out using a `GeneticAlgorithm`, taking into account their preferences.

The server also supports Server-Sent Events (SSE) for real-time updates. Clients can subscribe to the `/assignment-stream` endpoint to receive updates whenever there is a change in the assignments.

## Running the Server

### Using the Pre-Built JAR

#### Prerequisites

- Java 21 or higher

1. Open a terminal in the project root directory.
2. Run the following command to start the server:

   ```bash
   java -jar server.jar
   ```

### Using Gradle Wrapper

The Gradle Wrapper is included in the project and can be used to build and run the server without needing to install Gradle.

1. Open a terminal in the project root directory.
2. Run the following command to start the server:

   ```bash
   ./gradlew run
   ```

   If you're on Windows, use `gradlew.bat run` instead of `./gradlew run`.

### Using Docker

You can also run the server using Docker. The Docker image for the server is available on Docker Hub. You can pull the image and run it with the following commands:

```bash
docker pull solodriven/final_project_server
docker run -p 8080:8080 solodriven/final_project_server
```

## Endpoints

- **`GET /destinations`**: This endpoint retrieves a list of all available destinations. It doesn't require any input and returns a JSON array of cities.

- **`POST /preferences`**: This endpoint creates a new student with their preferred cities. It accepts a JSON body with an `email` field and a `preferences` field, which is an array of strings representing the student's preferred cities. If the student is successfully created, it returns a 201 status code with a message indicating the student was created. If the student already exists or there's an error with the input, it returns an appropriate error message and status code.

- **`PUT /preferences`**: This endpoint updates a student's preferred cities. It accepts a JSON body with an `email` field and a `preferences` field, which is an array of strings representing the student's new preferred cities. If the student's preferences are successfully updated, it returns a 200 status code with a message indicating the student was updated. If the student doesn't exist or there's an error with the input, it returns an appropriate error message and status code.

- **`GET /students`**: This endpoint retrieves a list of all students. It doesn't require any input and returns a JSON array of student objects, each with an `email` field and a `preferences` field.

- **`POST /assign`**: This endpoint assigns a student to a destination based on their preferences and the current assignments. It accepts a JSON body with an `email` field and a `preferences` field. If the student doesn't exist or there's an error with the input, it returns an appropriate error message and status code. If the assignment is successful, it returns a JSON object with the new assignment for the student who requested it.

- **`GET /assignments`**: This endpoint retrieves all the assignments. It doesn't require any input and returns a JSON object representing all the assignments. Each key in the object is a student's email and the corresponding value is the city they've been assigned to.

- **`GET /assignment-stream`**: This endpoint subscribes a student to updates on the assignments. It requires a `clientId` query parameter, which should be the email of the student subscribing to the updates. It doesn't return a traditional response. Instead, it keeps the connection open and sends updates in the form of server-sent events whenever there's a change in the assignments. If the `clientId` isn't provided or the student doesn't exist, it returns an appropriate error message and status code.

## Server-Sent Events (SSE)

Server-Sent Events (SSE) is a standard that allows a web server to push updates to the client. Unlike WebSockets, SSE is a one-way communication channel from the server to the client. In this application, SSE is used to push assignment updates to the clients in real-time.

## Dependencies

- **Gson**: Utilized for converting Java objects to JSON and vice versa.

# Run App

`pip3 install tkinter`

`python3 app.py`
