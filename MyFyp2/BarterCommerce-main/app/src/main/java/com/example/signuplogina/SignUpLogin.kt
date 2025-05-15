package com.example.signuplogina

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.signuplogina.databinding.ActivitySignUpLoginBinding

class SignUpLogin : AppCompatActivity() {
    private lateinit var binding:ActivitySignUpLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
//
        binding.buttonLogin.setOnClickListener {
            val intent=Intent(this,LoginActivity::class.java)
            startActivity(intent)
        }

        binding.buttonRegister.setOnClickListener {
            val intent=Intent(this,RegisterActivity::class.java)
            startActivity(intent)
        }



    }
}
