package com.example.signuplogina

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.signuplogina.databinding.ActivityUserprofileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.squareup.picasso.Picasso

class UserProfileActivity : AppCompatActivity() {
    private lateinit var textViewWelcome: TextView
    private lateinit var textViewFullName: TextView
    private lateinit var textViewEmail: TextView
    private lateinit var textViewDob: TextView
    private lateinit var textViewGender: TextView
    private lateinit var textViewMobile: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var binding: ActivityUserprofileBinding
    private lateinit var authProfile: FirebaseAuth
    private lateinit var imageView: ImageView
    private lateinit var buttonToData: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserprofileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Home"

        // Initialize views and FirebaseAuth
        textViewWelcome = binding.textViewShowWelcome
        textViewFullName = binding.textViewShowFullName
        textViewEmail = binding.textViewShowEmail
        textViewDob = binding.textViewShowDob
        textViewGender = binding.textViewShowGender
        textViewMobile = binding.textViewShowMobile
        progressBar = binding.progressBar

//        buttonToData.setOnClickListener {
//            val intent=Intent(this@UserProfileActivity,AddItemFragment::class.java)
//            startActivity(intent)
//        }
        // Set On clickListener on imageView to open UploadProfilePic Activity
        imageView = binding.imageViewProfileDp
        imageView.setOnClickListener {
            val intent = Intent(this@UserProfileActivity, AddItemFragment::class.java)
            startActivity(intent)// we are not finishing the current activity , once the user set the profile picture then he can return back here

        }

        val backButton: ImageView = findViewById(R.id.imageView_Back)
        backButton.setOnClickListener {
//            onBackPressed() // Go back to the previous activity or fragment
        }
        authProfile = FirebaseAuth.getInstance()

        val firebaseUser: FirebaseUser? = authProfile.currentUser

        if (firebaseUser == null) {
            Toast.makeText(
                this,
                "Something went wrong. User details are not available",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            progressBar.visibility = View.VISIBLE
            showUserProfile(firebaseUser)
        }
    }

    private fun showUserProfile(firebaseUser: FirebaseUser) {
        val userID = firebaseUser.uid
        val referenceProfile: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("Users").child(userID)

        referenceProfile.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val readUserDetails: User? = snapshot.getValue(User::class.java)

                if (readUserDetails != null) {
                    val fullName = readUserDetails.fullName
                    val email = firebaseUser.email ?: "Unknown"
                    val gender = readUserDetails.gender
                    val dob = readUserDetails.dob
                    val mobile = readUserDetails.mobile

                    textViewWelcome.text = "Welcome, $fullName!"
                    textViewFullName.text = fullName
                    textViewEmail.text = email
                    textViewGender.text = gender
                    textViewDob.text = dob
                    textViewMobile.text = mobile

                    //set User Dp( After User has uploaded
//set User Dp (After User has uploaded)
                    val uri: Uri? = firebaseUser.photoUrl
                    if (uri != null) {
                        // Load the image using Picasso
                        Picasso.get().load(uri).into(imageView)
                    } else {
                        // Use a placeholder or default image if no profile picture is available
                        imageView.setImageResource(R.drawable.default_profile_image_background) // Replace with your default image resource
                    }


                } else {
                    Toast.makeText(
                        this@UserProfileActivity,
                        "User details not found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@UserProfileActivity,
                    "Something went wrong: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                progressBar.visibility = View.GONE
            }
        })
    }

    // creating ActionBar Menu

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // inflate menu items
        menuInflater.inflate(R.menu.common_menu, menu)
        return super.onCreateOptionsMenu(menu)

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_refresh -> {
                // refresh activity
                startActivity(intent)
                finish()
                true
            }

            R.id.menu_update_profile -> {
                val intent = Intent(this@UserProfileActivity, UpdateProfileActivity::class.java)
                startActivity(intent)
                true
            }
        R.id.menu_update_email -> {
            val intent = Intent(this@UserProfileActivity, UpdateEmailActivity::class.java)
            startActivity(intent)
            true
        }
            R.id.menu_settings -> {
                Toast.makeText(this, "menu settings", Toast.LENGTH_SHORT).show()
                true
            }
        R.id.menu_change_pass -> {
            val intent = Intent(this@UserProfileActivity, ChangePasswordActivity::class.java)
            startActivity(intent)
            true
        }
        R.id.menu_delete_profile -> {
            val intent = Intent(this@UserProfileActivity, DeleteProfileActivity::class.java)
            startActivity(intent)
            true
        }
            R.id.menu_logout -> {
                authProfile.signOut()
                Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@UserProfileActivity, SignUpLogin::class.java)
                // clear the stack to prevent the user from coming back to the UserProfileActivity
                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish() // close UserProfileActivity
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}


