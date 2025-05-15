package com.example.signuplogina.modal

enum class FeedbackTags(
    val label: String,
    val isPositiveContext: Boolean,
    val isReportContext: Boolean,
    val isAdminBlockReason: Boolean // New flag
) {
    // Positive Feedback Tags
    BEST_SELLER("Best Seller", true, false, false),
    TIME_PUNCTUAL("Time Punctual", true, false, false),
    FRIENDLY("Friendly Communication", true, false, false),
    SMOOTH_EXCHANGE("Smooth Exchange", true, false, false),
    ACCURATE_ITEM_DESCRIPTION_POSITIVE("Accurate Item Description", true, false, false),

    // User Report Reason Tags
    ITEM_NOT_AS_DESCRIBED("Item Not as Described", false, true, true), // Can also be an admin block reason
    ITEM_FAULTY("Item Faulty/Damaged", false, true, true),          // Can also be an admin block reason
    SCAM_ATTEMPT("Suspected Scam", false, true, true),               // Can also be an admin block reason
    POOR_COMMUNICATION_NEGATIVE("Poor Communication", false, true, false), // User might report, admin might not block for this alone
    LATE_OR_NO_SHOW("Late or No-Show", false, true, true),
    HARASSMENT_OR_ABUSE("Harassment or Abuse", false, true, true),  // Strong admin block reason
    OTHER_REPORT_REASON("Other (User Specified)", false, true, false),

    // Admin Specific Block Reasons (Can overlap with user report reasons)
    POLICY_VIOLATION("Policy Violation", false, false, true),
    MULTIPLE_WARNINGS("Multiple Warnings Ignored", false, false, true),
    FRAUDULENT_ACTIVITY("Fraudulent Activity", false, false, true),
    OTHER_ADMIN_REASON("Other (Admin Specified)", false, false, true),


    NOT_RECOMMENDED("Not Recommended", false, false, false);

    companion object {
        val allTags = values().toList()
        fun fromLabel(label: String): FeedbackTags? = allTags.find { it.label == label }

        fun getPositiveFeedbackTags(): List<FeedbackTags> = allTags.filter { it.isPositiveContext }
        fun getReportReasonTags(): List<FeedbackTags> = allTags.filter { it.isReportContext }
        fun getAdminBlockReasonTags(): List<FeedbackTags> = allTags.filter { it.isAdminBlockReason }
    }
}