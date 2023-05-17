import org.apache.http.NameValuePair;

import java.io.BufferedReader;
import java.util.List;

public class Request {
    public String method;
    public String path;
    public BufferedReader body;
    public List<NameValuePair> queryParams;

    public Request(String method, String path, BufferedReader body, List<NameValuePair> params) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.queryParams = params;
    }
}
