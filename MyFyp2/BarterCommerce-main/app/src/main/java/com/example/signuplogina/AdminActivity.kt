//



package com.example.signuplogina

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
//import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions // Import NavOptions
import androidx.navigation.ui.NavigationUI // Keep this if needed for other items later
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // Correct way to get NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_admin_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        // --- Explicitly handle main navigation items ---
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_users -> {
                    // Navigate to ShowUsersFragment, potentially popping the stack
                    // Make sure R.id.showUsersFragment is the correct ID in your nav graph
                    // Pop up to the start destination to avoid building a large back stack
                    // like Users -> Profile -> Users -> Profile ...
                    val navOptions = navOptions {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = false // Keep the start destination (Users list)
                        }
                        launchSingleTop = true // Avoid multiple copies of the Users list
                    }
                    // *** IMPORTANT: Use the actual ID of your ShowUsersFragment ***
                    // *** from admin_nav_graph.xml if it's different from R.id.nav_users ***
                    navController.navigate(R.id.showUsersFragment, null, navOptions)
                    true // Event handled
                }
                R.id.nav_admin_items -> {
                    val navOptions = navOptions {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                    }
                    // Navigate to the HOST fragment for items
                    navController.navigate(R.id.adminItemHostFragment, null, navOptions)
                    true
                }
                R.id.nav_admin_reports -> {
                    val navOptions = navOptions {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                    }
                    // Navigate to the HOST fragment for items
                    navController.navigate(R.id.adminPendingReportsFragment, null, navOptions)
                    true
                }
                R.id.nav_logout -> {
                    // Handle logout manually
                    FirebaseAuth.getInstance().signOut()
                    // Optional: Redirect to Login Activity
                    // Intent(this, LoginActivity::class.java).also {
                    //     it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    //     startActivity(it)
                    // }
                    finish() // Close AdminActivity
                    true // Event handled
                }
                // Optional: Keep this for potential future items that *do* map directly
                // from the current location, although often explicit handling is clearer.
                else -> {
                    // Try default NavigationUI behavior ONLY if not handled above
                    NavigationUI.onNavDestinationSelected(item, navController)
                    // Note: This might still fail if the graph doesn't support the navigation
                    // from the current fragment. Explicit handling (like above) is safer.
                }
            }
        }

        // Optional: Set the initial selection listener (less critical if handling everything manually)
        // You might want to connect it initially IF you want the selected item to update
        // visually when navigating through other means (e.g., button clicks within fragments)
        // but the setOnItemSelectedListener above will override the click behavior.
        // NavigationUI.setupWithNavController(bottomNav, navController)
    }

    // Optional: Handle Up button if you have a Toolbar managed by NavigationUI
//     override fun onSupportNavigateUp(): Boolean {
//         val navController = findNavController(R.id.nav_host_admin_fragment)
//         return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
//     }
}







// package com.example.signuplogina
//
//import android.content.Intent // Import Intent if navigating to Login on logout
//import android.os.Bundle
//import androidx.appcompat.app.AppCompatActivity
//import androidx.navigation.NavController // Import NavController
//import androidx.navigation.fragment.NavHostFragment // Import NavHostFragment
//import androidx.navigation.ui.NavigationUI
//import com.google.android.material.bottomnavigation.BottomNavigationView
//import com.google.firebase.auth.FirebaseAuth
//
//class AdminActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_admin)
//
//        // --- Correct way to get NavController ---
//        // 1. Find the NavHostFragment using the FragmentManager
//        val navHostFragment = supportFragmentManager
//            .findFragmentById(R.id.nav_host_admin_fragment) as NavHostFragment
//        // 2. Get the NavController from the NavHostFragment
//        val navController = navHostFragment.navController
//        // --- End of correction ---
//
//        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
//
//        // --- Use NavigationUI for standard setup ---
//        // This connects the BottomNavigationView with the NavController.
//        // It handles navigation for items whose IDs match destination IDs in the graph.
//        NavigationUI.setupWithNavController(bottomNav, navController)
//        // --- End of standard setup ---
//
//
//        // --- Add custom listener for specific items AFTER standard setup ---
//        // setupWithNavController sets its own listener. To add custom logic (like logout),
//        // we need to set our own listener *after* calling setupWithNavController.
//        // This listener will override the one set by NavigationUI.
//        bottomNav.setOnItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.nav_logout -> {
//                    // Handle logout manually
//                    FirebaseAuth.getInstance().signOut()
//                    // Optional: Redirect to Login Activity after logout
//                    // Intent(this, LoginActivity::class.java).also {
//                    //     it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                    //     startActivity(it)
//                    // }
//                    finish() // Close AdminActivity
//                    true // Important: return true to indicate event was handled
//                }
//                // For all other menu items, use the default NavigationUI behavior
//                // This will navigate to the destination with the matching ID.
//                else -> {
//                    NavigationUI.onNavDestinationSelected(item, navController)
//                    // Note: onNavDestinationSelected returns true if navigation occurred,
//                    // false otherwise. We need to return this boolean.
//                }
//            }
//        }
//    }
//}