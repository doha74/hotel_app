package com.example.hotelbooking

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hotelbooking.databinding.ActivityHotelPageBinding
import com.example.hotelbooking.databinding.ItemCommentBinding
import com.google.firebase.database.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class HotelPage : AppCompatActivity() {
    private lateinit var binding: ActivityHotelPageBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var hotelId: String
    private var rooms: List<DataSnapshot> = listOf()
    private var selectedRoomId: String? = null
    private lateinit var commentsAdapter: CommentsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHotelPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        hotelId = intent.getStringExtra("hotelId") ?: ""
        setupRecyclerView()
        setupClickListeners()
        loadHotelData()
    }

    private fun setupRecyclerView() {
        commentsAdapter = CommentsAdapter()
        binding.commentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HotelPage)
            adapter = commentsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }

        binding.roomListButton.setOnClickListener { showRoomsDialog() }

        binding.bookingNowButton.setOnClickListener {
            if (selectedRoomId != null) {
                val selectedRoom = rooms.find { it.key == selectedRoomId }
                if (selectedRoom?.child("availability")?.getValue(Boolean::class.java) == true) {
                    startActivity(Intent(this, PaymentActivity::class.java).apply {
                        putExtra("hotelId", hotelId)
                        putExtra("roomId", selectedRoomId)
                    })
                } else {
                    showAvailabilityWarning()
                }
            } else {
                showRoomSelectionWarning()
            }
        }
    }

    private fun loadHotelData() {
        database.reference.child("hotels").child(hotelId).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.child("name").getValue(String::class.java)?.let {
                        binding.titleTxt.text = it
                    }
                    snapshot.child("imageUrl").getValue(String::class.java)?.let { url ->
                        Glide.with(this@HotelPage).load(url).into(binding.picDetail)
                    }
                    snapshot.child("address").getValue(String::class.java)?.let {
                        binding.addressTxt.text = it
                    }

                    rooms = snapshot.child("rooms").children.toList()
                    loadAverageRating()
                    loadComments()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HotelPage, "Failed to load hotel data", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun showRoom(roomSnapshot: DataSnapshot) {
        binding.roomCard.visibility = View.VISIBLE
        binding.roomNameTextView.text = roomSnapshot.child("name").getValue(String::class.java)
        binding.bedCountTextView.text = "${roomSnapshot.child("bedCount").getValue(Int::class.java)} Bed"
        binding.bathroomCountTextView.text = "${roomSnapshot.child("bathroomCount").getValue(Int::class.java)} Bath"
        binding.wifiAvailabilityTextView.text = if (roomSnapshot.child("wifiAvailable").getValue(Boolean::class.java) == true) "WiFi" else "No WiFi"
        binding.priceTextView.text = "$${roomSnapshot.child("price").getValue(Int::class.java)} / Month"

        roomSnapshot.child("imageUrl").getValue(String::class.java)?.let { url ->
            Glide.with(this).load(url).into(binding.picDetail)
        }
    }

    private fun showRoomsDialog() {
        val sortedRooms = rooms.sortedBy { it.child("price").getValue(Int::class.java) ?: 0 }
        val roomNames = sortedRooms.map {
            "${it.child("name").getValue(String::class.java)} - $${it.child("price").getValue(Int::class.java)}/mo"
        }

        AlertDialog.Builder(this)
            .setTitle("Select a Room")
            .setItems(roomNames.toTypedArray()) { _, position ->
                selectedRoomId = sortedRooms[position].key
                showRoom(sortedRooms[position])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadAverageRating() {
        database.reference.child("comments")
            .orderByChild("hotelId").equalTo(hotelId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val (totalRating, count) = snapshot.children.fold(0.0 to 0) { (sum, cnt), comment ->
                        (sum + (comment.child("rating").getValue(Double::class.java) ?: 0.0)) to (cnt + 1)
                    }

                    binding.averageRatingTextView.text = when {
                        count > 0 -> "★ ${String.format("%.1f", totalRating / count)} ($count reviews)"
                        else -> "No reviews yet"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.averageRatingTextView.text = "Rating unavailable"
                }
            })
    }

    private fun loadComments() {
        database.reference.child("hotels").child(hotelId).child("comments")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val commentIds = snapshot.children.mapNotNull { it.key }
                    if (commentIds.isEmpty()) {
                        commentsAdapter.submitList(emptyList())
                        return
                    }

                    database.reference.child("comments").addListenerForSingleValueEvent(
                        object : ValueEventListener {
                            override fun onDataChange(commentsSnapshot: DataSnapshot) {
                                val comments = commentIds.mapNotNull { id ->
                                    commentsSnapshot.child(id).let { comment ->
                                        val rating = comment.child("rating").getValue(Double::class.java)
                                        val text = comment.child("commentText").getValue(String::class.java)
                                        if (rating != null && text != null) Comment(rating, text) else null
                                    }
                                }
                                commentsAdapter.submitList(comments)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                commentsAdapter.submitList(emptyList())
                            }
                        }
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    commentsAdapter.submitList(emptyList())
                }
            })
    }

    private fun showRoomSelectionWarning() {
        AlertDialog.Builder(this)
            .setTitle("Select a Room")
            .setMessage("Please select a room before booking.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAvailabilityWarning() {
        AlertDialog.Builder(this)
            .setTitle("Room Not Available")
            .setMessage("The selected room is currently not available.")
            .setPositiveButton("OK", null)
            .show()
    }
}

data class Comment(val rating: Double, val text: String)

class CommentsAdapter : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {
    private var comments: List<Comment> = emptyList()

    fun submitList(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.bind(comment)
    }

    override fun getItemCount(): Int = comments.size

    class CommentViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: Comment) {
            binding.commentRatingTextView.text = "★ ${String.format("%.1f", comment.rating)}"
            binding.commentTextView.text = comment.text
        }
    }
}