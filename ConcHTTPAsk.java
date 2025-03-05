import java.net.*;
import java.io.*;

public class ConcHTTPAsk {
    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Usage: java ConcHTTPAsk <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ConcHTTPAsk server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept new client
                System.out.println("New client connected!");

                MyRunnable myRunnable = new MyRunnable(clientSocket); // MyRunnable object
                Thread clientThread = new Thread(myRunnable);
                clientThread.start(); // Start new thread to MyRunnable

            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }

    }
}