import javax.xml.crypto.dsig.keyinfo.KeyValue;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocket serverSocket = null;
    Map<String, Handler> innerHandlersMap;
    Map<String, Map<String, Handler>> handlers;
    List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
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

    /*
        private void handleRequest(Socket socket) {
            try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 final var out = new BufferedOutputStream(socket.getOutputStream());) {

                final var requestLine = in.readLine();
                System.out.println(requestLine);
                System.out.println(Thread.currentThread().getName());
                final var parts = requestLine.split(" ");

                if (parts.length != 3) {

                } else {
                    final var path = parts[1];
                    if (!validPaths.contains(path)) {
                        out.write((
                                "HTTP/1.1 404 Not Found\r\n" +
                                        "Content-Length: 0\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n"
                        ).getBytes());
                        out.flush();
                    } else {
                        final var filePath = Path.of(".", "public", path);
                        final var mimeType = Files.probeContentType(filePath);

                        // special case for classic
                        if (path.equals("/classic.html")) {
                            handleClassic(out, filePath, mimeType);
                        } else {
                            final var length = Files.size(filePath);
                            out.write((
                                    "HTTP/1.1 200 OK\r\n" +
                                            "Content-Type: " + mimeType + "\r\n" +
                                            "Content-Length: " + length + "\r\n" +
                                            "Connection: close\r\n" +
                                            "\r\n"
                            ).getBytes());
                            Files.copy(filePath, out);
                            out.flush();
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void handleClassic(BufferedOutputStream out, Path filePath, String mimeType) throws IOException {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
        }
    */
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