package org.example.whiteboard;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class WhiteboardServer {
    private static final int PORT = 12345;
    private static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("🚀 සර්වර් එක ආරම්භ කර ඇත. Port: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                ClientHandler clientThread = new ClientHandler(clientSocket, clients);
                clients.add(clientThread);
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("සර්වර් දෝෂය: " + e.getMessage());
        }
    }
}