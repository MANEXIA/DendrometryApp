package com.example.myappkotlin

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowInsetsController
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
import com.example.myappkotlin.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

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

        // Initially display HomeFragment and set the checked item in the navigation view
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            navigationView.setCheckedItem(R.id.nav_home)
        }
    }//END OF ON CREATE

//     Hide system navigation and status bar
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
            R.id.nav_history-> {
                replaceFragment(HistoryFragment())
            }
            R.id.nav_guide -> {
                replaceFragment(GuideFragment())
            }
            R.id.nav_aboutus -> {
                replaceFragment(AboutUsFragment())
            }
            // Add more cases for other menu items to navigate to different fragments
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
