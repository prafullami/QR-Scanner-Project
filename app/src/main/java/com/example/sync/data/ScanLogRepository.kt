package com.example.sync.data

import android.content.Context
import com.example.sync.ScanRecord
import java.io.File

class ScanLogRepository(private val context: Context) {

    fun appendScan(qr: String, mode: String, sdStatus: String, timestamp: Long = System.currentTimeMillis()) {
        val line = "$qr | $mode | $sdStatus | $timestamp\n"
        context.openFileOutput(FILE_NAME, Context.MODE_APPEND).use { output ->
            output.write(line.toByteArray())
        }
    }

    fun loadRecords(): List<ScanRecord> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        val aggregated = linkedMapOf<String, ScanRecord>()
        file.forEachLine { line ->
            val parts = line.split(" | ")
            if (parts.size < 3) return@forEachLine

            val qr = parts[0]
            val mode = parts[1]
            val status = parts[2]
            val record = aggregated.getOrPut(qr) { ScanRecord(qr = qr) }

            when (mode) {
                "GIVE" -> record.given = true
                "TAKE" -> {
                    record.taken = true
                    if (status == "MISSING") {
                        record.sdMissing = true
                    }
                }
            }
        }
        return aggregated.values.toList()
    }

    companion object {
        private const val FILE_NAME = "scan_log.txt"
    }
}
