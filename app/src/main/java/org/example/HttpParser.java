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

            // Parse headers
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length: ")) {
                    contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
                }
                String[] headerParts = line.split(": ");
                headers.put(headerParts[0], headerParts[1]);
            }

            // Parse body
            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                reader.read(bodyChars, 0, contentLength);
                body = new String(bodyChars);
            } else {
                body = "";
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