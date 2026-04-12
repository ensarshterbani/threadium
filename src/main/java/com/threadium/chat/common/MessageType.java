package com.threadium.chat.common;

public enum MessageType {
    LOGIN,
    LOGIN_ACK,
    LOGIN_REJECT,
    REGISTER,
    REGISTER_ACK,
    REGISTER_REJECT,
    DELETE_ACCOUNT,
    DELETE_ACCOUNT_ACK,
    CREATE_OR_JOIN_ROOM,
    LEAVE_ROOM,
    ROOM_MESSAGE,
    ROOM_LIST_UPDATE,
    ERROR
}
