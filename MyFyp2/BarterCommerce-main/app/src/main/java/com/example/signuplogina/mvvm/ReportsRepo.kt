// ReportsRepo.kt
package com.example.signuplogina.mvvm // Or your repo package

import android.util.Log
import com.example.signuplogina.modal.Report
import com.google.firebase.database.*

class ReportsRepo {
    private val reportsRef = FirebaseDatabase.getInstance().getReference("reports")
    private val TAG = "ReportsRepo"

    fun getPendingReports(onResult: (List<Report>) -> Unit) {
        reportsRef.orderByChild("status").equalTo("pending_review")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val reports = snapshot.children.mapNotNull { it.getValue(Report::class.java) }
                    onResult(reports.sortedByDescending { it.timestamp }) // Show newest first
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "getPendingReports failed: ${error.message}", error.toException())
                    onResult(emptyList())
                }
            })
    }

    fun getReportsForUser(userId: String, onResult: (List<Report>) -> Unit) {
        reportsRef.orderByChild("reportedUserId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val reports = snapshot.children.mapNotNull { it.getValue(Report::class.java) }
                    onResult(reports.sortedByDescending { it.timestamp })
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "getReportsForUser failed for $userId: ${error.message}", error.toException())
                    onResult(emptyList())
                }
            })
    }

    fun getReportsForUserFilteredByStatus(userId: String, status: String?, onResult: (List<Report>) -> Unit) {
        val query = if (status != null) {
            // This requires a composite index on reportedUserId_status if you create one
            // Otherwise, Firebase allows filtering by one child then client-side for the other
            reportsRef.orderByChild("reportedUserId").equalTo(userId)
        } else {
            reportsRef.orderByChild("reportedUserId").equalTo(userId) // All reports for user
        }

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var reports = snapshot.children.mapNotNull { it.getValue(Report::class.java) }
                if (status != null) {
                    reports = reports.filter { it.status == status }
                }
                onResult(reports.sortedByDescending { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "getReportsForUserFiltered failed: ${error.message}")
                onResult(emptyList())
            }
        })
    }


    fun updateReport(reportId: String, updates: Map<String, Any?>, onComplete: (Boolean) -> Unit) {
        if (reportId.isBlank()) {
            onComplete(false)
            return
        }
        reportsRef.child(reportId).updateChildren(updates)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e(TAG, "updateReport failed for $reportId", e)
                onComplete(false)
            }
    }
}