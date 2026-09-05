package com.example.fitsforyou

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.fitsforyou.database.AppDatabase
import com.example.fitsforyou.model.User as RoomUser
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signupRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = AppDatabase.getDatabase(this)

        val nameInputLayout = findViewById<TextInputLayout>(R.id.nameInputLayout)
        val emailInputLayout = findViewById<TextInputLayout>(R.id.emailInputLayout)
        val passwordInputLayout = findViewById<TextInputLayout>(R.id.passwordInputLayout)
        val confirmPasswordInputLayout = findViewById<TextInputLayout>(R.id.confirmPasswordInputLayout)

        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPasswordEditText)
        val signupButton = findViewById<Button>(R.id.signupButton)
        val loginTextView = findViewById<TextView>(R.id.loginTextView)
        val backButton = findViewById<View>(R.id.signupBackButton)
        val progressBar = findViewById<ProgressBar>(R.id.signupProgressBar)

        backButton.setOnClickListener { finish() }

        signupButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            var hasError = false
            if (name.isEmpty()) {
                nameInputLayout.error = "Name is required"
                hasError = true
            } else { nameInputLayout.error = null }

            if (email.isEmpty()) {
                emailInputLayout.error = "Email is required"
                hasError = true
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInputLayout.error = "Invalid email format"
                hasError = true
            } else { emailInputLayout.error = null }

            if (password.length < 6) {
                passwordInputLayout.error = "Min. 6 characters required"
                hasError = true
            } else { passwordInputLayout.error = null }

            if (password != confirmPassword) {
                confirmPasswordInputLayout.error = "Passwords do not match"
                hasError = true
            } else { confirmPasswordInputLayout.error = null }

            if (hasError) return@setOnClickListener

            signupButton.text = ""
            signupButton.isEnabled = false
            progressBar.visibility = View.VISIBLE

            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val profileUpdates = userProfileChangeRequest {
                        displayName = name
                    }

                    firebaseUser?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        lifecycleScope.launch {
                            // Create local profile
                            if (firebaseUser != null) {
                                database.userDao().insert(RoomUser(firebaseUser.uid, name, email))
                            }
                            
                            val intent = Intent(this@SignupActivity, HomeActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            if (profileTask.isSuccessful) {
                                Toast.makeText(this@SignupActivity, "Welcome to FitScan!", Toast.LENGTH_SHORT).show()
                            }
                            startActivity(intent)
                            finish()
                        }
                    }
                } else {
                    signupButton.text = "Sign Up"
                    signupButton.isEnabled = true
                    progressBar.visibility = View.GONE
                    
                    val errorMessage = when (task.exception) {
                        is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "This email is already registered."
                        is com.google.firebase.FirebaseNetworkException -> "Network error. Please check your connection."
                        else -> "Registration failed: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }

        loginTextView.setOnClickListener { finish() }
    }
}
