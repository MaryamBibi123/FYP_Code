package com.example.signuplogina

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.signuplogina.databinding.ActivityRegisterBinding
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase

import java.util.Calendar
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var radioBtnRegisterGenderSelected: RadioButton? = null
    private lateinit var picker: DatePickerDialog
    private var selectedDobCalendar: Calendar? = null

    private val passwordPattern: Pattern = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    )

    private val mobilePattern: Pattern = Pattern.compile("^03[0-9]{2}[0-9]{7}$")

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fullNameField = binding.editTextRegisterFullName
        val emailField = binding.editTextRegisterEmail
        val dobField = binding.editTextRegisterDob
        val mobileField = binding.editTextRegisterMobile
        val passwordField = binding.editTextRegisterPassword
        val confirmPassField = binding.editTextRegisterConfirmPass
        val genderGroup = binding.radioGroupRegisterGender
        val progressBar = binding.progressBar
        val backButton = binding.imageViewBack

        Toast.makeText(this, "You Can Register Now", Toast.LENGTH_SHORT).show()
        genderGroup.clearCheck()
        progressBar.visibility = View.GONE

        dobField.setOnClickListener {
            val calendar = Calendar.getInstance()
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH)
            val year = calendar.get(Calendar.YEAR)

            picker = DatePickerDialog(
                this@RegisterActivity,
                { _, selectedYear, selectedMonth, selectedDay ->
                    selectedDobCalendar = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    dobField.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear))
                    dobField.error = null
                },
                year,
                month,
                day
            )
            // Optional: prevent future dates picker.datePicker.maxDate = System.currentTimeMillis()
            picker.show()
        }

        backButton.setOnClickListener {
//            val intent = Intent(this, WelcomeActivity::class.java) // Or LoginActivity if preferred
//            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
//            startActivity(intent)
//            finish()
            onBackPressed()
        }

        binding.buttonRegister.setOnClickListener {
            clearErrors()

            val selectedGenderId = genderGroup.checkedRadioButtonId
            radioBtnRegisterGenderSelected = if (selectedGenderId != -1) findViewById(selectedGenderId) else null

            val fullName = fullNameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val dob = dobField.text.toString().trim()
            val mobile = mobileField.text.toString().trim()
            val password = passwordField.text.toString()
            val confirmPass = confirmPassField.text.toString()
            val gender: String? = radioBtnRegisterGenderSelected?.text?.toString()

            var isValid = true

            if (fullName.isEmpty()) {
                fullNameField.error = "Full Name is Required"
                fullNameField.requestFocus()
                isValid = false
            } else if (fullName.matches(Regex("^[0-9].*"))) {
                fullNameField.error = "Full Name cannot start with a number"
                fullNameField.requestFocus()
                isValid = false
            } else if (fullName.length < 3) {
                fullNameField.error = "Full Name seems too short"
                fullNameField.requestFocus()
                isValid = false
            }

            if (isValid && email.isEmpty()) {
                emailField.error = "Email is Required"
                emailField.requestFocus()
                isValid = false
            } else if (isValid && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailField.error = "Valid Email is Required"
                emailField.requestFocus()
                isValid = false
            }

            if (isValid && mobile.isEmpty()) {
                mobileField.error = "Mobile Number is Required"
                mobileField.requestFocus()
                isValid = false
            } else if (isValid && !mobilePattern.matcher(mobile).matches()) {
                mobileField.error = "Invalid Mobile Number format"
                mobileField.requestFocus()
                isValid = false
            }

            if (isValid && dob.isEmpty()) {
                dobField.error = "Date of Birth is Required"
                dobField.requestFocus()
                Toast.makeText(this, "Please select Date of Birth", Toast.LENGTH_SHORT).show()
                isValid = false
            } else if (isValid && selectedDobCalendar == null) {
                dobField.error = "Please select a valid Date of Birth"
                dobField.requestFocus()
                Toast.makeText(this, "Invalid Date of Birth selected", Toast.LENGTH_SHORT).show()
                isValid = false
            } else if (isValid) {
                val todayCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (selectedDobCalendar!!.after(todayCalendar)) {
                    dobField.error = "Date of Birth cannot be in the future"
                    dobField.requestFocus()
                    Toast.makeText(this, "Date of Birth cannot be in the future", Toast.LENGTH_SHORT).show()
                    isValid = false
                }
                // Optional Age Check
                val ageCalendar = Calendar.getInstance().apply { timeInMillis = todayCalendar.timeInMillis }
                ageCalendar.add(Calendar.YEAR, -13)
                if (selectedDobCalendar!!.after(ageCalendar)) {
                    dobField.error = "You must be at least 13 years old"
                    dobField.requestFocus()
                    Toast.makeText(this, "You must be at least 13 years old", Toast.LENGTH_SHORT).show()
                    isValid = false
                }

            }

            if (isValid && selectedGenderId == -1) {
                Toast.makeText(this, "Please select your Gender", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (isValid && password.isEmpty()) {
                passwordField.error = "Password is Required"
                passwordField.requestFocus()
                isValid = false
            } else if (isValid && password.length < 8) {
                passwordField.error = "Password must be at least 8 characters"
                passwordField.requestFocus()
                isValid = false
            } else if (isValid && !passwordPattern.matcher(password).matches()) {
                passwordField.error = "Password must include uppercase, lowercase, digit, and special char (@#$%^&+=!)"
                passwordField.requestFocus()
                isValid = false
            }

            if (isValid && confirmPass.isEmpty()) {
                confirmPassField.error = "Confirm Password is Required"
                confirmPassField.requestFocus()
                isValid = false
            } else if (isValid && password != confirmPass) {
                confirmPassField.error = "Passwords do not match"
                confirmPassField.requestFocus()
                passwordField.text?.clear()
                confirmPassField.text?.clear()
                isValid = false
            }

            if (isValid && gender != null) {
                progressBar.visibility = View.VISIBLE
                binding.buttonRegister.isEnabled = false
                registerUser(fullName, email, mobile, dob, gender, password)
            } else if (isValid && gender == null) {
                Toast.makeText(this, "Gender selection error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearErrors() {
        binding.editTextRegisterFullName.error = null
        binding.editTextRegisterEmail.error = null
        binding.editTextRegisterMobile.error = null
        binding.editTextRegisterDob.error = null
        binding.editTextRegisterPassword.error = null
        binding.editTextRegisterConfirmPass.error = null
    }

    private fun registerUser(
        fullName: String,
        email: String,
        mobile: String,
        dob: String,
        gender: String,
        password: String
    ) {
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    firebaseUser?.let { user ->
                        user.sendEmailVerification().addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                Toast.makeText(this, "Verification email sent to ${user.email}", Toast.LENGTH_LONG).show()
                            } else {
                                Log.e("RegisterActivity", "sendEmailVerification failed: ", verificationTask.exception)
                                Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                            }
                        }

                        val userId = user.uid
                        val userDatabaseRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
                        val userData = User(userId, fullName, dob, gender, mobile, email, "Offline")

                        userDatabaseRef.setValue(userData).addOnCompleteListener { dbTask ->
                            binding.progressBar.visibility = View.GONE
                            binding.buttonRegister.isEnabled = true

                            if (dbTask.isSuccessful) {
                                Toast.makeText(this, "Registration successful. Please check your email for verification.", Toast.LENGTH_LONG).show()

                                val intent = Intent(this, PreferenceSelection::class.java)
                                // intent.putExtra("USER_ID", userId)
                                startActivity(intent)
                                finishAffinity()
                            } else {
                                Log.e("RegisterActivity", "Failed to save user data: ", dbTask.exception)
                                Toast.makeText(this,"Registration completed, but failed to save profile details. Please try updating profile later.", Toast.LENGTH_LONG).show()
                                // val intent = Intent(this, LoginActivity::class.java)
                                // startActivity(intent)
                                // finishAffinity()
                            }
                        }
                    } ?: run {
                        Log.e("RegisterActivity", "Firebase user is null after successful registration")
                        Toast.makeText(this, "Registration failed: User not found.", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                        binding.buttonRegister.isEnabled = true
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
                    handleRegistrationFailure(task.exception)
                }
            }
    }

    private fun handleRegistrationFailure(exception: Exception?) {
        val message = when (exception) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak. ${exception.reason}"
            is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
            is FirebaseAuthUserCollisionException -> "An account already exists with this email address."
            else -> "Registration failed: ${exception?.message ?: "Unknown error"}"
        }
        Log.e("RegisterActivity", "Registration failed", exception)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        when (exception) {
            is FirebaseAuthWeakPasswordException -> binding.editTextRegisterPassword.requestFocus()
            is FirebaseAuthInvalidCredentialsException -> binding.editTextRegisterEmail.requestFocus()
            is FirebaseAuthUserCollisionException -> binding.editTextRegisterEmail.requestFocus()
        }
    }

    data class User(
        val userid: String = "",
        val fullName: String = "",
        val dob: String = "",
        val gender: String = "",
        val mobile: String = "",
        val useremail: String = "",
        val status: String = "Offline"
    )
}






