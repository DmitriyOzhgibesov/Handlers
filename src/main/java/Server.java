import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocket serverSocket = null;
    Map<String, Handler> innerHandlersMap;
    Map<String, Map<String, Handler>> handlers;
    ExecutorService threadPool = Executors.newFixedThreadPool(64);

    public Server() {
        innerHandlersMap = new HashMap<>();
        handlers = new HashMap<>();
    }

    public void listen(int socketNumber) {
        try {
            serverSocket = new ServerSocket(socketNumber);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while (true) {
            try {
                var socket = serverSocket.accept();
                threadPool.execute(() -> handleRequest(socket));
            } catch (IOException ioe) {
                System.out.println("Error accepting connection");
                ioe.printStackTrace();
            }
        }
    }

    private void handleRequest(Socket socket) {
        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream());) {

            final var requestLine = in.readLine();
            System.out.println(requestLine);
            System.out.println(Thread.currentThread().getName());
            final var parts = requestLine.split(" ");

            Request request = new Request(parts[0], parts[1], in);

            synchronized (handlers) {
                Handler handler = handlers.get(request.method).get(request.headers);
                handler.handle(request, out);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addHandler(String methodName, String methodPath, Handler handler) {
        synchronized (handlers) {
            synchronized (innerHandlersMap) {
                innerHandlersMap.put(methodPath, handler);
                handlers.put(methodName, innerHandlersMap);
            }
        }
    }
}