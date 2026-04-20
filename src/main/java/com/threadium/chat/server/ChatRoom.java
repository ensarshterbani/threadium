package com.threadium.chat.server;

import com.threadium.chat.common.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents an active chat room managed by the server. 
 * Provides thread-safe interaction by utilizing ReentrantLocks for user membership 
 * and a BlockingQueue mapped to a background worker thread for robust sequential 
 * message broadcasting.
 */
public class ChatRoom {
    private final String name;                       // The unique name of the room
    private final ChatServer server;                 // Broad server context
    private final List<String> memberUsernames;      // List of users currently viewing/listening to the room
    private final BlockingQueue<Message> messageQueue; // Thread-safe queue absorbing rapid incoming messages
    private final List<Message> messageHistory;      // Maintains historical context for late joiners
    private final ReentrantLock lock;                // Ensures atomic operations over shared resources (members/history)
    private final Thread workerThread;               // Independent background thread to dispatch messages

    public ChatRoom(String name, ChatServer server) {
        this.name = name;
        this.server = server;
        this.memberUsernames = new ArrayList<>();
        this.messageQueue = new LinkedBlockingQueue<>();
        this.messageHistory = new ArrayList<>();
        this.lock = new ReentrantLock();

        // Dedicated worker thread for this room
        this.workerThread = new Thread(this::processMessages, "RoomThread-" + name);
        this.workerThread.start();
    }

    public String getName() {
        return name;
    }

    public void addMember(String username) {
        lock.lock();
        try {
            // Only add them if not already present
            if (!memberUsernames.contains(username)) {
                memberUsernames.add(username);
            }
            // Rapidly flush the entire recorded history to newly joined/reconnected member
            ClientHandler handler = server.getActiveClient(username);
            if (handler != null) {
                for (Message msg : messageHistory) {
                    handler.sendMessage(msg);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void removeMember(String username, boolean deleteIfEmpty) {
        boolean isEmpty = false;
        lock.lock();
        try {
            memberUsernames.remove(username);
            isEmpty = memberUsernames.isEmpty();
        } finally {
            lock.unlock();
        }

        if (isEmpty && deleteIfEmpty) {
            server.deleteRoom(this.name);
        }
    }

    public void shutdown() {
        workerThread.interrupt();
    }

    public void broadcastMessage(Message message) {
        try {
            // Place message in the blocking queue.
            messageQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * The core loop for the worker thread dedicated to this chat room.
     * Continuously waits (blocks) until a new message is placed into the queue,
     * then retrieves it safely and pushes the message to all currently connected active members.
     */
    private void processMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Blocks until a message is available
                Message message = messageQueue.take();
                
                // Save to history and keep members safe
                List<String> currentMembers;
                lock.lock();
                try {
                    messageHistory.add(message);
                    currentMembers = new ArrayList<>(memberUsernames);
                } finally {
                    lock.unlock();
                }

                // Push message to all users in the room who are currently online!
                for (String username : currentMembers) {
                    ClientHandler handler = server.getActiveClient(username);
                    if (handler != null) {
                        handler.sendMessage(message);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
