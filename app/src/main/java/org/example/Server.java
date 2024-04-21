package org.example;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class Server {
    protected int port;
    protected ServerSocket serverSocket;
    protected volatile boolean running = true;
    protected ExecutorService pool = Executors.newCachedThreadPool();
    protected Map<Route, RequestHandler> handlers = new HashMap<>();
    private Map<Class<? extends Exception>, ExceptionHandler> exceptionHandlers = new HashMap<>();


    @FunctionalInterface
    interface RequestHandler {
        Response handle(Request requestBody);
    }

    @FunctionalInterface
    interface ExceptionHandler {
        Response handle(Exception e);
    }

    public Server(int port) throws IOException {
        this.port = port;
    }


    public void addExceptionHandler(Class<? extends Exception> exceptionClass, ExceptionHandler handler) {
        exceptionHandlers.put(exceptionClass, handler);
    }

    protected Response handleRequest(Request req) {
        Route route = new Route(req.method, req.path);
        RequestHandler handler = handlers.get(route);
    
        if (handler == null) {
            return new RESTResponse(404, "Not Found", new Message("The requested route does not exist."));
        }
    
        try {
            return handler.handle(req);
        } catch (Exception e) {
            for (Map.Entry<Class<? extends Exception>, ExceptionHandler> entry : exceptionHandlers.entrySet()) {
                if (entry.getKey().isInstance(e)) {
                    return entry.getValue().handle(e);
                }
            }
        }
    
        return new RESTResponse(500, "Internal Server Error", new Message("An error occurred while processing the request"));
    }
    

    protected void registerRequestHandler(Route route, RequestHandler handler) {
        handlers.put(route, handler);
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

    protected void handleClient(Socket clientSocket) {
        try {

            Request req = new Request(clientSocket);
            System.out.println(req);
            
            Response response = handleRequest(req);

            OutputStream outputStream = clientSocket.getOutputStream();
            outputStream.write(response.toString().getBytes());

            if ("keep-alive".equals(req.headers.get("Connection"))) {
                outputStream.flush();
            } else {
                outputStream.close();
            }

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