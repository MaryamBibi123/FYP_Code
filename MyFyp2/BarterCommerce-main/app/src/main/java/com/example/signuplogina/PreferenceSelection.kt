package com.example.signuplogina

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.signuplogina.databinding.ActivityPreferenceSelectionBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
class PreferenceSelection : AppCompatActivity() {

    private lateinit var binding: ActivityPreferenceSelectionBinding
    private val selectedCategories = mutableListOf<String>() // Store selected categories

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferenceSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up card click listeners for the 6 categories
        setupCardClickListeners()

        // Handle Continue button click
        binding.buttonContinue.setOnClickListener {
            if (selectedCategories.isNotEmpty()) {
                savePreferencesToFirebase()
            } else {
                Toast.makeText(this, "Please select at least one category.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCardClickListeners() {
        // Only 6 categories
        binding.cardElectronics.setOnClickListener {
            handleCardSelection(binding.cardElectronics, "Electronics")
        }

        binding.cardFurniture.setOnClickListener {
            handleCardSelection(binding.cardFurniture, "Furniture")
        }

        binding.cardCosmetics.setOnClickListener {
            handleCardSelection(binding.cardCosmetics, "Cosmetics")
        }

        binding.cardClothing.setOnClickListener {
            handleCardSelection(binding.cardClothing, "Clothing")
        }

        binding.cardBooks.setOnClickListener {
            handleCardSelection(binding.cardBooks, "Books")
        }

        binding.cardVehicles.setOnClickListener {
            handleCardSelection(binding.cardVehicles, "Sports")
        }
    }

    private fun handleCardSelection(card: CardView, category: String) {
        // Toggle card selection state
        if (selectedCategories.contains(category)) {
            selectedCategories.remove(category)
            card.setCardBackgroundColor(Color.parseColor("#FFFFFF")) // Unselected state
        } else {
            selectedCategories.add(category)
            card.setCardBackgroundColor(Color.parseColor("#19097E")) // Selected state
        }

        // Display the selected categories
        Toast.makeText(this, "$category Selected", Toast.LENGTH_SHORT).show()
    }

    private fun savePreferencesToFirebase() {
        Log.e("SavedPreference","true")
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val databaseRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
            databaseRef.child("category").setValue(selectedCategories)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Preferences saved successfully!", Toast.LENGTH_SHORT).show()
                        // Navigate to the next screen or perform any other action
//                        Login activity
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish() // Close the current activity
                    } else {
                        Toast.makeText(this, "Failed to save preferences.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
