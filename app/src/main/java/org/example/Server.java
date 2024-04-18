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
    protected Map<Route, Handler> handlers = new HashMap<>();

    @FunctionalInterface
    interface Handler {
        Response handle(Request requestBody);
    }

    public Server(int port) throws IOException {
        this.port = port;
        setupHandlers();
    }

    protected abstract void setupHandlers();

    protected void registerRoute(Route route, Handler handler) {
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

            Route route = new Route(req.method, req.path);

            Handler handler = handlers.get(route);

            Response response;

            if (handler != null) {
                response = handler.handle(req);
            } else {
                response = new Response(404, "Not Found",
                        "The requested route does not exist.");
            }

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