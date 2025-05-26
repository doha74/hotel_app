package com.example.hotelbooking

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hotelbooking.databinding.ActivityMyTripsBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyTripsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyTripsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyTripsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        binding.backButton.setOnClickListener {
            finish()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView?.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, Dashboard::class.java))
                    true
                }
                R.id.myTrips -> {
                    true
                }
                R.id.profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        loadBookings()
    }

    private fun loadBookings() {
        val userId = auth.currentUser?.uid
        val bookingsRef = database.reference.child("bookings")

        bookingsRef.orderByChild("userId").equalTo(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    binding.noBookingsTextView.visibility = View.VISIBLE
                    return
                }

                for (bookingSnapshot in snapshot.children) {
                    val isActive = bookingSnapshot.child("active").getValue(Boolean::class.java) ?: false
                    val hotelId = bookingSnapshot.child("hotelId").getValue(String::class.java) ?: ""
                    val roomId = bookingSnapshot.child("roomId").getValue(String::class.java) ?: ""
                    val departDate = bookingSnapshot.child("departDate").getValue(String::class.java) ?: ""
                    val returnDate = bookingSnapshot.child("returnDate").getValue(String::class.java) ?: ""

                    addBookingToLayout(bookingSnapshot.key ?: "", hotelId, roomId, isActive, departDate, returnDate)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun addBookingToLayout(bookingId: String, hotelId: String, roomId: String, isActive: Boolean, departDate: String, returnDate: String) {
        val hotelRef = database.reference.child("hotels").child(hotelId)
        hotelRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hotelName = snapshot.child("name").getValue(String::class.java) ?: ""
                val roomName = snapshot.child("rooms").child(roomId).child("name").getValue(String::class.java) ?: ""
                val bookingDates = "$departDate - $returnDate"

                val bookingView = LayoutInflater.from(this@MyTripsActivity).inflate(R.layout.booking_item, null)
                bookingView.findViewById<TextView>(R.id.hotelNameTextView).text = hotelName
                bookingView.findViewById<TextView>(R.id.roomNameTextView).text = roomName
                bookingView.findViewById<TextView>(R.id.bookingDatesTextView).text = bookingDates

                val statusButton = bookingView.findViewById<Button>(R.id.statusButton)
                val cancelButton = bookingView.findViewById<Button>(R.id.cancelBookingButton)
                val rateButton = bookingView.findViewById<Button>(R.id.rateAndCommentButton)

                // Durum düğmesinin tıklama dinleyicisi
                statusButton.setOnClickListener {
                    changeBookingStatus(bookingId, isActive, cancelButton, rateButton, statusButton)
                }

                // Rezervasyonu iptal etme düğmesinin tıklama dinleyicisi
                cancelButton.setOnClickListener {
                    cancelBooking(bookingId, bookingView, hotelId, roomId)
                }

                // Otele puan ve yorum ekleme düğmesinin tıklama dinleyicisi
                rateButton.setOnClickListener {
                    val intent = Intent(this@MyTripsActivity, CommentActivity::class.java)
                    intent.putExtra("hotelId", hotelId)
                    startActivity(intent)
                }

                // Başlangıçta düğme görünürlüğünü ve renklerini ayarla
                if (isActive) {
                    cancelButton.visibility = View.VISIBLE
                    cancelButton.setBackgroundColor(ContextCompat.getColor(this@MyTripsActivity, R.color.red))
                    statusButton.text = "Change Status"
                    statusButton.setBackgroundColor(ContextCompat.getColor(this@MyTripsActivity, R.color.dark_blue))
                } else {
                    rateButton.visibility = View.VISIBLE
                    rateButton.setBackgroundColor(ContextCompat.getColor(this@MyTripsActivity, R.color.dark_blue))
                    statusButton.text = "Change Status"
                    statusButton.setBackgroundColor(ContextCompat.getColor(this@MyTripsActivity, R.color.dark_blue))
                }

                binding.profileLl.addView(bookingView)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun cancelBooking(bookingId: String, bookingView: View, hotelId: String, roomId: String) {
        val bookingRef = database.reference.child("bookings").child(bookingId)
        bookingRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val roomRef = database.reference.child("hotels").child(hotelId).child("rooms").child(roomId)
                roomRef.child("availability").setValue(true).addOnCompleteListener { roomTask ->
                    if (roomTask.isSuccessful) {
                        binding.profileLl.removeView(bookingView)
                    } else {
                        // Handle error
                    }
                }
            } else {
                // Handle error
            }
        }
    }

    private fun changeBookingStatus(bookingId: String, isActive: Boolean, cancelButton: Button, rateButton: Button, statusButton: Button) {
        val bookingRef = database.reference.child("bookings").child(bookingId)
        bookingRef.child("active").setValue(!isActive).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (!isActive) {
                    rateButton.visibility = View.GONE
                    cancelButton.visibility = View.VISIBLE
                    cancelButton.setBackgroundColor(ContextCompat.getColor(this@MyTripsActivity, R.color.red))
                } else {
                    cancelButton.visibility = View.GONE
                    rateButton.visibility = View.VISIBLE
                    rateButton.setBackgroundColor(ContextCompat.getColor(this@MyTripsActivity, R.color.dark_blue))
                }
                statusButton.text = "Change Status"
                statusButton.setBackgroundColor(ContextCompat.getColor(this@MyTripsActivity, R.color.dark_blue))
            } else {
                // Handle error
            }
        }
    }
}
