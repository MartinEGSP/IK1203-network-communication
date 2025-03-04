import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import tcpclient.TCPClient;

public class HTTPAsk {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java HTTPAsk <port>");
            System.exit(1);
        }
        
        int port = Integer.parseInt(args[0]);

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("HTTPAsk server started on port " + port);

            while (true) { // Keep running and accept clients
                Socket clientConnection = serverSocket.accept();
                System.out.println("Client connected!");

                try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
                    OutputStream out = clientConnection.getOutputStream()
                ) {
                    // Read HTTP request
                    String requestLine = in.readLine();
                    if (requestLine == null || !requestLine.startsWith("GET")) {
                        sendResponse(out, 400, "Bad Request");
                        continue;
                    }

                    System.out.println("Received request: " + requestLine);
                    String[] requestParts = requestLine.split(" ");
                    if (requestParts.length < 2) {
                        sendResponse(out, 400, "Bad Request");
                        continue;
                    }

                    // Ensure full HTTP header is read
                    boolean validHeader = false;
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.isEmpty()) { // Empty line marks end of HTTP header
                            validHeader = true;
                            break;
                        }
                    }
                    if (!validHeader) {
                        sendResponse(out, 400, "Bad Request");
                        continue;
                    }

                    // Parse request
                    String queryString = requestParts[1];
                    if (!queryString.startsWith("/ask?")) {
                        sendResponse(out, 404, "Not Found"); // ✅ Changed from 400 to 404
                        continue;
                    }

                    // Parse parameters
                    HashMap<String, String> params = parseQueryString(queryString.substring(5));

                    // Validate required params
                    if (!params.containsKey("hostname") || !params.containsKey("port")) {
                        sendResponse(out, 400, "Missing hostname or port");
                        continue;
                    }

                    String hostname = params.get("hostname");
                    int serverPort;
                    try {
                        serverPort = Integer.parseInt(params.get("port"));
                    } catch (NumberFormatException e) {
                        sendResponse(out, 400, "Invalid port number");
                        continue;
                    }

                    // Extract optional params
                    String message = params.getOrDefault("string", "");
                    if (!message.isEmpty()) {
                        message += "\n"; 
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

                    // Call TCPClient
                    try {
                        TCPClient client = new TCPClient(shutdown, timeout, limit);
                        byte[] serverResponseBytes = client.askServer(hostname, serverPort, message.getBytes(StandardCharsets.UTF_8));
                        String serverResponse = new String(serverResponseBytes, StandardCharsets.UTF_8);

                        System.out.println("DEBUG: Response from TCPClient: " + serverResponse);
                        sendResponse(out, 200, serverResponse);
                    } catch (IOException e) {
                        sendResponse(out, 404, "Error contacting server: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
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
