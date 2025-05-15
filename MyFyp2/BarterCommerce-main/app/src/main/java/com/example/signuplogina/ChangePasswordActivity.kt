package com.example.signuplogina

import android.content.Intent
import android.os.Bundle
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
import androidx.core.content.ContextCompat
import com.example.signuplogina.databinding.ActivityChangePasswordBinding
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var authProfile: FirebaseAuth
    private lateinit var editTextPwdCurrent: EditText
    private lateinit var editTextPwdNew: EditText
    private lateinit var editTextPwdConfirmNew: EditText
    private lateinit var textViewAuthenticated: TextView
    private lateinit var buttonChangePassword: Button
    private lateinit var buttonReAuthenticate: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var userPasswordCurrent: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        supportActionBar!!.title = "Change Password"


        editTextPwdNew = binding.editTextChangePasswordNew
        editTextPwdCurrent = binding.editTextChangePasswordCurrent
        editTextPwdConfirmNew = binding.editTextChangePasswordNewConfirm
        textViewAuthenticated = binding.textViewChangePasswordAuthenticated
        progressBar = binding.progressBar
        buttonReAuthenticate = binding.buttonChangePasswordAuthenticate
        buttonChangePassword = binding.buttonChangePassword


        //Disable the edittext for new password , confirm new password and make changePassword button unClickable
        editTextPwdNew.isEnabled = false
        editTextPwdConfirmNew.isEnabled = false
        buttonChangePassword.isEnabled = false

        authProfile = FirebaseAuth.getInstance()
        val firebaseUser: FirebaseUser = authProfile.currentUser!!

        if (firebaseUser.equals("")) {
            Toast.makeText(
                this, "Something went wrong ! User's details not available", Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(this@ChangePasswordActivity, UserProfileActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            reAuthenticateUser(firebaseUser)
        }

        val backButton: ImageView = findViewById(R.id.imageView_Back)
        backButton.setOnClickListener {
            onBackPressed() // Go back to the previous activity or fragment
        }

    }

    private fun reAuthenticateUser(firebaseUser: FirebaseUser) {
        buttonReAuthenticate.setOnClickListener {
            userPasswordCurrent = editTextPwdCurrent.text.toString()
            if (userPasswordCurrent.isEmpty()) {
                Toast.makeText(this, "Password is needed", Toast.LENGTH_SHORT).show()
                editTextPwdCurrent.error = "Please Enter your current password to authenticate"
                editTextPwdCurrent.requestFocus()


            } else {
                progressBar.visibility = View.VISIBLE

                // ReAuthenticate User New
                val credential: AuthCredential =
                    EmailAuthProvider.getCredential(firebaseUser.email!!, userPasswordCurrent)
                firebaseUser.reauthenticate(credential).addOnCompleteListener {
                    if (it.isSuccessful) {

                        progressBar.visibility =
                            View.GONE// we have to stop the progress bar whether the user is authenticated or not
                        // disable edittext for current password and reAuthenticate button
                        // enable edit text fore the new password and confirm new password, change password button
                        //
                        editTextPwdNew.isEnabled = true
                        editTextPwdConfirmNew.isEnabled = true
                        buttonChangePassword.isEnabled = true

                        editTextPwdCurrent.isEnabled = false
                        buttonReAuthenticate.isEnabled = false


                        // set text view to show the user is authenticated/verifies

                        textViewAuthenticated.text =
                            "You are Authenticated! , You can change Password Now"
                        Toast.makeText(
                            this,
                            "Password has been verified , Change Password Now",
                            Toast.LENGTH_SHORT
                        ).show()
                        // update the color
                        buttonChangePassword.backgroundTintList = ContextCompat.getColorStateList(
                            this@ChangePasswordActivity, R.color.dark_green
                        )

                        buttonChangePassword.setOnClickListener {
                            changePassword(firebaseUser)
                        }

                    } else {
                        try {
                            throw it.exception ?: Exception("Unknown User")
                        } catch (e: Exception) {
                            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()


                        }
                    }
                    progressBar.visibility = View.GONE
                }

            }
        }
    }

    private fun changePassword(firebaseUser: FirebaseUser) {
        val userPasswordNew: String = editTextPwdNew.text.toString()
        val userPasswordConfirmNew: String = editTextPwdConfirmNew.text.toString()

        if (userPasswordNew.isEmpty()) {
            Toast.makeText(this, "New Password is needed", Toast.LENGTH_SHORT).show()
            editTextPwdNew.error = " Please Enter Your New Password"
            editTextPwdNew.requestFocus()

        } else if (userPasswordConfirmNew.isEmpty()) {
            Toast.makeText(this, "Please Confirm your new Password", Toast.LENGTH_SHORT).show()
            editTextPwdConfirmNew.error = " Please re-Enter Your New Password"
            editTextPwdConfirmNew.requestFocus()

        } else if (userPasswordNew != userPasswordConfirmNew) {
            Toast.makeText(this, "Password did not Match", Toast.LENGTH_SHORT).show()
            editTextPwdNew.error = " Please re-Enter Same Password"
            editTextPwdNew.requestFocus()

        } else if (userPasswordNew == userPasswordCurrent) {
            Toast.makeText(
                this, "New Password can't be same as the old password", Toast.LENGTH_SHORT
            ).show()
            editTextPwdNew.error = " Please re-Enter Your New Password"
            editTextPwdNew.requestFocus()

        } else {
            progressBar.visibility = View.VISIBLE
            firebaseUser.updatePassword(userPasswordNew).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Password has been changed", Toast.LENGTH_SHORT).show()
                    val intent =
                        Intent(this@ChangePasswordActivity, UserProfileActivity::class.java)
                    startActivity(intent)
                    finish()


                } else {
                    try {
                        throw it.exception ?: Exception("Unknown User")

                    } catch (e: Exception) {
                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
                progressBar.visibility=View.GONE
            }
        }

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
                val intent = Intent(this@ChangePasswordActivity, UpdateProfileActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.menu_update_email -> {
                val intent = Intent(this@ChangePasswordActivity, UpdateEmailActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.menu_change_pass -> {
                val intent = Intent(this@ChangePasswordActivity, ChangePasswordActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.menu_settings -> {
                Toast.makeText(this, "menu settings", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_delete_profile -> {
                val intent = Intent(this@ChangePasswordActivity, DeleteProfileActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            R.id.menu_logout -> {
                authProfile.signOut()
                Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@ChangePasswordActivity, SignUpLogin::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
