
package com.example.signuplogina

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Handler
import android.os.Looper

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.signuplogina.databinding.ActivityMainBinding
import com.example.signuplogina.mvvm.UsersRepo
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authProfile: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val usersRepo = UsersRepo()
    private val TAG = "MainActivityBlockCheck"
    private var blockCheckInProgress = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // Initialize ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        authProfile = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val currentUser: FirebaseUser? = authProfile.currentUser

        // Display user's profile picture and name
        loadUserProfile(currentUser)


        checkIfAdmin(currentUser)

        // Set up the NavHostFragment and Navigation Controller
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Set up the BottomNavigationView with the NavController
        val bottomNavigationView: BottomNavigationView = binding.bottomNavigation
        NavigationUI.setupWithNavController(bottomNavigationView, navController)

        // Set up the DrawerLayout
        val drawerLayout = binding.drawerLayout
        val navigationView = binding.navigationView
        authProfile = FirebaseAuth.getInstance()
        val firebaseUser: FirebaseUser? = authProfile.currentUser
        // Open drawer when menu icon is clicked
        binding.menuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        // Handle navigation item clicks in the NavigationView
        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleDrawerNavigation(menuItem)
            drawerLayout.closeDrawers() // Close the drawer after selection
            true
        }

        // Set up the BottomNavigationView item selection
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
//

                R.id.nav_search -> {
                    navController.navigate(R.id.searchFragment)
                    true
                }
//                R.id.nav_users -> {
//                    navController.navigate(R.id.FilteredUsersFragment)
//                    true
//                }
                R.id.nav_messages -> {
                    navController.navigate(R.id.homeChatFragment)
                    true
                }
                R.id.nav_profile -> {
                    navController.navigate(R.id.profileFragment)
                    true
                }
                else -> false
            }
        }
//after

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment -> {
                    binding.fab.visibility = View.VISIBLE
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.toolbar.visibility = View.VISIBLE
                    binding.menuIcon.visibility = View.VISIBLE
                }
                R.id.searchFragment, R.id.profileFragment -> {
                    binding.fab.visibility = View.GONE
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.toolbar.visibility = View.VISIBLE
                    binding.menuIcon.visibility = View.VISIBLE
                }
                R.id.homeChatFragment -> {
                    binding.fab.visibility = View.GONE
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.toolbar.visibility = View.GONE
                    binding.menuIcon.visibility = View.GONE
                }

                R.id.homeExchangeFragment -> {
                    binding.fab.visibility = View.GONE
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.toolbar.visibility = View.GONE
                    binding.menuIcon.visibility = View.GONE
                }
                else -> {
                    binding.fab.visibility = View.GONE
                    binding.bottomNavigation.visibility = View.GONE
                    binding.toolbar.visibility = View.GONE
                    binding.menuIcon.visibility = View.GONE
                }
            }
        }


