package tcpclient;
import java.net.*;
import java.io.*;

public class TCPClient {
   //---------Task------------
    private boolean shutdown;
    private Integer timeout;
    private Integer limit;
       
    public TCPClient(boolean shutdown, Integer timeout, Integer limit) {
        this.shutdown = shutdown;
        this.timeout = timeout;
        this.limit = limit;
    }//---------Task------------

    public byte[] askServer(String hostname, int port, byte [] toServerBytes) throws IOException {
             //----Task----
        try (Socket socket = new Socket(hostname, port)){
            if (timeout != null){
                socket.setSoTimeout(timeout);
            } //----Task----

            else if (toServerBytes != null) {
                OutputStream output = socket.getOutputStream();
                output.write(toServerBytes); // Send data
                output.flush(); // Ensure all data is sent
            }
            else if (shutdown){
                socket.shutdownOutput(); //---Task---
            }

            int bytesRead;
            int bytesTotal = 0; //---Task---
            InputStream input = socket.getInputStream();  //Server response
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            byte[] bufferT = new byte[1024]; // Temporary buffer 

            while ((bytesRead = input.read(bufferT)) != -1) { //Add data to temp buffer 
                if(limit != null && bytesTotal + bytesRead > limit){
                    byteStream.write(bufferT, 0, limit - bytesTotal); 
                    break;
                }
                else {
                    byteStream.write(bufferT, 0, bytesRead); //Write it to the ByteArrayOutputStream
                }
                bytesTotal += bytesRead;
            }
            return byteStream.toByteArray(); // Convert the collected data to a byte array
        }
    }
    public byte[] askServer(String hostname, int port) throws IOException { //Connects to the server, no data4
        return new byte[0];
    }
}
