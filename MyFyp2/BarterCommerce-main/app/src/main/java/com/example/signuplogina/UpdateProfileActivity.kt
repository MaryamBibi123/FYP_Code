package com.example.signuplogina

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.signuplogina.databinding.ActivityUpdateProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.regex.Matcher
import java.util.regex.Pattern

class UpdateProfileActivity : AppCompatActivity() {
    // Declare variables for UI components and Firebase authentication
    private lateinit var editTextUpdateName: EditText
    private lateinit var editTextUpdateDoB: EditText
    private lateinit var editTextUpdateMobile: EditText
    private lateinit var radioGroupUpdateGender: RadioGroup
    private lateinit var radioButtonUpdateGenderSelected: RadioButton
    private lateinit var textFullName: String
    private lateinit var textDoB: String
    private lateinit var textGender: String
    private lateinit var textMobile: String
    private lateinit var authProfile: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var picker: DatePickerDialog
    private lateinit var binding: ActivityUpdateProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize UI components and FirebaseAuth instance
        progressBar = binding.progressBar
        editTextUpdateName = binding.editTextUpdateProfileName
        editTextUpdateDoB = binding.editTextUpdateProfileDob
        editTextUpdateMobile = binding.editTextUpdateProfileMobile
        radioGroupUpdateGender = binding.radioGroupUpdateProfileGender
        authProfile = FirebaseAuth.getInstance()

        val firebaseUser: FirebaseUser = authProfile.currentUser!!
        showProfile(firebaseUser)

        // Upload profile pic button listener
        val buttonUploadProfilePic: Button = binding.btnUploadProfilePicture
        buttonUploadProfilePic.setOnClickListener {
            val intent = Intent(this@UpdateProfileActivity, UploadProfilePicActivity::class.java)
            startActivity(intent)
//            finish()
        }

