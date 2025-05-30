package com.example.hotelbooking

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hotelbooking.adapters.BookingsAdapter
import com.example.hotelbooking.data.Booking
import com.example.hotelbooking.databinding.ActivityMyTripsBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyTripsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyTripsBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var bookingsAdapter: BookingsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyTripsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        // Setup Toolbar
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Setup Bottom Navigation
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, Dashboard::class.java))
                    true
                }
                R.id.myTrips -> true
                R.id.profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Setup RecyclerView
        bookingsAdapter = BookingsAdapter { booking, action ->
            when (action) {
                BookingAction.ChangeStatus -> changeBookingStatus(booking)
                BookingAction.Cancel -> cancelBooking(booking)
                BookingAction.Rate -> {
                    val intent = Intent(this, CommentActivity::class.java)
                    intent.putExtra("hotelId", booking.hotelId)
                    startActivity(intent)
                }
            }
        }
        binding.bookingsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MyTripsActivity)
            adapter = bookingsAdapter
        }

        // Load bookings
        loadBookings()
    }

    private fun loadBookings() {
        val userId = auth.currentUser?.uid ?: return
        val bookingsRef = database.reference.child("bookings")

        bookingsRef.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        binding.noBookingsTextView.visibility = View.VISIBLE
                        binding.bookingsRecyclerView.visibility = View.GONE
                        return
                    }

                    val bookings = mutableListOf<Booking>()
                    for (bookingSnapshot in snapshot.children) {
                        val bookingId = bookingSnapshot.key ?: continue
                        val hotelId = bookingSnapshot.child("hotelId").getValue(String::class.java) ?: ""
                        val roomId = bookingSnapshot.child("roomId").getValue(String::class.java) ?: ""
                        val isActive = bookingSnapshot.child("active").getValue(Boolean::class.java) ?: false
                        val departDate = bookingSnapshot.child("departDate").getValue(String::class.java) ?: ""
                        val returnDate = bookingSnapshot.child("returnDate").getValue(String::class.java) ?: ""

                        // Fetch hotel and room details
                        database.reference.child("hotels").child(hotelId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(hotelSnapshot: DataSnapshot) {
                                    val hotelName = hotelSnapshot.child("name").getValue(String::class.java) ?: ""
                                    val roomName = hotelSnapshot.child("rooms").child(roomId).child("name").getValue(String::class.java) ?: ""
                                    bookings.add(
                                        Booking(
                                            bookingId,
                                            hotelId,
                                            roomId,
                                            hotelName,
                                            roomName,
                                            isActive,
                                            departDate,
                                            returnDate
                                        )
                                    )
                                    bookingsAdapter.submitList(bookings)
                                    updateVisibility(bookings)
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Snackbar.make(binding.root, "Failed to load hotel data", Snackbar.LENGTH_SHORT).show()
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.noBookingsTextView.visibility = View.VISIBLE
                    binding.bookingsRecyclerView.visibility = View.GONE
                    Snackbar.make(binding.root, "Failed to load bookings", Snackbar.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateVisibility(bookings: List<Booking>) {
        if (bookings.isEmpty()) {
            binding.noBookingsTextView.visibility = View.VISIBLE
            binding.bookingsRecyclerView.visibility = View.GONE
        } else {
            binding.noBookingsTextView.visibility = View.GONE
            binding.bookingsRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun cancelBooking(booking: Booking) {
        val bookingRef = database.reference.child("bookings").child(booking.bookingId)
        bookingRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val roomRef = database.reference.child("hotels").child(booking.hotelId).child("rooms").child(booking.roomId)
                roomRef.child("availability").setValue(true).addOnCompleteListener { roomTask ->
                    if (roomTask.isSuccessful) {
                        bookingsAdapter.removeBooking(booking)
                        Snackbar.make(binding.root, "Booking cancelled", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(binding.root, "Failed to update room availability", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } else {
                Snackbar.make(binding.root, "Failed to cancel booking", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun changeBookingStatus(booking: Booking) {
        val bookingRef = database.reference.child("bookings").child(booking.bookingId)
        bookingRef.child("active").setValue(!booking.isActive).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                bookingsAdapter.updateBooking(booking.copy(isActive = !booking.isActive))
                Snackbar.make(binding.root, "Booking status updated", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, "Failed to update status", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}


