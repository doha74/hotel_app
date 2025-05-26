package com.example.hotelbooking

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.hotelbooking.databinding.ActivityAdminPageBinding
import com.google.firebase.auth.FirebaseAuth

class AdminPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addHotelButton.setOnClickListener {
            val intent = Intent(this, AddHotelActivity::class.java)
            startActivity(intent)
        }

        binding.addRoomButton.setOnClickListener {
            val intent = Intent(this, AddRoomActivity::class.java)
            startActivity(intent)
        }

        // Logout button
        binding.logoutButton.setOnClickListener {
            // Çıkış yap
            FirebaseAuth.getInstance().signOut()

            // LoginActivity'e geri dön
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
