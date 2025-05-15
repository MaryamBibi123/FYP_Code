package com.example.signuplogina.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.example.signuplogina.Bid
import com.example.signuplogina.Item
import com.example.signuplogina.R
import com.example.signuplogina.adapter.OfferedItemsViewPagerAdapter
import com.example.signuplogina.databinding.FragmentOfferedItemsDetailsBinding
import com.example.signuplogina.mvvm.OfferedItemsViewModel

class OfferedItemsDetailsFragment : Fragment() {

    private var _binding: FragmentOfferedItemsDetailsBinding? = null
    private val binding get() = _binding!!

    private val navArgs: OfferedItemsDetailsFragmentArgs by navArgs()
    private val viewModel: OfferedItemsViewModel by viewModels()

    private lateinit var viewPagerAdapter: OfferedItemsViewPagerAdapter
    private var currentBid: Bid? = null
    private var currentDisplayedOfferedItems: List<Item> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOfferedItemsDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupViewPager()
        setupButtons()
        observeViewModel()

        viewModel.loadBidAndItems(navArgs.bidId, navArgs.offeredItemIds.toList(), navArgs.requestedItemId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupViewPager() {
        viewPagerAdapter = OfferedItemsViewPagerAdapter()
        binding.viewPagerOfferedItems.adapter = viewPagerAdapter
        binding.dotsIndicatorOfferedItems.setViewPager2(binding.viewPagerOfferedItems)

        binding.viewPagerOfferedItems.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateUiForCurrentPage(position)
            }
        })
    }

    private fun setupButtons() {
        binding.btnPreviousItem.setOnClickListener {
            binding.viewPagerOfferedItems.currentItem = binding.viewPagerOfferedItems.currentItem - 1
        }
        binding.btnNextItem.setOnClickListener {
            binding.viewPagerOfferedItems.currentItem = binding.viewPagerOfferedItems.currentItem + 1
        }

        binding.btnViewFullItemDetails.setOnClickListener {
            if (currentDisplayedOfferedItems.isNotEmpty()) {
                val currentItemPosition = binding.viewPagerOfferedItems.currentItem
                if (currentItemPosition < currentDisplayedOfferedItems.size) {
                    val selectedItem = currentDisplayedOfferedItems[currentItemPosition]
                    navigateToFullItemDetails(selectedItem)
                }
            }
        }

//        binding.btnAcceptOffer.setOnClickListener {
//            currentBid?.let { bid ->
//                showConfirmationDialog(
//                    "Accept Offer",
//                    "Are you sure you want to accept this offer (includes ${bid.offeredItemIds.size} item(s))?"
//                ) {
//                    viewModel.acceptBid(bid)
//                }
//            }
//        }
//
//        binding.btnRejectOffer.setOnClickListener {
//            currentBid?.let { bid ->
//                showConfirmationDialog("Reject Offer", "Are you sure you want to reject this offer?") {
//                    viewModel.rejectBid(bid.bidId)
//                }
//            }
//        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarOfferedItems.visibility = if (isLoading) View.VISIBLE else View.GONE
            // Enable/disable buttons based on loading state
            val enableControls = !isLoading
            binding.btnAcceptOffer.isEnabled = enableControls
            binding.btnRejectOffer.isEnabled = enableControls
            binding.btnViewFullItemDetails.isEnabled = enableControls
            binding.btnNextItem.isEnabled = enableControls
            binding.btnPreviousItem.isEnabled = enableControls
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.consumeToastMessage()
            }
        }

        viewModel.bidDetails.observe(viewLifecycleOwner) { bid ->
            currentBid = bid
            updateToolbarAndHeader()
            bid?.let {
                updateActionButtonsVisibility(it)
                if (it.status != "pending" && it.status != "start") {
                    // Optionally disable controls or show status more prominently
                }
            }
        }

        viewModel.offeredItemsList.observe(viewLifecycleOwner) { items ->
            currentDisplayedOfferedItems = items
            viewPagerAdapter.submitList(items)
            if (items.isEmpty()) {
                binding.tvItemCounter.text = "No items in this offer."
                binding.btnViewFullItemDetails.visibility = View.GONE
                binding.dotsIndicatorOfferedItems.visibility = View.GONE
                binding.btnNextItem.visibility = View.GONE
                binding.btnPreviousItem.visibility = View.GONE
            } else {
                binding.btnViewFullItemDetails.visibility = View.VISIBLE
                binding.dotsIndicatorOfferedItems.visibility = if (items.size > 1) View.VISIBLE else View.GONE
                updateUiForCurrentPage(binding.viewPagerOfferedItems.currentItem) // Initial setup
            }
        }

        viewModel.bidUpdateStatus.observe(viewLifecycleOwner) { (success, message) ->
            Toast.makeText(context, message, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
            if (success) {
                findNavController().popBackStack() // Go back after successful action
            }
        }
    }
    private fun updateToolbarAndHeader() {
        val requestedItemName = viewModel.getRequestedItemDetails()?.details?.productName ?: "Item"
        val bidderName = currentBid?.bidderName?.takeIf { it.isNotBlank() }
            ?: currentBid?.bidderId?.let { "Bidder ${it.take(6)}..." }
            ?: "A bidder"

        binding.toolbar.title = "Offer for '$requestedItemName'"
        // You could add subtitle like: binding.toolbar.subtitle = "From: $bidderName"
    }


    private fun updateUiForCurrentPage(position: Int) {
        val totalItems = currentDisplayedOfferedItems.size
        if (totalItems > 0 && position < totalItems) {
            binding.tvItemCounter.text = "Item ${position + 1} of $totalItems"
        } else if (totalItems == 0) {
            binding.tvItemCounter.text = "No items offered"
        }

        binding.btnPreviousItem.visibility = if (position > 0 && totalItems > 1) View.VISIBLE else View.GONE
        binding.btnNextItem.visibility = if (position < totalItems - 1 && totalItems > 1) View.VISIBLE else View.GONE
        binding.btnViewFullItemDetails.isEnabled = totalItems > 0 // Enable if there's at least one item
    }


    private fun updateActionButtonsVisibility(bid: Bid) {
        val amITheReceiver = navArgs.isCurrentUserBidReceiver
        val viewingFromOtherProfile = navArgs.isFromOtherUserProfile // **** GET NEW FLAG ****

        // Bid can be managed if user is receiver AND bid is in a manageable state
        // AND they are NOT viewing it in the context of another user's profile list
        val canManage = amITheReceiver &&
                (bid.status == "pending" || bid.status == "start") &&
                !viewingFromOtherProfile // **** ADD THIS CONDITION ****

        Log.d("inUpdateActionButton", "updateActionButtons: amITheReceiver=$amITheReceiver, bidStatus=${bid.status}, viewingFromOtherProfile=$viewingFromOtherProfile, canManage=$canManage")

        binding.buttonsLayout.visibility = if (canManage) View.VISIBLE else View.GONE

        // Update toolbar subtitle logic
        if (amITheReceiver && !canManage) { // Still the receiver, but cannot manage (e.g. status changed or viewing from other profile)
            if (viewingFromOtherProfile) {
                binding.toolbar.subtitle = "Viewing offer on your item (via other user's profile)"
            } else {
                binding.toolbar.subtitle = "Offer Status: ${bid.status.capitalize()}"
            }
        } else if (amITheReceiver && canManage) {
            binding.toolbar.subtitle = null // Clear subtitle if manageable
        } else if (!amITheReceiver) {
            binding.toolbar.subtitle = "Viewing offer" // General viewing
        }
    }
    private fun navigateToFullItemDetails(item: Item) {
        val bundle = Bundle().apply {
            putParcelable("ITEM_DETAILS", item)
            putBoolean("IS_VIEWING_OFFER_COMPONENT", true)
        }
        try {
            findNavController().navigate(R.id.action_offeredItemsDetailsFragment_to_itemDetailsFragment, bundle)
        } catch (e: Exception) {
            Log.e("OfferedItemsDetails", "Navigation to ItemDetailsFragment failed", e)
            Toast.makeText(context, "Could not open item details.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirm") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPagerOfferedItems.adapter = null // Important for ViewPager2
        _binding = null
    }
}