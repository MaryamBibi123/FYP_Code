package com.example.signuplogina.mvvm

import android.util.Log
import com.example.signuplogina.modal.ExchangeAgreement
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class FirebaseAgreementManager {
    val refDb = FirebaseDatabase.getInstance()
        .getReference("ExchangeAgreements")

    companion object {
        private const val TAG = "FirebaseAgreementManager"
    }


    fun getExchangeAgreementRef(
        bidId: String,
        onComplete: (ExchangeAgreement?) -> Unit
    ) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("ExchangeAgreements")
            .child(bidId)

        ref.get().addOnSuccessListener { snapshot ->
            val agreement = snapshot.getValue(ExchangeAgreement::class.java)
            onComplete(agreement)
        }.addOnFailureListener {
            onComplete(null)
        }
    }

    fun generateOtpsAndStore(bidId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("ExchangeAgreements").child(bidId)
        ref.get().addOnSuccessListener { snap ->
            val agreement =
                snap.getValue(ExchangeAgreement::class.java) ?: return@addOnSuccessListener

            if (agreement.otpByBidder.isNullOrBlank() || agreement.otpByReceiver.isNullOrBlank()) {
                val otpForReceiver = (100000..999999).random().toString()
                Log.d("Otes", "generateOtpsAndStore: $otpForReceiver")
                val otpForBidder = (100000..999999).random().toString()
                Log.d("Otes", "generateOtpsAndStore: $otpForBidder")

                val otpData = mapOf(
                    "otpByReceiver" to otpForReceiver,
                    "otpByBidder" to otpForBidder,
//            "isBidderConfirmed" to false,
//            "isReceiverConfirmed" to false,
                    "status" to "agreement_reached", // or "PendingOTP" if using that
                )
                FirebaseDatabase.getInstance().getReference("ExchangeAgreements")
                    .child(bidId)
                    .updateChildren(otpData)

            }


        }

    }


    fun verifyOtp(
        bidId: String,
        currentUserId: String,
        enteredOtp: String,
        // MODIFIED CALLBACK:
        onResult: (otpVerificationSuccess: Boolean, isExchangeNowCompleted: Boolean, updatedAgreement: ExchangeAgreement?) -> Unit
    ) {
        val agreementRef = refDb.child(bidId)

        agreementRef.get().addOnSuccessListener { initialSnapshot ->
            val initialAgreement = initialSnapshot.getValue(ExchangeAgreement::class.java)
            if (initialAgreement == null) {
                Log.e(TAG, "verifyOtp: Agreement not found for bidId $bidId")
                onResult(false, false, null)
                return@addOnSuccessListener
            }

            var isOtpActuallyCorrect = false
            var verificationFieldToUpdate: String? = null

            if (currentUserId == initialAgreement.bidderId && enteredOtp == initialAgreement.otpByReceiver) {
                if (!initialAgreement.bidderOtpVerified) { // Only proceed if not already verified
                    isOtpActuallyCorrect = true
                    verificationFieldToUpdate = "bidderOtpVerified"
                } else {
                    Log.d(TAG, "Bidder OTP already verified for bid $bidId")
                    // OTP is correct, but already marked. Treat as success for this step.
                    // The isExchangeNowCompleted will be determined by re-fetching.
                    isOtpActuallyCorrect = true
                }
            } else if (currentUserId == initialAgreement.receiverId && enteredOtp == initialAgreement.otpByBidder) {
                if (!initialAgreement.receiverOtpVerified) { // Only proceed if not already verified
                    isOtpActuallyCorrect = true
                    verificationFieldToUpdate = "receiverOtpVerified"
                } else {
                    Log.d(TAG, "Receiver OTP already verified for bid $bidId")
                    isOtpActuallyCorrect = true
                }
            }

            if (isOtpActuallyCorrect) {
                if (verificationFieldToUpdate != null) { // If there's a flag to update
                    agreementRef.child(verificationFieldToUpdate).setValue(true).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Re-fetch the agreement to get the latest state of both verification flags
                            agreementRef.get().addOnSuccessListener { updatedSnapshot ->
                                val currentAgreement = updatedSnapshot.getValue(ExchangeAgreement::class.java)
                                if (currentAgreement == null) {
                                    onResult(true, false, null) // OTP set, but couldn't re-fetch agreement
                                    return@addOnSuccessListener
                                }

                                val bothVerified = currentAgreement.bidderOtpVerified && currentAgreement.receiverOtpVerified
                                if (bothVerified && currentAgreement.status != "completed") {
                                    // Both are now verified, and status isn't "completed" yet
                                    agreementRef.child("status").setValue("completed").addOnCompleteListener { statusUpdateTask ->
                                        if (statusUpdateTask.isSuccessful) {
                                            Log.d(TAG, "Agreement $bidId status set to completed.")
                                            val finalAgreement = currentAgreement.copy(status = "completed")
                                            onResult(true, true, finalAgreement) // OTP success, exchange NOW completed
                                        } else {
                                            Log.e(TAG, "Failed to set agreement $bidId status to completed.")
                                            onResult(true, false, currentAgreement) // OTP success, but status update failed
                                        }
                                    }
                                } else if (bothVerified && currentAgreement.status == "completed") {
                                    // Already completed, nothing more to do here from status update perspective
                                    onResult(true, true, currentAgreement) // OTP success, exchange WAS ALREADY completed
                                }
                                else {
                                    // Only one (or none, if this was just re-confirming an already set flag) verified
                                    onResult(true, false, currentAgreement) // OTP success, but exchange not yet completed by both
                                }
                            }.addOnFailureListener {
                                Log.e(TAG, "Failed to re-fetch agreement after OTP verification for $bidId")
                                onResult(true, false, null) // OTP set, but re-fetch failed
                            }
                        } else { // setValue(true) for verificationFieldToUpdate failed
                            Log.e(TAG, "Failed to set $verificationFieldToUpdate for $bidId")
                            onResult(false, false, initialAgreement) // OTP verification process failed
                        }
                    }
                } else {
                    // No verificationFieldToUpdate means the OTP was correct but already marked as verified for this user.
                    // Check if this makes the exchange complete.
                    val bothStillVerified = initialAgreement.bidderOtpVerified && initialAgreement.receiverOtpVerified
                    if (bothStillVerified && initialAgreement.status == "completed") {
                        onResult(true, true, initialAgreement) // Correct OTP (re-entry), exchange already complete
                    } else if (bothStillVerified && initialAgreement.status != "completed") {
                        // This case is tricky: OTP was correct (already verified), both are verified, but status not completed.
                        // This might indicate a race condition or previous failure to set status. Attempt to set it.
                        agreementRef.child("status").setValue("completed").addOnCompleteListener { statusUpdateTask ->
                            if (statusUpdateTask.isSuccessful) {
                                val finalAgreement = initialAgreement.copy(status = "completed")
                                onResult(true, true, finalAgreement)
                            } else {
                                onResult(true, false, initialAgreement)
                            }
                        }
                    }
                    else {
                        onResult(true, false, initialAgreement) // Correct OTP (re-entry), but not yet completed by other party
                    }
                }
            } else { // OTP was incorrect
                Log.d(TAG, "Incorrect OTP entered for bid $bidId by user $currentUserId")
                onResult(false, false, initialAgreement)
            }
        }.addOnFailureListener {
            Log.e(TAG, "Failed to initially fetch agreement $bidId for OTP verification")
            onResult(false, false, null)
        }
    }
