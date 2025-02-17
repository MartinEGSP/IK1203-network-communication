package tcpclient;

import java.io.*;
import java.net.*;

public class TCPClient {
    
    public TCPClient() {
    }

    public byte[] askServer(String hostname, int port, byte [] toServerBytes) throws IOException { //sends and recives data to the server
        try (Socket socket = new Socket(hostname, port)) { //Creates TCP sockets 
            if (toServerBytes != null) {
                OutputStream output = socket.getOutputStream();
                output.write(toServerBytes); // Send data
                output.flush(); // Ensure all data is sent
            }
            int bytesRead;
            InputStream input = socket.getInputStream();  //Server response
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            byte[] bufferT = new byte[1024]; // Temporary buffer 
            
    
            while ((bytesRead = input.read(bufferT)) != -1) { //Add data to temp buffer 
                byteStream.write(bufferT, 0, bytesRead); //Write it to the ByteArrayOutputStream
            }

            return byteStream.toByteArray(); // Convert the collected data to a byte array
        }
    }

    public byte[] askServer(String hostname, int port) throws IOException { //Connects to the server, no data4
        return new byte[0];
    }

}
