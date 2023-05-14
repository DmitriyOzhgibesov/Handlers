import java.io.BufferedReader;

public class Request {
    public String method;
    public String headers;
    public BufferedReader body;

    public Request(String method, String headers, BufferedReader body) {
        this.method = method;
        this.headers = headers;
        this.body = body;
    }
}
