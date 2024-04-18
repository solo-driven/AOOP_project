# Start with a base image containing Java runtime
FROM openjdk:21-jdk

# Add Maintainer Info
LABEL maintainer="askrai.yusif@gmail.com"

# Make port 8080 available to the world outside this container
EXPOSE 8080

# The application's fat jar file (created by command ./gradlew fatJar)
ARG JAR_FILE=app/build/libs/app-fat.jar

# Add the application's jar to the container
ADD ${JAR_FILE} app.jar

# Run the jar file 
ENTRYPOINT ["java","-jar","/app.jar"]