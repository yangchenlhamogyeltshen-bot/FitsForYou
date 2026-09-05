package com.example.fitsforyou

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitsforyou.adapter.ClothingAdapter
import com.example.fitsforyou.database.AppDatabase
import com.example.fitsforyou.model.Clothing
import com.example.fitsforyou.model.WearEvent
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase
    private lateinit var closetPreviewAdapter: ClothingAdapter

    private var suggestedPieces = listOf<Clothing>()
    private var allClothing = listOf<Clothing>()
    private var currentTemp: Double = 20.0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = AppDatabase.getDatabase(this)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Greeting
        lifecycleScope.launch {
            database.userDao().getUserById(currentUser.uid).collectLatest { user ->
                findViewById<TextView>(R.id.greetingTextView).text = "Hello, ${user?.fullName?.split(" ")?.firstOrNull() ?: "User"}!"
            }
        }

        initClosetPreview()
        observeStats(currentUser.uid)
        setupNavigation()
        fetchWeather()

        findViewById<View>(R.id.viewAllCloset).setOnClickListener { startActivity(Intent(this, WardrobeActivity::class.java)) }
        findViewById<View>(R.id.homeAddPieceBtn).setOnClickListener { startActivity(Intent(this, AddClothingActivity::class.java)) }
        findViewById<View>(R.id.homeCreateLookBtn).setOnClickListener { startActivity(Intent(this, OutfitsActivity::class.java)) }
        findViewById<View>(R.id.homeMarkWornBtn).setOnClickListener { startActivity(Intent(this, MarkWornActivity::class.java)) }
        findViewById<View>(R.id.changeSuggestionBtn).setOnClickListener { rollSuggestion() }
        findViewById<View>(R.id.markWornQuickBtn).setOnClickListener { markSuggestedAsWorn() }
        findViewById<View>(R.id.emptyAddClothesBtn).setOnClickListener { startActivity(Intent(this, AddClothingActivity::class.java)) }
        
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            auth.signOut()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun initClosetPreview() {
        val recyclerView = findViewById<RecyclerView>(R.id.closetPreviewRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        closetPreviewAdapter = ClothingAdapter(
            onItemClick = { clothing ->
                val intent = Intent(this, ClothingDetailActivity::class.java)
                intent.putExtra("CLOTHING_ID", clothing.id)
                startActivity(intent)
            },
            onItemLongClick = { /* No-op */ }
        )
        recyclerView.adapter = closetPreviewAdapter
    }

    private fun observeStats(userId: String) {
        lifecycleScope.launch {
            combine(
                database.clothingDao().countAllClothing(userId),
                database.outfitDao().countTotalOutfits(userId),
                database.clothingDao().countNeverWornClothing(userId)
            ) { clothes, looks, notWorn ->
                Triple(clothes, looks, notWorn)
            }.collectLatest { (clothes, looks, notWorn) ->
                findViewById<TextView>(R.id.closetCountText).text = "$clothes Pieces"
                findViewById<TextView>(R.id.looksCountText).text = "$looks Looks"
                findViewById<TextView>(R.id.notWornCountText).text = "$notWorn Not Worn"
            }
        }

        lifecycleScope.launch {
            database.clothingDao().getAllClothing(userId).collectLatest { list ->
                allClothing = list
                closetPreviewAdapter.submitList(list.take(10))
                if (suggestedPieces.isEmpty()) rollSuggestion()
            }
        }
    }

    private fun fetchWeather() {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = URL("https://api.open-meteo.com/v1/forecast?latitude=26.86&longitude=89.38&current_weather=true")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.inputStream.bufferedReader().use { it.readText() }
                }
                val json = JSONObject(result)
                val current = json.getJSONObject("current_weather")
                currentTemp = current.getDouble("temperature")
                val condition = getWeatherCondition(current.getInt("weathercode"))
                
                withContext(Dispatchers.Main) {
                    findViewById<TextView>(R.id.weatherInfoText).text = "Phuentsholing · ${currentTemp}°C · $condition"
                    rollSuggestion()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                findViewById<TextView>(R.id.weatherInfoText).text = "Weather unavailable"
            }
        }
    }

    private fun rollSuggestion() {
        if (allClothing.isEmpty()) {
            showEmptySuggestion()
            return
        }
        
        val seasonTag = when {
            currentTemp < 15.0 -> "Winter"
            currentTemp > 25.0 -> "Summer"
            else -> "All Season"
        }
        
        val eligible = allClothing.filter { it.season == seasonTag || it.season == "All Season" }
        if (eligible.isNotEmpty()) {
            val tops = eligible.filter { it.category == "Tops" || it.category == "Traditional" }
            val bottoms = eligible.filter { it.category == "Bottoms" }
            
            val newList = mutableListOf<Clothing>()
            if (tops.isNotEmpty()) newList.add(tops.random())
            if (bottoms.isNotEmpty()) newList.add(bottoms.random())
            
            if (newList.isEmpty()) newList.add(eligible.random())
            
            suggestedPieces = newList
            updateSuggestionUI()
        } else {
            showEmptySuggestion()
        }
    }

    private fun showEmptySuggestion() {
        suggestedPieces = emptyList()
        findViewById<View>(R.id.noOutfitState).visibility = View.VISIBLE
        findViewById<View>(R.id.suggestionDetails).visibility = View.GONE
        findViewById<View>(R.id.suggestionImage).visibility = View.GONE
        findViewById<View>(R.id.markWornQuickBtn).visibility = View.GONE
    }

    private fun updateSuggestionUI() {
        if (suggestedPieces.isEmpty()) {
            showEmptySuggestion()
            return
        }
        
        findViewById<View>(R.id.noOutfitState).visibility = View.GONE
        findViewById<View>(R.id.suggestionDetails).visibility = View.VISIBLE
        findViewById<ImageView>(R.id.suggestionImage).visibility = View.VISIBLE
        findViewById<View>(R.id.markWornQuickBtn).visibility = View.VISIBLE
        
        val names = suggestedPieces.joinToString(" + ") { it.name }
        findViewById<TextView>(R.id.suggestedPieceName).text = names
        
        findViewById<ImageView>(R.id.suggestionImage).load(suggestedPieces.first().imageUri) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun markSuggestedAsWorn() {
        if (suggestedPieces.isEmpty()) return
        lifecycleScope.launch {
            suggestedPieces.forEach { piece ->
                val updated = piece.copy(timesWorn = piece.timesWorn + 1, lastWorn = System.currentTimeMillis())
                database.clothingDao().update(updated)
                database.wearEventDao().insert(WearEvent(userId = updated.userId, itemId = updated.id))
            }
            Toast.makeText(this@HomeActivity, "Marked as worn!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getWeatherCondition(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1, 2, 3 -> "Mainly Clear"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            95 -> "Thunderstorm"
            else -> "Clear"
        }
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_wardrobe -> { startActivity(Intent(this, WardrobeActivity::class.java)); true }
                R.id.nav_outfits -> { startActivity(Intent(this, OutfitsActivity::class.java)); true }
                R.id.nav_stats -> { startActivity(Intent(this, StatsActivity::class.java)); true }
                R.id.nav_capsule -> { startActivity(Intent(this, CapsuleActivity::class.java)); true }
                else -> false
            }
        }
    }
}
