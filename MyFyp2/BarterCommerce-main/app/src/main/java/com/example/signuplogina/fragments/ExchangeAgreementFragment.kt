package com.example.signuplogina.fragments

import android.graphics.Color
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
// import androidx.core.content.ContentProviderCompat.requireContext // Not needed
import androidx.navigation.fragment.findNavController
import com.example.signuplogina.Bid
import com.example.signuplogina.R
import com.example.signuplogina.Utils
import com.example.signuplogina.mvvm.FirebaseAgreementManager
import com.example.signuplogina.databinding.FragmentExchangeAgreementBinding
import com.example.signuplogina.modal.ExchangeAgreement
// import com.google.firebase.auth.FirebaseAuth // Not directly used for auth here
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import android.util.Log
import java.util.Date
import java.util.Locale
// Removed redundant import of String

class ExchangeAgreementFragment : Fragment() {

    private var _binding: FragmentExchangeAgreementBinding? = null
    private val binding get() = _binding!!

    private lateinit var currentUserId: String
    private lateinit var agreement: ExchangeAgreement // Will be initialized in loadBidAndAgreement
    private lateinit var bid: Bid // Will be initialized in loadBidAndAgreement
    private lateinit var roomId: String
    private lateinit var firebaseAgreementManager: FirebaseAgreementManager // Renamed for clarity
    private var bidIdFromArgs: String? = null // Renamed for clarity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentUserId = Utils.getUidLoggedIn()
        bidIdFromArgs = arguments?.getString("bidId")
        Log.d("ExchangeAgreementFrag", "onCreate: Bid ID from args: $bidIdFromArgs")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExchangeAgreementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (bidIdFromArgs.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Bid ID missing. Cannot load agreement.", Toast.LENGTH_LONG).show()
            findNavController().popBackStack()
            return
        }

