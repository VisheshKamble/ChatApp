package com.example.chatx

data class User(
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var lastMessage: String = "",
    var lastMessageTime: String = "",
    var pendingMessages: Int = 0
)

