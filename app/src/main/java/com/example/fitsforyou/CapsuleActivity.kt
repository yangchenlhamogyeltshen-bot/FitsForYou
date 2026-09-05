package com.example.fitsforyou

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.fitsforyou.adapter.ClothingAdapter
import com.example.fitsforyou.database.AppDatabase
import com.example.fitsforyou.model.Clothing
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CapsuleActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase
    private lateinit var adapter: ClothingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capsule)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.capsuleRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = AppDatabase.getDatabase(this)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        val capsuleCountText = findViewById<TextView>(R.id.capsuleCountText)
        val recyclerView = findViewById<RecyclerView>(R.id.capsuleRecyclerView)
        val emptyStateLayout = findViewById<LinearLayout>(R.id.emptyStateLayout)
        val goToWardrobeBtn = findViewById<Button>(R.id.goToWardrobeBtn)
        val addMoreBtn = findViewById<Button>(R.id.addMoreCapsuleBtn)

        adapter = ClothingAdapter(
            onItemClick = { clothing ->
                val intent = Intent(this, ClothingDetailActivity::class.java)
                intent.putExtra("CLOTHING_ID", clothing.id)
                startActivity(intent)
            },
            onItemLongClick = { clothing ->
                showRemoveFromCapsuleDialog(clothing)
            }
        )
        recyclerView.adapter = adapter

        backBtn.setOnClickListener { finish() }
        goToWardrobeBtn.setOnClickListener {
            startActivity(Intent(this, WardrobeActivity::class.java))
            finish()
        }
        addMoreBtn.setOnClickListener {
            startActivity(Intent(this, CapsulePickerActivity::class.java))
        }

        // Observe Capsule Items (Filtered by isCapsule = true)
        lifecycleScope.launch {
            database.clothingDao().getAllClothing(currentUser.uid).collectLatest { list ->
                val capsuleList = list.filter { it.isCapsule }
                adapter.submitList(capsuleList)
                
                capsuleCountText.text = "${capsuleList.size} Pieces"

                if (capsuleList.isEmpty()) {
                    emptyStateLayout.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    addMoreBtn.visibility = View.GONE
                } else {
                    emptyStateLayout.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    addMoreBtn.visibility = View.VISIBLE
                }
            }
        }

        setupNavigation()
    }

    private fun showRemoveFromCapsuleDialog(clothing: Clothing) {
        AlertDialog.Builder(this)
            .setTitle("Remove from Collection")
            .setMessage("Remove '${clothing.name}' from your capsule? It will remain in your closet.")
            .setPositiveButton("Remove") { _, _ ->
                lifecycleScope.launch {
                    val updated = clothing.copy(isCapsule = false)
                    database.clothingDao().update(updated)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_capsule
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.nav_wardrobe -> {
                    startActivity(Intent(this, WardrobeActivity::class.java))
                    true
                }
                R.id.nav_outfits -> {
                    startActivity(Intent(this, OutfitsActivity::class.java))
                    true
                }
                R.id.nav_capsule -> true
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
