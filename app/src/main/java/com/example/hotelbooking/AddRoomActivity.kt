package com.example.hotelbooking

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.hotelbooking.databinding.ActivityAddRoomBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AddRoomActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddRoomBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private var selectedImageUri: Uri? = null
    private var hotelList: MutableList<String> = mutableListOf()
    private var hotelIdList: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference.child("room_images")

        loadHotelNames()

        binding.selectImageButton.setOnClickListener {
            openImageChooser()
        }

        binding.addRoomButton.setOnClickListener {
            val roomName = binding.roomName.text.toString()
            val roomPrice = binding.roomPrice.text.toString().toIntOrNull()
            val bedCount = binding.bedCount.text.toString().toIntOrNull()
            val wifiAvailable = binding.wifiSwitch.isChecked
            val bathroomCount = binding.bathroomCount.text.toString().toIntOrNull()
            val selectedHotelIndex = binding.hotelSpinner.selectedItemPosition

            if (roomName.isNotEmpty() && roomPrice != null && bedCount != null && bathroomCount != null && selectedHotelIndex >= 0 && selectedImageUri != null) {
                val selectedHotelId = hotelIdList[selectedHotelIndex]
                uploadImageToStorage(selectedHotelId, roomName, roomPrice, bedCount, wifiAvailable, bathroomCount)
            } else {
                Toast.makeText(this, "All fields are required and must be valid!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backToAdminPageButton.setOnClickListener {
            val intent = Intent(this, AdminPageActivity::class.java)
            startActivity(intent)
        }
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.selectedImageView.setImageURI(it)
            binding.selectedImageView.visibility = View.VISIBLE
        }
    }

    private fun openImageChooser() {
        getContent.launch("image/*")
    }

    private fun loadHotelNames() {
        database.reference.child("hotels").get().addOnSuccessListener { snapshot ->
            for (hotelSnapshot in snapshot.children) {
                hotelList.add(hotelSnapshot.child("name").getValue(String::class.java) ?: "")
                hotelIdList.add(hotelSnapshot.key ?: "")
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hotelList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.hotelSpinner.adapter = adapter
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load hotel names.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageToStorage(hotelId: String, roomName: String, roomPrice: Int, bedCount: Int, wifiAvailable: Boolean, bathroomCount: Int) {
        selectedImageUri?.let { uri ->
            val imageRef = storageRef.child("${hotelId}_${roomName}_${System.currentTimeMillis()}")

            imageRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                        val room = Room(roomName, uri.toString(), roomPrice, bedCount, wifiAvailable, bathroomCount, true)

                        database.reference.child("hotels").child(hotelId).child("rooms").push().setValue(room)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Room Added Successfully!", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to Add Room! ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to Upload Image! ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    data class Room(
        val name: String = "",
        val imageUrl: String = "",
        val price: Int = 0,
        val bedCount: Int = 0,
        val wifiAvailable: Boolean = false,
        val bathroomCount: Int = 0,
        val availability: Boolean = true
    )
}
