package com.example.Dendrometry.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.Dendrometry.R
import com.example.Dendrometry.databinding.ActivityMainBinding
import com.example.Dendrometry.dbmshelpers.UserDatabaseHelper
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    lateinit var binding: ActivityMainBinding
    private lateinit var databaseHelper: UserDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        databaseHelper = UserDatabaseHelper(this)
        // Adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Hide system UI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
        }

        // Set up navigation drawer
        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolBar)
        setSupportActionBar(toolbar)

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.open_nav, R.string.close_nav
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        // Access the SharedPreferences where the data was saved
        val sharedPreferences = getSharedPreferences("userSession", MODE_PRIVATE)
        // Retrieve the stored values
        val loggedInUsername = sharedPreferences.getString("loggedInUsername", null)
        val loggedInName = sharedPreferences.getString("loggedInName", null)
        val loggedStatus = sharedPreferences.getString("loggedStatus", null)

        // Initially display HomeFragment and set the checked item in the navigation view
        if (savedInstanceState == null) {
            // Check if the user has already seen the tutorial
            if (loggedStatus == "New") {
                // User has not seen the tutorial, show the tutorial fragment
                if (loggedInUsername != null) {
                databaseHelper.updateUserStatus(loggedInUsername, "Old")
                }
                replaceFragment(TutorialFragment())
            } else {
                // User has seen the tutorial, show the home fragment or any default fragment
                replaceFragment(HomeFragment())
                navigationView.setCheckedItem(R.id.nav_home)
            }

        }

        // Check if the values are retrieved successfully
        if (loggedInUsername != null && loggedInName != null) {
            // You can now use the logged-in username and name
            Toast.makeText(this, "Logged in as: $loggedInName ($loggedInUsername) status: $loggedStatus", Toast.LENGTH_SHORT).show()
        } else {
            // Handle the case where no user is logged in (if needed)
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
        }

    }//END OF ON CREATE

    //Hide system navigation and status bar
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

//    override fun onBackPressed() {
//        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
//            drawerLayout.closeDrawer(GravityCompat.START)
//        } else {
//            super.onBackPressed()
//        }
//    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                replaceFragment(HomeFragment())
            }
            R.id.nav_history -> {
                replaceFragment(HistoryFragment())
            }
            R.id.nav_guide -> {
                replaceFragment(GuideFragment())
            }
            R.id.nav_tutorial -> {
                replaceFragment(TutorialFragment())
            }
            R.id.nav_logout -> {
                logout()
            }
            // Add more cases for other menu items to navigate to different fragments
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logout() {
        // Clear the SharedPreferences to log out the user
        val sharedPreferences = getSharedPreferences("userSession", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Remove the stored session data
        editor.remove("loggedInUsername")
        editor.remove("loggedInName")
        editor.apply()

        // Navigate back to the LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close the current activity so the user can't go back to it
    }




}
