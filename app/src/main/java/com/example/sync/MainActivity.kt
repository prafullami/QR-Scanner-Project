package com.example.sync

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sync.data.FactoryLocalStore
import com.example.sync.data.FactoryRemoteRepository
import com.example.sync.data.ScanLogRepository
import com.example.sync.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var factoryStore: FactoryLocalStore
    private lateinit var factoryRemoteRepository: FactoryRemoteRepository
    private lateinit var scanLogRepository: ScanLogRepository
    private lateinit var factoryAdapter: ArrayAdapter<String>
    private lateinit var dropdown: AutoCompleteTextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        factoryStore = FactoryLocalStore(this)
        factoryRemoteRepository = FactoryRemoteRepository()
        scanLogRepository = ScanLogRepository(this)

        dropdown = findViewById(R.id.factoryDropdown)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        factoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            factoryStore.getFactories().sorted().toMutableList()
        )
        dropdown.setAdapter(factoryAdapter)

        loadDeviceId()
        setupFactoryControls()
        setupFabMenu()
        restoreSelectedFactory()
        loadTable()
    }

    private fun setupFactoryControls() {
        val btnCorrect = findViewById<ImageButton>(R.id.btnCorrect)
        val btnEdit = findViewById<ImageButton>(R.id.btnEdit)

        btnEdit.isEnabled = false
        btnEdit.alpha = 0.5f

        btnCorrect.setOnClickListener {
            val selectedFactory = dropdown.text.toString().trim()
            if (selectedFactory.isEmpty()) {
                Toast.makeText(this, "Select a factory first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lockFactorySelection(selectedFactory, btnCorrect, btnEdit)
            Toast.makeText(this, "Factory locked", Toast.LENGTH_SHORT).show()
        }

        btnEdit.setOnClickListener {
            unlockFactorySelection(btnCorrect, btnEdit)
            factoryStore.saveCurrentFactory(null)
            Toast.makeText(this, "Factory unlocked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun lockFactorySelection(
        selectedFactory: String,
        btnCorrect: ImageButton,
        btnEdit: ImageButton
    ) {
        dropdown.setText(selectedFactory, false)
        dropdown.isEnabled = false
        btnCorrect.isEnabled = false
        btnEdit.isEnabled = true
        btnCorrect.alpha = 0.5f
        btnEdit.alpha = 1f
        factoryStore.saveCurrentFactory(selectedFactory)
    }

    private fun unlockFactorySelection(btnCorrect: ImageButton, btnEdit: ImageButton) {
        dropdown.isEnabled = true
        btnCorrect.isEnabled = true
        btnEdit.isEnabled = false
        btnEdit.alpha = 0.5f
        btnCorrect.alpha = 1f
    }

    private fun restoreSelectedFactory() {
        val currentFactory = factoryStore.getCurrentFactory().trim()
        if (currentFactory.isEmpty()) return

        val btnCorrect = findViewById<ImageButton>(R.id.btnCorrect)
        val btnEdit = findViewById<ImageButton>(R.id.btnEdit)
        lockFactorySelection(currentFactory, btnCorrect, btnEdit)
    }

    private fun setupFabMenu() {
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.fab_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_local_sync -> {
                        Toast.makeText(this, "Local sync not implemented", Toast.LENGTH_SHORT).show()
                        true
                    }

                    R.id.action_global_sync -> {
                        Toast.makeText(this, "Global sync not implemented", Toast.LENGTH_SHORT).show()
                        true
                    }

                    R.id.action_open_for_give -> {
                        openScanner("GIVE")
                        true
                    }

                    R.id.action_open_for_take -> {
                        openScanner("TAKE")
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun loadTable() {
        recyclerView.adapter = ScanAdapter(scanLogRepository.loadRecords())
    }

    private fun refreshFactoryDropdown() {
        val updatedFactories = factoryStore.getFactories().sorted()
        factoryAdapter.clear()
        factoryAdapter.addAll(updatedFactories)
        factoryAdapter.notifyDataSetChanged()
    }

    private val qrLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            Toast.makeText(this, "Scanned: ${result.contents}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Scanner closed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openScanner(purpose: String) {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan QR Code")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        options.captureActivity = SmallScanActivity::class.java
        options.addExtra("MODE", purpose)
        qrLauncher.launch(options)
    }

    private fun loadDeviceId() {
        val toolbarTitle = findViewById<TextView>(R.id.toolbarTitle)
        toolbarTitle.text = factoryStore.getDeviceId()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_gen_factory -> {
                showFactoryDialog()
                true
            }

            R.id.action_sync_factory -> {
                syncFactories()
                true
            }

            R.id.action_gen_device_id -> {
                showDeviceIdDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeviceIdDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Device ID")

        val input = EditText(this)
        input.hint = "Device ID"
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Submit") { dialog, _ ->
            val deviceId = input.text.toString().trim()
            if (deviceId.isNotEmpty()) {
                factoryStore.saveDeviceId(deviceId)
                loadDeviceId()
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter Device ID", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun showFactoryDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Factory")

        val nameInput = EditText(this)
        nameInput.hint = "Enter Factory Name"

        val locationInput = EditText(this)
        locationInput.hint = "Enter Location"

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = 40
            setPadding(padding, 16, padding, 8)
            addView(
                nameInput,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                locationInput,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 16 }
            )
        }

        builder.setView(container)

        builder.setPositiveButton("Add") { dialog, _ ->
            val factoryName = nameInput.text.toString().trim()
            val location = locationInput.text.toString().trim()
            val createdBy = factoryStore.getDeviceId()

            if (factoryName.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Enter factory name and location", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setPositiveButton
            }

            if (!factoryStore.hasDeviceId()) {
                Toast.makeText(this, "Set Device ID first", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setPositiveButton
            }

            createFactory(factoryName, location, createdBy)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun createFactory(factoryName: String, location: String, createdBy: String) {
        lifecycleScope.launch {
            val result = factoryRemoteRepository.createFactory(
                name = factoryName,
                location = location,
                createdBy = createdBy
            )

            if (result.isSuccess) {
                val createResult = result.getOrNull()
                val returnedName = createResult?.record?.name?.trim().orEmpty()
                val nameToStore = if (returnedName.isNotEmpty()) returnedName else factoryName

                factoryStore.addFactory(nameToStore)
                refreshFactoryDropdown()
                val successMessage = createResult?.message ?: "Factory created"
                Toast.makeText(this@MainActivity, successMessage, Toast.LENGTH_SHORT).show()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun syncFactories() {
        lifecycleScope.launch {
            val result = factoryRemoteRepository.listFactories()
            if (result.isSuccess) {
                val factories = result.getOrNull().orEmpty()
                    .mapNotNull { it.name?.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()

                factoryStore.replaceFactories(factories)
                refreshFactoryDropdown()
                dropdown.setText("", false)
                Toast.makeText(
                    this@MainActivity,
                    "Synced ${factories.size} factories",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Factory sync failed"
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadTable()
    }
}
