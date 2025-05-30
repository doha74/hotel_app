package com.example.hotelbooking

import android.os.Bundle
import android.util.Log
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hotelbooking.data.Comment
import com.example.hotelbooking.databinding.ActivityCommentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CommentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommentBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var hotelId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        hotelId = intent.getStringExtra("hotelId") ?: ""

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    startActivity(android.content.Intent(this, Dashboard::class.java))
                    true
                }
                R.id.myTrips -> {
                    startActivity(android.content.Intent(this, MyTripsActivity::class.java))
                    true
                }
                R.id.profile -> {
                    startActivity(android.content.Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            if (rating < 0.5f) binding.ratingBar.rating = 0.5f
            else if (rating > 5.0f) binding.ratingBar.rating = 5.0f
        }

        binding.sendButton.setOnClickListener {
            val commentText = binding.commentText.text.toString().trim()
            val rating = binding.ratingBar.rating.toDouble()

            if (commentText.isEmpty()) {
                binding.commentText.error = "Please enter a comment"
                return@setOnClickListener
            }

            if (rating < 0.5) {
                Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val comment = Comment(
                userId = userId,
                hotelId = hotelId,
                rating = rating,
                commentText = commentText
            )

            addCommentToDatabase(comment)
        }
    }

    private fun addCommentToDatabase(comment: Comment) {
        val databaseReference = database.reference.child("comments").push()
        val commentId = databaseReference.key

        if (commentId != null) {
            val hotelCommentReference = database.reference
                .child("hotels")
                .child(comment.hotelId)
                .child("comments")
                .child(commentId)
            hotelCommentReference.setValue(true)
                .addOnSuccessListener {
                    databaseReference.setValue(comment)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Feedback Successful!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save comment: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save comment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Failed to generate comment ID", Toast.LENGTH_SHORT).show()
            Log.e("CommentActivity", "Failed to get comment ID")
        }
    }
}