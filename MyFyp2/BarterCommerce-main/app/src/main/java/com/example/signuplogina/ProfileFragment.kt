
package com.example.signuplogina

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.signuplogina.databinding.FragmentProfileBinding
import com.example.signuplogina.User
import com.example.signuplogina.mvvm.UsersRepo
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.coroutines.joinAll

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null && isAdded && view != null) {
                        loadUserProfile(user)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        binding.editProfileIcon.setOnClickListener {
            val intent = Intent(requireContext(), UpdateProfileActivity::class.java)
            startActivity(intent)
        }


        binding.BidsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_bidsFragment)
        }

        binding.barterBuzz.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_barterBuzzFragment)
        }

//        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab?) {
//                when (tab?.position) {
//                    0 -> {
//                        Toast.makeText(requireContext(), "Sell Products Tab Selected", Toast.LENGTH_SHORT).show()
//                    }
//                    1 -> {
//                        Toast.makeText(requireContext(), "Liked Items Tab Selected", Toast.LENGTH_SHORT).show()
//                        findNavController().navigate(R.id.action_profileFragment_to_filteredUsersFragment)
//                    }
//                }
//            }
//
//            override fun onTabUnselected(tab: TabLayout.Tab?) {}
//            override fun onTabReselected(tab: TabLayout.Tab?) {}
//        })
    }

    private fun loadUserProfile(user: User) {
        _binding?.let { binding ->
            if (!user.imageUrl.isNullOrEmpty()) {
                Picasso.get()
                    .load(user.imageUrl)
                    .transform(CropCircleTransformation())
                    .into(binding.profileImage)
            } else {
                binding.profileImage.setImageResource(R.drawable.ic_profile)
            }

            binding.userName.text = user.fullName?.ifEmpty { "Username not set" }

            val stats = user.ratings
            if (stats != null) {
                binding.userRatingBar.rating = stats.averageRating
                binding.ratingCount.text = "${stats.averageRating} / 5\nRating"
                binding.exchangeText.text = "${stats.totalExchanges}\nExchanges"
            } else {
                binding.userRatingBar.rating = 0f
                binding.ratingCount.text = "0 / 5"
                binding.exchangeText.text = "Exchanges: 0"
            }
        }
    }



    override fun onStart() {
        super.onStart()

        val uid = Utils.getUidLoggedIn()
        if (uid.isNullOrBlank()) {
            FirebaseAuth.getInstance().signOut()
            return
        }

        UsersRepo().getUserDetailsById(uid) { user ->
            if (user?.statusByAdmin == "blocked") {
                FirebaseAuth.getInstance().signOut()
//                    showBlockedDialog()
                AlertDialog.Builder(requireContext())
                    .setTitle("Access Denied")
                    .setMessage("Your account has been blocked by the admin.")
                    .setCancelable(false)
                    .setPositiveButton("OK") { _, _ ->

                        //remove all activies
                        activity?.finishAffinity() // close all activities
                    }
                    .show()
            }
        }}
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}










