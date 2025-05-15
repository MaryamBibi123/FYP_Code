package com.example.signuplogina

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.example.signuplogina.R
import com.google.firebase.auth.FirebaseAuth

class AdminDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        findViewById<Button>(R.id.btn_pending_items).setOnClickListener {
            openItemList("pending")
        }

        findViewById<Button>(R.id.btn_approved_items).setOnClickListener {
            openItemList("approved")
        }

        findViewById<Button>(R.id.btn_rejected_items).setOnClickListener {
            openItemList("rejected")
        }

        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            finish()
        }
    }

    private fun openItemList(status: String) {
        val intent = Intent(this, AdminItemsActivity::class.java)
        intent.putExtra("STATUS", status)
        startActivity(intent)
    }
}
