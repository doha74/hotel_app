package com.example.hotelbooking

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hotelbooking.databinding.ActivityPaymentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: FirebaseUser
    private lateinit var hotelId: String
    private lateinit var roomId: String
    private var departDate: String? = null
    private var returnDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser!!

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.payButton.setOnClickListener {
            saveBookingToDatabase()
        }

        hotelId = intent.getStringExtra("hotelId") ?: ""
        roomId = intent.getStringExtra("roomId") ?: ""
        departDate = intent.getStringExtra("departDate")
        returnDate = intent.getStringExtra("returnDate")
    }

    private fun saveBookingToDatabase() {
        val userId = currentUser.uid
        val bookingsRef = database.reference.child("bookings")
        val bookingId = bookingsRef.push().key ?: ""

        val bookingData = hashMapOf(
            "userId" to userId,
            "hotelId" to hotelId,
            "roomId" to roomId,
            "departDate" to (departDate ?: ""),
            "returnDate" to (returnDate ?: ""),
            "active" to true
        )

        bookingsRef.child(bookingId).setValue(bookingData)
            .addOnSuccessListener {
                updateRoomAvailability()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Booking Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRoomAvailability() {
        val roomRef = database.reference.child("hotels").child(hotelId).child("rooms").child(roomId)
        roomRef.child("availability").setValue(false)
            .addOnSuccessListener {
                Toast.makeText(this, "Booking Successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MyTripsActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update room availability: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
