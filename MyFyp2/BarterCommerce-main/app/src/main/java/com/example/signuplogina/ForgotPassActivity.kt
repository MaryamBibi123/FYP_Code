package com.example.signuplogina

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.signuplogina.databinding.ActivityForgotPassBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class ForgotPassActivity : AppCompatActivity() {
    private lateinit var buttonPasswordReset: Button
    private lateinit var editTextPwdResetEmail:EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var authProfile:FirebaseAuth
    private lateinit var binding: ActivityForgotPassBinding
    private val TAG:String="ForgotPasswordActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityForgotPassBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title="Forget Password"
        editTextPwdResetEmail=binding.editTextPasswordResetEmail
        buttonPasswordReset=binding.buttonPasswordReset
        progressBar=binding.progressBar

        buttonPasswordReset.setOnClickListener {
            val email=editTextPwdResetEmail.text.toString()
            if ( email.isEmpty()){
                Toast.makeText(this,"Please enter your registered email",Toast.LENGTH_SHORT).show()
                editTextPwdResetEmail.error="Email is required"
                editTextPwdResetEmail.requestFocus()

        }
            else if ( !Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                Toast.makeText(this,"Please enter valid email",Toast.LENGTH_SHORT).show()
                editTextPwdResetEmail.error="Valid Email is required"
                editTextPwdResetEmail.requestFocus()

            }
            else{
                progressBar.visibility= View.VISIBLE
                resetPassword(email)
            }


        }
    }

    private fun resetPassword(email:String) {
        //creating the instance of the firebase authentication
        authProfile=FirebaseAuth.getInstance()
        authProfile.sendPasswordResetEmail(email).addOnCompleteListener{
            if ( it.isSuccessful){
                Toast.makeText(this,"Please check your inbox for password reset link",Toast.LENGTH_SHORT).show()

                val intent = Intent(this@ForgotPassActivity,SignUpLogin::class.java)
                // clear the stack to prevent the user from coming back to the forgotPasswordActivity
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK )
                startActivity(intent)
                finish()// close ForgotPasswordActivity
            }
            else{
                try {
                    throw it.exception ?: Exception("Unknown error")
                } catch (e: FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(this, "Invalid email format. Please enter a valid email.", Toast.LENGTH_SHORT).show()
                }
                catch (e: FirebaseAuthInvalidUserException) {
                    Toast.makeText(this, "User doesn't exist or is no longer valid .Please register again.", Toast.LENGTH_SHORT).show()
                }
                catch (e: Exception) {

                    Log.e(TAG,e.message!!)
                    Toast.makeText(this, "Something went wrong: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            progressBar.visibility=View.GONE

        }
    }
}