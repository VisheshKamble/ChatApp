package com.example.chatx


data class Message(
    var senderId: String = "",
    var receiverId: String = "",
    var message: String = "",
    var timestamp: Long = 0L,
    var seen: Boolean = false
)
