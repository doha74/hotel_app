package com.example.hotelbooking.data

data class Booking(
    val bookingId: String,
    val hotelId: String,
    val roomId: String,
    val hotelName: String,
    val roomName: String,
    val isActive: Boolean,
    val departDate: String,
    val returnDate: String
)