//package com.example.signuplogina
//
//import android.annotation.SuppressLint
//import android.content.Intent
//import android.os.Bundle
//import android.view.*
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.navigation.fragment.findNavController
//import com.example.signuplogina.databinding.FragmentProfileBinding
//import com.example.signuplogina.User
//import com.google.android.material.tabs.TabLayout
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.*
//import com.squareup.picasso.Picasso
//import jp.wasabeef.picasso.transformations.CropCircleTransformation
//
//class ProfileFragment : Fragment() {
//
//    private var _binding: FragmentProfileBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var database: DatabaseReference
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout using ViewBinding
//        _binding = FragmentProfileBinding.inflate(inflater, container, false)
//        setHasOptionsMenu(true)
//
//        return binding.root // Return the root of the ViewBinding to be used
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // Initialize Firebase database reference
////        database = FirebaseDatabase.getInstance().getReference("bids")
//
//        // Get current user UID
//        val uid = FirebaseAuth.getInstance().currentUser?.uid
//        if (uid != null) {
//            // Get user data from Firebase
//            val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
//            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val user = snapshot.getValue(User::class.java)
//                    if (user != null) {
//                        loadUserProfile(user)  // Load user profile data
//                    } else {
//                        Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
//                }
//            })
//        }
//
//        // Set up button click listeners
//        binding.editProfileButton.setOnClickListener {
//            val intent = Intent(requireContext(), UpdateProfileActivity::class.java)
//            startActivity(intent)
//        }
//
//        binding.BidsButton.setOnClickListener {
//            findNavController().navigate(R.id.action_profileFragment_to_bidsFragment)
//        }
//
//        binding.barterBuzz.setOnClickListener {
//            findNavController().navigate(R.id.action_profileFragment_to_barterBuzzFragment)
//        }
//
//        // Set up tab listener
//        // Assuming you have your binding object (e.g., binding) initialized correctly
//// in onCreateView or onViewCreated
//
//        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab?) {
//                when (tab?.position) {
//                    // Position 0 corresponds to the FIRST TabItem ("Sell Products")
//                    0 -> {
//                        Toast.makeText(requireContext(), "Sell Products Tab Selected", Toast.LENGTH_SHORT).show()
//                        // Add logic for the "Sell Products" tab here
//                        // Maybe navigate to a fragment showing user's products for sale
//                    }
//                    // Position 1 corresponds to the SECOND TabItem ("Liked Items")
//                    1 -> {
//                        Toast.makeText(requireContext(), "Liked Items Tab Selected", Toast.LENGTH_SHORT).show()
//                        // Navigate or update UI for Liked Items
//                        findNavController().navigate(R.id.action_profileFragment_to_filteredUsersFragment) // Or wherever Liked Items should go
//                    }
//                }
//            }
//
//            override fun onTabUnselected(tab: TabLayout.Tab?) {
//                // Optional: Handle tab unselection
//            }
//
//            override fun onTabReselected(tab: TabLayout.Tab?) {
//                // Optional: Handle tab reselection
//            }
//        })
//    }
//
//    // Method to load the user's profile data
//    private fun loadUserProfile(user: User) {
//        // Load profile image using Picasso
//        if (!user.imageUrl.isNullOrEmpty()) {
//            Picasso.get()
//                .load(user.imageUrl)// set the image by the user ,
////                .load(R.drawable.ic_profile)// to test
//
//                .transform(CropCircleTransformation())
//                .into(binding.profileImage)
//        } else {
//            binding.profileImage.setImageResource(R.drawable.ic_profile)
//        }
//
//        // Set user name
//        binding.userName.text = user.fullName?.ifEmpty { "Username not set" }
//
//        // Load user ratings and exchange stats
//        val stats = user.ratings
//        if (stats != null) {
//            binding.userRatingBar.rating = stats.averageRating
//            binding.ratingCount.text = "${stats.averageRating} / 5\nRating"
//            binding.exchangeText.text = "${stats.totalExchanges}\nExchanges"
//        } else {
//            binding.userRatingBar.rating = 0f
//            binding.ratingCount.text = "0 / 5"
//            binding.exchangeText.text = "Exchanges: 0"
//        }
//    }
//
//    // Method to load the count of bids (if required)
//    private fun loadBidsCount() {
//        database.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val bidCount = snapshot.childrenCount
//                // You can update the bid count in your UI here if necessary
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Toast.makeText(requireContext(), "Failed to load bids: ${error.message}", Toast.LENGTH_SHORT).show()
//            }
//        })
//    }
//
//    // Make sure to clean up the binding in onDestroyView
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}

//========================================

//package com.example.signuplogina
//
//import android.annotation.SuppressLint
//import android.content.Intent
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.ImageView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.navigation.fragment.findNavController
//import com.example.signuplogina.modal.UserRatingStats
//import com.google.android.material.tabs.TabLayout
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseUser
//import com.google.firebase.database.*
//import com.squareup.picasso.Picasso
//import jp.wasabeef.picasso.transformations.CropCircleTransformation
//
//class ProfileFragment : Fragment() {
//
//    private lateinit var profileImageView: ImageView
//    private lateinit var usernameTextView: TextView
//    private lateinit var database: DatabaseReference
//
//    private var currentUser: FirebaseUser? = null
//    private var userRatingStats: UserRatingStats? = null
//
//    @SuppressLint("MissingInflatedId")
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.fragment_profile, container, false)
//        setHasOptionsMenu(true)
//
//        // Initialize views
//        profileImageView = view.findViewById(R.id.profileImage)
//        usernameTextView = view.findViewById(R.id.userName)
//
//        // Get current user
//        currentUser = FirebaseAuth.getInstance().currentUser
//
//        // Load user profile
//        loadUserProfile()
//
//        // Edit Profile Button
//        val editProfileButton = view.findViewById<Button>(R.id.editProfileButton)
//        editProfileButton.setOnClickListener {
//            val intent = Intent(requireContext(), UpdateProfileActivity::class.java)
//            startActivity(intent)
//        }
//
//        // Bids Button
//        val bidsButton = view.findViewById<Button>(R.id.BidsButton)
//        bidsButton.setOnClickListener {
//            findNavController().navigate(R.id.action_profileFragment_to_bidsFragment)
//        }
//
//        // BarterBuzz Button
//        val barterBuzzButton = view.findViewById<Button>(R.id.barterBuzz)
//        barterBuzzButton.setOnClickListener {
//            findNavController().navigate(R.id.action_profileFragment_to_barterBuzzFragment)
//        }
//
//        // Tab Layout
//        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
//        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab?) {
//                when (tab?.position) {
//                    0 -> Toast.makeText(requireContext(), "Like Products Tab Selected", Toast.LENGTH_SHORT).show()
//                    1 -> findNavController().navigate(R.id.action_profileFragment_to_filteredUsersFragment)
//                }
//            }
//
//            override fun onTabUnselected(tab: TabLayout.Tab?) {}
//            override fun onTabReselected(tab: TabLayout.Tab?) {}
//        })
//
//        return view
//    }
//
//    private fun loadUserProfile() {
//        if (currentUser != null) {
//            currentUser?.photoUrl?.let { uri ->
//                Picasso.get()
//                    .load(uri)
//                    .transform(CropCircleTransformation())
//                    .into(profileImageView)
//            } ?: run {
//                profileImageView.setImageResource(R.drawable.ic_profile)
//            }
//
//            val displayName = currentUser?.displayName
//            usernameTextView.text = displayName ?: "Username not set"
//        } else {
//            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
//        }
//    }
//}
//
//
//================================================



