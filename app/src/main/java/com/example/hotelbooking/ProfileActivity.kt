package com.example.hotelbooking

import android.content.Intent
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
            finishAffinity()
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
                            binding.nameTv.text = it.name ?: "Name not set"
                            binding.emailValueTv.text = it.email ?: "Email not set"
                            binding.phoneNumberValueTv.text = it.phoneNumber ?: "Phone not set"
                            binding.addressValueTv.text = it.address ?: "Address not set"
                            binding.postalCodeValueTv.text = it.postalCode ?: "Postal code not set"
                            it.profileImageUrl?.let { imageUrl ->
                                Glide.with(this@ProfileActivity)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_profile_image)
                                    .into(binding.profileImageView)
                            }
                        }
                    } else {
                        binding.nameTv.text = "Name not set"
                        binding.emailValueTv.text = currentUser.email ?: "Email not set"
                        binding.phoneNumberValueTv.text = "Phone not set"
                        binding.addressValueTv.text = "Address not set"
                        binding.postalCodeValueTv.text = "Postal code not set"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.nameTv.text = "Name not set"
                    binding.emailValueTv.text = currentUser.email ?: "Email not set"
                    binding.phoneNumberValueTv.text = "Phone not set"
                    binding.addressValueTv.text = "Address not set"
                    binding.postalCodeValueTv.text = "Postal code not set"
                }
            })
        } ?: run {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}