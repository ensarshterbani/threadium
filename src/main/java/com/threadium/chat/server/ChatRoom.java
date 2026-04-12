package com.threadium.chat.server;

import com.threadium.chat.common.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class ChatRoom {
    private final String name;
    private final ChatServer server;
    private final List<String> memberUsernames;
    private final BlockingQueue<Message> messageQueue;
    private final List<Message> messageHistory;
    private final ReentrantLock lock;
    private final Thread workerThread;

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
            if (!memberUsernames.contains(username)) {
                memberUsernames.add(username);
            }
            // Send entire history to newly joined/reconnected member
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

    public void removeMember(String username) {
        lock.lock();
        try {
            memberUsernames.remove(username);
        } finally {
            lock.unlock();
        }
    }

    public void broadcastMessage(Message message) {
        try {
            // Place message in the blocking queue.
            messageQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

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
