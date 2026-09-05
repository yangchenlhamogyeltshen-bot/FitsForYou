package com.example.fitsforyou

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
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

class WardrobeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase
    private lateinit var adapter: ClothingAdapter

    private var fullList: List<Clothing> = emptyList()
    private var currentFilterCategory: String = "All"
    private var currentSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wardrobe)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.wardrobeRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = AppDatabase.getDatabase(this)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        val addClothingButton = findViewById<View>(R.id.addClothingButton)
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        val recyclerView = findViewById<RecyclerView>(R.id.wardrobeRecyclerView)
        val emptyStateLayout = findViewById<View>(R.id.emptyStateLayout)
        val emptyAddButton = findViewById<Button>(R.id.emptyAddButton)

        adapter = ClothingAdapter(
            onItemClick = { clothing ->
                val intent = Intent(this, ClothingDetailActivity::class.java)
                intent.putExtra("CLOTHING_ID", clothing.id)
                startActivity(intent)
            },
            onItemLongClick = { clothing ->
                showDeleteConfirmation(clothing)
            }
        )
        recyclerView.adapter = adapter

        // Set active item in bottom nav
        setupNavigation()

        val openAddClothing = {
            startActivity(Intent(this, AddClothingActivity::class.java))
        }

        addClothingButton.setOnClickListener { openAddClothing() }
        emptyAddButton?.setOnClickListener { openAddClothing() }

        // Search logic
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString().lowercase()
                applyFilterAndSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Filter button setup
        setupFilterButtons()

        // Observe data
        lifecycleScope.launch {
            database.clothingDao().getAllClothing(currentUser.uid).collectLatest { list ->
                fullList = list
                applyFilterAndSearch()
                
                // Toggle empty state
                if (fullList.isEmpty()) {
                    emptyStateLayout?.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyStateLayout?.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupFilterButtons() {
        val filters = mapOf(
            R.id.filterAll to "All",
            R.id.filterTops to "Tops",
            R.id.filterBottoms to "Bottoms",
            R.id.filterDresses to "Dresses",
            R.id.filterTraditional to "Traditional",
            R.id.filterOuterwear to "Outerwear",
            R.id.filterShoes to "Shoes",
            R.id.filterAccessories to "Accessories"
        )

        for ((id, category) in filters) {
            val button = findViewById<Button>(id) ?: continue
            button.setOnClickListener {
                // Update selection state
                filters.keys.forEach { filterId ->
                    findViewById<Button>(filterId)?.isSelected = (filterId == id)
                }
                currentFilterCategory = category
                applyFilterAndSearch()
            }
        }
    }

    private fun showDeleteConfirmation(clothing: Clothing) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete '${clothing.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    database.clothingDao().delete(clothing)
                    android.widget.Toast.makeText(this@WardrobeActivity, "Item deleted", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyFilterAndSearch() {
        var filteredList = if (currentFilterCategory == "All") {
            fullList
        } else {
            fullList.filter { it.category == currentFilterCategory }
        }

        if (currentSearchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.name.lowercase().contains(currentSearchQuery) ||
                it.category.lowercase().contains(currentSearchQuery) ||
                it.color.lowercase().contains(currentSearchQuery)
            }
        }

        adapter.submitList(filteredList)
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_wardrobe
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.nav_wardrobe -> true
                R.id.nav_outfits -> {
                    startActivity(Intent(this, OutfitsActivity::class.java))
                    true
                }
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
