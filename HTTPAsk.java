import java.net.*;
import java.io.*;
import java.util.*;
import tcpclient.TCPClient;


public class HTTPAsk {
    public static void main(String[] args) {
        int port = 8080; 

        try { //Server socket
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("HTTPAsk server started on port " + port);

            while (true) {
                // wait and accept incoming client connection
                Socket clientConnection = serverSocket.accept();
                System.out.println("Client connected!");

                try ( //Output stream
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientConnection.getInputStream()));
                    OutputStream out = clientConnection.getOutputStream()
                ) {
                    //Read the HTTP request
                    String requestLine = in.readLine();
                    if (requestLine == null || !requestLine.startsWith("GET")) {
                        sendResponse(out, 400, "Bad Request");
                        clientConnection.close();
                        continue;
                    }
                    System.out.println("Received request: " + requestLine);

                    //Request URI
                    String[] requestParts = requestLine.split(" ");
                    if (requestParts.length < 2) {
                        sendResponse(out, 400, "Bad Request");
                        continue;
                    }
                    //URL Path
                    String queryString = requestParts[1];
                    if (!queryString.startsWith("/ask?")) {
                        sendResponse(out, 400, "Invalid endpoint");
                        continue;
                    }

                    //Parse query parameters
                    HashMap<String, String> params = parseQueryString(queryString.substring(5));

                    //Validate requirments
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

                    //Extract optional parameters
                    String message = params.getOrDefault("string", "") + "\n";
                    boolean shutdown = Boolean.parseBoolean(params.getOrDefault("shutdown", "false"));
                    int timeout = params.containsKey("timeout") ? Integer.parseInt(params.get("timeout")) : 0;
                    int limit = params.containsKey("limit") ? Integer.parseInt(params.get("limit")) : 0;

                    //Call TCPClient.askServer()
                    try {
                        TCPClient client = new TCPClient(shutdown, timeout, limit);
                        byte[] serverResponseBytes = client.askServer(hostname, serverPort, message.getBytes());
                        String serverResponse = new String(serverResponseBytes);
                        System.out.println("Response from TCPClient: " + serverResponse);


                        //Send the response back to the client
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

    //Parse query string
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

    //Send HTTP response
    private static void sendResponse(OutputStream out, int statusCode, String message) throws IOException {
        String statusText = (statusCode == 200) ? "OK" : "Bad Request";
        String httpResponse = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                              "Content-Type: text/plain\r\n" +
                              "Content-Length: " + message.length() + "\r\n" +
                              "Connection: close\r\n" +
                              "\r\n" +
                              message + "\n";
        out.write(httpResponse.getBytes());
        out.flush();
    }
}
