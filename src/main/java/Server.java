import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
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
            var params = getQueryParams("http://localhost:9999/" + parts[1]);
            String queryPath = parts[1];

            if (parts[1].indexOf('?') != -1) {
                queryPath = parts[1].substring(0, parts[1].indexOf('?'));
            }

            Request request = new Request(parts[0], queryPath, in, params);

            synchronized (handlers) {
                Handler handler = handlers.get(request.method).get(request.path);
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

    private List<NameValuePair> getQueryParams(String query){
        URI uri = URI.create(query);
        return URLEncodedUtils.parse(uri, String.valueOf(StandardCharsets.UTF_8));
    }
}