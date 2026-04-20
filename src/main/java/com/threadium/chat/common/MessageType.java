package com.threadium.chat.common;

/**
 * Enumeration of all possible message types exchanged between the client and the server.
 * This acts as the command/protocol definition for the chat system.
 */
public enum MessageType {
    LOGIN,                  // Client requests to log in
    LOGIN_ACK,              // Server acknowledges successful login
    LOGIN_REJECT,           // Server rejects login (e.g., wrong password, already online)
    REGISTER,               // Client requests to register a new account
    REGISTER_ACK,           // Server acknowledges successful registration
    REGISTER_REJECT,        // Server rejects registration (e.g., user already exists)
    DELETE_ACCOUNT,         // Client requests to delete their account
    DELETE_ACCOUNT_ACK,     // Server confirms account deletion
    CREATE_OR_JOIN_ROOM,    // Client requests to join a room, or server forces client to join a room
    LEAVE_ROOM,             // Client explicitly leaves their current room
    ROOM_MESSAGE,           // A standard chat message sent within a room
    ROOM_LIST_UPDATE,       // Server broadcasts the updated list of available rooms to clients
    ERROR                   // Generic error message from the server
}
