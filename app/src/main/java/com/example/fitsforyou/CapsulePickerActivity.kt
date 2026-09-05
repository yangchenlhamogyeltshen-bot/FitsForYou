package com.example.fitsforyou

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.fitsforyou.adapter.SectionedSelectableAdapter
import com.example.fitsforyou.database.AppDatabase
import com.example.fitsforyou.model.Clothing
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CapsulePickerActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase
    private lateinit var adapter: SectionedSelectableAdapter
    private var allItems = listOf<Clothing>()
    private val selectedIds = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capsule_picker)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.capsulePickerRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = AppDatabase.getDatabase(this)

        val recyclerView = findViewById<RecyclerView>(R.id.pickerRecyclerView)
        adapter = SectionedSelectableAdapter { id, isSelected ->
            toggleCapsule(id, isSelected)
        }
        recyclerView.adapter = adapter

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<Button>(R.id.doneBtn).setOnClickListener { finish() }

        loadData()
    }

    private fun loadData() {
        val userId = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            allItems = database.clothingDao().getAllClothing(userId).first()
            selectedIds.addAll(allItems.filter { it.isCapsule }.map { it.id })
            
            val list = mutableListOf<Any>()
            list.add("All Clothing Items")
            list.addAll(allItems)
            
            adapter.submitData(list)
            adapter.setSelection(selectedIds.toList())
        }
    }

    private fun toggleCapsule(id: Int, isCapsule: Boolean) {
        lifecycleScope.launch {
            val clothing = database.clothingDao().getClothingById(id) ?: return@launch
            val updated = clothing.copy(isCapsule = isCapsule)
            database.clothingDao().update(updated)
            if (isCapsule) selectedIds.add(id) else selectedIds.remove(id)
            adapter.toggleSelection(id)
        }
    }
}
