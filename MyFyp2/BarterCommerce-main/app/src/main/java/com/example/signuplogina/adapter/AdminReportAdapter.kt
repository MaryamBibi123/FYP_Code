// AdminReportAdapter.kt
package com.example.signuplogina.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.signuplogina.databinding.ItemAdminReportBinding // Generated from item_admin_report.xml
import com.example.signuplogina.modal.Report
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class AdminReportAdapter(
    private val onDismissReportClicked: (Report) -> Unit,
    private val onTakeActionClicked: (Report) -> Unit,
    private val onReportedUserClicked: (String) -> Unit // Pass reportedUserId
) : ListAdapter<Report, AdminReportAdapter.ReportViewHolder>(ReportDiffCallback()) {

    inner class ReportViewHolder(private val binding: ItemAdminReportBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        fun bind(report: Report) {
            binding.tvReportId.text = report.reportId.take(12) // Show partial ID
            binding.tvReportDate.text = dateFormat.format(Date(report.timestamp))
            binding.tvReportStatus.text = report.status.replace("_", " ").capitalizeWords()
            binding.tvReportedUserName.text = "UID: ${report.reportedUserId.take(12)}..." // Fetch actual name later
            binding.tvReporterUserName.text = "UID: ${report.reporterUserId.take(12)}..." // Fetch actual name later
            binding.tvReportBidId.text = report.bidId.take(12)

            binding.chipGroupReportItemReasons.removeAllViews()
            report.reportReasonTags.forEach { reason ->
                val chip = Chip(binding.chipGroupReportItemReasons.context).apply { text = reason }
                binding.chipGroupReportItemReasons.addView(chip)
            }

            if (report.reportDescription.isNullOrBlank()) {
                binding.tvReportDescriptionLabel.visibility = View.GONE
                binding.tvReportDescription.visibility = View.GONE
            } else {
                binding.tvReportDescriptionLabel.visibility = View.VISIBLE
                binding.tvReportDescription.visibility = View.VISIBLE
                binding.tvReportDescription.text = report.reportDescription
            }

            // Show/Hide buttons based on report status
            if (report.status == "pending_review") {
                binding.btnDismissReport.visibility = View.VISIBLE
                binding.btnTakeAction.visibility = View.VISIBLE
            } else {
                binding.btnDismissReport.visibility = View.GONE
                binding.btnTakeAction.visibility = View.GONE // Or change to "View Action Taken"
            }


            binding.btnDismissReport.setOnClickListener { onDismissReportClicked(report) }
            binding.btnTakeAction.setOnClickListener { onTakeActionClicked(report) }
            binding.tvReportedUserName.setOnClickListener { onReportedUserClicked(report.reportedUserId)}
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemAdminReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReportDiffCallback : DiffUtil.ItemCallback<Report>() {
        override fun areItemsTheSame(oldItem: Report, newItem: Report) = oldItem.reportId == newItem.reportId
        override fun areContentsTheSame(oldItem: Report, newItem: Report) = oldItem == newItem
    }
}

// Helper extension function
fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar(Char::titlecase) }