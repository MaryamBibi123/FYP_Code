// AdminPendingReportsFragment.kt
package com.example.signuplogina.fragments // Or your package

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.signuplogina.R
import com.example.signuplogina.adapter.AdminReportAdapter // Create this adapter
import com.example.signuplogina.databinding.FragmentAdminPendingReportsBinding
import com.example.signuplogina.modal.FeedbackTags
import com.example.signuplogina.modal.Report
import com.example.signuplogina.mvvm.AdminViewModel
import com.example.signuplogina.mvvm.UsersRepo
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class AdminPendingReportsFragment : Fragment() {

    private var _binding: FragmentAdminPendingReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminViewModel by viewModels()
    private lateinit var reportAdapter: AdminReportAdapter
    private val usersRepo = UsersRepo() // Instance of UsersRepo for fetching user details


    private var filterByUserId: String? = null
    private var filterByStatus: String? = "pending_review" // Default for "all pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            filterByUserId = it.getString("userIdToFilterReports") // From nav args
            if (filterByUserId != null) {
                filterByStatus = it.getString("statusFilterForUser") // Optional: allow filtering status for specific user too
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminPendingReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()

        loadReports()
    }

    private fun loadReports() {
        if (filterByUserId != null) {
            binding.tvReportsTitle.text = "Reports for User: ${filterByUserId?.take(8)}..." // Or fetch name
            viewModel.fetchReportsForTargetUser(filterByUserId!!, filterByStatus)
        } else {
            binding.tvReportsTitle.text = "All Pending Reports"
            viewModel.fetchAllPendingReports() // Fetches only "pending_review"
        }
    }

    private fun setupRecyclerView() {
        reportAdapter = AdminReportAdapter(
            onDismissReportClicked = { report ->
                viewModel.updateReportStatus(report.reportId, "dismissed", "Dismissed by admin.")
            },
            onTakeActionClicked = { report ->
                // This is where you show the dialog to choose block type and reason
                showAdminActionChoiceDialog(report)
            },
            onReportedUserClicked = { userId ->
                // Navigate to UserProfileForAdminFragment
                fetchUserAndNavigateToProfile(userId)


            }
        )
        binding.rvPendingReports.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = reportAdapter
        }
    }

    private fun fetchUserAndNavigateToProfile(userId: String) {
        if (userId.isBlank()) {
            Toast.makeText(context, "Invalid User ID.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.pbLoadingReports.visibility = View.VISIBLE // Show loading indicator

        // Use the UsersRepo method you provided
        usersRepo.getUserDetailsById(userId) { user ->
            binding.pbLoadingReports.visibility = View.GONE // Hide loading indicator
            if (user != null) {
                try {
                    // Assuming your nav action takes a User object
                    val action = AdminPendingReportsFragmentDirections
                        .actionAdminPendingReportsFragmentToUserProfileForAdminFragment(user) // Pass the User object
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    Log.e("AdminPendingReports", "Navigation to UserProfileForAdmin failed", e)
                    Toast.makeText(context, "Could not open user profile.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "User details not found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.pbLoadingReports.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        val reportListObserver = Observer<List<Report>?> { reports ->
            binding.tvNoReports.visibility = if (reports.isNullOrEmpty()) View.VISIBLE else View.GONE
            reportAdapter.submitList(reports ?: emptyList())
            binding.tvNoReports.text = if (filterByUserId != null) "No reports found for this user." else "No pending reports."
        }

        if (filterByUserId != null) {
            viewModel.reportsForTargetUser.observe(viewLifecycleOwner, reportListObserver)
        } else {
            viewModel.allPendingReports.observe(viewLifecycleOwner, reportListObserver)
        }


        viewModel.reportActionOutcome.observe(viewLifecycleOwner) { outcome ->
            outcome?.let {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                if (it.success) {
                    loadReports() // Refresh list after action
                }
                viewModel.clearReportActionOutcome()
            }
        }
        viewModel.userActionOutcome.observe(viewLifecycleOwner) { outcome ->
            outcome?.let {
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                // User block actions are complex, UserProfileForAdmin will handle its own refresh if navigated to.
                // If staying on this screen, refreshing reports is good.
                if (it.success) {
                    loadReports()
                }
                viewModel.clearUserActionOutcome()
            }
        }
    }

    private fun showAdminActionChoiceDialog(report: Report) {
        val options = arrayOf("Warn User", "Temporary Block (7 days)", "Temporary Block (30 days)", "Permanent Block")
        AlertDialog.Builder(requireContext())
            .setTitle("Action for Report: ${report.reportId.take(8)}")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Warn User
                        val adminNotes = "User warned regarding report ${report.reportId.take(8)}: ${report.reportReasonTags.joinToString()}"
                        viewModel.updateReportStatus(report.reportId, "resolved_warned", adminNotes)
                    }
                    1 -> showAdminBlockReasonInputDialog(report, false, 7) // Temp Block 7 days
                    2 -> showAdminBlockReasonInputDialog(report, false, 30) // Temp Block 30 days
                    3 -> { // Permanent Block - Check pre-requisites
                        viewModel.selectedUserDetails.value?.let { user -> // Assuming user details are already loaded if on UserProfile screen
                            val hasPreviousTempBlock = user.ratings?.blockHistory?.any { it.blockType.startsWith("temporary") } ?: false
                            if (!hasPreviousTempBlock && (user.ratings?.reportCount ?: 0) < 3) { // Example: 3 reports or prior temp block for perm
                                Toast.makeText(context, "User must have a prior temporary block or multiple reports for permanent block.", Toast.LENGTH_LONG).show()
                                return@setItems
                            }
                            showAdminBlockReasonInputDialog(report, true, null)
                        } ?: run { // If user details not loaded (e.g. from general pending reports list)
                            Toast.makeText(context, "Cannot determine block history. View user profile first for permanent block.", Toast.LENGTH_LONG).show()
                            // Or fetch user details here first before allowing perm block
                            // For now, simpler to direct admin to user profile for permanent block if coming from general list.
                            // If this dialog IS being shown from UserProfileForAdmin, user details SHOULD be loaded.
                            // This logic path is more for when "Take Action" is from the general pending reports list.
                            // A better UX from general list might be: "Take Action" -> navigates to UserProfileForAdmin(report.reportedUserId)
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAdminBlockReasonInputDialog(report: Report, isPermanent: Boolean, durationDays: Int?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.admin_block_reason, null)
        val titleTv = dialogView.findViewById<TextView>(R.id.tv_admin_block_dialog_title)
        val reasonEditText = dialogView.findViewById<EditText>(R.id.et_admin_block_reason_custom)
        val reasonChipGroup = dialogView.findViewById<ChipGroup>(R.id.chip_group_admin_block_reasons)

        titleTv.text = if (isPermanent) "Reason for Permanent Block" else "Reason for Temporary Block (${durationDays} days)"

        // Populate admin block reason chips
        FeedbackTags.getAdminBlockReasonTags().forEach { tag ->
            val chip = Chip(requireContext()).apply { text = tag.label; isCheckable = true; this.tag = tag }
            reasonChipGroup.addView(chip)
        }
        // Pre-select chips based on report.reportReasonTags if they match admin block reasons
        report.reportReasonTags.forEach { reportReasonLabel ->
            val matchingAdminTag = FeedbackTags.getAdminBlockReasonTags().find { it.label == reportReasonLabel }
            if(matchingAdminTag != null){
                for(i in 0 until reasonChipGroup.childCount){
                    val chip = reasonChipGroup.getChildAt(i) as Chip
                    if(chip.tag == matchingAdminTag){
                        chip.isChecked = true
                        break
                    }
                }
            }
        }


        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Confirm Block Action") { _, _ ->
                val customReason = reasonEditText.text.toString().trim()
                val selectedChipReasons = mutableListOf<String>()
                for (i in 0 until reasonChipGroup.childCount) {
                    val chip = reasonChipGroup.getChildAt(i) as Chip
                    if (chip.isChecked) { (chip.tag as? FeedbackTags)?.let { selectedChipReasons.add(it.label) } }
                }

                var combinedReason = selectedChipReasons.joinToString(", ")
                if (customReason.isNotBlank()) {
                    if (combinedReason.isNotBlank()) combinedReason += " (Details: $customReason)"
                    else combinedReason = customReason
                }

                if (combinedReason.isBlank()) {
                    Toast.makeText(context, "A reason for blocking is required.", Toast.LENGTH_SHORT).show()
                    // Re-show dialog or handle better
                    return@setPositiveButton
                }

                // Fetch user's full name for the toast message - can be passed or fetched
                val userName = report.reportedUserId // Placeholder, ideally fetch full name
                if (isPermanent) {
                    viewModel.permanentlyBlockUser(report.reportedUserId, userName, combinedReason, report.reportId)
                } else {
                    durationDays?.let {
                        viewModel.temporarilyBlockUser(report.reportedUserId, userName, it, combinedReason, report.reportId)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvPendingReports.adapter = null
        _binding = null
    }
}