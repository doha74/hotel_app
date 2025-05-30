package com.example.hotelbooking

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hotelbooking.databinding.ActivityPaymentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private var hotelId: String? = null
    private var roomId: String? = null
    private var departDate: String? = null
    private var returnDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        hotelId = intent.getStringExtra("hotelId")
        roomId = intent.getStringExtra("roomId")
        departDate = intent.getStringExtra("departDate")
        returnDate = intent.getStringExtra("returnDate")

        if (hotelId.isNullOrEmpty() || roomId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid booking details", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, Dashboard::class.java))
                    true
                }
                R.id.myTrips -> {
                    startActivity(Intent(this, MyTripsActivity::class.java))
                    true
                }
                R.id.profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        binding.payButton.setOnClickListener {
            validateAndSaveBooking()
        }
    }

    private fun validateAndSaveBooking() {
        val cardNumber = binding.cardNumberInput.text.toString().trim()
        val cardHolder = binding.cardHolderInput.text.toString().trim()
        val expiryDate = binding.expiryDateInput.text.toString().trim()
        val cvv = binding.cvvInput.text.toString().trim()

        if (cardNumber.length != 16) {
            binding.cardNumberInputLayout.error = "Enter a valid 16-digit card number"
            return
        }
        if (cardHolder.isEmpty()) {
            binding.cardHolderInputLayout.error = "Enter cardholder name"
            return
        }
        if (!expiryDate.matches(Regex("\\d{2}/\\d{2}"))) {
            binding.expiryDateInputLayout.error = "Enter valid expiry date (MM/YY)"
            return
        }
        if (cvv.length != 3) {
            binding.cvvInputLayout.error = "Enter a valid 3-digit CVV"
            return
        }

        val userId = auth.currentUser!!.uid
        val bookingsRef = database.reference.child("bookings")
        val bookingId = bookingsRef.push().key ?: return

        val bookingData = hashMapOf(
            "userId" to userId,
            "hotelId" to hotelId,
            "roomId" to roomId,
            "departDate" to (departDate ?: ""),
            "returnDate" to (returnDate ?: ""),
            "active" to true,
            "cardLastFour" to cardNumber.takeLast(4)
        )

        bookingsRef.child(bookingId).setValue(bookingData)
            .addOnSuccessListener {
                updateRoomAvailability()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Booking failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRoomAvailability() {
        val roomRef = database.reference.child("hotels").child(hotelId!!).child("rooms").child(roomId!!)
        roomRef.child("availability").setValue(false)
            .addOnSuccessListener {
                Toast.makeText(this, "Booking successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MyTripsActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update room availability: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}