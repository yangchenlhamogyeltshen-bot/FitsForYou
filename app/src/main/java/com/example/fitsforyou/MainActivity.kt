package com.example.fitsforyou

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen before calling super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        auth = FirebaseAuth.getInstance()
        
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is logged in, go to Home
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            // No user, go to Welcome
            startActivity(Intent(this, WelcomeActivity::class.java))
        }
        
        // Finish MainActivity so it's removed from the back stack
        finish()
    }
}
