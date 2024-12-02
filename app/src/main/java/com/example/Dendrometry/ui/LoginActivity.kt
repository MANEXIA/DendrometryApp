package com.example.Dendrometry.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.Dendrometry.R
import com.example.Dendrometry.databinding.ActivityLoginBinding
import com.example.Dendrometry.dbmshelpers.UserDatabaseHelper

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var databaseHelper: UserDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        databaseHelper = UserDatabaseHelper(this)
        binding.loginBtn.setOnClickListener {
            // Retrieve input values
            val loginUsername = binding.username.text.toString().trim()
            val loginPassword = binding.password.text.toString().trim()

            // Validate input fields
            when {
                loginUsername.isEmpty() -> {
                    binding.username.error = "Username cannot be empty"
                    binding.username.requestFocus() // Move cursor to the Username field
                }
                loginPassword.isEmpty() -> {
                    binding.password.error = "Password cannot be empty"
                    binding.password.requestFocus() // Move cursor to the Password field
                }
                else -> {
                    // If inputs are valid, proceed to check login credentials
                    loginDatabase(loginUsername, loginPassword)
                }
            }
        }


        binding.signupBtn.setOnClickListener(){
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    private fun loginDatabase(username: String, password: String) {
        // Get the cursor from the database helper
        val cursor = databaseHelper.readUser(username, password)

        // If the cursor is not null and contains data, proceed with login
        if (cursor != null && cursor.moveToFirst()) {
            // Get the column index for 'name' and 'username'
            val nameColumnIndex = cursor.getColumnIndex(UserDatabaseHelper.COLUMN_NAME)
            val usernameColumnIndex = cursor.getColumnIndex(UserDatabaseHelper.COLUMN_USERNAME)

            // Check if the columns exist by ensuring the index is not -1
            if (nameColumnIndex >= 0 && usernameColumnIndex >= 0) {
                val name = cursor.getString(nameColumnIndex)

                // Save username and name in SharedPreferences to maintain a session
                val sharedPreferences = getSharedPreferences("userSession", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("loggedInUsername", username)
                editor.putString("loggedInName", name)
                editor.putString("loogedPwd", password)
                editor.apply()

                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                // Navigate to the MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Close the current activity to prevent going back to the login screen
            } else {
                // Column not found error
                Toast.makeText(this, "Error: Missing expected columns", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Login unsuccessful", Toast.LENGTH_SHORT).show()
        }
        // Close the cursor after use
        cursor?.close()
    }




}