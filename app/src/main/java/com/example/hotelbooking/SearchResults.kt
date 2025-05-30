package com.example.hotelbooking

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hotelbooking.adapters.HotelAdapter
import com.example.hotelbooking.data.Hotel
import com.example.hotelbooking.databinding.ActivitySearchResultsBinding
import com.google.firebase.database.*
import java.util.*

class SearchResults : AppCompatActivity() {
    private lateinit var binding: ActivitySearchResultsBinding
    private lateinit var database: FirebaseDatabase
    private var searchQuery: String? = null
    private var departDate: String? = null
    private var returnDate: String? = null
    private lateinit var hotelAdapter: HotelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance("https://ocp-resv-default-rtdb.europe-west1.firebasedatabase.app/")
        searchQuery = intent.getStringExtra("searchQuery")?.trim()?.lowercase(Locale.getDefault())
        departDate = intent.getStringExtra("departDate")
        returnDate = intent.getStringExtra("returnDate")

        // Initialize RecyclerView
        hotelAdapter = HotelAdapter(mutableListOf()) { hotel ->
            val intent = Intent(this, HotelPage::class.java).apply {
                putExtra("hotelId", hotel.id)
                putExtra("departDate", departDate)
                putExtra("returnDate", returnDate)
            }
            startActivity(intent)
        }
        binding.hotelRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchResults)
            adapter = hotelAdapter
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

        loadSearchResults()
    }

    private fun loadSearchResults() {
        database.reference.child("hotels").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val hotelsList = mutableListOf<Hotel>()
                for (hotelSnapshot in snapshot.children) {
                    val hotelId = hotelSnapshot.key ?: ""
                    val hotelName = hotelSnapshot.child("name").getValue(String::class.java)
                    val hotelLocation = hotelSnapshot.child("location").getValue(String::class.java)
                    val hotelImageUrl = hotelSnapshot.child("imageUrl").getValue(String::class.java)

                    if (hotelName != null && hotelLocation != null && hotelImageUrl != null) {
                        if (searchQuery.isNullOrEmpty() || hotelLocation.lowercase(Locale.getDefault()).contains(searchQuery!!)) {
                            hotelsList.add(Hotel(hotelId, hotelName, hotelLocation, hotelImageUrl))
                        }
                    }
                }
                hotelsList.reverse()
                hotelAdapter.updateHotels(hotelsList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error (e.g., show Toast)
            }
        })
    }

}