//        before
        // Set up FAB click listener
        binding.fab.setOnClickListener {
            binding.fab.visibility = View.GONE
//            navController.navigate(R.id.action_homeFragment_to_addItemFragment)
            navController.navigate(R.id.action_global_addItemFragment)
        }


}
    override fun onStart() {
        super.onStart()
        performUserBlockCheck()

//        val uid = Utils.getUidLoggedIn()
//        if (uid.isNullOrBlank()) {
//            FirebaseAuth.getInstance().signOut()
//            return
//        }
//
//        UsersRepo().getUserDetailsById(uid) { user ->
//            if (user?.statusByAdmin == "blocked") {
//                FirebaseAuth.getInstance().signOut()
////                    showBlockedDialog()
//                AlertDialog.Builder(this)
//                    .setTitle("Access Denied")
//                    .setMessage("Your account has been blocked by the admin.")
//                    .setCancelable(false)
//                    .setPositiveButton("OK") { _, _ ->
//
//                        //remove all activies
//                        finishAffinity() // close all activities
//                    }
//                    .show()
//            }
//        }
    }
    private fun performUserBlockCheck() {
        if (blockCheckInProgress) {
            Log.d(TAG, "Block check already in progress, skipping.")
            return
        }

        val currentUser = Utils.getUidLoggedIn()
        if (currentUser == null) {
            // No user logged in, or auth state not yet ready.
            // This might lead to LoginActivity or Splash logic handling it.
            // For this check, if no user, we assume they'll be redirected to login.
            Log.d(TAG, "No current user found for block check.")
            // Consider if you need to navigate to login here if not handled elsewhere
            // if (this !is LoginActivity && this !is SignUpActivity) { // Example condition
            //     startActivity(Intent(this, LoginActivity::class.java))
            //     finishAffinity()
            // }
            return
        }

        blockCheckInProgress = true
        Log.d(TAG, "Performing block check for user: ${currentUser}")

        usersRepo.getUserDetailsById(currentUser) { user ->
            if (user?.ratings == null && user?.statusByAdmin !in listOf("blocked", "permanently_blocked_override")) { // Check both new and old
                Log.w(TAG, "User ${currentUser} has no ratings node or explicit top-level block. Assuming allowed.")
                blockCheckInProgress = false
                // If user has no ratings node, they are not blocked by the new system.
                // Check old `statusByAdmin` just in case as a fallback.
                // If you are fully migrating, you might remove the `statusByAdmin` check later.
                return@getUserDetailsById
            }

            val userRatings = user?.ratings
            val legacyBlockStatus = user?.statusByAdmin // For backward compatibility or specific overrides

            // Priority 1: Permanent Block
            if (userRatings?.isPermanentlyBlocked == true || legacyBlockStatus == "permanently_blocked_override") {
                Log.w(TAG, "User ${currentUser} is PERMANENTLY BLOCKED.")
                showBlockedDialog(
                    "Account Permanently Blocked",
                    "Your account has been permanently blocked. Reason: ${userRatings?.blockReason ?: "Policy violation."}\nPlease contact support for assistance.",
                    isPermanent = true
                )
                // blockCheckInProgress will be reset in showBlockedDialog's dismiss/positive action
                return@getUserDetailsById
            }

            // Priority 2: Temporary Block (from new system)
            if (userRatings?.isTemporarilyBlocked == true && userRatings.blockExpiryTimestamp != 0L) {
                if (System.currentTimeMillis() >= userRatings.blockExpiryTimestamp) {
                    Log.i(TAG, "User ${currentUser} temporary block has expired. Attempting auto-unblock.")
                    // Temporary block expired, try to unblock
                    usersRepo.handleExpiredTemporaryBlock(currentUser) { unblockSuccess, userAllowedToProceed ->
                        if (unblockSuccess && userAllowedToProceed) {
                            Log.i(TAG, "User ${currentUser} successfully auto-unblocked.")
                            Toast.makeText(this, "Your temporary account restriction has been lifted.", Toast.LENGTH_LONG).show()
                            blockCheckInProgress = false
                            // User can proceed, MainActivity will continue as normal.
                            // You might want to refresh user data if it's cached in a ViewModel.
                        } else if (!userAllowedToProceed) {
                            Log.w(TAG, "User ${currentUser} still considered blocked after expiry check or unblock failed.")
                            // Potentially still blocked or unblock process failed. Show blocked dialog.
                            showBlockedDialog(
                                "Account Temporarily Restricted",
                                "Your account is still temporarily restricted. Reason: ${userRatings.blockReason ?: "Pending review."}\nRestriction lifts: ${formatTimestamp(userRatings.blockExpiryTimestamp)}",
                                isPermanent = false
                            )
                        } else {
                            Log.w(TAG, "User ${currentUser} auto-unblock action completed, but status indicates not proceeding yet. This is unusual.")
                            blockCheckInProgress = false; // Allow re-check if needed
                        }
                    }
                } else {
                    // Still temporarily blocked
                    Log.w(TAG, "User ${currentUser} is TEMPORARILY BLOCKED until ${formatTimestamp(userRatings.blockExpiryTimestamp)}.")
                    showBlockedDialog(
                        "Account Temporarily Restricted",
                        "Your account is temporarily restricted. Reason: ${userRatings.blockReason ?: "Pending review."}\nRestriction lifts on: ${formatTimestamp(userRatings.blockExpiryTimestamp)}",
                        isPermanent = false
                    )
                }
                return@getUserDetailsById
            }

            // Priority 3: Legacy/Simpler top-level block (if still in use)
            if (legacyBlockStatus == "blocked" && userRatings?.isPermanentlyBlocked != true && userRatings?.isTemporarilyBlocked != true) {
                Log.w(TAG, "User ${currentUser} is BLOCKED via legacy statusByAdmin field.")
                showBlockedDialog(
                    "Account Restricted",
                    "Your account access is currently restricted. Please contact support.",
                    isPermanent = false // Treat as non-permanent unless specified, admin should migrate to new system
                )
                return@getUserDetailsById
            }

            // If none of the above, user is not blocked.
            Log.d(TAG, "User ${currentUser} is not blocked. Proceeding normally.")
            blockCheckInProgress = false
        }
    }

    private fun showBlockedDialog(title: String, message: String, isPermanent: Boolean) {
        // Ensure dialog is shown on the main thread
        Handler(Looper.getMainLooper()).post {
            if (isFinishing || isDestroyed) {
                blockCheckInProgress = false
                return@post
            }
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
//                    auth.signOut()
                    FirebaseAuth.getInstance().signOut()

                    val intent = Intent(this, LoginActivity::class.java) // Or your initial login/splash screen
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finishAffinity() // Close all activities in the task
                }
                .setOnDismissListener {
                    blockCheckInProgress = false // Reset flag when dialog is dismissed
                }
                .show()
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return "N/A"
        return try {
            val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "Date Error"
        }
    }

    override fun onStop() {
        super.onStop()
        // Reset flag if activity is stopped, so check runs again if resumed.
        // However, onStart() is usually the better place.
        // blockCheckInProgress = false; // Consider implications
    }
    private fun checkIfAdmin(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val userId = currentUser.uid
            val userEmail = currentUser.email

            // Check if the email matches the predefined admin emails
            if (userEmail == "areebaeman524@gmail.com" || userEmail == "mbibi2949@gmail.com") {
                // Show dialog asking user to choose role
                showAdminUserChoiceDialog()
            } else {
                // Regular user check
                firestore.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val isAdmin = document.getBoolean("isAdmin") ?: false
                            if (isAdmin) {
                                startActivity(Intent(this, AdminActivity::class.java))
                                finish()
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            this,
                            "Error fetching user data: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    private fun showAdminUserChoiceDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Login Role")
            .setMessage("Do you want to log in as an Admin or a User?")
            .setPositiveButton("Admin") { _, _ ->
                // Redirect to Admin Dashboard
                startActivity(Intent(this, AdminActivity::class.java))
                finish()
            }
            .setNegativeButton("User") { _, _ ->
                // Proceed as a regular user (stay on the main screen)
                Toast.makeText(this, "Logged in as User", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }



    /**
     * Handles navigation actions for items in the NavigationView.
     */private fun handleDrawerNavigation(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.menu_refresh -> {
                // refresh activity
                startActivity(intent)
                finish()
                true
            }

            R.id.menu_update_profile -> {
                val intent = Intent(this@MainActivity, UpdateProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_update_email -> {
                val intent = Intent(this@MainActivity, UpdateEmailActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_settings -> {
                Toast.makeText(this, "menu settings", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_change_pass -> {
                val intent = Intent(this@MainActivity, ChangePasswordActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_delete_profile -> {
                val intent = Intent(this@MainActivity, DeleteProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_logout -> {
                authProfile.signOut()
                Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@MainActivity, SignUpLogin::class.java)
                // clear the stack to prevent the user from coming back to the UserProfileActivity
                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish() // close UserProfileActivity
                true
            }

            else -> super.onOptionsItemSelected(menuItem)
        }
    }
    private fun loadUserProfile(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            // Get current hour
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when {
                currentHour in 0..11 -> "Good Morning"
                currentHour in 12..17 -> "Good Afternoon"
                else -> "Good Evening"
            }

            // Set greeting message
            val displayName = currentUser.displayName ?: "User"
            val greetingMessage = "$greeting, $displayName"
            binding.usernameTextView.text = greetingMessage

            // Load profile picture using Glide
            currentUser.photoUrl?.let { uri ->
                Glide.with(this)
                    .load(uri)
                    .apply(RequestOptions.circleCropTransform()) // Makes the image circular
                    .placeholder(R.drawable.ic_profile) // Placeholder image
                    .into(binding.profileImageView) // Use binding to access the ImageView
            } ?: run {
                // Set a placeholder image if no profile picture is available
                binding.profileImageView.setImageResource(R.drawable.ic_profile)
            }

        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * Displays a toast message.
     */
    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        // Close the drawer if it is open, otherwise handle back press as usual
        val drawerLayout = binding.drawerLayout
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Ensure the drawer toggle works
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        // Make FAB visible again when returning to the HomeFragment
        binding.fab.visibility = View.VISIBLE
    }
}


//==========================================================


//package com.example.signuplogina
//
//import android.Manifest // FCM: Added for permission check
//import android.content.Intent
//import android.content.pm.PackageManager // FCM: Added for permission check
//import android.os.Build
//import android.os.Bundle
//import android.util.Log // FCM: Added for logging
//import android.view.MenuItem
//import android.view.View
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts // FCM: Added for permission launcher
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat // FCM: Added for permission check
//import androidx.core.view.GravityCompat
//import androidx.navigation.fragment.NavHostFragment
//import androidx.navigation.ui.NavigationUI
//import com.bumptech.glide.Glide
//import com.bumptech.glide.request.RequestOptions
//import com.example.signuplogina.databinding.ActivityMainBinding
//import com.example.signuplogina.mvvm.UsersRepo
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import com.google.firebase.FirebaseApp
//import com.google.firebase.appcheck.FirebaseAppCheck
//import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseUser
//import com.google.firebase.database.FirebaseDatabase // FCM: Added for storing token
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.messaging.FirebaseMessaging // FCM: Added for getting token
//import java.util.Calendar
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private lateinit var authProfile: FirebaseAuth
//    private lateinit var firestore: FirebaseFirestore
//
//    // FCM: Companion object for TAG
//    companion object {
//        private const val TAG = "MainActivity_FCM" // Renamed TAG to avoid conflict if you have one
//    }
//
//    // FCM: Launcher for the notification permission request
//    private val requestPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
//            if (isGranted) {
//                Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show()
//                // Optional: You might want to re-fetch/ensure token is sent if permission was just granted
//                // For example, by calling the token fetching logic again.
//            } else {
//                Toast.makeText(this, "Notification permission denied. You won't receive bid updates via push.", Toast.LENGTH_LONG).show()
//                // TODO: Inform user about the consequences of denying permission in a more user-friendly way.
//            }
//        }
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // --- Existing Initializations ---
//        FirebaseApp.initializeApp(this) // Note: FirebaseApp.initializeApp is usually called automatically by the Google Services plugin.
//        // If it's working for you, keep it. Otherwise, it might be redundant.
//        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
//            DebugAppCheckProviderFactory.getInstance()
//        )
//
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        authProfile = FirebaseAuth.getInstance()
//        firestore = FirebaseFirestore.getInstance()
//
//        val currentUser: FirebaseUser? = authProfile.currentUser
//
//        loadUserProfile(currentUser)
//        checkIfAdmin(currentUser) // This might navigate away, consider FCM logic placement
//
//        // --- FCM: Get and store FCM token if user is logged in ---
//        // Placed after authProfile is initialized and currentUser is available
//        currentUser?.uid?.let { userId ->
//            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
//                if (!task.isSuccessful) {
//                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
//                    return@addOnCompleteListener
//                }
//                val token = task.result
//                Log.d(TAG, "Current FCM Token: $token for user $userId")
//                sendRegistrationToServer(token, userId)
//            }
//        }
//
//        // --- FCM: Ask for notification permission (Android 13+) ---
//        askNotificationPermission()
//
//        // --- FCM: Handle intent if app was opened from a notification ---
//        // This needs to be called in onCreate as well, in case the app was launched from a killed state by a notification tap.
//        handleNotificationIntent(intent)
//
//
//        // --- Existing UI Setup and Navigation ---
//        val navHostFragment = supportFragmentManager
//            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//        val navController = navHostFragment.navController
//
//        val bottomNavigationView: BottomNavigationView = binding.bottomNavigation
//        NavigationUI.setupWithNavController(bottomNavigationView, navController)
//
//        val drawerLayout = binding.drawerLayout
//        val navigationView = binding.navigationView
//        // authProfile already initialized
//        // val firebaseUser: FirebaseUser? = authProfile.currentUser // Redundant, currentUser already fetched
//
//        binding.menuIcon.setOnClickListener {
//            drawerLayout.openDrawer(GravityCompat.END)
//        }
//
//        navigationView.setNavigationItemSelectedListener { menuItem ->
//            handleDrawerNavigation(menuItem)
//            drawerLayout.closeDrawers()
//            true
//        }
//
//        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.nav_home -> {
//                    navController.navigate(R.id.homeFragment)
//                    true
//                }
//                R.id.nav_search -> {
//                    navController.navigate(R.id.searchFragment)
//                    true
//                }
//                R.id.nav_messages -> {
//                    navController.navigate(R.id.homeChatFragment)
//                    true
//                }
//                R.id.nav_profile -> {
//                    navController.navigate(R.id.profileFragment)
//                    true
//                }
//                else -> false
//            }
//        }
//
//        navController.addOnDestinationChangedListener { _, destination, _ ->
//            when (destination.id) {
//                R.id.homeFragment -> {
//                    binding.fab.visibility = View.VISIBLE
//                    binding.bottomNavigation.visibility = View.VISIBLE
//                    binding.toolbar.visibility = View.VISIBLE
//                    binding.menuIcon.visibility = View.VISIBLE
//                }
//                R.id.searchFragment, R.id.profileFragment -> {
//                    binding.fab.visibility = View.GONE
//                    binding.bottomNavigation.visibility = View.VISIBLE
//                    binding.toolbar.visibility = View.VISIBLE
//                    binding.menuIcon.visibility = View.VISIBLE
//                }
//                R.id.homeChatFragment, R.id.homeExchangeFragment -> { // Combined these as they have same visibility
//                    binding.fab.visibility = View.GONE
//                    binding.bottomNavigation.visibility = View.VISIBLE
//                    binding.toolbar.visibility = View.GONE
//                    binding.menuIcon.visibility = View.GONE
//                }
//                else -> {
//                    binding.fab.visibility = View.GONE
//                    binding.bottomNavigation.visibility = View.GONE
//                    binding.toolbar.visibility = View.GONE
//                    binding.menuIcon.visibility = View.GONE
//                }
//            }
//        }
//
//        binding.fab.setOnClickListener {
//            binding.fab.visibility = View.GONE
//            navController.navigate(R.id.action_global_addItemFragment)
//        }
//    }
//
//    // FCM: Handle intent if activity is already running and receives a new intent
//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        handleNotificationIntent(intent)
//    }
//
//    // FCM: Function to handle data from notification intent
//    private fun handleNotificationIntent(intent: Intent?) {
//        intent?.let {
//            if (it.getBooleanExtra("open_bid_details", false)) {
//                val productId = it.getStringExtra("productId")
//                val bidId = it.getStringExtra("bidId") // Assuming Cloud Function sends this
//                if (productId != null) {
//                    Log.d(TAG, "Notification tapped: Opening details for product ID: $productId, Bid ID: $bidId")
//                    // TODO: Navigate to your bid details screen for this productId/bidId
//                    // Example:
//                    // val action = HomeFragmentDirections.actionHomeFragmentToBidDetailsFragment(productId, bidId)
//                    // findNavController(R.id.nav_host_fragment).navigate(action)
//                    Toast.makeText(this, "FCM: Open details for product: $productId", Toast.LENGTH_LONG).show()
//                }
//            }
//            // It's good practice to remove extras after processing to prevent re-processing on config change if activity is re-created
//            // However, be careful if you need these extras for other logic before activity recreation.
//            // For now, let's keep it simple. If you notice issues with re-processing, you might need to clear them:
//            // it.removeExtra("open_bid_details")
//            // it.removeExtra("productId")
//            // it.removeExtra("bidId")
//        }
//    }
//
//    // FCM: Function to send FCM token to Firebase Realtime Database
//    private fun sendRegistrationToServer(token: String?, userId: String) {
//        // Ensure userId is not empty and token is not null or empty
//        if (userId.isNotEmpty() && !token.isNullOrEmpty()) {
//            FirebaseDatabase.getInstance().getReference("Users") // Using Realtime Database as per previous discussion
//                .child(userId)
//                .child("fcmToken")
//                .setValue(token)
//                .addOnSuccessListener { Log.d(TAG, "FCM Token explicitly updated for user: $userId") }
//                .addOnFailureListener { e -> Log.e(TAG, "Failed to explicitly update FCM Token for $userId", e) }
//        } else {
//            Log.w(TAG, "Cannot send registration to server: userId or token is invalid. UserID: $userId, Token: $token")
//        }
//    }
//
//    // FCM: Function to ask for notification permission on Android 13+
//    private fun askNotificationPermission() {
//        // This is only necessary for API level >= 33 (TIRAMISU)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            when {
//                ContextCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.POST_NOTIFICATIONS
//                ) == PackageManager.PERMISSION_GRANTED -> {
//                    // Permission is already granted
//                    Log.d(TAG, "Notification permission already granted.")
//                }
//                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
//                    // Display an educational UI to the user explaining why the permission is needed.
//                    // After the user interacts with the UI (e.g., clicks "OK"), then launch the permission request.
//                    Log.d(TAG, "Showing rationale for notification permission.")
//                    // Example:
//                    AlertDialog.Builder(this)
//                        .setTitle("Notification Permission Needed")
//                        .setMessage("This app needs permission to send you notifications about new bids and other important updates.")
//                        .setPositiveButton("OK") { _, _ ->
//                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
//                        }
//                        .setNegativeButton("No thanks") { dialog, _ ->
//                            dialog.dismiss()
//                            // Optionally, inform the user that notifications will be disabled.
//                            Toast.makeText(this, "Notifications will be disabled.", Toast.LENGTH_SHORT).show()
//                        }
//                        .show()
//                }
//                else -> {
//                    // Directly ask for the permission if it has not been requested before,
//                    // or if the user previously denied without "Don't ask again".
//                    Log.d(TAG, "Requesting notification permission.")
//                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
//                }
//            }
//        }
//    }
//
//
//    // --- Existing Functions ---
//    override fun onStart() {
//        super.onStart()
//        val uid = Utils.getUidLoggedIn()
//        if (uid.isNullOrBlank()) {
//            FirebaseAuth.getInstance().signOut() // Consider if this should navigate to login
//            return
//        }
//
//        UsersRepo().getUserDetailsById(uid) { user ->
//            if (user?.statusByAdmin == "blocked") {
//                FirebaseAuth.getInstance().signOut()
//                AlertDialog.Builder(this)
//                    .setTitle("Access Denied")
//                    .setMessage("Your account has been blocked by the admin.")
//                    .setCancelable(false)
//                    .setPositiveButton("OK") { _, _ ->
//                        finishAffinity()
//                    }
//                    .show()
//            }
//        }
//    }
//
//    private fun checkIfAdmin(currentUser: FirebaseUser?) {
//        if (currentUser != null) {
//            val userId = currentUser.uid
//            val userEmail = currentUser.email
//
//            if (userEmail == "areebaeman524@gmail.com" || userEmail == "mbibi2949@gmail.com") {
//                showAdminUserChoiceDialog()
//            } else {
//                firestore.collection("users").document(userId).get()
//                    .addOnSuccessListener { document ->
//                        if (document.exists()) {
//                            val isAdmin = document.getBoolean("isAdmin") ?: false
//                            if (isAdmin) {
//                                // Ensure this intent is correct and AdminActivity handles its own lifecycle
//                                val adminIntent = Intent(this, AdminActivity::class.java)
//                                adminIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack for admin
//                                startActivity(adminIntent)
//                                finish() // Finish MainActivity for admin
//                            }
//                            // If not admin, normal flow continues (FCM token, permissions etc. in onCreate will run)
//                        }
//                    }
//                    .addOnFailureListener { exception ->
//                        Toast.makeText(this, "Error fetching user data: ${exception.message}", Toast.LENGTH_SHORT).show()
//                    }
//            }
//        } else {
//            // If current user is null, might want to navigate to login screen
//            // For now, FCM token and permission logic in onCreate will also check for currentUser.
//        }
//    }
//
//    private fun showAdminUserChoiceDialog() {
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Select Login Role")
//            .setMessage("Do you want to log in as an Admin or a User?")
//            .setPositiveButton("Admin") { _, _ ->
//                val adminIntent = Intent(this, AdminActivity::class.java)
//                adminIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                startActivity(adminIntent)
//                finish()
//            }
//            .setNegativeButton("User") { _, _ ->
//                Toast.makeText(this, "Logged in as User", Toast.LENGTH_SHORT).show()
//                // User continues in MainActivity. FCM token and permission logic in onCreate will apply.
//            }
//            .setCancelable(false)
//            .show()
//    }
//
//    private fun handleDrawerNavigation(menuItem: MenuItem) { // Removed "true" return type; handled by listener
//        when (menuItem.itemId) {
//            R.id.menu_refresh -> {
//                startActivity(intent)
//                finish()
//            }
//            R.id.menu_update_profile -> {
//                startActivity(Intent(this@MainActivity, UpdateProfileActivity::class.java))
//            }
//            R.id.menu_update_email -> {
//                startActivity(Intent(this@MainActivity, UpdateEmailActivity::class.java))
//            }
//            R.id.menu_settings -> {
//                Toast.makeText(this, "menu settings", Toast.LENGTH_SHORT).show()
//            }
//            R.id.menu_change_pass -> {
//                startActivity(Intent(this@MainActivity, ChangePasswordActivity::class.java))
//            }
//            R.id.menu_delete_profile -> {
//                startActivity(Intent(this@MainActivity, DeleteProfileActivity::class.java))
//            }
//            R.id.menu_logout -> {
//                authProfile.signOut()
//                Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
//                val intent = Intent(this@MainActivity, SignUpLogin::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
//                startActivity(intent)
//                finish()
//            }
//            // Removed else case as NavigationItemSelectedListener expects a boolean return from the lambda itself
//        }
//    }
//
//    private fun loadUserProfile(currentUser: FirebaseUser?) {
//        if (currentUser != null) {
//            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
//            val greeting = when {
//                currentHour in 0..11 -> "Good Morning"
//                currentHour in 12..17 -> "Good Afternoon"
//                else -> "Good Evening"
//            }
//            val displayName = currentUser.displayName?.takeIf { it.isNotBlank() } ?: "User" // More robust display name
//            val greetingMessage = "$greeting, $displayName"
//            binding.usernameTextView.text = greetingMessage
//
//            currentUser.photoUrl?.let { uri ->
//                Glide.with(this)
//                    .load(uri)
//                    .apply(RequestOptions.circleCropTransform())
//                    .placeholder(R.drawable.ic_profile)
//                    .error(R.drawable.ic_profile) // Add error placeholder
//                    .into(binding.profileImageView)
//            } ?: run {
//                binding.profileImageView.setImageResource(R.drawable.ic_profile)
//            }
//        } else {
//            // This case should ideally not happen if MainActivity is protected by login
//            // binding.usernameTextView.text = "Welcome" // Default message
//            // binding.profileImageView.setImageResource(R.drawable.ic_profile)
//            Toast.makeText(this, "User not authenticated to load profile", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    // Removed showMessage as Toast is used directly
//
//    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
//    override fun onBackPressed() {
//        val drawerLayout = binding.drawerLayout
//        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
//            drawerLayout.closeDrawer(GravityCompat.END)
//        } else {
//            // Check if NavController can pop back stack
//            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//            if (!navHostFragment.navController.popBackStack()) {
//                // If no back stack entry to pop, then call super
//                super.onBackPressed()
//            }
//        }
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        // This is typically for ActionBar/Toolbar menu items, not for NavigationView.
//        // Your NavigationView clicks are handled by `navigationView.setNavigationItemSelectedListener`
//        // If menuIcon is part of a Toolbar and meant to open the drawer, that's handled by its click listener.
//        return super.onOptionsItemSelected(item)
//    }
//
//    override fun onResume() {
//        super.onResume()
//        // Consider the current fragment to decide FAB visibility.
//        // This might be better handled by the addOnDestinationChangedListener
//        // For now, keeping your logic:
//        // val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//        // if (navHostFragment.navController.currentDestination?.id == R.id.homeFragment) {
//            binding.fab.visibility = View.VISIBLE
//        // }
//        // The addOnDestinationChangedListener should correctly manage FAB visibility.
//        // Re-evaluating this: if navigating away and coming back, the listener might not fire immediately.
//        // However, your current listener logic is quite comprehensive.
//        // Let's rely on the addOnDestinationChangedListener for FAB visibility.
//        // If you navigate away from homeFragment and then press back to homeFragment,
//        // the addOnDestinationChangedListener WILL fire and set fab.visibility = View.VISIBLE.
//        // So, the line below might be redundant or could cause a flicker if the listener also runs.
//        // binding.fab.visibility = View.VISIBLE // This might be overriding the destination listener logic.
//    }
//}
//
//
//
//
//
//
//
//
//

