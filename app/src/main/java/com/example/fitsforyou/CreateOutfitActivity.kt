package com.example.fitsforyou

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitsforyou.adapter.SectionedSelectableAdapter
import com.example.fitsforyou.adapter.SelectedItemsAdapter
import com.example.fitsforyou.database.AppDatabase
import com.example.fitsforyou.model.Clothing
import com.example.fitsforyou.model.Outfit
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CreateOutfitActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase
    private lateinit var sectionedAdapter: SectionedSelectableAdapter
    private lateinit var selectedItemsAdapter: SelectedItemsAdapter

    private var allUserItems = listOf<Clothing>()
    private val selectedClothingIds = mutableSetOf<Int>()
    private var isEditMode = false
    private var outfitIdToEdit: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_outfit)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.createOutfitRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top)
            findViewById<View>(R.id.previewStripCard).updatePadding(bottom = systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = AppDatabase.getDatabase(this)

        outfitIdToEdit = intent.getLongExtra("OUTFIT_ID", -1L)
        val preselectedId = intent.getIntExtra("PRESELECTED_CLOTHING_ID", -1)
        if (preselectedId != -1) selectedClothingIds.add(preselectedId)

        isEditMode = outfitIdToEdit != -1L

        initViews()
        loadData()

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<Button>(R.id.saveOutfitBtn).setOnClickListener { saveOutfit() }
    }

    private fun initViews() {
        if (isEditMode) findViewById<TextView>(R.id.createOutfitTitle).text = "Edit Look"

        val clothingRv = findViewById<RecyclerView>(R.id.clothingRecyclerView)
        val layoutManager = GridLayoutManager(this, 3)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (sectionedAdapter.getItemViewType(position) == 0) 3 else 1 // Header spans 3 columns
            }
        }
        clothingRv.layoutManager = layoutManager
        sectionedAdapter = SectionedSelectableAdapter { id, isSelected ->
            if (isSelected) selectedClothingIds.add(id) else selectedClothingIds.remove(id)
            sectionedAdapter.toggleSelection(id)
            updatePreviewStrip()
        }
        clothingRv.adapter = sectionedAdapter

        val selectedRv = findViewById<RecyclerView>(R.id.selectedItemsRecyclerView)
        selectedItemsAdapter = SelectedItemsAdapter { id ->
            selectedClothingIds.remove(id)
            sectionedAdapter.toggleSelection(id)
            updatePreviewStrip()
        }
        selectedRv.adapter = selectedItemsAdapter
    }

    private fun loadData() {
        val userId = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            allUserItems = database.clothingDao().getAllClothing(userId).first()
            
            if (isEditMode) {
                val outfitWithClothing = database.outfitDao().getOutfitsWithClothing(userId).first().find { it.outfit.id == outfitIdToEdit }
                outfitWithClothing?.let {
                    findViewById<EditText>(R.id.outfitNameEditText).setText(it.outfit.name)
                    selectedClothingIds.addAll(it.clothingItems.map { c -> c.id })
                }
            }

            sectionedAdapter.setSelection(selectedClothingIds.toList())
            updateListWithSections()
            updatePreviewStrip()
        }
    }

    private fun updateListWithSections() {
        val list = mutableListOf<Any>()
        val categories = listOf("Traditional", "Tops", "Bottoms", "Outerwear", "Shoes", "Accessories")
        categories.forEach { cat ->
            val itemsInCategory = allUserItems.filter { it.category == cat }
            if (itemsInCategory.isNotEmpty()) {
                list.add("Choose from $cat")
                list.addAll(itemsInCategory)
            }
        }
        sectionedAdapter.submitData(list)
    }

    private fun updatePreviewStrip() {
        val selectedItems = allUserItems.filter { selectedClothingIds.contains(it.id) }
        selectedItemsAdapter.submitList(selectedItems)
    }

    private fun saveOutfit() {
        val name = findViewById<EditText>(R.id.outfitNameEditText).text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please name your look", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedClothingIds.isEmpty()) {
            Toast.makeText(this, "Select pieces to curate a look", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            if (isEditMode) {
                // Update existing
                val old = database.outfitDao().getOutfitsWithClothing(userId).first().find { it.outfit.id == outfitIdToEdit }?.outfit ?: return@launch
                val updatedOutfit = old.copy(name = name)
                database.outfitDao().updateOutfit(updatedOutfit)
                database.outfitDao().deleteCrossRefsForOutfit(outfitIdToEdit)
                selectedClothingIds.forEach { id ->
                    database.outfitDao().insertOutfitClothingCrossRef(com.example.fitsforyou.model.OutfitClothingCrossRef(outfitIdToEdit, id))
                }
            } else {
                // Create new
                val outfit = Outfit(userId = userId, name = name)
                database.outfitDao().insertOutfitWithItems(outfit, selectedClothingIds.toList())
            }
            Toast.makeText(this@CreateOutfitActivity, "Look saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
