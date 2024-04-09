    package org.example;


    import java.io.BufferedReader;
    import java.io.IOException;
    import java.io.InputStream;
    import java.io.InputStreamReader;
    import java.util.HashMap;
    import java.util.Map;

    public class HttpParser {
        private String method;
        private String path;
        private String version;
        private Map<String, String> headers = new HashMap<>();
        private String body;

        public void parseRequest(InputStream inputStream) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            int contentLength = 0;
        
            // Parse request line
            String requestLine = reader.readLine();
            if (requestLine != null) {
                String[] requestParts = requestLine.split(" ");
                method = requestParts[0];
                path = requestParts[1];
                version = requestParts[2];
            }
            System.out.println("Parser Method: " + method);
            System.out.println("Parser Path: " + path);
            System.out.println("Parser Version: " + version);
        
            // Parse headers
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                String[] headerParts = line.split(": ");
                headers.put(headerParts[0], headerParts[1]);
            
            }
        
            // Read request body
            StringBuilder builder = new StringBuilder();
            while (reader.ready()) {
                builder.append((char) reader.read());
            }
            body = builder.toString();
            System.out.println("Parser Body: " + body.replace("\n", "\\n").replace("\r", "\\r"));
            


            contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
            // Verify body length
            System.out.println("Content-Length: " + contentLength);
            System.out.println("Body length: " + body.length());

            
            if (body!=null && body.length() != contentLength) {
                throw new IOException("Body length doesn't match Content-Length header.");
            }

        }
        
        public String getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public String getVersion() {
            return version;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public String getBody() {
            return body;
        }

        @Override
        public String toString() {
            return "HttpParser{" +
                    "method='" + method + '\'' +
                    ", path='" + path + '\'' +
                    ", version='" + version + '\'' +
                    ", headers=" + headers +
                    ", body='" + body + '\'' +
                    '}';
        }
    }