        val backButton: ImageView = findViewById(R.id.imageView_Back)
        backButton.setOnClickListener {
            onBackPressed() // Go back to the previous activity or fragment
        }

//         Uncomment if email update functionality is needed
         val buttonUpdateEmail: Button = binding.btnUpdateEmail
         buttonUpdateEmail.setOnClickListener {
             val intent = Intent(this@UpdateProfileActivity, UpdateEmailActivity::class.java)
             startActivity(intent)
             finish() // because we want to go back to the user profile activity
         }
    }

    private fun showProfile(fireBaseUser: FirebaseUser) {
        // Get the current user's ID
        val userIDofRegistered = fireBaseUser.uid
        val referenceProfile: DatabaseReference = FirebaseDatabase.getInstance().getReference("Users")
        progressBar.visibility = View.VISIBLE

        // Retrieve user details from the Firebase Realtime Database
        referenceProfile.child(userIDofRegistered).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val readUserDetails: User? = snapshot.getValue(User::class.java)
                if (readUserDetails != null) {
                    // Assign retrieved data to variables
                    textFullName = readUserDetails.fullName!!
                    textGender = readUserDetails.gender!!
                    textMobile = readUserDetails.mobile!!
                    textDoB = readUserDetails.dob!!


                    // Populate the UI fields with retrieved data
                    editTextUpdateName.setText(textFullName)
                    editTextUpdateDoB.setText(textDoB)
                    editTextUpdateMobile.setText(textMobile)

                    // Assign the correct RadioButton based on gender comparison
                    radioButtonUpdateGenderSelected = if (textGender == "Male") {
                        binding.radioMale
                    } else {
                        binding.radioFemale
                    }

                    // Set the selected RadioButton as checked
                    radioButtonUpdateGenderSelected.isChecked = true

                    // Date Picker setup for updating the date of birth
                    editTextUpdateDoB.setOnClickListener {
                        // Split the DoB string into day, month, and year
                        val textSADoB: List<String> = textDoB.split("/")
                        val day: Int = textSADoB[0].toInt()
                        val month = textSADoB[1].toInt() - 1
                        val year = textSADoB[2].toInt()

                        // Initialize DatePickerDialog with the current DoB
                        picker = DatePickerDialog(
                            this@UpdateProfileActivity,
                            { _, selectedYear, selectedMonth, selectedDay ->
                                editTextUpdateDoB.setText("$selectedDay/${selectedMonth + 1}/$selectedYear")
                            },
                            year,
                            month,
                            day
                        )
                        picker.show()
                    }

                    // Save the updated profile when the save button is clicked
                    val buttonSaveProfile: Button = binding.btnUpdateProfileSave
                    buttonSaveProfile.setOnClickListener {
                        updateProfile(fireBaseUser)
                    }
                } else {
                    // Handle case where user details are null
                    Toast.makeText(
                        this@UpdateProfileActivity,
                        "Something went wrong! Null data received.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database read cancellation
                Toast.makeText(
                    this@UpdateProfileActivity,
                    "Something went wrong during the data fetch.",
                    Toast.LENGTH_SHORT
                ).show()
                progressBar.visibility = View.GONE
            }
        })
    }

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
                val intent = Intent(this@UpdateProfileActivity, UpdateProfileActivity::class.java)
                startActivity(intent)
                true
            }
        R.id.menu_update_email -> {
            val intent = Intent(this@UpdateProfileActivity, UpdateEmailActivity::class.java)
            startActivity(intent)
            true
        }
            R.id.menu_settings -> {
                Toast.makeText(this, "menu settings", Toast.LENGTH_SHORT).show()
                true
            }
        R.id.menu_change_pass -> {
            val intent = Intent(this@UpdateProfileActivity, ChangePasswordActivity::class.java)
            startActivity(intent)
            finish()
            true
        }
        R.id.menu_delete_profile -> {
            val intent = Intent(this@UpdateProfileActivity, DeleteProfileActivity::class.java)
            startActivity(intent)
            finish()
            true
        }
            R.id.menu_logout -> {
                authProfile.signOut()
                Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@UpdateProfileActivity, SignUpLogin::class.java)
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



    private fun updateProfile(fireBaseUser: FirebaseUser) {
        // Get the selected gender RadioButton ID and find the corresponding view
        val selectedGenderID: Int = radioGroupUpdateGender.checkedRadioButtonId
        radioButtonUpdateGenderSelected = findViewById(selectedGenderID)

        // Validate mobile number using regular expressions
        val mobileRegex = "^03[0-9]{2}[0-9]{7}$"
        val mobileMatcher: Matcher
        val mobilePattern: Pattern = Pattern.compile(mobileRegex)
        mobileMatcher = mobilePattern.matcher(textMobile)

        // Validate user inputs
        when {
            textFullName.isEmpty() -> {
                editTextUpdateName.error = "Full Name is Required"
                editTextUpdateName.requestFocus()
            }
            textDoB.isEmpty() -> {
                editTextUpdateDoB.error = "DOB is Required"
                editTextUpdateDoB.requestFocus()
            }
            radioButtonUpdateGenderSelected.text.isEmpty() -> {
                Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show()
                radioButtonUpdateGenderSelected.requestFocus()
            }
            textMobile.isEmpty() -> {
                editTextUpdateMobile.error = "Mobile is Required"
                editTextUpdateMobile.requestFocus()
            }
            textMobile.length != 11 -> {
                editTextUpdateMobile.error = "Number must be 11 digits"
                editTextUpdateMobile.requestFocus()
            }
            !mobileMatcher.find() -> {
                editTextUpdateMobile.error = "Not a valid Number"
                editTextUpdateMobile.requestFocus()
            }
            else -> {
                // If all inputs are valid, update the profile

                // Update gender and other fields with user-entered data
                textGender = radioButtonUpdateGenderSelected.text.toString()
                textFullName = editTextUpdateName.text.toString()
                textDoB = editTextUpdateDoB.text.toString()
                textMobile = editTextUpdateMobile.text.toString()

                // Prepare the User object with updated data
                val updates = mapOf(
                    "fullName" to textFullName,
                    "dob" to textDoB,
                    "gender" to textGender,
                    "mobile" to textMobile
                )




                val writeUserDetails = User(textFullName, textDoB, textGender, textMobile)
                val referenceProfile: DatabaseReference = FirebaseDatabase.getInstance().getReference("Users")

                // Get the user ID and update the Firebase Realtime Database
                val userID: String = fireBaseUser.uid
                referenceProfile.child(userID).updateChildren(updates).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Set the new display name in the Firebase User profile
                        val profileUpdates: UserProfileChangeRequest =
                            UserProfileChangeRequest.Builder().setDisplayName(textFullName).build()
                        fireBaseUser.updateProfile(profileUpdates)

                        Toast.makeText(this@UpdateProfileActivity, "Update Successful", Toast.LENGTH_SHORT).show()

                        // Redirect to UserProfileActivity and clear activity stack
                        val intent = Intent(this@UpdateProfileActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Handle errors during profile update
                        try {
                            throw task.exception ?: Exception("Unknown error")
                        } catch (e: Exception) {
                            Toast.makeText(this@UpdateProfileActivity, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    progressBar.visibility = View.GONE
                }
            }
        }
    }
}
