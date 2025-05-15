package com.example.signuplogina.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import com.example.signuplogina.databinding.FragmentAskGeneralPollBinding
import com.example.signuplogina.modal.PollModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AskGeneralPollBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentAskGeneralPollBinding? = null
    private val binding get() = _binding!!

    private val database by lazy { FirebaseDatabase.getInstance().reference }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        _binding = FragmentAskGeneralPollBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        binding.savePollButton.setOnClickListener {
            savePollToFirebase()
        }

        return dialog
    }

    private fun savePollToFirebase() {
        val question = binding.questionEditText.text.toString().trim()
        val option1 = binding.option1EditText.text.toString().trim()
        val option2 = binding.option2EditText.text.toString().trim()
        val option3 = binding.option3EditText.text.toString().trim()

        if (question.isEmpty() || option1.isEmpty() || option2.isEmpty() || option3.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return

        // Generate pollId using push under Uploader
        val pollRef = database
            .child("Polls")
            .child("General")
            .child("Uploader")
            .child(userId)
            .push()

        val pollId = pollRef.key ?: return

        val poll = PollModel(
            id = pollId,
            userId = userId,
            question = question,
            option1 = option1,
            option2 = option2,
            option3 = option3
        )

        pollRef.setValue(poll)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Poll saved successfully!", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save poll", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
