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

    private fun loginDatabase(username: String, password: String){
        val userExists = databaseHelper.readUser(username, password)
        if(userExists){
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }else{
            Toast.makeText(this, "Login unsuccessful", Toast.LENGTH_SHORT).show()
        }
    }

}