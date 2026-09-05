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
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        val emailInputLayout = findViewById<TextInputLayout>(R.id.emailInputLayout)
        val passwordInputLayout = findViewById<TextInputLayout>(R.id.passwordInputLayout)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signupTextView = findViewById<TextView>(R.id.signupTextView)
        val backButton = findViewById<View>(R.id.loginBackButton)
        val progressBar = findViewById<ProgressBar>(R.id.loginProgressBar)
        val forgotPassword = findViewById<TextView>(R.id.forgotPasswordTextView)

        backButton.setOnClickListener { finish() }

        forgotPassword.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                emailInputLayout.error = "Enter email to reset password"
                return@setOnClickListener
            }
            emailInputLayout.error = null
            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Reset email sent to $email", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

            var hasError = false
            if (email.isEmpty()) {
                emailInputLayout.error = "Email is required"
                hasError = true
            } else {
                emailInputLayout.error = null
            }

            if (password.isEmpty()) {
                passwordInputLayout.error = "Password is required"
                hasError = true
            } else {
                passwordInputLayout.error = null
            }

            if (hasError) return@setOnClickListener

            loginButton.text = ""
            loginButton.isEnabled = false
            progressBar.visibility = View.VISIBLE

            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    loginButton.text = "Login"
                    loginButton.isEnabled = true
                    progressBar.visibility = View.GONE

                    val errorMessage = when (task.exception) {
                        is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "No account found with this email."
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
                        is com.google.firebase.FirebaseNetworkException -> "Network error. Please check your connection."
                        else -> "Login failed: ${task.exception?.message}"
                    }
                    // Show error inline on the first relevant field if possible, or as toast
                    if (task.exception is com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                        emailInputLayout.error = errorMessage
                    } else if (task.exception is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                        passwordInputLayout.error = errorMessage
                    } else {
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        signupTextView.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