        firebaseAgreementManager = FirebaseAgreementManager()
        loadBidAndAgreement(bidIdFromArgs!!)

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSave.setOnClickListener {
            saveConfirmationAndUpdateStatuses() // Renamed for clarity
        }
    }

    private fun loadBidAndAgreement(bidIdStr: String) {
        binding.progressBarAgreement?.visibility = View.VISIBLE // Show progress
        val bidRef = FirebaseDatabase.getInstance().getReference("Bids").child(bidIdStr)

        bidRef.get().addOnSuccessListener { bidSnap ->
            val fetchedBid = bidSnap.getValue(Bid::class.java)
            if (fetchedBid == null) {
                Toast.makeText(requireContext(), "Error: Bid details not found.", Toast.LENGTH_LONG).show()
                binding.progressBarAgreement?.visibility = View.GONE
                findNavController().popBackStack()
                return@addOnSuccessListener
            }
            bid = fetchedBid // Assign to class property
            roomId = "$bidIdStr-${listOf(bid.bidderId, bid.receiverId).sorted().joinToString("")}"

            val meetingRef = FirebaseDatabase.getInstance()
                .getReference("Messages/ExchangeChats/$roomId/meeting")

            meetingRef.get().addOnSuccessListener { meetingSnap ->
                val location = meetingSnap.child("location").getValue(String::class.java)
                val date = meetingSnap.child("date").getValue(String::class.java)
                val time = meetingSnap.child("time").getValue(String::class.java)

                if (location.isNullOrBlank() || date.isNullOrBlank() || time.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "Meeting details not yet set up in chat.", Toast.LENGTH_LONG).show()
                    binding.btnSave.isEnabled = false
                    binding.btnSave.alpha = 0.5f
                    binding.progressBarAgreement?.visibility = View.GONE
                    // Populate UI with whatever agreement exists, or default message
                    // It's important to still try and load the agreement
                } else {
                    binding.btnSave.isEnabled = true
                    binding.btnSave.alpha = 1f
                }

                val agreementRef = FirebaseDatabase.getInstance().getReference("ExchangeAgreements").child(bidIdStr)
                agreementRef.get().addOnSuccessListener { agreementSnap ->
                    agreement = agreementSnap.getValue(ExchangeAgreement::class.java)
                        ?: ExchangeAgreement(
                            bidId = bidIdStr,
                            bidderId = bid.bidderId,
                            receiverId = bid.receiverId,
                            meetingLocation = location ?: "Not set",
                            meetingTime = if(date!=null && time!=null) "$date at $time" else "Not set",
                            terms = agreementSnap.child("terms").getValue(String::class.java) ?: "- No refund/tradeback.\n- Items are as-is.\n✔ Both users to inspect items at meetup.",
                            status = agreementSnap.child("status").getValue(String::class.java) ?: "start", // Default to start if not set
                            timestamp = agreementSnap.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                        )
                    populateUI()
                    binding.progressBarAgreement?.visibility = View.GONE
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to load agreement details.", Toast.LENGTH_SHORT).show()
                    binding.progressBarAgreement?.visibility = View.GONE
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load meeting details.", Toast.LENGTH_SHORT).show()
                binding.progressBarAgreement?.visibility = View.GONE
                // Still try to load agreement even if meeting details fail, it might exist
                val agreementRef = FirebaseDatabase.getInstance().getReference("ExchangeAgreements").child(bidIdStr)
                agreementRef.get().addOnSuccessListener { agreementSnap ->
                    // ... (similar logic as above to load or create default agreement) ...
                    populateUI()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to load bid details.", Toast.LENGTH_LONG).show()
            binding.progressBarAgreement?.visibility = View.GONE
            findNavController().popBackStack()
        }
    }

    private fun populateUI() {
        if (!::agreement.isInitialized || !::bid.isInitialized) {
            Log.e("ExchangeAgreementFrag", "Agreement or Bid not initialized for UI population.")
            return
        }

        if (currentUserId == bid.bidderId) {
            binding.checkboxUserB.text = "Receiver: ##${bid.receiverId.takeLast(6)}"
            binding.checkboxUserA.text = "Bidder: You | Id:##${bid.bidderId.takeLast(6)}"
        } else {
            binding.checkboxUserB.text = "Receiver: You | Id:##${bid.receiverId.takeLast(6)}"
            binding.checkboxUserA.text = "Bidder: ##${bid.bidderId.takeLast(6)}"
        }

        binding.meetingLocation.text = "• Location: ${agreement.meetingLocation}"
        binding.meetingTime.text = "• Date and Time: ${agreement.meetingTime}"
        binding.terms.text = agreement.terms
        val timestampMillis = agreement.timestamp
        val date = Date(timestampMillis)
        val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) // More complete format
        binding.timestamp.text = "Last updated: ${format.format(date)}"

        binding.checkboxUserA.isChecked = agreement.isBidderConfirmed
        binding.checkboxUserB.isChecked = agreement.isReceiverConfirmed

        // Enable checkbox only if current user is the respective party AND they haven't confirmed yet AND agreement is not completed
        val canBidderConfirm = currentUserId == bid.bidderId && !agreement.isBidderConfirmed && agreement.status != "agreement_reached" && agreement.status != "completed"
        val canReceiverConfirm = currentUserId == bid.receiverId && !agreement.isReceiverConfirmed && agreement.status != "agreement_reached" && agreement.status != "completed"

        binding.checkboxUserA.isEnabled = canBidderConfirm
        binding.checkboxUserB.isEnabled = canReceiverConfirm

        // Enable save button only if the current user can make a change AND meeting details are set
        val meetingDetailsSet = !agreement.meetingLocation.equals("Not set", ignoreCase = true) && !agreement.meetingTime.equals("Not set", ignoreCase = true)
        binding.btnSave.isEnabled = (canBidderConfirm || canReceiverConfirm) && meetingDetailsSet
        binding.btnSave.alpha = if (binding.btnSave.isEnabled) 1.0f else 0.5f

        updateStatusTextAndOtpGeneration() // Update status text based on loaded agreement
    }

    private fun saveConfirmationAndUpdateStatuses() {
        if (!::agreement.isInitialized || !::bid.isInitialized) return

        val previousBidderConfirmed = agreement.isBidderConfirmed
        val previousReceiverConfirmed = agreement.isReceiverConfirmed

        if (currentUserId == bid.bidderId) {
            agreement.isBidderConfirmed = binding.checkboxUserA.isChecked
        }
        if (currentUserId == bid.receiverId) {
            agreement.isReceiverConfirmed = binding.checkboxUserB.isChecked
        }
        agreement.timestamp = System.currentTimeMillis() // Update timestamp on save

        // Only proceed if there was a change in confirmation status
        if (agreement.isBidderConfirmed != previousBidderConfirmed || agreement.isReceiverConfirmed != previousReceiverConfirmed) {
            binding.progressBarAgreement?.visibility = View.VISIBLE
            binding.btnSave.isEnabled = false // Disable while saving

            val agreementRef = FirebaseDatabase.getInstance().getReference("ExchangeAgreements").child(agreement.bidId)
            agreementRef.setValue(agreement).addOnSuccessListener {
                Toast.makeText(requireContext(), "Confirmation saved ✅", Toast.LENGTH_SHORT).show()
                updateStatusTextAndOtpGeneration() // This will check if both confirmed and update statuses

                // Re-enable checkboxes based on new state
                binding.checkboxUserA.isEnabled = currentUserId == bid.bidderId && !agreement.isBidderConfirmed && agreement.status != "agreement_reached" && agreement.status != "completed"
                binding.checkboxUserB.isEnabled = currentUserId == bid.receiverId && !agreement.isReceiverConfirmed && agreement.status != "agreement_reached" && agreement.status != "completed"
                // Save button will be handled by populateUI/updateStatusText
                binding.progressBarAgreement?.visibility = View.GONE

            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save confirmation. Try again!", Toast.LENGTH_SHORT).show()
                // Revert checkbox state if save failed (optional, or re-fetch data)
                agreement.isBidderConfirmed = previousBidderConfirmed
                agreement.isReceiverConfirmed = previousReceiverConfirmed
                binding.checkboxUserA.isChecked = previousBidderConfirmed
                binding.checkboxUserB.isChecked = previousReceiverConfirmed
                binding.progressBarAgreement?.visibility = View.GONE
                binding.btnSave.isEnabled = true // Re-enable on failure
            }
        } else {
            Toast.makeText(requireContext(), "No changes to save.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatusTextAndOtpGeneration() {
        if (!::agreement.isInitialized) return

        val bothConfirmed = agreement.isBidderConfirmed && agreement.isReceiverConfirmed
        var statusChanged = false

        when {
            bothConfirmed -> {
                binding.agreementStatus.text = "✅ Both users have confirmed. Proceed to exchange OTPs."
                binding.agreementStatus.setTextColor(Color.parseColor("#006400")) // Dark Green
                if (agreement.status != "agreement_reached" && agreement.status != "completed") { // Check if already agreed or completed
                    agreement.status = "agreement_reached" // Use "agreement_reached" as per your workflow
                    firebaseAgreementManager.generateOtpsAndStore(agreement.bidId) // Generate OTPs
                    statusChanged = true
                }
                disableSaveButtonAndCheckboxes()
            }
            agreement.isBidderConfirmed || agreement.isReceiverConfirmed -> {
                binding.agreementStatus.text = "⏳ Waiting for the other user's confirmation..."
                binding.agreementStatus.setTextColor(Color.parseColor("#FFA500")) // Orange
                // agreement.status remains "start" or whatever it was before one confirmed
            }
            else -> {
                binding.agreementStatus.text = "❌ Awaiting user confirmations..."
                binding.agreementStatus.setTextColor(Color.parseColor("#B00020")) // Dark Red
                // agreement.status remains "start"
            }
        }

        if (statusChanged) {
            // If agreement.status changed to "agreement_reached", update Bid status as well
            val updates = mutableMapOf<String, Any>()
            updates["/ExchangeAgreements/${agreement.bidId}/status"] = agreement.status
            updates["/Bids/${agreement.bidId}/status"] = agreement.status // Sync Bid status

            FirebaseDatabase.getInstance().reference.updateChildren(updates)
                .addOnSuccessListener {
                    Log.d("ExchangeAgreementFrag", "Agreement and Bid status updated to ${agreement.status}")
                    // Optionally notify users that agreement is reached and they can proceed to HomeChatExchangeFragment for OTPs
                }
                .addOnFailureListener { e ->
                    Log.e("ExchangeAgreementFrag", "Failed to update Bid/Agreement status: ${e.message}")
                    // Handle failure, maybe revert local agreement.status and show error
                }
        }
    }

    private fun disableSaveButtonAndCheckboxes() {
        binding.btnSave.isEnabled = false
        binding.btnSave.alpha = 0.5f
        binding.checkboxUserA.isEnabled = false
        binding.checkboxUserB.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}









//package com.example.signuplogina.fragments
//
//import android.graphics.Color
//import androidx.fragment.app.Fragment
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.core.content.ContentProviderCompat.requireContext
//import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
//import androidx.navigation.fragment.findNavController
//
//
//import com.example.signuplogina.Bid
//import com.example.signuplogina.R
//import com.example.signuplogina.Utils
//import com.example.signuplogina.mvvm.FirebaseAgreementManager
//import com.example.signuplogina.databinding.FragmentExchangeAgreementBinding
//import com.example.signuplogina.modal.ExchangeAgreement
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.FirebaseDatabase
//import java.text.DateFormat
//import java.text.SimpleDateFormat
//import android.util.Log
//
//import java.util.Date
//import java.util.Locale
//import kotlin.String
//
//class ExchangeAgreementFragment : Fragment() {
//
//    private var _binding: FragmentExchangeAgreementBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var currentUserId: String
//    private lateinit var agreement: ExchangeAgreement
//    private lateinit var bid: Bid
//    private lateinit var roomId: String
//    lateinit var firebaseAgreement: FirebaseAgreementManager
//    private var bidId: String? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        currentUserId = Utils.getUidLoggedIn()
//        bidId = arguments?.getString("bidId")
//        Log.e("first time bid fetch", "the bid id here is $bidId")
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentExchangeAgreementBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        if (bidId == null) {
////            Toast.makeText(requireContext(), "Bid ID missing", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        firebaseAgreement = FirebaseAgreementManager()
////        agreement = ExchangeAgreement()
//        loadBidAndAgreement(bidId!!)
//
//        binding.btnClose.setOnClickListener {
//            findNavController().popBackStack()
//
//        }
//
//        binding.btnSave.setOnClickListener {
//            saveConfirmation()
//        }
//    }
//
//    private fun loadBidAndAgreement(bidId: String) {
//        val bidRef = FirebaseDatabase.getInstance().getReference("Bids").child(bidId)
//
//        bidRef.get().addOnSuccessListener { bidSnap ->
//            bid = bidSnap.getValue(Bid::class.java) ?: return@addOnSuccessListener
//            roomId = "$bidId-${listOf(bid.bidderId, bid.receiverId).sorted().joinToString("")}"
//
//            val meetingRef = FirebaseDatabase.getInstance()
//                .getReference("Messages")
//                .child("ExchangeChats")
//                .child(roomId)
//                .child("meeting")
//
//            meetingRef.get().addOnSuccessListener { meetingSnap ->
//                val location = meetingSnap.child("location").getValue(String::class.java)
//                val date = meetingSnap.child("date").getValue(String::class.java)
//                val time = meetingSnap.child("time").getValue(String::class.java)
//
//                if (location.isNullOrBlank() || date.isNullOrBlank() || time.isNullOrBlank()) {
//                    Toast.makeText(requireContext(), "Meeting not set up yet!", Toast.LENGTH_SHORT)
//                        .show()
//                    binding.btnSave?.isEnabled = false
//                    binding.btnSave?.alpha = 0.5f // visually show it's disabled
//                    return@addOnSuccessListener
//                }
//                binding.btnSave?.isEnabled = true
//                binding.btnSave?.alpha = 1f
//                val agreementRef = FirebaseDatabase.getInstance()
//                    .getReference("ExchangeAgreements")
//                    .child(bidId)
//
//
//                agreementRef.get().addOnSuccessListener { agreementSnap ->
//                    agreement = agreementSnap.getValue(ExchangeAgreement::class.java)
//                        ?: ExchangeAgreement(
//                            bidId = bidId,
//                            bidderId = bid.bidderId,
//                            receiverId = bid.receiverId,
//                            meetingLocation = location,
//                            meetingTime = "$date at $time",
//                            terms = "- No refund/tradeback.\n" +
//                                    "- Items are as-is.\n" +
//                                    "✔ Both users agreed.\n", // or leave it empty if you prefer
//                            status = "start",
//                            timestamp = System.currentTimeMillis(),
////
//
//                        )
//                    Log.e("BidId","the bid id here in exchangeagreement $agreement.bidId"
//                    )
//                    populateUI()
//                }
//            }
//        }
//
//    }
//
//    private fun populateUI() {
//
//        if (currentUserId == bid.bidderId) {
//            binding.checkboxUserB.text = "Receiver: ##${bid.receiverId.takeLast(6)}\n"
//            binding.checkboxUserA.text = "Bidder: You | Id:##${bid.bidderId.takeLast(6)}"
//
//        } else {
//            binding.checkboxUserB.text = "Receiver: You | Id:##${bid.receiverId.takeLast(6)}"
//            binding.checkboxUserA.text = "Bidder: ##${bid.bidderId.takeLast(6)}"
//
//
//        }
//
//        // Meeting Details
//        binding.meetingLocation.text = "• Location: ${agreement.meetingLocation}"
//        binding.meetingTime.text = "• Date and Time: ${agreement.meetingTime}"
//        binding.terms.text = agreement.terms
//        val timestampMillis = agreement.timestamp.let {
//            val date = Date(it)
//            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
//            format.format(date)
//        }
//        binding.timestamp.text = timestampMillis ?: ""
//
//
//        // Status
//        updateStatusText()
//
//        // Checkbox States
//        binding.checkboxUserA.isChecked = agreement.isBidderConfirmed
//        binding.checkboxUserB.isChecked = agreement.isReceiverConfirmed
//
//        // Enable only for current user
//        binding.checkboxUserA.isEnabled =
//            currentUserId == bid.bidderId && !agreement.isBidderConfirmed
//        binding.checkboxUserB.isEnabled =
//            currentUserId == bid.receiverId && !agreement.isReceiverConfirmed
//    }
//
//    private fun saveConfirmation() {
//        if (currentUserId == bid.bidderId) {
//
//            agreement.isBidderConfirmed = binding.checkboxUserA.isChecked
//        }
//        if (currentUserId == bid.receiverId) {
//            agreement.isReceiverConfirmed = binding.checkboxUserB.isChecked
//        }
//
//        val agreementRef = FirebaseDatabase.getInstance()
//            .getReference("ExchangeAgreements")
//            .child(agreement.bidId)
//Log.e("inSave functin","the bid id here in save is  $agreement.bidId")
//        agreementRef.setValue(agreement).addOnSuccessListener {
//            Toast.makeText(requireContext(), "Confirmation saved ✅", Toast.LENGTH_SHORT).show()
//            updateStatusText()
//        }.addOnFailureListener {
//            Toast.makeText(requireContext(), "Failed to save. Try again!", Toast.LENGTH_SHORT)
//                .show()
//        }
//    }
//
//    private fun generateOtp() {
//        Log.e("the Bid Id in the ExchangeAgreement :", "$bidId")
//        firebaseAgreement.generateOtpsAndStore(bidId!!)
//
//    }
//
//    private fun updateStatusText() {
//        when {
//            agreement.isBidderConfirmed && agreement.isReceiverConfirmed -> {
//                binding.agreementStatus.text = "✅ Both users have confirmed."
//                binding.agreementStatus.setTextColor(Color.parseColor("#006400"))
//
//                if (agreement.status != "agreed") {
//                    agreement.status = "agreed"
//                    generateOtp() // only generate once
//                }
//                disableSaveButton() // also disable Save button
//
//            }
//
//            agreement.isBidderConfirmed || agreement.isReceiverConfirmed -> {
//                binding.agreementStatus.text = "⏳ Waiting for one more confirmation..."
//                binding.agreementStatus.setTextColor(Color.parseColor("#FFA500"))
//                agreement.status = "start"
//            }
//
//            else -> {
//                binding.agreementStatus.text = "❌ Awaiting user confirmation..."
//                binding.agreementStatus.setTextColor(Color.parseColor("#B00020"))
//            }
//        }
//    }
//
//    private fun disableSaveButton() {
//        binding.btnSave.isEnabled = false
//        binding.btnSave.alpha = 0.5f
//    }
//
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}
