package com.example.hotelbooking.data

data class Comment(
    val userId: String = "",
    val hotelId: String = "",
    val rating: Double = 0.0,
    val commentText: String = "",
    var text: String = ""
)