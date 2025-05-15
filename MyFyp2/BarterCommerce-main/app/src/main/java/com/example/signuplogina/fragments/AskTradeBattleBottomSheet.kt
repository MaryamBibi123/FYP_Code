import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.signuplogina.Item
import com.example.signuplogina.Utils
import com.example.signuplogina.adapter.SelectableBidsAdapter
import com.example.signuplogina.adapter.SelectableItemsAdapter
import com.example.signuplogina.databinding.FragmentAskTradeBattleBinding
import com.example.signuplogina.mvvm.BidModel
import com.example.signuplogina.mvvm.PollViewModel
import com.example.signuplogina.mvvm.Ratings
import com.example.signuplogina.mvvm.TradeBattleModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AskTradeBattleBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentAskTradeBattleBinding? = null
    private val binding get() = _binding!!

    private lateinit var itemsAdapter: SelectableItemsAdapter
    private lateinit var bidsAdapter: SelectableBidsAdapter
    private val database by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var tradeBattleViewModel: PollViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAskTradeBattleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tradeBattleViewModel = ViewModelProvider(requireActivity())[PollViewModel::class.java]

        setupRecyclerViews()

        val userId = Utils.getUidLoggedIn()
        tradeBattleViewModel.loadItemsWithMultipleBids(userId)

        observeViewModel()

        binding.btnSaveBattle.setOnClickListener {
            val selectedItem = itemsAdapter.getSelectedItems().firstOrNull()
            val selectedBids = bidsAdapter.getSelectedItems()

            // Get the selected bidIds along with the selected items
            val selectedBidIds = selectedBids.map { it.second } // Extract the bidIds from the Pair

            if (selectedItem == null) {
                Toast.makeText(requireContext(), "Please select an item first", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (selectedBids.size != 2) {
                Toast.makeText(
                    requireContext(),
                    "Please select exactly two bids",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            uploadTradeBattle(selectedItem, selectedBidIds)
        }

//        binding.btnSaveBattle.setOnClickListener {
//            val selectedItem = itemsAdapter.getSelectedItems().firstOrNull()
//            val selectedBids = bidsAdapter.getSelectedItems()
//
//            if (selectedItem == null) {
//                Toast.makeText(requireContext(), "Please select an item first", Toast.LENGTH_SHORT)
//                    .show()
//                return@setOnClickListener
//            }
//
//            if (selectedBids.size != 2) {
//                Toast.makeText(
//                    requireContext(),
//                    "Please select exactly two bids",
//                    Toast.LENGTH_SHORT
//                ).show()
//                return@setOnClickListener
//            }
//
//            uploadTradeBattle(selectedItem)
//        }
    }

    private fun setupRecyclerViews() {
        Log.d("recyclerView of askitem", "I am inthe recyclerset up view")
        itemsAdapter = SelectableItemsAdapter { selectedItem ->
            onItemSelected(selectedItem)
        }
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = itemsAdapter

        bidsAdapter = SelectableBidsAdapter { selectedItems ->
            // Handle when selection changes
            if (selectedItems.size == 2) {
                binding.btnSaveBattle.isEnabled = true
            } else {
                binding.btnSaveBattle.isEnabled = false
            }
        }
        binding.rvBids.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBids.adapter = bidsAdapter
    }







    private fun uploadTradeBattle(selectedItem: Item, selectedBidIds: List<String>) {
        val selectedOfferedItems = bidsAdapter.getSelectedItems()

        if (selectedOfferedItems.size != 2) return

        val battleId = database.child("Polls/TradeBattle").push().key ?: return
        var createdAt = System.currentTimeMillis()
         val createConverted = Utils.convertToPakistanTime(createdAt)// Use it to show the date formatted ,
//        createdAt = convertPakistanTimeToMillis(formated)

        val endsAt = createdAt + (24 * 60 * 60 * 1000)

        val bidsMap = selectedOfferedItems.associate { item ->
            item.first.id to BidModel(
                offeredId = item.first.id,
                userId = item.first.userId,
                bidId = item.second,
                offer = item.first.details.productName,
                imageUrl = item.first.details.imageUrls.firstOrNull() ?: "",
                ratings = Ratings(),
                totalPoints = 0
            )
        }

        val tradeBattle = TradeBattleModel(
            battleId = battleId,
            itemId = selectedItem.id,
            listerUid = Utils.getUidLoggedIn(),
            status = "active",
            createdAt = createdAt,
            endsAt = endsAt,
            winningBidId = null,
            chatRoomId = null,
            bids = bidsMap
        )

        database.child("Polls/TradeBattle")
            .child(battleId)
            .setValue(tradeBattle)
            .addOnSuccessListener {
                dismiss()
            }
            .addOnFailureListener {
                // Handle failure
            }
    }

//        private fun uploadTradeBattle(selectedItem: Item) {
//            val selectedOfferedItems = bidsAdapter.getSelectedItems()
//
//            if (selectedOfferedItems.size != 2) return
//
//            val battleId = database.child("Polls/TradeBattle").push().key ?: return
//            val createdAt = System.currentTimeMillis()
//            val endsAt = createdAt + (24 * 60 * 60 * 1000)
//
//            val bidsMap = selectedOfferedItems.associate { item ->
//                item.id to BidModel(
//                    offeredId =item.id,
//                    userId = item.userId,
//                    offer = item.details.productName,
//                    imageUrl = item.details.imageUrls.firstOrNull() ?: "",
//                    ratings = Ratings(),
//                    totalPoints = 0
//                )
//            }
//
//            val tradeBattle = TradeBattleModel(
//                battleId=battleId,
//                itemId = selectedItem.id,
//                listerUid = Utils.getUidLoggedIn(),
//                status = "active",
//                createdAt = createdAt,
//                endsAt = endsAt,
//                winningBidId = null,
//                chatRoomId = null,
//                bids = bidsMap
//            )
//
//            database.child("Polls/TradeBattle")
//                .child(battleId)
//                .setValue(tradeBattle)
//                .addOnSuccessListener {
//
//
//                    dismiss()
//                }
//                .addOnFailureListener {
//                    // Handle error
//                }
//        }


    private fun observeViewModel() {
        tradeBattleViewModel.userItems.observe(viewLifecycleOwner) { filteredItems ->
            itemsAdapter.submitList(filteredItems)
        }

        tradeBattleViewModel.bids.observe(viewLifecycleOwner) { bidsList ->
            updateBidsAndOfferedItems()
        }

        tradeBattleViewModel.offeredItems.observe(viewLifecycleOwner) { offeredItems ->
            updateBidsAndOfferedItems()
        }
    }

    private fun onItemSelected(item: Item) {
        binding.tvSelectBids.visibility = View.VISIBLE
        binding.rvBids.visibility = View.VISIBLE
//        tradeBattleViewModel.loadBidsWithOfferedItems(item.id)
    }

    private fun updateBidsAndOfferedItems() {
        val bids = tradeBattleViewModel.bids.value.orEmpty()
        val offeredItems = tradeBattleViewModel.offeredItems.value.orEmpty()

        if (bids.isNotEmpty() && offeredItems.isNotEmpty() && bids.size == offeredItems.size) {
            bidsAdapter.submitLists(bids, offeredItems)
        } else {
            Log.d("AskTradeBattle", "Waiting for both bids and offered items to load properly.")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
