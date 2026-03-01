package com.example.sync

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.example.sync.data.ScanLogRepository
import com.example.sync.data.FactoryLocalStore
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.Size

class SmallScanActivity : CaptureActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var btnMissing: Button
    private lateinit var scanLogRepository: ScanLogRepository

    private lateinit var factoryLocalStore: FactoryLocalStore
    private var currentMode: String = "GIVE"
    private var sdMissingPressed = false
    private var lastScannedText: String? = null
    private var lastScanTime: Long = 0
    private lateinit var currentFactoryName: String;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_small_scan)

        scanLogRepository = ScanLogRepository(this)
        factoryLocalStore = FactoryLocalStore(this)
        currentMode = intent.getStringExtra("MODE") ?: "GIVE"

        btnMissing = findViewById(R.id.btnSdMissing)
        barcodeView = findViewById(R.id.barcode_scanner)

        btnMissing.visibility = if (currentMode == "TAKE") View.VISIBLE else View.GONE

        btnMissing.setOnClickListener {
            sdMissingPressed = true
            scanLogRepository.upsertScan(
                deviceId = lastScannedText as String,
                mode = currentMode,
                userId = factoryLocalStore.getDeviceId(),
                factoryId = factoryLocalStore.getFactoryId(factoryLocalStore.getCurrentFactory()),
                isSdMissing = sdMissingPressed
            )
            val data = scanLogRepository.getDeviceDetails(lastScannedText as String)
            Toast.makeText(this, "Marked SD Missing", Toast.LENGTH_SHORT).show()
        }

        barcodeView.barcodeView.framingRectSize = Size(400, 400)
        barcodeView.decodeContinuous {
            val qrText = it.text
            val currentTime = System.currentTimeMillis()
            if (qrText == lastScannedText && currentTime - lastScanTime < 1000) {
                return@decodeContinuous
            }

            lastScannedText = qrText
            lastScanTime = currentTime
            val sdStatus = if (sdMissingPressed) "MISSING" else "PRESENT"
            sdMissingPressed = false
            scanLogRepository.upsertScan(
                deviceId = qrText,
                mode = currentMode,
                userId = factoryLocalStore.getDeviceId(),
                factoryId = factoryLocalStore.getFactoryId(factoryLocalStore.getCurrentFactory()),
                isSdMissing = sdMissingPressed
            )
            scanLogRepository.generateDeviceIdIndexDict(lastScannedText)
            Log.d("IndexforDeviceId", scanLogRepository.getDeviceIdIndex(lastScannedText).toString())
            val data = scanLogRepository.getDeviceDetails(lastScannedText as String)
            runOnUiThread {
                Toast.makeText(this, "Scanned: $qrText", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}