//    fun verifyOtp(
//        bidId: String,
//        currentUserId: String,
//        enteredOtp: String,
//        onResult: (Boolean) -> Unit
//    ) {
//        val ref = FirebaseDatabase.getInstance().getReference("ExchangeAgreements").child(bidId)
//
//        ref.get().addOnSuccessListener { snapshot ->
//            val agreement = snapshot.getValue(ExchangeAgreement::class.java) ?: return@addOnSuccessListener
//
//            var isOtpCorrect = false
//            var verificationFieldToUpdate: String? = null
//
//            if (currentUserId == agreement.bidderId && enteredOtp == agreement.otpByReceiver) {
//                isOtpCorrect = true
//                verificationFieldToUpdate = "bidderOtpVerified"
//            } else if (currentUserId == agreement.receiverId && enteredOtp == agreement.otpByBidder) {
//                isOtpCorrect = true
//                verificationFieldToUpdate = "receiverOtpVerified"
//            }
//
//            if (isOtpCorrect && verificationFieldToUpdate != null) {
//                ref.child(verificationFieldToUpdate).setValue(true).addOnCompleteListener {
//                    if (it.isSuccessful) {
//                        // Re-fetch data after updating the verification flag
//                        ref.get().addOnSuccessListener { updatedSnapshot ->
//                            val bidderVerified = updatedSnapshot.child("bidderOtpVerified").getValue(Boolean::class.java) == true
//                            val receiverVerified = updatedSnapshot.child("receiverOtpVerified").getValue(Boolean::class.java) == true
//
//                            Log.d("AgreementStatusUpdated", "Bidder Verified: $bidderVerified, Receiver Verified: $receiverVerified")
//
//                            if (bidderVerified && receiverVerified) {
//                                ref.child("status").setValue("completed")
//
//                            }
//
//                            onResult(true)
//                        }
//                    } else {
//                        onResult(false)
//                    }
//                }
//            } else {
//                onResult(false)
//            }
//        }
//    }






    }
