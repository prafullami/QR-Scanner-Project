package com.example.sync

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sync.data.FactoryLocalStore
import com.example.sync.data.ScanLogRepository
import com.example.sync.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var factoryStore: FactoryLocalStore
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
        scanLogRepository = ScanLogRepository(this)

        dropdown = findViewById(R.id.factoryDropdown)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        factoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            factoryStore.getFactories().sorted()
        )
        dropdown.setAdapter(factoryAdapter)

        loadDeviceId()
        setupFactoryControls()
        setupFabMenu()
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

            dropdown.isEnabled = false
            btnCorrect.isEnabled = false
            btnEdit.isEnabled = true
            btnCorrect.alpha = 0.5f
            btnEdit.alpha = 1f
            Toast.makeText(this, "Factory locked", Toast.LENGTH_SHORT).show()
        }

        btnEdit.setOnClickListener {
            dropdown.isEnabled = true
            btnCorrect.isEnabled = true
            btnEdit.isEnabled = false
            btnEdit.alpha = 0.5f
            btnCorrect.alpha = 1f
            Toast.makeText(this, "Factory unlocked", Toast.LENGTH_SHORT).show()
        }
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
                Toast.makeText(this, "Factory sync not implemented", Toast.LENGTH_SHORT).show()
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
            dialog.cancel()
        }

        builder.show()
    }

    private fun showFactoryDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Factory")

        val input = EditText(this)
        input.hint = "Enter Factory Name"
        builder.setView(input)

        builder.setPositiveButton("Add") { dialog, _ ->
            val factoryName = input.text.toString().trim()
            if (factoryName.isNotEmpty()) {
                factoryStore.addFactory(factoryName)
                refreshFactoryDropdown()
                Toast.makeText(this, "Factory added", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Enter Factory Name", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    override fun onResume() {
        super.onResume()
        loadTable()
    }
}
