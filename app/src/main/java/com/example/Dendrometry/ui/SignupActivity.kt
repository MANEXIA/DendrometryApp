package com.example.Dendrometry.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.Dendrometry.R
import com.example.Dendrometry.databinding.ActivitySignupBinding
import com.example.Dendrometry.dbmshelpers.UserDatabaseHelper

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    private lateinit var databaseHelper: UserDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        databaseHelper = UserDatabaseHelper(this)

        binding.signupBtn.setOnClickListener {
            // Retrieve input values
            val signupName = binding.name.text.toString().trim()
            val signupUsername = binding.username.text.toString().trim()
            val signupPassword = binding.password.text.toString().trim()

            // Validate input fields
            when {
                signupName.isEmpty() -> {
                    binding.name.error = "Name cannot be empty"
                    binding.name.requestFocus() // Move cursor to the Name field
                }
                signupUsername.isEmpty() -> {
                    binding.username.error = "Username cannot be empty"
                    binding.username.requestFocus() // Move cursor to the Username field
                }
                signupPassword.isEmpty() -> {
                    binding.password.error = "Password cannot be empty"
                    binding.password.requestFocus() // Move cursor to the Password field
                }
                signupPassword.length < 6 -> { // Optional: Check for password strength
                    binding.password.error = "Password must be at least 6 characters long"
                    binding.password.requestFocus()
                }
                else -> {
                    // If all inputs are valid, proceed to save the data
                    signupDatabase(signupName, signupUsername, signupPassword)
                }
            }
        }

        binding.loginBtn.setOnClickListener(){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun signupDatabase(name: String, username: String, password: String) {
        val insertRowId = databaseHelper.insertUser(name, username, password)
        if (insertRowId > -1) {
            Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Signup unsuccessful", Toast.LENGTH_SHORT).show()
        }
    }



}