package org.example.whiteboard;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private List<ClientHandler> clients;

    public ClientHandler(Socket socket, List<ClientHandler> clients) {
        this.clientSocket = socket;
        this.clients = clients;
    }

    public void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String clientAddress = clientSocket.getInetAddress().getHostAddress();
            System.out.println("✅ නව සම්බන්ධතාවය: " + clientAddress);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // සරලම පණිවිඩ ආකෘතිය: "DRAW:oldX,oldY,newX,newY,colorHex,size"
                if (inputLine.startsWith("DRAW:")) {
                    System.out.println("📢 ලැබුණු දත්ත: " + inputLine);
                    broadcast(inputLine);
                }
            }

        } catch (IOException e) {
            System.out.println("❌ සම්බන්ධතාවය විසන්ධි විය: " + clientSocket.getInetAddress().getHostAddress());
        } finally {
            try {
                clients.remove(this);
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
}