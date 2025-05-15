package com.example.signuplogina

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.signuplogina.databinding.ActivityDeleteProfileBinding
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class DeleteProfileActivity : AppCompatActivity() {
    private lateinit var binding:ActivityDeleteProfileBinding
    private lateinit var authProfile:FirebaseAuth
    private  var firebaseUser:FirebaseUser?=null
    private lateinit var editTextUserPassword:EditText
    private lateinit var textViewAuthenticated:TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var userPassword:String
    private lateinit var buttonReAuthenticate:Button
    private lateinit var buttonDeleteUser:Button
    private val TAG:String="DeleteProfileActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityDeleteProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        supportActionBar!!.title="Delete Your Profile"
        progressBar=binding.progressBar
        editTextUserPassword=binding.editTextDeleteUserPassword
        textViewAuthenticated=binding.textViewDeleteUserAuthenticated
        buttonDeleteUser=binding.buttonDeleteUser
        buttonReAuthenticate=binding.buttonDeleteUserAuthenticate


    //Delete Button disabled until user is authenticated
        authProfile=FirebaseAuth.getInstance()
        firebaseUser = authProfile.currentUser
       if ( firebaseUser==null){
           Toast.makeText(this,"Something went wrong!" +
                   "User Details are not available at the moment",Toast.LENGTH_SHORT ).show()
           val intent=Intent(this@DeleteProfileActivity,UserProfileActivity::class.java)
           startActivity(intent)
           finish()
       }
        else{
            reAuthenticateUser(firebaseUser)
       }

        val backButton: ImageView = findViewById(R.id.imageView_Back)
        backButton.setOnClickListener {
            onBackPressed() // Go back to the previous activity or fragment
        }

    }


    private fun reAuthenticateUser(firebaseUser: FirebaseUser?) {
        if (firebaseUser == null) {
            Toast.makeText(this, "Something went wrong! User details are not available.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@DeleteProfileActivity, UserProfileActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Proceed with re-authentication
        buttonReAuthenticate.setOnClickListener {
            userPassword = editTextUserPassword.text.toString()
            if (userPassword.isEmpty()) {
                Toast.makeText(this, "Password is needed", Toast.LENGTH_SHORT).show()
                editTextUserPassword.error = "Please Enter your current password to authenticate"
                editTextUserPassword.requestFocus()
            } else {
                progressBar.visibility = View.VISIBLE
                val credential: AuthCredential = EmailAuthProvider.getCredential(firebaseUser.email!!, userPassword)
                firebaseUser.reauthenticate(credential).addOnCompleteListener {
                    if (it.isSuccessful) {
                        progressBar.visibility = View.GONE
                        editTextUserPassword.isEnabled = false
                        buttonDeleteUser.isEnabled = true
                        buttonReAuthenticate.isEnabled = false
                        textViewAuthenticated.text = "You are Authenticated! You can delete your Profile and related Data now."
                        Toast.makeText(this, "Password verified. You can delete your account now.", Toast.LENGTH_SHORT).show()
                        buttonDeleteUser.backgroundTintList = ContextCompat.getColorStateList(this@DeleteProfileActivity, R.color.dark_green)
                        buttonDeleteUser.setOnClickListener { showAlertDialog() }
                    } else {
                        try {
                            throw it.exception ?: Exception("Unknown error")
                        } catch (e: Exception) {
                            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    progressBar.visibility = View.GONE
                }
            }
        }
    }


    private fun showAlertDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Delete User and Related Data?")
        builder.setMessage("Do you really want to delete your profile and related data ? This action is irreversible")
        builder.setCancelable(false) // Makes the dialog non-dismissible
        builder.setPositiveButton("Continue") { dialog, _ ->
            // Open email app
            deleteUserData(firebaseUser)
        }
        //Return to User profile activity
        builder.setNegativeButton("Cancel") { dialog, _ ->
           val intent=Intent(this@DeleteProfileActivity,UserProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
        // create the alertDialog
        val alertDialog:AlertDialog=builder.create()

        alertDialog.setOnShowListener{
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.red))
        }
        alertDialog.show()
    }
    private fun deleteUserData(firebaseUser: FirebaseUser?) {
        // Ensure firebaseUser and its photoUrl are not null
        firebaseUser?.photoUrl?.let { photoUrl ->
            val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()
            val storageReference: StorageReference = firebaseStorage.getReferenceFromUrl(photoUrl.toString())

            // Delete the profile photo first
            storageReference.delete().addOnSuccessListener {
                Log.d(TAG, "onSuccess: Photo Deleted")
                Toast.makeText(this, "Photo Deleted Successfully", Toast.LENGTH_SHORT).show()

                // Now, delete the user data from Realtime Database
                val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("Users")
                databaseReference.child(firebaseUser.uid).removeValue().addOnSuccessListener {
                    Log.d(TAG, "OnSuccess: User data deleted")

                    // After deleting the photo and database data, delete the Firebase user
                    deleteFirebaseUser(firebaseUser)
                }.addOnFailureListener { e: Exception ->
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }

            }.addOnFailureListener { e: Exception ->
                Log.d(TAG, e.message ?: "Unknown error")
                Toast.makeText(this, "Failed to delete photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        } ?: run {
            Log.d(TAG, "No photo to delete or user is null")
            Toast.makeText(this, "No profile photo to delete.", Toast.LENGTH_SHORT).show()

            // Delete the Firebase user and database data if there's no profile photo
            deleteFirebaseUser(firebaseUser)
        }
    }

    private fun deleteFirebaseUser(firebaseUser: FirebaseUser?) {
        firebaseUser?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                authProfile.signOut()
                Toast.makeText(this, "User has been deleted!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@DeleteProfileActivity, SignUpLogin::class.java)
                startActivity(intent)
                finish()
            } else {
                try {
                    throw task.exception ?: Exception("Unknown error occurred")
                } catch (e: Exception) {
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
            }
            progressBar.visibility = View.GONE
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // inflate menu items
        menuInflater.inflate(R.menu.common_menu, menu)
        return super.onCreateOptionsMenu(menu)


    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.menu_refresh -> {
                // refresh activity
                startActivity(intent)
                finish()
            }
            R.id.menu_update_profile -> {
                val intent = Intent(this@DeleteProfileActivity,UpdateProfileActivity::class.java)
                startActivity(intent)
                finish()// we don't want multiple duplicate activities running, we just want the UserProfile activity Running in the background
            }
            R.id.menu_update_email -> {
                val intent = Intent(this@DeleteProfileActivity, UpdateEmailActivity::class.java)
                startActivity(intent)
            }
            R.id.menu_settings -> {
                Toast.makeText(this, "menu settings", Toast.LENGTH_SHORT).show()
            }
            R.id.menu_change_pass -> {
                val intent = Intent(this@DeleteProfileActivity,ChangePasswordActivity::class.java)
                startActivity(intent)
            }
            R.id.menu_delete_profile -> {
                val intent = Intent(this@DeleteProfileActivity,DeleteProfileActivity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.menu_logout -> {
                authProfile.signOut()
                Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@DeleteProfileActivity, SignUpLogin::class.java)
                // clear the stack to prevent the user from coming back to the userProfileActivity
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()// close UserProfileActivity
            }

            else -> {
                Toast.makeText(this, "Something Went Wrong!!!", Toast.LENGTH_SHORT).show()

            }
        }
        return super.onOptionsItemSelected(item)


    }


}