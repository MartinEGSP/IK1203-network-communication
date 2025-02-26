package tcpclient;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.io.*;

public class TCPClient {
    //---------Task 2: ------------
    private boolean shutdown;
    private Integer timeout;
    private Integer limit;

    public TCPClient(boolean shutdown, Integer timeout, Integer limit) {
        this.shutdown = shutdown;
        this.timeout = timeout;
        this.limit = limit;
    }
    //---------End of Task 2 ------------

    public byte[] askServer(String hostname, int port, byte[] toServerBytes) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        
        //----Task 2: Socket Setup----
        try (Socket socket = new Socket(hostname, port)) {
            if (timeout != null) {
                socket.setSoTimeout(timeout);
            } 
            //----End of Task 2 ----
            
            // Task 1: Send Data
            if (toServerBytes.length > 0) {
                OutputStream output = socket.getOutputStream();
                output.write(toServerBytes);
                output.flush();
            }
            
            // Task 3: Only shutdown if required
            if (shutdown) {
                socket.shutdownOutput();
            }

            int bytesRead;
            InputStream input = socket.getInputStream();
            byte[] bufferT = new byte[1024];

            //--------- Task 3: Read Data and Store it Correctly ------------
            while ((bytesRead = input.read(bufferT)) != -1) { 
                System.out.println("DEBUG: Read " + bytesRead + " bytes from server");
                System.out.println("DEBUG: Data received: " + new String(bufferT, 0, bytesRead, StandardCharsets.UTF_8));
                byteStream.write(bufferT, 0, bytesRead);
            }

            // Ensure Data is Stored Before Returning
            byteStream.flush();
            byte[] responseBytes = byteStream.toByteArray();

            System.out.println("DEBUG: TCPClient final response length: " + responseBytes.length);
            System.out.println("DEBUG: TCPClient final response data: " + new String(responseBytes, StandardCharsets.UTF_8));
            
            return responseBytes;
        }
    }

    public byte[] askServer(String hostname, int port) throws IOException {
        return new byte[0]; 
    }
}
