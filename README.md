# Assignment Server

The **Assignment Server** project is a server application designed to manage student assignments. It offers various endpoints to handle tasks such as retrieving a list of destinations, fetching assignments, posting preferences, updating preferences, and obtaining a list of students.

## How it Works

The server utilizes a `ServerSocket` to listen for incoming connections. Upon connection, it delegates the task to a thread from a thread pool for handling. It employs an `HttpParser` to parse incoming HTTP requests and a map of routes to handlers to determine how to process the requests.

The assignment of students to destinations is carried out using a `GeneticAlgorithm`, considering their preferences. Additionally, the server validates student preferences to ensure their validity and absence of duplicates.

## How to Run

This project relies on Gradle for both build and dependency management. To run the server, you can utilize the `gradle run` command.

### Steps to Run the Server:

1. Open a terminal in the project root directory.
2. Execute the following command to start the server:

```bash
gradle run
```

The server will commence and listen for incoming connections on the specified port.

In case Gradle isn't installed, you can use the Gradle Wrapper (`gradlew` or `gradlew.bat` for Windows), typically included in the project root. The Gradle Wrapper automatically downloads and installs the appropriate Gradle version and executes it.

### Steps to Run Using the Gradle Wrapper:

1. Open a terminal in the project root directory.
2. If you're on a Unix-based system (like Linux or Mac), make the Gradle Wrapper script executable using the following command:

```bash
chmod +x gradlew
```

3. Execute the following command to start the server:
```bash
./gradlew run
```

If you're on Windows, use `gradlew.bat` instead of `./gradlew`.

The server will initiate and listen for incoming connections on the specified port.

## Endpoints

- **`GET /destinations`**: Returns a list of all destinations.
- **`GET /assignment`**: Returns the assignment.
- **`POST /preferences`**: Creates a new student with the specified preferences.
- **`PUT /preferences`**: Updates the preferences of an existing student.
- **`GET /students`**: Returns a list of all students.

## Dependencies

- **Gson**: Utilized for converting Java objects to JSON and vice versa.

