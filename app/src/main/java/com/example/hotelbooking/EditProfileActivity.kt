package com.example.hotelbooking

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

    data class User(
        val name: String? = null,
        val email: String? = null,
        val phoneNumber: String? = null,
        val address: String? = null,
        val postalCode: String? = null,
        val profileImageUrl: String? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance()

        currentUser?.let { user ->
            val userId = user.uid
            database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let {
                            binding.nameEditText.setText(it.name)
                            binding.emailEditText.setText(it.email)
                            binding.phoneNumberEditText.setText(it.phoneNumber)
                            binding.addressEditText.setText(it.address)
                            binding.postalCodeEditText.setText(it.postalCode)
                            it.profileImageUrl?.let { imageUrl ->
                                binding.profileImageButton.setImageURI(Uri.parse(imageUrl))
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }

        binding.profileImageButton.setOnClickListener {
            openImageChooser()
        }

        binding.saveButton.setOnClickListener {
            saveChanges()
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageData = result.data!!.data
                imageData?.let {
                    selectedImageUri = it
                    binding.profileImageButton.setImageURI(selectedImageUri)
                }
            }
        }

    private fun saveChanges() {
        currentUser?.let { user ->
            val userId = user.uid

            val name = binding.nameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val phoneNumber = binding.phoneNumberEditText.text.toString()
            val address = binding.addressEditText.text.toString()
            val postalCode = binding.postalCodeEditText.text.toString()

            val userUpdates = mapOf<String, Any>(
                "/users/$userId/name" to name,
                "/users/$userId/email" to email,
                "/users/$userId/phoneNumber" to phoneNumber,
                "/users/$userId/address" to address,
                "/users/$userId/postalCode" to postalCode
            )
            database.updateChildren(userUpdates)
                .addOnSuccessListener {
                    if (selectedImageUri != null) {
                        uploadImage(selectedImageUri!!, userId)
                    } else {
                        finish()
                    }
                }
                .addOnFailureListener {
                    // Güncelleme başarısız
                }
        }
    }

    private fun uploadImage(imageUri: Uri, userId: String) {
        val storageRef = storage.reference
        val imagesRef = storageRef.child("images/${UUID.randomUUID()}")
        val uploadTask = imagesRef.putFile(imageUri)

        uploadTask.addOnSuccessListener { _ ->
            imagesRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()
                database.child("users").child(userId).child("profileImageUrl")
                    .setValue(downloadUrl)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        // Hata durumunda
                    }
            }
        }.addOnFailureListener {
            // Hata durumunda
        }
    }
}