//package com.example.signuplogina
//
//import android.annotation.SuppressLint
//import android.content.Intent
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Button
//import android.widget.ImageView
//import android.widget.RatingBar
//import android.widget.TextView
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.navigation.fragment.findNavController
//import com.example.signuplogina.fragments.FilteredUsersFragment
//import com.example.signuplogina.modal.UserRatingStats
//import com.google.android.material.tabs.TabLayout
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.*
//import com.squareup.picasso.Picasso
//import jp.wasabeef.picasso.transformations.CropCircleTransformation
//
//class ProfileFragment : Fragment() {
//
//    private lateinit var profileImageView: ImageView
//    private lateinit var usernameTextView: TextView
//    private lateinit var bidsCountTextView: TextView
//    private lateinit var database: DatabaseReference
//    val ratings: UserRatingStats? = null // <-- Added this line
//
//    @SuppressLint("MissingInflatedId")
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val view = inflater.inflate(R.layout.fragment_profile, container, false)
//        setHasOptionsMenu(true)
//
//        // Initialize views
//        profileImageView = view.findViewById(R.id.profileImage)
//        usernameTextView = view.findViewById(R.id.userName)
////        bidsCountTextView = view.findViewById(R.id.bidsCount)
//
//        // Initialize Firebase Database
//        database = FirebaseDatabase.getInstance().getReference("bids")
//
//        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
//        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid)
//
//        // Load user profile data
//        loadUserProfile()
//
//        // Load bids count
////        loadBidsCount()
//
//        // Edit Profile Button
//        val editProfileButton = view.findViewById<Button>(R.id.editProfileButton)
//        editProfileButton.setOnClickListener {
//            val intent = Intent(requireContext(), UpdateProfileActivity::class.java)
//            startActivity(intent)
//        }
//
//        // Add Friends Button
//        val BidsButton = view.findViewById<Button>(R.id.BidsButton)
//       BidsButton.setOnClickListener {
//                findNavController().navigate(R.id.action_profileFragment_to_bidsFragment)
//            }
//
//        // BarterBuzz Button
//        val BarterBuzz = view.findViewById<Button>(R.id.barterBuzz)
//        BarterBuzz.setOnClickListener {
//            findNavController().navigate(R.id.action_profileFragment_to_barterBuzzFragment)
//        }
//
//        // Tab Layout
//        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
//        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab?) {
//                when (tab?.position) {
////                    0 -> Toast.makeText(requireContext(), "Sell Products Tab Selected", Toast.LENGTH_SHORT).show()
//                    0 -> Toast.makeText(requireContext(), "Like Products Tab Selected", Toast.LENGTH_SHORT).show()
//
//                    1 -> findNavController().navigate(R.id.action_profileFragment_to_filteredUsersFragment)
//
//                }
//            }
//
//            override fun onTabUnselected(tab: TabLayout.Tab?) {}
//
//            override fun onTabReselected(tab: TabLayout.Tab?) {}
//        })
//
//        return view
//    }
//
//    private fun loadUserProfile(user: User) {
//
//        if (currentUser != null) {
//            currentUser.photoUrl?.let { uri ->
//                Picasso.get()
//                    .load(uri)
//                    .transform(CropCircleTransformation())
//                    .into(profileImageView)
//            } ?: run {
//                profileImageView.setImageResource(R.drawable.ic_profile)
//            }
//
//            val displayName = currentUser.displayName
//            usernameTextView.text = displayName ?: "Username not set"
//        } else {
//            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
//        }
//        val stats = currentUser.ratings
//        if (stats != null) {
//            view?.findViewById<RatingBar>(R.id.ratingBar)?.rating = stats.averageRating
//            view?.findViewById<TextView>(R.id.ratingValue)?.text = "${stats.averageRating} / 5"
//            view?.findViewById<TextView>(R.id.reviewCountText)?.text = "Reviews: ${stats.feedbackList.size}"
//            view?.findViewById<TextView>(R.id.exchangeText)?.text = "Exchanges: ${stats.totalExchanges}"
//
//
//
//    }
//
//    private fun loadBidsCount() {
//        database.addValueEventListener(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                // Count the number of child nodes under "bids"
//                val bidCount = snapshot.childrenCount
//                bidsCountTextView.text = "$bidCount\nBids"
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                Toast.makeText(requireContext(), "Failed to load bids: ${error.message}", Toast.LENGTH_SHORT).show()
//            }
//        })
//    }
//}
