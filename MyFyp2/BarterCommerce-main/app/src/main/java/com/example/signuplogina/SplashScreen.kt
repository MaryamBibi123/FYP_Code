package com.example.signuplogina

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashScreen : AppCompatActivity() { // Rename class to SplashScreenActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val logo: ImageView = findViewById(R.id.logo)
        val appName: TextView = findViewById(R.id.app_name)
        val barterIcon1: ImageView = findViewById(R.id.barter_icon_1)
        val barterIcon2: ImageView = findViewById(R.id.barter_icon_2)

        // Create animations for the icons moving towards each other
        val slideInLeftToRight = TranslateAnimation(-1000f, 0f, 0f, 0f)
        slideInLeftToRight.duration = 1000

        val slideInRightToLeft = TranslateAnimation(1000f, 0f, 0f, 0f)
        slideInRightToLeft.duration = 1000

        // Set icons visible
        barterIcon1.visibility = ImageView.VISIBLE
        barterIcon2.visibility = ImageView.VISIBLE

        // Start initial animation for both icons
        barterIcon1.startAnimation(slideInLeftToRight)
        barterIcon2.startAnimation(slideInRightToLeft)

        slideInRightToLeft.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                // Swap directions after icons meet
                val slideOutLeftToRight = TranslateAnimation(0f, 1000f, 0f, 0f)
                slideOutLeftToRight.duration = 1000

                val slideOutRightToLeft = TranslateAnimation(0f, -1000f, 0f, 0f)
                slideOutRightToLeft.duration = 1000

                // Start the reverse animation
                barterIcon1.startAnimation(slideOutRightToLeft)
                barterIcon2.startAnimation(slideOutLeftToRight)

                // Show logo and app name after swap animation ends
                slideOutLeftToRight.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        logo.visibility = ImageView.VISIBLE
                        appName.visibility = TextView.VISIBLE
                        logo.startAnimation(TranslateAnimation(0f, 0f, -500f, 0f).apply { duration = 500 })
                        appName.startAnimation(TranslateAnimation(0f, 0f, -500f, 0f).apply { duration = 500 })
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}
                })
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // Move to the next activity after the splash
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, SignUpLogin::class.java)
            startActivity(intent)
            finish()
        }, 4000) // 4 seconds delay for splash screen
        }
}
