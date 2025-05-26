package com.example.hotelbooking

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.hotelbooking.databinding.ActivityHotelPageBinding
import com.google.firebase.database.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class HotelPage : AppCompatActivity() {
    private lateinit var binding: ActivityHotelPageBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var hotelId: String
    private var rooms: List<DataSnapshot> = listOf()

    private var roomListDialog: AlertDialog? = null
    private var selectedRoomId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHotelPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        hotelId = intent.getStringExtra("hotelId") ?: ""

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.roomListButton.setOnClickListener {
            showRoomsDialog()
        }

        binding.bookingNowButton.setOnClickListener {
            if (selectedRoomId != null) {
                val selectedRoom = rooms.find { it.key == selectedRoomId }
                val isAvailable = selectedRoom?.child("availability")?.getValue(Boolean::class.java) ?: false

                if (isAvailable) {
                    val intent = Intent(this, PaymentActivity::class.java)
                    intent.putExtra("hotelId", hotelId)
                    intent.putExtra("roomId", selectedRoomId)
                    startActivity(intent)
                } else {
                    showAvailabilityWarning()
                }
            } else {
                showRoomSelectionWarning()
            }
        }

        loadHotelData()
        loadAverageRating()
        loadComments()
    }

    private fun loadHotelData() {
        val hotelRef = database.reference.child("hotels").child(hotelId)

        hotelRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hotelName = snapshot.child("name").getValue(String::class.java) ?: ""
                val hotelImageUrl = snapshot.child("imageUrl").getValue(String::class.java) ?: ""

                binding.titleTxt.text = hotelName
                Glide.with(this@HotelPage).load(hotelImageUrl).into(binding.picDetail)

                rooms = snapshot.child("rooms").children.toList()

                // Set default hotel image if no room is selected
                if (selectedRoomId == null) {
                    Glide.with(this@HotelPage).load(hotelImageUrl).into(binding.picDetail)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun showRoom(roomSnapshot: DataSnapshot) {
        val roomName = roomSnapshot.child("name").getValue(String::class.java) ?: ""
        val bathroomCount = roomSnapshot.child("bathroomCount").getValue(Int::class.java) ?: 0
        val bedCount = roomSnapshot.child("bedCount").getValue(Int::class.java) ?: 0
        val wifiAvailable = roomSnapshot.child("wifiAvailable").getValue(Boolean::class.java) ?: false
        val price = roomSnapshot.child("price").getValue(Int::class.java) ?: 0
        val roomImageUrl = roomSnapshot.child("imageUrl").getValue(String::class.java) ?: ""

        binding.roomNameTextView.text = roomName
        binding.bathroomCountTextView.text = "Bathroom Count: $bathroomCount"
        binding.bedCountTextView.text = "Bed Count: $bedCount"
        binding.wifiAvailabilityTextView.text = "WiFi Available: ${if (wifiAvailable) "Yes" else "No"}"
        binding.priceTextView.text = "Price: $$price"

        Glide.with(this).load(roomImageUrl).into(binding.picDetail)
    }

    private fun showRoomsDialog() {
        val sortedRooms = rooms.sortedBy { it.child("price").getValue(Int::class.java) ?: 0 }
        val roomIds = sortedRooms.map { it.key }

        val roomNamesAndPrices = sortedRooms.map {
            val roomName = it.child("name").getValue(String::class.java) ?: "Unknown"
            val price = it.child("price").getValue(Int::class.java) ?: 0
            "$roomName - $$price"
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select a Room")

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, roomNamesAndPrices)
        val listView = ListView(this)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            selectedRoomId = roomIds[position]
            showRoom(sortedRooms[position])
            roomListDialog?.dismiss()
        }

        builder.setView(listView)
        roomListDialog = builder.create()
        roomListDialog?.show()
    }

    private fun loadAverageRating() {
        val commentsRef = database.reference.child("comments").orderByChild("hotelId").equalTo(hotelId)

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalRating = 0.0
                var ratingCount = 0

                for (commentSnapshot in snapshot.children) {
                    val rating = commentSnapshot.child("rating").getValue(Double::class.java) ?: 0.0
                    totalRating += rating
                    ratingCount++
                }

                if (ratingCount > 0) {
                    val averageRating = totalRating / ratingCount
                    binding.averageRatingTextView.text = "Average Rating: %.1f".format(averageRating)
                } else {
                    binding.averageRatingTextView.text = "Average Rating: N/A"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun loadComments() {
        val hotelCommentsRef = database.reference.child("hotels").child(hotelId).child("comments")

        hotelCommentsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val commentIds = snapshot.children.map { it.key ?: "" }

                val commentsRef = database.reference.child("comments")

                commentsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(commentsSnapshot: DataSnapshot) {
                        val comments = mutableListOf<String>()
                        for (commentId in commentIds) {
                            val commentSnapshot = commentsSnapshot.child(commentId)
                            val commentText = commentSnapshot.child("commentText").getValue(String::class.java) ?: ""
                            val rating = commentSnapshot.child("rating").getValue(Double::class.java) ?: 0.0
                            comments.add("Rating: $rating\nComment: $commentText")
                        }
                        val adapter = ArrayAdapter(this@HotelPage, android.R.layout.simple_list_item_1, comments)
                        binding.commentsListView.adapter = adapter
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Handle error
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun showRoomSelectionWarning() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select a Room")
            .setMessage("Please select a room before booking.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showAvailabilityWarning() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Room Not Available")
            .setMessage("The selected room is currently not available.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }
}
