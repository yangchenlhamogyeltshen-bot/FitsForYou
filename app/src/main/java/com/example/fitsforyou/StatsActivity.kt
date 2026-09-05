package com.example.fitsforyou

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.fitsforyou.database.AppDatabase
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StatsActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.statsRoot)) { v, insets ->
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

        val userId = currentUser.uid

        observeStatistics(userId)
        setupNavigation()
    }

    private fun observeStatistics(userId: String) {
        val clothingDao = database.clothingDao()
        val outfitDao = database.outfitDao()
        val wearDao = database.wearEventDao()

        // 1. Summary Row
        lifecycleScope.launch {
            combine(
                clothingDao.countAllClothing(userId),
                outfitDao.countTotalOutfits(userId),
                clothingDao.countCapsuleClothing(userId)
            ) { total, outfits, capsule ->
                Triple(total, outfits, capsule)
            }.collectLatest { (total, outfits, capsule) ->
                findViewById<View>(R.id.cardTotalClothes).findViewById<TextView>(R.id.statsValue).text = total.toString()
                findViewById<View>(R.id.cardTotalClothes).findViewById<TextView>(R.id.statsLabel).text = "Total Clothes"

                findViewById<View>(R.id.cardTotalOutfits).findViewById<TextView>(R.id.statsValue).text = outfits.toString()
                findViewById<View>(R.id.cardTotalOutfits).findViewById<TextView>(R.id.statsLabel).text = "Total Outfits"

                findViewById<View>(R.id.cardCapsuleItems).findViewById<TextView>(R.id.statsValue).text = capsule.toString()
                findViewById<View>(R.id.cardCapsuleItems).findViewById<TextView>(R.id.statsLabel).text = "Capsule Items"
            }
        }

        // 2. Most/Least Worn
        lifecycleScope.launch {
            clothingDao.getMostWornClothing(userId).collectLatest { clothing ->
                val title = findViewById<TextView>(R.id.mostWornTitle)
                val sub = findViewById<TextView>(R.id.mostWornCount)
                if (clothing != null && clothing.timesWorn > 0) {
                    title.text = clothing.name
                    sub.text = "${clothing.timesWorn} times"
                } else {
                    title.text = "None yet"
                    sub.text = "0 times"
                }
            }
        }

        lifecycleScope.launch {
            clothingDao.getLeastWornClothing(userId).collectLatest { clothing ->
                val title = findViewById<TextView>(R.id.leastWornTitle)
                val sub = findViewById<TextView>(R.id.leastWornCount)
                if (clothing != null) {
                    title.text = clothing.name
                    sub.text = "${clothing.timesWorn} times"
                } else {
                    title.text = "None yet"
                    sub.text = "0 times"
                }
            }
        }

        // 3. Category Distribution
        setupCategoryDistribution(userId)

        // 4. Weekly Trend
        calculateWeeklyTrend(userId)
    }

    private fun calculateWeeklyTrend(userId: String) {
        val wearDao = database.wearEventDao()
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L
        val weekMillis = 7 * dayMillis
        
        lifecycleScope.launch {
            val thisWeekCount = wearDao.countWornEventsInRange(userId, now - weekMillis, now).first()
            val lastWeekCount = wearDao.countWornEventsInRange(userId, now - 2 * weekMillis, now - weekMillis).first()
            
            val diff = thisWeekCount - lastWeekCount
            val percent = if (lastWeekCount == 0) {
                if (thisWeekCount > 0) 100 else 0
            } else {
                (diff.toDouble() / lastWeekCount * 100).toInt()
            }
            
            val trendBadge = findViewById<TextView>(R.id.wearCountBadge)
            val trendText = findViewById<TextView>(R.id.usageInsightText)
            
            val prefix = if (percent >= 0) "+" else ""
            trendBadge.text = "$prefix$percent% Worn"
            trendText.text = if (percent >= 0) "Your wardrobe usage is increasing! Great job." else "You've worn fewer items this week. Let's plan some looks!"
        }
    }

    private fun setupCategoryDistribution(userId: String) {
        val categories = listOf("Tops", "Traditional", "Bottoms", "Outerwear", "Shoes")
        val barIds = listOf(R.id.barTops, R.id.barTraditional, R.id.barBottoms, R.id.barOuterwear, R.id.barShoes)
        
        lifecycleScope.launch {
            database.clothingDao().getAllClothing(userId).collectLatest { allClothing ->
                val totalCount = allClothing.size
                categories.forEachIndexed { index, category ->
                    val count = allClothing.count { it.category == category }
                    val barView = findViewById<View>(barIds[index])
                    setupBar(barView, category, count, totalCount)
                }
            }
        }
    }

    private fun setupBar(view: View, label: String, value: Int, total: Int) {
        view.findViewById<TextView>(R.id.barLabel).text = label
        view.findViewById<TextView>(R.id.barValue).text = value.toString()
        val indicator = view.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.barIndicator)
        indicator.max = if (total > 0) total else 10
        indicator.progress = value
    }

    private fun setupNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_stats
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, HomeActivity::class.java)); true }
                R.id.nav_wardrobe -> { startActivity(Intent(this, WardrobeActivity::class.java)); true }
                R.id.nav_outfits -> { startActivity(Intent(this, OutfitsActivity::class.java)); true }
                R.id.nav_capsule -> { startActivity(Intent(this, CapsuleActivity::class.java)); true }
                R.id.nav_stats -> true
                else -> false
            }
        }
    }
}
