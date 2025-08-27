package com.study.simplechat.cores;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.study.simplechat.configs.Config;

public class Server {

    private static final int PORT = Integer.parseInt(Config.get("sv.port"));
    private static final Map<String, Set<ClientHandler>> rooms = new HashMap<>();
    private static final Set<ClientHandler> allClients = new HashSet<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running ...");
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                synchronized (allClients) {
                    allClients.add(client);
                }
                client.start();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }

    }

    private static class ClientHandler extends Thread {

        private final Socket socket;
        private PrintWriter out;
        private String nickname;
        private String room;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("SUBMIT_NICKNAME");
                nickname = in.readLine();
                System.out.println("New client connected: " + nickname);

                sendRooms();

                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.startsWith("GET_ROOMS")) {
                        sendRooms();
                    } else if (msg.startsWith("CREATE_ROOM:")) {
                        String newRoom = msg.substring("CREATE_ROOM:".length());
                        synchronized (rooms) {
                            rooms.putIfAbsent(newRoom, new HashSet<>());

                            if (room != null) {
                                rooms.get(room).remove(this);
                                broadcast("<- " + nickname + " left room " + room, room);
                            }

                            room = newRoom;
                            rooms.get(room).add(this);
                            broadcast("-> " + nickname + " created & joined " + room, room);
                        }
                        broadcastRooms();

                    } else if (msg.startsWith("JOIN_ROOM:")) {
                        String joinRoom = msg.substring("JOIN_ROOM:".length());
                        synchronized (rooms) {
                            if (!rooms.containsKey(joinRoom)) {
                                out.println("Room not exist.");
                            } else {
                                if (room != null) {
                                    rooms.get(room).remove(this);
                                    broadcast("<- " + nickname + " left room " + room, room);
                                }
                                room = joinRoom;
                                rooms.get(room).add(this);
                                broadcast("-> " + nickname + " has joined " + room, room);
                            }
                        }
                    } else if (msg.startsWith("MESSAGE:")) {
                        String[] parts = msg.split(":", 3);
                        if (parts.length == 3) {
                            String room = parts[1];
                            String message = parts[2];
                            broadcastToRoom(room, nickname + ": " + message);
                        }
                    }
                }

            } catch (IOException exception) {
                exception.printStackTrace();
            } finally {
                if (room != null) {
                    synchronized (rooms) {
                        rooms.get(room).remove(this);
                        broadcast("<- " + nickname + " left the room.", room);
                    }
                }
                synchronized (allClients) {
                    allClients.remove(this);
                }
                // broadcastRooms();
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

        private void broadcast(String message, String roomName) {
            if (roomName == null) {
                return;
            }
            for (ClientHandler client : rooms.get(roomName)) {
                client.out.println(message);
            }
        }

        private void broadcastToRoom(String room, String msg) {
            synchronized (rooms) {
                Set<ClientHandler> members = rooms.get(room);
                if (members != null) {
                    for (ClientHandler ch : members) {
                        ch.out.println(msg);
                    }
                }
            }
        }

        private void broadcastRooms() {
            String roomListStr = String.join(",", rooms.keySet());
            synchronized (allClients) {
                for (ClientHandler ch : allClients) {
                    ch.out.println("ROOMS:" + roomListStr);
                }
            }
        }

        private void sendRooms() {
            synchronized (rooms) {
                String roomList = String.join(",", rooms.keySet());
                out.println("ROOMS:" + roomList);
            }
        }
    }
}
