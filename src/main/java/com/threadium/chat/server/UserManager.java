package com.threadium.chat.server;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Lightweight authentication management utilizing serialized Map storage.
 * Synchronizes file reads and writes heavily using a ReentrantLock ensuring no
 * credential data is corrupted during parallel login/registration attempts.
 */
public class UserManager {
    private final File dataFile = new File("users.dat"); // Binary file for retaining mapped users
    private final Map<String, String> credentials = new ConcurrentHashMap<>();
    private final ReentrantLock fileLock = new ReentrantLock();

    public UserManager() {
        loadData();
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        if (!dataFile.exists()) return;
        fileLock.lock();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
            Map<String, String> loaded = (Map<String, String>) ois.readObject();
            credentials.putAll(loaded);
        } catch (Exception e) {
            System.err.println("Failed to load users: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    private void saveData() {
        fileLock.lock();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            oos.writeObject(credentials);
        } catch (IOException e) {
            System.err.println("Failed to save users: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    public boolean register(String username, String password) {
        if (credentials.containsKey(username)) return false;
        credentials.put(username, password);
        saveData();
        return true;
    }

    public boolean authenticate(String username, String password) {
        if (!credentials.containsKey(username)) return false;
        return credentials.get(username).equals(password);
    }

    public boolean deleteUser(String username, String password) {
        if (authenticate(username, password)) {
            credentials.remove(username);
            saveData();
            return true;
        }
        return false;
    }
}
