package com.example.hotelbooking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.hotelbooking.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null
    private var selectedImageUri: Uri? = null
    private lateinit var storage: FirebaseStorage


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openImageChooser()
        } else {
            Toast.makeText(this, "Permission denied to access storage", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        loadUserData()

        binding.profileImageView.setOnClickListener {
            checkStoragePermission()
        }

        binding.saveButton.setOnClickListener {
            saveChanges()
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadUserData() {
        currentUser?.let { user ->
            val userId = user.uid
            database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let {
                            binding.nameInput.setText(it.name)
                            binding.emailInput.setText(it.email)
                            binding.phoneInput.setText(it.phoneNumber)
                            binding.addressInput.setText(it.address)
                            binding.postalCodeInput.setText(it.postalCode)
                            it.profileImageUrl?.let { imageUrl ->
                                Glide.with(this@EditProfileActivity)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_profile_image)
                                    .into(binding.profileImageView)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditProfileActivity, "Failed to load profile: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun checkStoragePermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                openImageChooser()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                selectedImageUri = result.data?.data
                selectedImageUri?.let {
                    Glide.with(this)
                        .load(it)
                        .placeholder(R.drawable.default_profile_image)
                        .into(binding.profileImageView)
                }
            }
        }

    private fun saveChanges() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val phoneNumber = binding.phoneInput.text.toString().trim()
        val address = binding.addressInput.text.toString().trim()
        val postalCode = binding.postalCodeInput.text.toString().trim()

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and email are required", Toast.LENGTH_SHORT).show()
            return
        }

        currentUser?.let { user ->
            val userId = user.uid
            val userUpdates = mapOf(
                "name" to name,
                "email" to email,
                "phoneNumber" to phoneNumber,
                "address" to address,
                "postalCode" to postalCode
            )

            database.child("users").child(userId).updateChildren(userUpdates)
                .addOnSuccessListener {
                    if (selectedImageUri != null) {
                        uploadImage(selectedImageUri!!, userId)
                    } else {
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            if (email != user.email) {
                user.updateEmail(email)
                    .addOnSuccessListener {
                        // Email updated in Firebase Auth
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update email: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun uploadImage(imageUri: Uri, userId: String) {
        val storageRef = storage.reference
        val imagesRef = storageRef.child("images/$userId/${UUID.randomUUID()}")
        val uploadTask = imagesRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            imagesRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()
                database.child("users").child(userId).child("profileImageUrl")
                    .setValue(downloadUrl)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}