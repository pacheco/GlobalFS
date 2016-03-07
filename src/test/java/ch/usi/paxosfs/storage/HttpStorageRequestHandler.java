package ch.usi.paxosfs.storage;

import org.apache.http.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Used as the server in the HttpStorage unit tests
 * Created by pacheco on 1/29/16.
 */
public class HttpStorageRequestHandler implements HttpRequestHandler {
    private Map<String, byte[]> storage = new HashMap<>();

    public String requestKey (HttpRequest request) {
        int lastSlash = request.getRequestLine().getUri().lastIndexOf("/");
        return request.getRequestLine().getUri().substring(lastSlash+1);
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        if (request.getRequestLine().getMethod().equalsIgnoreCase("GET")) {
            byte[] val;
            val = storage.get(requestKey(request));
            if (val == null) {
                response.setStatusCode(404);
            } else {
                response.setStatusCode(200);
                response.setEntity(new ByteArrayEntity(val));
            }
        } else if (request.getRequestLine().getMethod().equalsIgnoreCase("PUT") && request instanceof HttpEntityEnclosingRequest) {
            final HttpEntity requestEntity = ((HttpEntityEnclosingRequest) request).getEntity();
            if (requestEntity != null) {
                String key = requestKey(request);
                if (storage.putIfAbsent(key, EntityUtils.toByteArray(requestEntity)) != null) {
                    response.setStatusCode(409);
                } else {
                    response.setStatusCode(200);
                }
            } else {
                response.setStatusCode(400);
            }
        } else {
            response.setStatusCode(405);
        }
    }
}
