package com.threadium.chat.server;

import com.threadium.chat.common.Message;
import com.threadium.chat.common.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;

/**
 * Executes a continuous read loop over an accepted Socket connection.
 * Acts as the server-side representative for a singular client instance.
 * It deserializes `Message` objects received sequentially, filters them by `MessageType`, 
 * and interacts securely with `ChatServer` logic to push state updates outward.
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Message message = (Message) in.readObject();
                handleMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected unexpectedly: " + username);
        } finally {
            cleanup();
        }
    }

    /**
     * Primary dispatcher linking abstract network intents to concrete private handler methods.
     */
    private void handleMessage(Message message) {
        switch (message.getType()) {
            case REGISTER:
                handleRegister(message);
                break;
            case LOGIN:
                handleLogin(message);
                break;
            case CREATE_OR_JOIN_ROOM:
                handleJoinRoom(message);
                break;
            case LEAVE_ROOM:
                handleLeaveRoom(true);
                break;
            case ROOM_MESSAGE:
                handleRoomMessage(message);
                break;
            case DELETE_ACCOUNT:
                handleDeleteAccount(message);
                break;
            default:
                break;
        }
    }

    private void handleRegister(Message message) {
        String reqUsername = message.getSender();
        String reqPassword = message.getPassword();
        if (server.getUserManager().register(reqUsername, reqPassword)) {
            sendMessage(new Message(MessageType.REGISTER_ACK, "Server", null, "Registration successful!", null, LocalDateTime.now()));
        } else {
            sendMessage(new Message(MessageType.REGISTER_REJECT, "Server", null, "Username already exists.", null, LocalDateTime.now()));
        }
    }

    private void handleDeleteAccount(Message message) {
        String reqUsername = message.getSender();
        String reqPassword = message.getPassword();
        if (server.getUserManager().deleteUser(reqUsername, reqPassword)) {
            sendMessage(new Message(MessageType.DELETE_ACCOUNT_ACK, "Server", null, "Account deleted successfully.", null, LocalDateTime.now()));
            
            // Remove them from the room silently and broadcast "went offline" instead of "explicitly left"
            String currentRoomName = server.getUserRoom(reqUsername);
            if (currentRoomName != null) {
                ChatRoom room = server.getOrCreateRoom(currentRoomName);
                room.removeMember(reqUsername, true);
                server.unanchorUserFromRoom(reqUsername);
                Message offlineMsg = new Message(MessageType.ROOM_MESSAGE, "System", null, reqUsername + " went offline.", room.getName(), LocalDateTime.now());
                room.broadcastMessage(offlineMsg);
            }
            
            server.logoutUser(reqUsername);
        } else {
            sendMessage(new Message(MessageType.ERROR, "Server", null, "Failed to delete account (Wrong password or user missing).", null, LocalDateTime.now()));
        }
    }

    private void handleLogin(Message message) {
        String reqUsername = message.getSender();
        String reqPassword = message.getPassword();

        if (server.loginUser(reqUsername, reqPassword, this)) {
            this.username = reqUsername;
            sendMessage(new Message(MessageType.LOGIN_ACK, "Server", null, "Login successful", null, LocalDateTime.now()));
            
            // Reattach to persistent room if applicable
            String existingRoom = server.getUserRoom(reqUsername);
            if (existingRoom != null) {
                // Tell the client to visually join the room
                sendMessage(new Message(MessageType.CREATE_OR_JOIN_ROOM, "Server", null, "Restored", existingRoom, LocalDateTime.now()));
                
                ChatRoom room = server.getOrCreateRoom(existingRoom);
                room.addMember(reqUsername);
                // System message notifying they are back online
                Message reMsg = new Message(MessageType.ROOM_MESSAGE, "System", null, username + " has reconnected to the server.", room.getName(), LocalDateTime.now());
                room.broadcastMessage(reMsg);
            }

            server.broadcastRoomList(this);
        } else {
            sendMessage(new Message(MessageType.LOGIN_REJECT, "Server", null, "Login Failed: Wrong password, user doesn't exist, or already online.", null, LocalDateTime.now()));
        }
    }

    private void handleJoinRoom(Message message) {
        String targetRoomName = message.getRoomName();
        if (targetRoomName != null && !targetRoomName.trim().isEmpty()) {
            handleLeaveRoom(false); // leave current explicit room first if any
            
            ChatRoom room = server.getOrCreateRoom(targetRoomName);
            room.addMember(username);
            server.anchorUserToRoom(username, targetRoomName);

            // Notify everyone
            Message joinMsg = new Message(MessageType.ROOM_MESSAGE, "System", null, username + " has joined the room.", room.getName(), LocalDateTime.now());
            room.broadcastMessage(joinMsg);
        }
    }

    private void handleLeaveRoom(boolean explicit) {
        String currentRoomName = server.getUserRoom(username);
        if (currentRoomName != null) {
            ChatRoom room = server.getOrCreateRoom(currentRoomName);
            room.removeMember(username, explicit);
            server.unanchorUserFromRoom(username);
            String text = explicit ? username + " has explicitly left the room." : username + " has left the room window.";
            Message leaveMsg = new Message(MessageType.ROOM_MESSAGE, "System", null, text, room.getName(), LocalDateTime.now());
            room.broadcastMessage(leaveMsg);
        }
    }

    private void handleRoomMessage(Message message) {
        String currentRoomName = server.getUserRoom(username);
        if (currentRoomName != null && currentRoomName.equals(message.getRoomName())) {
            ChatRoom room = server.getOrCreateRoom(currentRoomName);
            room.broadcastMessage(message);
        }
    }

    public synchronized void sendMessage(Message message) {
        try {
            if (out != null) {
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cleanup() {
        if (username != null) {
            // Note: We DO NOT call handleLeaveRoom() here! They stay in the room conceptually!
            // We just log them out so their socket is gone.
            server.logoutUser(username);
            
            // Notify room they went offline (optional) but we keep it silent or minimal
            String currentRoomName = server.getUserRoom(username);
            if(currentRoomName != null) {
                 ChatRoom room = server.getOrCreateRoom(currentRoomName);
                 Message offlineMsg = new Message(MessageType.ROOM_MESSAGE, "System", null, username + " went offline.", room.getName(), LocalDateTime.now());
                 room.broadcastMessage(offlineMsg);
            }
        }
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }
}
