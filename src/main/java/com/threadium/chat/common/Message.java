package com.threadium.chat.common;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A standard Data Transfer Object (DTO) used to communicate across network sockets.
 * It encapsulates everything related to a single packet of information sent between client and server.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;        // The protocol command/intent of the message
    private String sender;           // The username of the sender (or "Server" / "System")
    private String password;         // Only used for authentication packets (login/register/delete)
    private String content;          // The main payload/text of the message
    private String roomName;         // The target room for room-specific messages
    private LocalDateTime timestamp; // Ensure all messages retain chronological order

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
