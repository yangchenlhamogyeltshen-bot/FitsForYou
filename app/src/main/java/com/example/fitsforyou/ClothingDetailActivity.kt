package com.example.fitsforyou

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.fitsforyou.database.AppDatabase
import com.example.fitsforyou.model.Clothing
import com.example.fitsforyou.model.WearEvent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ClothingDetailActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var currentClothing: Clothing? = null

    private lateinit var imageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var categoryTag: TextView
    private lateinit var capsuleBadge: TextView
    private lateinit var colorIndicator: View
    private lateinit var colorText: TextView
    private lateinit var seasonText: TextView
    private lateinit var addedOnText: TextView
    private lateinit var timesWornText: TextView
    private lateinit var lastWornText: TextView
    private lateinit var markWornBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clothing_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detailRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top)
            findViewById<View>(R.id.bottomActionRow).updatePadding(bottom = systemBars.bottom)
            insets
        }

        database = AppDatabase.getDatabase(this)
        val clothingId = intent.getIntExtra("CLOTHING_ID", -1)

        if (clothingId == -1) {
            finish()
            return
        }

        initViews()

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<Button>(R.id.editBtn).setOnClickListener {
            val intent = Intent(this, AddClothingActivity::class.java)
            intent.putExtra("CLOTHING_ID", currentClothing?.id)
            startActivity(intent)
        }
        findViewById<Button>(R.id.deleteBtn).setOnClickListener { showDeleteConfirmation() }
        findViewById<Button>(R.id.addToOutfitBtn).setOnClickListener {
            val intent = Intent(this, CreateOutfitActivity::class.java)
            intent.putExtra("PRESELECTED_CLOTHING_ID", currentClothing?.id)
            startActivity(intent)
        }

        markWornBtn.setOnClickListener { markItemAsWorn() }

        loadClothing(clothingId)
    }

    private fun initViews() {
        imageView = findViewById(R.id.clothingImage)
        nameTextView = findViewById(R.id.clothingName)
        categoryTag = findViewById(R.id.categoryTag)
        capsuleBadge = findViewById(R.id.capsuleBadge)
        colorIndicator = findViewById(R.id.colorIndicator)
        colorText = findViewById(R.id.colorText)
        seasonText = findViewById(R.id.seasonText)
        addedOnText = findViewById(R.id.addedOnText)
        timesWornText = findViewById(R.id.timesWornText)
        lastWornText = findViewById(R.id.lastWornText)
        markWornBtn = findViewById(R.id.markWornBtn)
    }

    private fun loadClothing(id: Int) {
        lifecycleScope.launch {
            val clothing = database.clothingDao().getClothingById(id)
            if (clothing == null) {
                finish()
                return@launch
            }
            currentClothing = clothing
            displayClothing(clothing)
        }
    }

    private fun displayClothing(clothing: Clothing) {
        nameTextView.text = clothing.name
        categoryTag.text = clothing.category
        capsuleBadge.visibility = if (clothing.isCapsule) View.VISIBLE else View.GONE
        
        try {
            colorIndicator.setBackgroundColor(Color.parseColor(clothing.color))
            colorText.text = getColorName(clothing.color)
        } catch (e: Exception) {
            colorIndicator.setBackgroundColor(Color.GRAY)
            colorText.text = "Custom"
        }
        
        seasonText.text = clothing.season
        addedOnText.text = formatDate(clothing.addedOn)
        timesWornText.text = "${clothing.timesWorn} times"
        lastWornText.text = if (clothing.lastWorn != null) formatDate(clothing.lastWorn) else "Never worn"

        imageView.load(clothing.imageUri) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery)
            error(android.R.drawable.ic_menu_gallery)
        }
        
        // Disable mark worn if already worn today
        if (clothing.lastWorn != null && isSameDay(clothing.lastWorn, System.currentTimeMillis())) {
            markWornBtn.isEnabled = false
            markWornBtn.text = "Worn Today"
        } else {
            markWornBtn.isEnabled = true
            markWornBtn.text = "Mark Worn Today"
        }
    }

    private fun markItemAsWorn() {
        val clothing = currentClothing ?: return
        val now = System.currentTimeMillis()
        
        lifecycleScope.launch {
            // Update clothing stats
            val updated = clothing.copy(
                timesWorn = clothing.timesWorn + 1,
                lastWorn = now
            )
            database.clothingDao().update(updated)
            
            // Log wear event for statistics trend
            database.wearEventDao().insert(WearEvent(userId = clothing.userId, itemId = clothing.id, timestamp = now))
            
            currentClothing = updated
            displayClothing(updated)
            Toast.makeText(this@ClothingDetailActivity, "Styled for today!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Remove Piece")
            .setMessage("Remove this piece from your closet permanently?")
            .setPositiveButton("Remove") { _, _ ->
                currentClothing?.let { clothing ->
                    lifecycleScope.launch {
                        database.clothingDao().delete(clothing)
                        // Also remove from any outfits (handled by junction/cascade if set, but we manually check refs if needed)
                        // In our case OutfitClothingCrossRef has no foreign key constraint currently probably. 
                        // I should add one or manually delete.
                        database.outfitDao().deleteCrossRefsForItem(clothing.id)
                        
                        Toast.makeText(this@ClothingDetailActivity, "Piece removed", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(Date(t1)) == fmt.format(Date(t2))
    }

    private fun getColorName(hex: String): String {
        return when(hex.uppercase()) {
            "#000000" -> "Black"
            "#FFFFFF" -> "White"
            "#808080" -> "Grey"
            "#000080" -> "Navy"
            "#0000FF" -> "Blue"
            "#FF0000" -> "Red"
            "#008000" -> "Green"
            "#FFFF00" -> "Yellow"
            "#F5F5DC" -> "Beige"
            "#A52A2A" -> "Brown"
            else -> "Custom"
        }
    }
}
