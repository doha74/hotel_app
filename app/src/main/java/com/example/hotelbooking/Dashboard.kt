package com.example.hotelbooking

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatActivity
import com.example.hotelbooking.databinding.ActivityDashboardBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar

class Dashboard : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private var departDate: String? = null
    private var returnDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView?.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> true
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

        // Gidiş tarihi için DatePicker
        binding.departOn.setOnClickListener {
            showDatePicker { date ->
                binding.departOn.setText(date)
                departDate = date
            }
        }

        // Dönüş tarihi için DatePicker
        binding.returnOn.setOnClickListener {
            showDatePicker { date ->
                binding.returnOn.setText(date)
                returnDate = date
            }
        }

        binding.searchButton.setOnClickListener {
            val city = binding.city.text.toString()
            val intent = Intent(this, SearchResults::class.java).apply {
                putExtra("searchQuery", city)
                putExtra("departDate", departDate)
                putExtra("returnDate", returnDate)
            }
            startActivity(intent)
        }
    }

    private fun showDatePicker(onDateSet: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
            val selectedDate = "${selectedDay}/${selectedMonth + 1}/${selectedYear}"
            onDateSet(selectedDate)
        }, year, month, day)

        datePickerDialog.show()
    }
}