//package com.example.signuplogina
//
//import android.annotation.SuppressLint
//import android.app.DatePickerDialog
//import android.content.Intent
//import android.os.Bundle
//import android.util.Patterns
//import android.view.View
//import android.widget.ImageView
//import android.widget.RadioButton
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.example.signuplogina.databinding.ActivityRegisterBinding
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.*
//import com.google.firebase.database.FirebaseDatabase
//import java.util.Calendar
//import java.util.regex.Matcher
//import java.util.regex.Pattern
//
//
//
//class RegisterActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityRegisterBinding
//    private lateinit var radioBtnRegisterGenderSelected: RadioButton
//    private lateinit var picker:DatePickerDialog
//
//    @SuppressLint("SetTextI18n")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityRegisterBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Initialize all the necessary views early
//        val fullNameField = binding.editTextRegisterFullName
//        val emailField = binding.editTextRegisterEmail
//        val dobField = binding.editTextRegisterDob
//        val mobileField = binding.editTextRegisterMobile
//        val passwordField = binding.editTextRegisterPassword
//        val confirmPassField = binding.editTextRegisterConfirmPass
//        val genderGroup = binding.radioGroupRegisterGender
//        val progressBar = binding.progressBar
//
//        Toast.makeText(this, "You Can Register Now", Toast.LENGTH_SHORT).show()
//
//        genderGroup.clearCheck()
//        // Setting up DatePicker on EditText
//        dobField.setOnClickListener {
//            val calendar = Calendar.getInstance()
//            val day = calendar.get(Calendar.DAY_OF_MONTH)
//            val month = calendar.get(Calendar.MONTH)
//            val year = calendar.get(Calendar.YEAR)
//
//            // Date Picker Dialog
//            picker = DatePickerDialog(
//                this@RegisterActivity,
//                { _, selectedYear, selectedMonth, selectedDay ->
//                    dobField.setText("$selectedDay/${selectedMonth + 1}/$selectedYear")
//                },
//                year,
//                month,
//                day
//            )
//            picker.show()
//        }
//
//        val backButton: ImageView = findViewById(R.id.imageView_Back)
//        backButton.setOnClickListener {
//            onBackPressed() // Go back to the previous activity or fragment
//        }
//
//
//        binding.buttonRegister.setOnClickListener {
//            val selectedGenderId = genderGroup.checkedRadioButtonId
//            if (selectedGenderId != -1) {
//                radioBtnRegisterGenderSelected = findViewById(selectedGenderId)
//            }
//
//            val fullName = fullNameField.text.toString().trim()
//            val email = emailField.text.toString().trim()
//            val dob = dobField.text.toString().trim()
//            val mobile = mobileField.text.toString().trim()
//            val password = passwordField.text.toString().trim()
//            val confirmPass = confirmPassField.text.toString().trim()
//            val gender: String?
//
//            // validate Mobile number using Matcher and pattern (Regular Expression)
//            val mobileRegex="^03[0-9]{2}[0-9]{7}$"
//            val mobileMatcher: Matcher
//            val mobilePattern:Pattern=Pattern.compile(mobileRegex)
//            mobileMatcher=mobilePattern.matcher(mobile)
//
//
//
//
//            when {
//                fullName.isEmpty() -> {
//                    fullNameField.error = "Full Name is Required"
//                    fullNameField.requestFocus()
//                }
//
//                email.isEmpty() -> {
//                    emailField.error = "Email is Required"
//                    emailField.requestFocus()
//                }
//
//                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
//                    emailField.error = "Valid Email is Required"
//                    emailField.requestFocus()
//                }
//
//                dob.isEmpty() -> {
//                    dobField.error = "DOB is Required"
//                    dobField.requestFocus()
//                }
//
//                selectedGenderId == -1 -> {
//                    Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show()
//                    genderGroup.requestFocus()
//                }
//
//                mobile.isEmpty() -> {
//                    mobileField.error = "Mobile is Required"
//                    mobileField.requestFocus()
//                }
//
//                mobile.length != 11 -> {
//                    mobileField.error = "Number must be 11 digits"
//                    mobileField.requestFocus()
//                }
//
//                !mobileMatcher.find()->{
//                    mobileField.error = "Not a valid Number"
//                    mobileField.requestFocus()
//                }
//                password.isEmpty() -> {
//                    passwordField.error = "Password is Required"
//                    passwordField.requestFocus()
//                }
//
//                password.length < 8 -> {
//                    passwordField.error = "Password must be at least 8 characters"
//                    passwordField.requestFocus()
//                }
//
//                confirmPass.isEmpty() -> {
//                    confirmPassField.error = "Confirm Password is Required"
//                    confirmPassField.requestFocus()
//                }
//
//                password != confirmPass -> {
//                    confirmPassField.error = "Passwords do not match"
//                    confirmPassField.requestFocus()
//                    passwordField.text?.clear()
//                    confirmPassField.text?.clear()
//                }
//
//                else -> {
//                    gender = radioBtnRegisterGenderSelected.text.toString()
//                    progressBar.visibility = View.VISIBLE
//
//                    registerUser(fullName, email, mobile, dob, gender, password)
//                }
//            }
//        }
//    }
//
//    private fun registerUser(
//        fullName: String,
//        email: String,
//        mobile: String,
//        dob: String,
//        gender: String,
//        password: String
//    ) {
//        val auth: FirebaseAuth = FirebaseAuth.getInstance()
//        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                val user = auth.currentUser
//
//                // Store user data in Firebase Realtime Database
//                val userId = user?.uid ?: return@addOnCompleteListener
//                val userDatabaseRef =
//                    FirebaseDatabase.getInstance().getReference("Users").child(userId)
//                val userData = User(userId, fullName, dob, gender,mobile, email ,"Online")
//
//                userDatabaseRef.setValue(userData).addOnCompleteListener { dbTask ->
//                    if (dbTask.isSuccessful) {
//                        binding.progressBar.visibility=View.GONE
//
//                        user.sendEmailVerification()
//
//                        Toast.makeText(
//                            this,
//                            "Registration successful: ${user.email}",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        val intent = Intent(this, PreferenceSelection::class.java)
//                        startActivity(intent)
//                        finish()
//                        // Start UserProfileActivity and clear previous activities
////                        val intent = Intent(this, UserProfileActivity::class.java)
////                        intent.flags =
////                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
////                        startActivity(intent)
////                        finish() // Close RegisterActivity
//                    } else {
//                        Toast.makeText(
//                            this,
//                            "Failed to save user data: ${dbTask.exception?.message}",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        binding.progressBar.visibility=View.GONE
//
//                    }
//                }
//            } else {
//                Toast.makeText(
//                    this,
//                    "Registration failed: ${task.exception?.message}",
//                    Toast.LENGTH_SHORT
//                ).show()
//                try {
//                    throw task.exception ?: Exception("Unknown error")
//                } catch (e: FirebaseAuthWeakPasswordException) {
//                    // Handle the weak password exception
//                    Toast.makeText(this, "Weak password: ${e.message}", Toast.LENGTH_SHORT).show()
//                } catch (e: FirebaseAuthInvalidCredentialsException) {
//                    // Handle invalid credentials exception
//                    Toast.makeText(this, "Invalid credentials: ${e.message}", Toast.LENGTH_SHORT)
//                        .show()
//                } catch (e: FirebaseAuthUserCollisionException) {
//                    // Handle user collision exception
//                    Toast.makeText(this, "User already exists: ${e.message}", Toast.LENGTH_SHORT)
//                        .show()
//                } catch (e: Exception) {
//                    // Handle any other exceptions
//                    Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_SHORT)
//                        .show()
//                }
//                binding.progressBar.visibility=View.GONE
//
//            }
//        }
//    }
//
//}
//
