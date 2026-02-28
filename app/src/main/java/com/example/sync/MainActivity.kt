package com.example.sync

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import android.view.Menu
import android.view.MenuItem
import com.example.sync.databinding.ActivityMainBinding
import android.app.AlertDialog
import android.content.SharedPreferences
import android.text.InputType
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.PopupMenu
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.recyclerview.widget.RecyclerView
import com.example.sync.data.FactoryLocalStore

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var factoryAdapter: ArrayAdapter<String>
    private lateinit var dropdown: AutoCompleteTextView
    private lateinit var recyclerView: RecyclerView

    private lateinit var factoryLocalStore: FactoryLocalStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        factoryLocalStore = FactoryLocalStore(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }


        loadDeviceId()
        dropdown = findViewById(R.id.factoryDropdown)

        val btnCorrect = findViewById<ImageButton>(R.id.btnCorrect)
        val btnEdit = findViewById<ImageButton>(R.id.btnEdit)
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        btnEdit.isEnabled = false
        val currentFactory = factoryLocalStore.getCurrentFactory()
        if(currentFactory != "") {
            dropdown.setText(currentFactory);
            dropdown.isEnabled = false
            btnCorrect.isEnabled = false
            btnEdit.isEnabled = true
        }
        factoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            factoryLocalStore.getFactories().toList()
        )

        findViewById<ImageButton>(R.id.btnCorrect).setOnClickListener {

            val selectedFactory = dropdown.text.toString()
            factoryLocalStore.saveCurrentFactory(selectedFactory)
            if (selectedFactory.isNotEmpty()) {
                Toast.makeText(this, "Selected: $selectedFactory", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Select a factory", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageButton>(R.id.btnEdit).setOnClickListener {

            val selectedFactory = dropdown.text.toString()

            if (selectedFactory.isNotEmpty()) {
                showEditFactoryDialog(selectedFactory)
                factoryLocalStore.saveCurrentFactory(null)
            } else {
                Toast.makeText(this, "Select factory to edit", Toast.LENGTH_SHORT).show()
            }
        }

        dropdown.setAdapter(factoryAdapter)

        btnCorrect.setOnClickListener {

            val selectedFactory = dropdown.text.toString()

            if (selectedFactory.isNotEmpty()) {

                dropdown.isEnabled = false
                btnCorrect.isEnabled = false
                btnEdit.isEnabled = true
                btnCorrect.alpha = 0.5f
                btnEdit.alpha = 1f
                factoryLocalStore.saveCurrentFactory(selectedFactory);
                Toast.makeText(this, "Factory Locked", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(this, "Select a factory first", Toast.LENGTH_SHORT).show()
            }
        }

        btnEdit.setOnClickListener {

            dropdown.isEnabled = true
            btnCorrect.isEnabled = true
            btnEdit.isEnabled = false
            btnEdit.alpha = 0.5f
            btnCorrect.alpha = 1f

            Toast.makeText(this, "Edit Mode Enabled", Toast.LENGTH_SHORT).show()
        }

        fab.setOnClickListener { view ->

            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.fab_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {

                    R.id.action_local_sync -> {
                        Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show()
                        true
                    }

                    R.id.action_global_sync -> {
                        Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show()
                        true
                    }

                    R.id.action_open_for_give -> {
                        openScanner("GIVE")
                        Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show()
                        true
                    }

                    R.id.action_open_for_take -> {
                        openScanner("TAKE")
                        Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show()
                        true
                    }

                    else -> false
                }
            }

            popup.show()
        }

//        recyclerView = findViewById(R.id.recyclerView)
//        recyclerView.layoutManager = LinearLayoutManager(this)

//        loadTable()
    }

//    private fun loadTable() {
//
//        val data = loadData()
//        recyclerView.adapter = ScanAdapter(data)
//    }

//    private fun loadData(): List<ScanRecord> {
//
//        val map = mutableMapOf<String, ScanRecord>()
//        val file = File(filesDir, "scan_log.txt")
//
//        if (!file.exists()) return emptyList()
//
//        file.forEachLine {
//            val parts = it.split(" | ")
//            if (parts.size < 3) return@forEachLine
//
//            val qr = parts[0]
//            val mode = parts[1]
//            val status = parts[2]
//
//            val record = map.getOrPut(qr) { ScanRecord(qr) }
//
//            if (mode == "GIVE") {
//                record.given = true
//            } else if (mode == "TAKE") {
//                record.taken = true
//                if (status == "MISSING") {
//                    record.sdMissing = true
//                }
//            }
//        }
//
//        return map.values.toList()
//    }

    private fun refreshFactoryDropdown() {
        val updatedFactories = factoryLocalStore.getFactories()
        Log.d("updatedFactories", updatedFactories.toList().toString())
        factoryAdapter.clear()
        factoryAdapter.addAll(updatedFactories.toList())
        factoryAdapter.notifyDataSetChanged()
    }

    private val qrLauncher = registerForActivityResult(
        ScanContract()
    ) { result ->
        if (result.contents != null) {
            Toast.makeText(this, "Scanned: ${result.contents}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openScanner(purpose: String) {

        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan QR Code")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        options.captureActivity = SmallScanActivity::class.java
        options.addExtra("MODE",purpose)
        qrLauncher.launch(options)
    }

    private fun showEditFactoryDialog(oldName: String) {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Factory")

        val input = EditText(this)
        input.setText(oldName)

        builder.setView(input)

        builder.setPositiveButton("Update") { dialog, _ ->

            val newName = input.text.toString().trim()

            if (newName.isNotEmpty()) {

                val factories = factoryLocalStore.getFactories().toMutableSet()
                factories.remove(oldName)
                factories.add(newName)

                factoryLocalStore.replaceFactories(factories)

                recreate() // refresh UI
            }

            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun loadDeviceId() {
        val deviceId = factoryLocalStore.getDeviceId()

        val toolbarTitle = findViewById<TextView>(R.id.toolbarTitle)
        toolbarTitle.text = deviceId
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_gen_factory -> {
                showFactoryDialog()
                true
            }
            R.id.action_sync_factory -> true
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

        // Create EditText
        val input = EditText(this)
        input.hint = "Device ID"
        input.inputType = InputType.TYPE_CLASS_TEXT

        builder.setView(input)

        builder.setPositiveButton("Submit") { dialog, _ ->
            val deviceId = input.text.toString().trim()

            if (deviceId.isNotEmpty()) {
                factoryLocalStore.saveDeviceId(deviceId)
                refreshFactoryDropdown()

                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
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
                factoryLocalStore.addFactory(factoryName);
                Toast.makeText(this, "Factory Added", Toast.LENGTH_SHORT).show()
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
//        loadTable()
    }

}