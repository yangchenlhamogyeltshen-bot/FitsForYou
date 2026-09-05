package com.example.fitsforyou

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitsforyou.adapter.OutfitAdapter
import com.example.fitsforyou.database.AppDatabase
import com.example.fitsforyou.model.OutfitWithClothing
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OutfitsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase
    private lateinit var adapter: OutfitAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outfits)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.outfitsRoot)) { v, insets ->
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

        val recyclerView = findViewById<RecyclerView>(R.id.outfitsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val emptyState = findViewById<View>(R.id.looksEmptyStateLayout)
        val emptyBtn = findViewById<Button>(R.id.emptyCreateLookBtn)

        adapter = OutfitAdapter(
            onDeleteClick = { outfitWithClothing ->
                showDeleteConfirmation(outfitWithClothing)
            },
            onEditClick = { outfitWithClothing ->
                val intent = Intent(this, CreateOutfitActivity::class.java)
                intent.putExtra("OUTFIT_ID", outfitWithClothing.outfit.id)
                startActivity(intent)
            }
        )
        recyclerView.adapter = adapter

        val createOutfitFab = findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.createOutfitFab)
        val openCreateOutfit = {
            startActivity(Intent(this, CreateOutfitActivity::class.java))
        }
        createOutfitFab.setOnClickListener { openCreateOutfit() }
        emptyBtn.setOnClickListener { openCreateOutfit() }

        // Observe outfits from DB
        lifecycleScope.launch {
            database.outfitDao().getOutfitsWithClothing(currentUser.uid).collectLatest { list ->
                adapter.submitList(list)
                if (list.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    createOutfitFab.hide()
                } else {
                    emptyState.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    createOutfitFab.show()
                }
            }
        }

        setupNavigation()
    }

    private fun showDeleteConfirmation(outfitWithClothing: OutfitWithClothing) {
        AlertDialog.Builder(this)
            .setTitle("Delete Look")
            .setMessage("Are you sure you want to delete '${outfitWithClothing.outfit.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    database.outfitDao().deleteOutfitWithRefs(outfitWithClothing.outfit)
                    Toast.makeText(this@OutfitsActivity, "Look deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_outfits
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
                R.id.nav_outfits -> true
                R.id.nav_capsule -> {
                    startActivity(Intent(this, CapsuleActivity::class.java))
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, StatsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
