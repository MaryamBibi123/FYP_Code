package com.example.signuplogina

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.signuplogina.databinding.ActivityUpdateEmailBinding
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class UpdateEmailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateEmailBinding
    private lateinit var authProfile: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewAuthenticated: TextView
    private lateinit var userOldEmail: String
    private lateinit var userNewEmail: String
    private lateinit var userPassword: String
    private lateinit var buttonUpdateEmail: Button
    private lateinit var editTextNewEmail: EditText
    private lateinit var editTextPwd: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressBar = binding.progressBar
        editTextPwd = binding.editTextUpdateEmailVerifyPassword
        editTextNewEmail = binding.editTextUpdateEmailNew
        textViewAuthenticated = binding.textViewUpdateEmailAuthenticated
        buttonUpdateEmail = binding.buttonUpdateEmail

        buttonUpdateEmail.isEnabled = false
        editTextNewEmail.isEnabled = false

        authProfile = FirebaseAuth.getInstance()
        firebaseUser = authProfile.currentUser!!

        val backButton: ImageView = findViewById(R.id.imageView_Back)
        backButton.setOnClickListener {
            onBackPressed() // Go back to the previous activity or fragment
        }

        // Set old Email ID on TextView
        userOldEmail = firebaseUser.email!!
        val textViewOldEmail: TextView = binding.textViewUpdateEmailOld
        textViewOldEmail.text = userOldEmail


        reAuthenticate(firebaseUser)
    }

    private fun reAuthenticate(firebaseUser: FirebaseUser) {
        val buttonVerifyUser: Button = binding.buttonAuthenticateUser
        buttonVerifyUser.setOnClickListener {
            userPassword = editTextPwd.text.toString()
            if (userPassword.isEmpty()) {
                Toast.makeText(this, "Password is needed to continue", Toast.LENGTH_SHORT).show()
                editTextPwd.requestFocus()
            } else {
                progressBar.visibility = View.VISIBLE
                val credential: AuthCredential = EmailAuthProvider.getCredential(userOldEmail, userPassword)
                firebaseUser.reauthenticate(credential).addOnCompleteListener {
                    if (it.isSuccessful) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Password has been verified, you can update email now", Toast.LENGTH_SHORT).show()
                        textViewAuthenticated.text = "You are authenticated. You can update your email now"
                        editTextNewEmail.isEnabled = true
                        editTextPwd.isEnabled = false
                        buttonVerifyUser.isEnabled = false
                        buttonUpdateEmail.isEnabled = true

                        buttonUpdateEmail.setOnClickListener {
                            userNewEmail = editTextNewEmail.text.toString()
                            if (userNewEmail.isEmpty()) {
                                Toast.makeText(this, "New Email is required", Toast.LENGTH_SHORT).show()
                                editTextNewEmail.error = "Please Enter new Email"
                                editTextNewEmail.requestFocus()
                            } else if (!Patterns.EMAIL_ADDRESS.matcher(userNewEmail).matches()) {
                                Toast.makeText(this, "Please Enter Valid Email", Toast.LENGTH_SHORT).show()
                                editTextNewEmail.error = "Please Enter valid Email"
                                editTextNewEmail.requestFocus()
                            } else if (userOldEmail == userNewEmail) {
                                Toast.makeText(this, "New Email can't be the same as the old email", Toast.LENGTH_SHORT).show()
                                editTextNewEmail.error = "Please Enter a new email"
                                editTextNewEmail.requestFocus()
                            } else {
                                progressBar.visibility = View.VISIBLE
                                verifyBeforeUpdateEmail(userNewEmail)
                            }
                        }
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Authentication failed. Please check your password and try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun verifyBeforeUpdateEmail(newEmail: String) {
        firebaseUser.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Verification email sent. Please verify the new email.", Toast.LENGTH_SHORT).show()
                showVerificationDialog()
                logOutUser()
            } else {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to send verification email. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showVerificationDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Email Verification Required")
        builder.setMessage("You must verify your new email before logging in.")
        builder.setCancelable(false) // Makes the dialog non-dismissible
        builder.setPositiveButton("Check Email") { dialog, _ ->
            // Open email app
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_EMAIL)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun logOutUser() {
        authProfile.signOut()
        val intent = Intent(this@UpdateEmailActivity, SignUpLogin::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("emailNotVerified", true)  // Pass flag to MainActivity
        startActivity(intent)
        finish()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.common_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_refresh -> {
                startActivity(intent)
                finish()
                true
            }
            R.id.menu_update_profile -> {
                val intent = Intent(this@UpdateEmailActivity, UpdateProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_update_email -> {
                val intent = Intent(this@UpdateEmailActivity, UpdateEmailActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_change_pass -> {
                val intent = Intent(this@UpdateEmailActivity, ChangePasswordActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_delete_profile -> {
                val intent = Intent(this@UpdateEmailActivity, DeleteProfileActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            R.id.menu_settings -> {
                Toast.makeText(this, "menu settings", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_logout -> {
                authProfile.signOut()
                Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@UpdateEmailActivity, SignUpLogin::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
