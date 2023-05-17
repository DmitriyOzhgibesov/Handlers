import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        final var server = new Server();
        // код инициализации сервера (из вашего предыдущего ДЗ)

        // добавление хендлеров (обработчиков)
        server.addHandler("GET", "/messages", (request, responseStream) -> {
            if (!request.queryParams.isEmpty()){
                String response = String.format("<!DOCTYPE html>\n" +
                        "<html lang=\"en\">\n" +
                        "<head>\n" +
                        "    <meta charset=\"UTF-8\">\n" +
                        "    <title>Title</title>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "Your params is:%s\n" +
                        "</body>\n" +
                        "</html>", request.queryParams);

                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + "text/html" + "\r\n" +
                                "Content-Length: " + response.getBytes().length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                responseStream.write(response.getBytes());
                responseStream.flush();
            } else {
                final var filePath = Path.of(".", "public", request.path + ".html");
                final var length = Files.size(filePath);
                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + "text/html" + "\r\n" +
                                "Content-Length: " + length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                Files.copy(filePath, responseStream);
                responseStream.flush();
            }
        });

        server.addHandler("GET", "/index.html", (request, responseStream) -> {
            final var filePath = Path.of(".", "public", request.path);
            final var mimeType = Files.probeContentType(filePath);
            final var length = Files.size(filePath);
            responseStream.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, responseStream);
            responseStream.flush();

        });

        server.addHandler("GET", "/favicon.ico", (request, responseStream) -> {
        });

        server.addHandler("GET", "/classic.html", (request, responseStream) -> {
            final var filePath = Path.of(".", "public", request.path);
            final var mimeType = Files.probeContentType(filePath);
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            responseStream.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            responseStream.write(content);
            responseStream.flush();

        });

        server.listen(9999);
    }
}