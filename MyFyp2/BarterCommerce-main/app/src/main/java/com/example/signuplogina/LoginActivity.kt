package com.example.signuplogina

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.signuplogina.databinding.ActivityLoginBinding
import com.example.signuplogina.mvvm.UsersRepo
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authProfile: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 123 // Request code for Google Sign-In

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authProfile = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Get this from Firebase Console
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Set up Google Sign-In button
//        binding.googleSignup.setOnClickListener {
//            signInWithGoogle()
//        }

        // Initialize fields inside onCreate after binding is set up
        val emailField = binding.editTextLoginEmail
        val passwordField = binding.editTextLoginPassword
        val progressBar = binding.progressBar
        val buttonForgotPassword: Button =binding.buttonForgotPassword
        //Reset Password
        buttonForgotPassword.setOnClickListener {
            Toast.makeText(this,"You can reset your password ", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@LoginActivity,ForgotPassActivity::class.java))
            // we don't need to finish() this activity because the user can navigate to this screen while on forgetPassActivity
            //after changing the password by forget pass email and they can get back to the login activity to login after change password

        }
        binding.chatBackBtn.setOnClickListener {
            onBackPressed()
        }

        // Show/Hide password using CheckBox
        val checkBoxShowHidePwd: CheckBox = binding.checkBoxShowHidePwd

// Set a listener on the CheckBox
        checkBoxShowHidePwd.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // If the checkbox is checked, show the password
                passwordField.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                // If the checkbox is unchecked, hide the password
                passwordField.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            // Move the cursor to the end of the text
            passwordField.setSelection(passwordField.text.length)
        }


        // Login user
        binding.buttonLogin.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            when {
                email.isEmpty() -> {
                    emailField.error = "Email is Required"
                    emailField.requestFocus()
                }

                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    emailField.error = "Please Give Valid Email"
                    emailField.requestFocus()
                }

                password.isEmpty() -> {
                    passwordField.error = "Password is Required"
                    passwordField.requestFocus()
                }

                else -> {
                    progressBar.visibility = View.VISIBLE
                    loginUser(email, password, emailField, passwordField, progressBar)
                }
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        authProfile.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = authProfile.currentUser
                Toast.makeText(this, "Signed in as ${user?.email}", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(
        email: String,
        password: String,
        emailField: EditText,
        passwordField: EditText,
        progressBar: ProgressBar
    ) {
        authProfile.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            progressBar.visibility = View.GONE
            if (task.isSuccessful) {
                // Proceed to the next screen or functionality
                // get instance of the current user
                val firebaseUser: FirebaseUser? = authProfile.currentUser
                if (firebaseUser!!.isEmailVerified) {

                    updateFCMTokenOnLogin(firebaseUser.uid)

                    Toast.makeText(this, "You are Logged in Now", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this,MainActivity::class.java))
                    finish()// close login activity
                } else {
                    firebaseUser.sendEmailVerification();
                    authProfile.signOut()
                    showAlertDialog()
                }
            } else {
                try {
                    throw task.exception ?: Exception("Unknown error")
                } catch (e: FirebaseAuthInvalidUserException) {
                    // Invalid email (not registered)
                    emailField.error =
                        "User doesn't exist or is no longer valid, Please Register Again"
                    emailField.requestFocus()
                } catch (e: FirebaseAuthInvalidCredentialsException) {
                    // Invalid credentials (likely wrong password)
                    emailField.error = "Invalid Credentials .Please Recheck and type Again"
                    emailField.requestFocus()
                } catch (e: Exception) {
                    Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateFCMTokenOnLogin(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            val userRef = FirebaseDatabase.getInstance().getReference("Users/$userId")

            // ✅ Store/Override token for the logged-in user
            userRef.child("fcmToken").setValue(token)
                .addOnSuccessListener { Log.d("FCM", "Token updated for user: $userId") }
                .addOnFailureListener { Log.e("FCM", "Failed to update token") }
        }
    }


    private fun showAlertDialog() {
        // setup the alert builder
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Email Not Verified")
        builder.setMessage(" Please verify your Email Now . You Can't login without Email verification")

        // Open Email Apps if user clickes continue button
        builder.setPositiveButton("Continue", DialogInterface.OnClickListener { dialog, which ->
            // Handle the "Continue" button click here
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_EMAIL)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)// To email app in the new window and not in our App
            startActivity(intent)
        }).setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                // Handle the "Cancel" button click here
                dialog.dismiss()
            })

        val alertDialog = builder.create()// created alert dialog box, using builder .create
        alertDialog.show()// we have to show the alert
    }
    override fun onStart() {
            super.onStart()

            val uid = Utils.getUidLoggedIn()
            if (uid.isNullOrBlank()) {
                FirebaseAuth.getInstance().signOut()
                return
            }

            UsersRepo().getUserDetailsById(uid) { user ->
                if (user?.statusByAdmin == "blocked") {
                    FirebaseAuth.getInstance().signOut()
//                    showBlockedDialog()
                    AlertDialog.Builder(this)
                        .setTitle("Access Denied")
                        .setMessage("Your account has been blocked by the admin.")
                        .setCancelable(false)
                        .setPositiveButton("OK") { _, _ ->
                            finishAffinity() // close all activities
                        }
                        .show()
            }
        }


        if ( authProfile.currentUser!=null){
            Toast.makeText(this,"Already Logged In", Toast.LENGTH_SHORT).show()
        // start the profile activity
//            startActivity(Intent(this,UserProfileActivity::class.java))
            startActivity(Intent(this@LoginActivity,MainActivity::class.java))
            finish()// close login activity
        }
        else{
            Toast.makeText(this,"You can login now", Toast.LENGTH_SHORT).show()

        }

    }


}
