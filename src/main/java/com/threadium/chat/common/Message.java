package com.threadium.chat.common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;
    private String sender;
    private String password; // used for auth packets
    private String content;
    private String roomName;
    private LocalDateTime timestamp;

    public Message(MessageType type, String sender, String password, String content, String roomName, LocalDateTime timestamp) {
        this.type = type;
        this.sender = sender;
        this.password = password;
        this.content = content;
        this.roomName = roomName;
        this.timestamp = timestamp;
    }

    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getPassword() {
        return password;
    }

    public String getContent() {
        return content;
    }

    public String getRoomName() {
        return roomName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", sender='" + sender + '\'' +
                ", content='" + content + '\'' +
                ", roomName='" + roomName + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
