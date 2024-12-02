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

        binding.signupBtn.setOnClickListener(){
            val signupName = binding.name.text.toString()
            val signupUsername = binding.username.text.toString()
            val signupPassword = binding.password.text.toString()
            signupDatabase(signupName, signupUsername, signupPassword)
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