import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import tcpclient.TCPClient;

public class MyRunnable implements Runnable {
    private Socket clientSocket;

    public MyRunnable(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream()) {
            // Read HTTP request
            String requestLine = in.readLine();
            if (requestLine == null || !requestLine.startsWith("GET")) {
                sendResponse(out, 400, "Bad Request");
                return;
            }

            System.out.println("Received request: " + requestLine);
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                sendResponse(out, 400, "Bad Request");
                return;
            }

            // Read and discard the full HTTP header
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isEmpty()) {
                    break; // Empty line marks end of HTTP header
                }
            }

            // Parse request
            String queryString = requestParts[1];
            if (queryString.length() < 5 || !queryString.startsWith("/ask?")) {
                sendResponse(out, 404, "Not Found");
                return;
            }

            // Parse query parameters
            HashMap<String, String> params = parseQueryString(queryString.substring(5));

            // Validate required parameters
            if (!params.containsKey("hostname") || !params.containsKey("port")) {
                sendResponse(out, 400, "Missing hostname or port");
                return;
            }

            String hostname = params.get("hostname");
            int serverPort;
            try {
                serverPort = Integer.parseInt(params.get("port"));
            } catch (NumberFormatException e) {
                System.err.println("ERROR: Invalid port number: " + params.get("port")); // ✅ Debugging
                sendResponse(out, 400, "Invalid port number");
                return;
            }

            // Extract optional parameters
            String message = params.getOrDefault("string", "");
            if (!message.isEmpty() && !message.endsWith("\n")) {
                message += "\n"; // ✅ Ensure message ends with newline
            }
            boolean shutdown = Boolean.parseBoolean(params.getOrDefault("shutdown", "false"));
            int timeout = params.containsKey("timeout") ? Integer.parseInt(params.get("timeout")) : 0;
            int limit = params.containsKey("limit") ? Integer.parseInt(params.get("limit")) : 0;

            System.out.println("DEBUG: Calling TCPClient.askServer with:");
            System.out.println(" - Hostname: " + hostname);
            System.out.println(" - Port: " + serverPort);
            System.out.println(" - Message: '" + message + "'");
            System.out.println(" - Shutdown: " + shutdown);
            System.out.println(" - Timeout: " + timeout);
            System.out.println(" - Limit: " + limit);

            // Call TCPClient to get the response
            try {
                TCPClient client = new TCPClient(shutdown, timeout, limit);
                byte[] serverResponseBytes = client.askServer(hostname, serverPort,
                        message.getBytes(StandardCharsets.UTF_8));
                String serverResponse = new String(serverResponseBytes, StandardCharsets.UTF_8);

                sendResponse(out, 200, serverResponse);
            } catch (IOException e) {
                sendResponse(out, 404, "Error contacting server: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    // Parse query string
    private static HashMap<String, String> parseQueryString(String query) {
        HashMap<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }

    // Send HTTP response
    private static void sendResponse(OutputStream out, int statusCode, String message) throws IOException {
        String statusText = (statusCode == 200) ? "OK" : (statusCode == 404 ? "Not Found" : "Bad Request");

        if (!message.endsWith("\n")) {
            message += "\n";
        }

        String httpResponse = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + message.length() + "\r\n" +
                "Connection: close\r\n" +
                "\r\n" +
                message;

        System.out.println("DEBUG: Sending HTTP Response:\n" + httpResponse);
        out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
