package com.threadium.chat.server;

import com.threadium.chat.common.Message;
import com.threadium.chat.common.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class ChatServer {

    private static final int PORT = 8080;
    
    // Maps storing users and rooms, thread-safe access needed
    private final Map<String, ClientHandler> activeUsers = new ConcurrentHashMap<>();
    private final Map<String, ChatRoom> activeRooms = new ConcurrentHashMap<>();
    private final Map<String, String> userToRoom = new ConcurrentHashMap<>(); // persistent presence!
    
    private final ReentrantLock userLock = new ReentrantLock();
    private final ReentrantLock roomLock = new ReentrantLock();
    
    private final ExecutorService clientThreadPool = Executors.newCachedThreadPool();
    private final UserManager userManager = new UserManager();

    public void start() {
        System.out.println("Chat Server starting on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clientThreadPool.submit(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public boolean loginUser(String username, String password, ClientHandler handler) {
        userLock.lock();
        try {
            if (!userManager.authenticate(username, password)) {
                return false; // Wrong password or doesn't exist
            }
            if (activeUsers.containsKey(username)) {
                return false; // Already online from another client instance
            }
            activeUsers.put(username, handler);
            System.out.println("User logged in: " + username);
            return true;
        } finally {
            userLock.unlock();
        }
    }

    public void logoutUser(String username) {
        userLock.lock();
        try {
            activeUsers.remove(username);
            System.out.println("User logged out: " + username);
        } finally {
            userLock.unlock();
        }
    }

    public ChatRoom getOrCreateRoom(String roomName) {
        roomLock.lock();
        try {
            if (!activeRooms.containsKey(roomName)) {
                ChatRoom newRoom = new ChatRoom(roomName, this);
                activeRooms.put(roomName, newRoom);
                System.out.println("Room created: " + roomName);
                broadcastRoomListUpdate();
            }
            return activeRooms.get(roomName);
        } finally {
            roomLock.unlock();
        }
    }

    public void anchorUserToRoom(String username, String roomName) {
        userToRoom.put(username, roomName);
    }
    
    public void unanchorUserFromRoom(String username) {
        userToRoom.remove(username);
    }

    public String getUserRoom(String username) {
        return userToRoom.get(username);
    }

    public ClientHandler getActiveClient(String username) {
        return activeUsers.get(username);
    }

    public void broadcastRoomList(ClientHandler specificClient) {
        List<String> roomNames;
        roomLock.lock();
        try {
            roomNames = new ArrayList<>(activeRooms.keySet());
        } finally {
            roomLock.unlock();
        }
        String list = String.join(",", roomNames);
        Message message = new Message(MessageType.ROOM_LIST_UPDATE, "Server", null, list, null, LocalDateTime.now());
        if (specificClient != null) {
            specificClient.sendMessage(message);
        } else {
            for (ClientHandler handler : activeUsers.values()) {
                handler.sendMessage(message);
            }
        }
    }

    public void broadcastRoomListUpdate() {
        broadcastRoomList(null); // to all users
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }
}
