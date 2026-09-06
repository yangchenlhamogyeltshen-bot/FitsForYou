package com.example.fitsforyou

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.fitsforyou.adapter.ColorAdapter
import com.example.fitsforyou.database.AppDatabase
import com.example.fitsforyou.model.Clothing
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddClothingActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase
    private var selectedImageUri: Uri? = null
    private var tempCameraUri: Uri? = null
    private var selectedColor: String? = null
    private var isEditMode = false
    private var clothingIdToEdit: Int = -1

    private lateinit var clothingImageView: ImageView
    private lateinit var addPhotoPlaceholder: View
    private lateinit var nameEditText: EditText
    private lateinit var nameInputLayout: TextInputLayout
    private lateinit var categoryAutoComplete: AutoCompleteTextView
    private lateinit var seasonChipGroup: ChipGroup
    private lateinit var capsuleSwitch: MaterialSwitch
    private lateinit var colorRecyclerView: RecyclerView
    private lateinit var colorAdapter: ColorAdapter

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            handleImageResult(uri, isPersistable = true)
        }
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempCameraUri?.let { handleImageResult(it, isPersistable = false) }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            handlePermissionDenial()
        }
    }

    private fun handleImageResult(uri: Uri, isPersistable: Boolean) {
        if (validateImage(uri)) {
            if (isPersistable) {
                persistUriPermission(uri)
            }
            selectedImageUri = uri
            clothingImageView.load(uri) { crossfade(true) }
            clothingImageView.alpha = 1.0f
            addPhotoPlaceholder.visibility = View.GONE
        }
    }

    private fun showPhotoOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Add Clothing Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationale()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Needed")
            .setMessage("The camera is used to take photos of your clothing items.")
            .setPositiveButton("OK") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handlePermissionDenial() {
        if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            // Permission permanently denied
            AlertDialog.Builder(this)
                .setTitle("Camera Permission Required")
                .setMessage("You have permanently denied camera access. Please enable it in the app settings to take photos.")
                .setPositiveButton("Settings") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchCamera() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val photoFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        tempCameraUri = uri
        takePicture.launch(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_clothing)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addClothingRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = AppDatabase.getDatabase(this)

        clothingIdToEdit = intent.getIntExtra("CLOTHING_ID", -1)
        isEditMode = clothingIdToEdit != -1

        initViews()
        setupDropdowns()
        setupColorPicker()

        if (isEditMode) {
            findViewById<TextView>(R.id.addTitle).text = "Edit Piece"
            loadClothingForEdit(clothingIdToEdit)
        }

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<Button>(R.id.uploadPhotoButton).setOnClickListener { showPhotoOptions() }
        findViewById<Button>(R.id.cancelButton).setOnClickListener { finish() }
        findViewById<Button>(R.id.saveButton).setOnClickListener { saveClothingItem() }
    }

    private fun initViews() {
        clothingImageView = findViewById(R.id.clothingImageView)
        addPhotoPlaceholder = findViewById(R.id.addPhotoPlaceholder)
        nameEditText = findViewById(R.id.nameEditText)
        nameInputLayout = findViewById(R.id.nameInputLayout)
        categoryAutoComplete = findViewById(R.id.categoryAutoComplete)
        seasonChipGroup = findViewById(R.id.seasonChipGroup)
        capsuleSwitch = findViewById(R.id.capsuleSwitch)
        colorRecyclerView = findViewById(R.id.colorRecyclerView)
    }

    private fun setupDropdowns() {
        val categories = arrayOf("Tops", "Bottoms", "Traditional", "Outerwear", "Shoes")
        categoryAutoComplete.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, categories))
    }

    private fun setupColorPicker() {
        val colors = listOf("#3E4A61", "#5B7FA6", "#4E8C6C", "#8B8F99", "#2E2E2E", "#000000", "#FFFFFF", "#D2B48C", "#800000", "#F4B942")
        colorAdapter = ColorAdapter(colors) { color -> selectedColor = color }
        colorRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        colorRecyclerView.adapter = colorAdapter
    }

    private fun validateImage(uri: Uri): Boolean {
        val contentResolver = contentResolver
        val type = contentResolver.getType(uri) ?: ""
        if (!type.startsWith("image/")) {
            Toast.makeText(this, "Please select an image file", Toast.LENGTH_SHORT).show()
            return false
        }
        
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            val size = cursor.getLong(sizeIndex)
            if (size > 5 * 1024 * 1024) {
                Toast.makeText(this, "Image size exceeds 5MB", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun persistUriPermission(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun loadClothingForEdit(id: Int) {
        lifecycleScope.launch {
            val clothing = database.clothingDao().getClothingById(id) ?: return@launch
            nameEditText.setText(clothing.name)
            categoryAutoComplete.setText(clothing.category, false)
            selectedColor = clothing.color
            colorAdapter.setSelectedColor(clothing.color)
            
            val seasonChipId = when(clothing.season) {
                "Summer" -> R.id.chipSummer
                "Winter" -> R.id.chipWinter
                else -> R.id.chipAllSeason
            }
            findViewById<Chip>(seasonChipId)?.isChecked = true
            
            capsuleSwitch.isChecked = clothing.isCapsule
            
            if (clothing.imageUri != null) {
                selectedImageUri = Uri.parse(clothing.imageUri)
                clothingImageView.load(selectedImageUri) { crossfade(true) }
                clothingImageView.alpha = 1.0f
                addPhotoPlaceholder.visibility = View.GONE
            }
        }
    }

    private fun saveClothingItem() {
        val name = nameEditText.text.toString().trim()
        val category = categoryAutoComplete.text.toString()
        val selectedSeasonId = seasonChipGroup.checkedChipId
        val season = if (selectedSeasonId != View.NO_ID) {
            findViewById<Chip>(selectedSeasonId).text.toString()
        } else ""
        
        val userId = auth.currentUser?.uid ?: return

        if (name.isEmpty()) {
            nameInputLayout.error = "Name is required"
            return
        }
        if (category.isEmpty() || season.isEmpty() || selectedColor == null) {
            Toast.makeText(this, "Please complete all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val clothing = Clothing(
            id = if (isEditMode) clothingIdToEdit else 0,
            userId = userId,
            name = name,
            category = category,
            color = selectedColor!!,
            season = season,
            imageUri = selectedImageUri?.toString(),
            isCapsule = capsuleSwitch.isChecked,
            addedOn = if (isEditMode) 0L else System.currentTimeMillis() // DAO handle if needed
        )

        lifecycleScope.launch {
            if (isEditMode) {
                val old = database.clothingDao().getClothingById(clothingIdToEdit)
                val updated = clothing.copy(
                    addedOn = old?.addedOn ?: System.currentTimeMillis(),
                    timesWorn = old?.timesWorn ?: 0,
                    lastWorn = old?.lastWorn ?: null
                )
                database.clothingDao().update(updated)
            } else {
                database.clothingDao().insert(clothing)
            }
            Toast.makeText(this@AddClothingActivity, "Saved to closet", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
