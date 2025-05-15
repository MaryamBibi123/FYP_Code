//package com.example.signuplogina.activities
//
//import android.app.ProgressDialog
//import android.content.Intent
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.util.Log
//import android.widget.Toast
//import androidx.databinding.DataBindingUtil
//import com.example.chatmessenger.MainActivity
//import com.example.chatmessenger.R
//import com.example.chatmessenger.databinding.ActivitySignInBinding
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
//import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.database.DatabaseReference
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.ValueEventListener
//
//class SignInActivity : AppCompatActivity() {
//
//    lateinit var name: String
//    lateinit var email: String
//    lateinit var password: String
//    lateinit private var fbauth: FirebaseAuth
//    lateinit private var pds: ProgressDialog
//    lateinit var binding: ActivitySignInBinding
//
//    // Add reference to Realtime Database
//    lateinit var database: FirebaseDatabase
//    lateinit var userRef: DatabaseReference
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)
//
//        fbauth = FirebaseAuth.getInstance()
//        database = FirebaseDatabase.getInstance()  // Initialize Realtime Database
//        userRef = database.reference.child("users")  // Reference to the "users" node
//
//        if (fbauth.currentUser != null) {
//            // If user is already signed in, start MainActivity
//            startActivity(Intent(this, MainActivity::class.java))
//        }
//
//        pds = ProgressDialog(this)
//
//        binding.signInTextToSignUp.setOnClickListener {
//            startActivity(Intent(this, SignUpActivity::class.java))
//        }
//
//        binding.loginButton.setOnClickListener {
//            email = binding.loginetemail.text.toString()
//            password = binding.loginetpassword.text.toString()
//
//            if (binding.loginetemail.text.isEmpty()) {
//                Toast.makeText(this, "Enter Email", Toast.LENGTH_SHORT).show()
//            }
//
//            if (binding.loginetpassword.text.isEmpty()) {
//                Toast.makeText(this, "Enter Password", Toast.LENGTH_SHORT).show()
//            }
//
//            if (binding.loginetemail.text.isNotEmpty() && binding.loginetpassword.text.isNotEmpty()) {
//                signIn(password, email)
//            }
//        }
//    }
//
//    private fun signIn(password: String, email: String) {
//        pds.show()
//        pds.setMessage("Signing In")
//
//        fbauth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
//
//            if (it.isSuccessful) {
//                // After successful sign-in, retrieve user data from Realtime Database
//                val currentUser = fbauth.currentUser
//                val userId = currentUser?.uid
//
//                if (userId != null) {
//                    // Fetch user data from Realtime Database
//                    userRef.child(userId)
//                        .addListenerForSingleValueEvent(object : ValueEventListener {
//                            override fun onDataChange(snapshot: DataSnapshot) {
//                                if (snapshot.exists()) {
//                                    // Assuming you have user data like name, email, etc.
//                                    val name = snapshot.child("name").value.toString()
//                                    // Store data if necessary or just pass it to the next activity
//                                    pds.dismiss()
//                                    startActivity(
//                                        Intent(
//                                            this@SignInActivity, MainActivity::class.java
//                                        )
//                                    )
//                                }
//                            }
//
//                            override fun onCancelled(error: DatabaseError) {
//                                pds.dismiss()
//                                Toast.makeText(
//                                    applicationContext,
//                                    "Failed to retrieve user data",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            }
//                        })
//                }
//            } else {
//                pds.dismiss()
//                Toast.makeText(applicationContext, "Invalid Credentials", Toast.LENGTH_SHORT).show()
//            }
//
//        }.addOnFailureListener { exception ->
//            when (exception) {
//                is FirebaseAuthInvalidCredentialsException -> {
//                    Toast.makeText(applicationContext, "Invalid Credentials", Toast.LENGTH_SHORT)
//                        .show()
//                }
//
//                else -> {
//                    // other exceptions
//                    Toast.makeText(applicationContext, "Auth Failed", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }
//
//    @Deprecated("Deprecated in Java")
//    override fun onBackPressed() {
//        super.onBackPressed()
//        pds.dismiss()
//        finish()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        pds.dismiss()
//    }
//}
