package com.example.fitsforyou

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
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
import com.example.fitsforyou.model.WearEvent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MarkWornActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase
    private lateinit var adapter: SectionedSelectableAdapter
    private var allItems = listOf<Clothing>()
    private val markedIds = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mark_worn)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.markWornRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = AppDatabase.getDatabase(this)

        val recyclerView = findViewById<RecyclerView>(R.id.pickerRecyclerView)
        adapter = SectionedSelectableAdapter { id, isSelected ->
            if (isSelected) markAsWorn(id)
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
            val list = mutableListOf<Any>()
            list.add("Choose items worn today")
            list.addAll(allItems)
            adapter.submitData(list)
            
            // Highlight items already worn today
            val wornTodayIds = allItems.filter { it.lastWorn != null && isToday(it.lastWorn) }.map { it.id }
            markedIds.addAll(wornTodayIds)
            adapter.setSelection(markedIds.toList())
        }
    }

    private fun markAsWorn(id: Int) {
        if (markedIds.contains(id)) return
        
        lifecycleScope.launch {
            val clothing = database.clothingDao().getClothingById(id) ?: return@launch
            val now = System.currentTimeMillis()
            val updated = clothing.copy(timesWorn = clothing.timesWorn + 1, lastWorn = now)
            database.clothingDao().update(updated)
            database.wearEventDao().insert(WearEvent(userId = updated.userId, itemId = updated.id, timestamp = now))
            
            markedIds.add(id)
            adapter.setSelection(markedIds.toList())
            Toast.makeText(this@MarkWornActivity, "${clothing.name} marked as worn!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(Date(timestamp)) == fmt.format(Date())
    }
}
