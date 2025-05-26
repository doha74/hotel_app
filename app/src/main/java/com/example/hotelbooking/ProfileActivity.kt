package com.example.hotelbooking

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.hotelbooking.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    data class User(
        val name: String? = null,
        val email: String? = null,
        val postalCode: String? = null,
        val address: String? = null,
        val phoneNumber: String? = null,
        val profileImageUrl: String? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.editButton.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.LogOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
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
                    false
                }
                else -> false
            }
        }

        val currentUser = auth.currentUser
        currentUser?.let {
            val userId = it.uid
            database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let {
                            binding.nameTv.text = it.name
                            binding.emailValueTv.text = it.email
                            binding.phoneNumberValueTv.text = it.phoneNumber
                            binding.addressValueTv.text = it.address
                            binding.postalCodeValueTv.text = it.postalCode
                            it.profileImageUrl?.let { imageUrl ->
                                Glide.with(this@ProfileActivity)
                                    .load(imageUrl)
                                    .into(binding.profileImageView)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }
}
