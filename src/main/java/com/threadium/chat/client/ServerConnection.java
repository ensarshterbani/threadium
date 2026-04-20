package com.threadium.chat.client;

import com.threadium.chat.common.Message;
import com.threadium.chat.common.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;

/**
 * Network facade for the JavaFX client application. It manages the Socket lifecycle and
 * spawns a daemon background thread that continuously reads from the ObjectInputStream.
 * Upon receiving a valid deserialized `Message`, it fires a synchronous callback interface  
 * to notify listening UI components. 
 */
public class ServerConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread listenerThread;
    private MessageListener listener;
    private String username;

    public interface MessageListener {
        void onMessageReceived(Message message);
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    public boolean connect(String host, int port) {
        if (socket != null && !socket.isClosed()) return true; // Already connected
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            listenerThread = new Thread(this::listenForMessages, "ClientListenerThread");
            listenerThread.setDaemon(true);
            listenerThread.start();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
            return false;
        }
    }

    /**
     * Infinite loop executed by the daemon listener Thread.
     * Blocks entirely on `in.readObject()` awaiting server push events.
     */
    private void listenForMessages() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Message message = (Message) in.readObject();
                if (listener != null) {
                    listener.onMessageReceived(message);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Disconnected from server.");
        } finally {
            close();
        }
    }

    public void sendLogin(String username, String password) {
        this.username = username;
        sendMessage(new Message(MessageType.LOGIN, username, password, "", null, LocalDateTime.now()));
    }

    public void sendRegister(String username, String password) {
        sendMessage(new Message(MessageType.REGISTER, username, password, "", null, LocalDateTime.now()));
    }

    public void sendDeleteAccount(String username, String password) {
        sendMessage(new Message(MessageType.DELETE_ACCOUNT, username, password, "", null, LocalDateTime.now()));
    }

    public void sendJoinRoom(String roomName) {
        sendMessage(new Message(MessageType.CREATE_OR_JOIN_ROOM, username, null, "", roomName, LocalDateTime.now()));
    }

    public void sendChatMessage(String content, String roomName) {
        sendMessage(new Message(MessageType.ROOM_MESSAGE, username, null, content, roomName, LocalDateTime.now()));
    }

    public void sendLeaveRoom() {
        sendMessage(new Message(MessageType.LEAVE_ROOM, username, null, "", null, LocalDateTime.now()));
    }

    private void sendMessage(Message message) {
        try {
            if (out != null) {
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (listenerThread != null) listenerThread.interrupt();
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            out = null;
            in = null;
            socket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
