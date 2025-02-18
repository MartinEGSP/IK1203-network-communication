package tcpclient;
import java.net.*;
import java.io.*;

public class TCPClient {
   //---------Task 2: ------------
    private boolean shutdown; // Determines if client should close outgoing connection after sending
    private Integer timeout;  // Specifies timeoutduration in ms
    private Integer limit;    // Specifies max data to receive before stopping

    public TCPClient(boolean shutdown, Integer timeout, Integer limit) {
        this.shutdown = shutdown;
        this.timeout = timeout;
        this.limit = limit;
    }
    //---------End of Task 2 ------------
    
    public byte[] askServer(String hostname, int port, byte [] toServerBytes) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        //----Task 2: Socket Setup with Timeout Support----
        try (Socket socket = new Socket(hostname, port)) {
            if (timeout != null) {
                socket.setSoTimeout(timeout); // Sets a timeout for receiving data
            } 
            //----End of Task 2 ----
            
            // Task 1
            if (toServerBytes != null) {
                OutputStream output = socket.getOutputStream();
                output.write(toServerBytes); // Send the data to the server
                output.flush(); // Ensure all data is transmitted
            }
            
            //----Task 2 Close connection if shutdown----
            if (shutdown) {
                socket.shutdownOutput(); //Stops new data from being sent
            }
            //----Task 2 End----

            int bytesRead;
            int bytesTotal = 0; // Task 2: Tracks total received bytes
            int counter = 0;//Task 2: Counts amount of reads
            int maxCount = 100; //Task 2: Max amount of reads, no loops

            InputStream input = socket.getInputStream();  // Server response stream
            byte[] bufferT = new byte[1024]; // Temporary buffer to store incoming data 

            try{
                while ((bytesRead = input.read(bufferT)) != -1) { // Read incoming data
                    //----Task 2 data limit----
                    if (limit != null && bytesTotal + bytesRead > limit) {
                        byteStream.write(bufferT, 0, limit - bytesTotal); // Stop reading once limit is reached
                        break;
                    }
                    byteStream.write(bufferT, 0, bytesRead); // Store received data
                    bytesTotal += bytesRead; // Update total bytes received
                    counter++; // Counts how many reads

                    if (counter >= maxCount) {  // If the amount of reads surpasses the allowed amount then break
                        System.out.println("Max read count reached, stopping.");
                        break;
                    }
                    //----Task 2 End----
                }
            }
            catch (SocketTimeoutException e) {
                System.out.println("Timeout reached, returning received data.");
            }
            return byteStream.toByteArray(); // Convert the collected data to a byte array and return it
        }
         
    }

    public byte[] askServer(String hostname, int port) throws IOException {//Connecting without sending data
        return new byte[0]; // Default return value for now
    }
}
