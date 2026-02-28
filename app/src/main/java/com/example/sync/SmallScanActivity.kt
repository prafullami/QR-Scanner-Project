package com.example.sync
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.Size

class SmallScanActivity : CaptureActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var btnMissing: Button
    private var currentMode: String = "GIVE"
    private var sdMissingPressed = false
    private var lastScannedText: String? = null
    private var lastScanTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_small_scan)
        currentMode = intent.getStringExtra("MODE") ?: "GIVE"
        btnMissing = findViewById(R.id.btnSdMissing)
        barcodeView = findViewById(R.id.barcode_scanner)

        if (currentMode == "TAKE") {
            btnMissing.visibility = View.VISIBLE
        } else {
            btnMissing.visibility = View.GONE
        }

        btnMissing.setOnClickListener {
            sdMissingPressed = true
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
            setResult(RESULT_OK, intent.putExtra("SCAN_RESULT", qrText))
            val sdStatus = if (sdMissingPressed) "MISSING" else "PRESENT"
            sdMissingPressed = false // reset for next scan
            runOnUiThread {
                Toast.makeText(this, "Scanned: $qrText", Toast.LENGTH_SHORT).show()
            }
            saveQrData(qrText, currentMode, sdStatus)
        }

    }


    private fun saveQrData(qr: String, mode: String, sdStatus: String) {
        val fileName = "scan_log.txt"
        val fileOutput = openFileOutput(fileName, MODE_APPEND)

        val time = System.currentTimeMillis()
        val data = "$qr | $mode | $sdStatus | $time\n"

        fileOutput.write(data.toByteArray())
        fileOutput.close()
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