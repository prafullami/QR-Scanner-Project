package com.example.sync.data

import android.content.Context
import android.content.SharedPreferences
import com.example.sync.DisplayItem
import com.example.sync.ScanRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDate

class ScanLogRepository(private val context: Context) {
    val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    fun upsertScan(
        deviceId: String,
        mode: String,
        userId: String,
        factoryId: String?,
        isSdMissing: Boolean
    ) {
        val factoryId1 = factoryId
        val file = File(context.filesDir, FILE_NAME)

        val jsonArray = if (file.exists()) {
            JSONArray(file.readText())
        } else {
            JSONArray()
        }

        var existingObject: JSONObject? = null
        var existingIndex = -1

        // 🔎 Check if deviceId already exists
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            if (obj.getString("deviceId") == deviceId) {
                existingObject = obj
                existingIndex = i
                if(mode == "TAKE") {
                    val allDevices = loadRecords()
                    allDevices.get(getDeviceIdIndex(deviceId) as Int).givenById = userId
                    allDevices.get(getDeviceIdIndex(deviceId) as Int).givenTime = Instant.now().toString()
                    if(isSdMissing) allDevices.get(getDeviceIdIndex(deviceId) as Int).sdCardMissingTime = Instant.now().toString()
                    val jsonArray = JSONArray(allDevices)
                    file.writeText(jsonArray.toString(2))
                }
                break
            }
        }

        val nowInstant = Instant.now().toString()
        val todayDate = LocalDate.now().toString()

        // 🆕 If NOT found → create new object
        if (existingObject == null) {

            existingObject = JSONObject().apply {
                put("deviceId", deviceId)
                put("givenById", "")
                put("givenTime", "")
                put("takenById", "")
                put("takenTime", "")
                put("sdCardMissingTime", "")
                put("factoryId", factoryId)
                put("recordingDate", todayDate)
            }

            jsonArray.put(existingObject)

        }

        // 🔄 Update fields
        when (mode) {

            "GIVE" -> {
                existingObject.put("givenById", userId)
                existingObject.put("givenTime", nowInstant)
            }

            "TAKE" -> {
                existingObject.put("takenById", userId)
                existingObject.put("takenTime", nowInstant)

                if (isSdMissing) {
                    existingObject.put("sdCardMissingTime", nowInstant)
                }
            }
        }

        // 💾 Save back to file
        file.writeText(jsonArray.toString(2))

    }

    fun loadRecords(): List<ScanRecord> {

        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        val jsonArray = JSONArray(file.readText())
        val list = mutableListOf<ScanRecord>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            list.add(
                ScanRecord(
                    deviceId = obj.getString("deviceId"),
                    givenById = obj.getString("givenById"),
                    givenTime = obj.getString("givenTime"),
                    takenById = obj.getString("takenById"),
                    takenTime = obj.getString("takenTime"),
                    sdCardMissingTime = obj.getString("sdCardMissingTime"),
                    factoryId = obj.getString("factoryId"),
                    recordingDate = obj.getString("recordingDate")
                )
            )
        }

        return list
    }

    fun generateDeviceIdIndexDict(currentScannedText:String?) {

        val allRecords = loadRecords();
        val dict = preferences.getString("devIdIndex","{}")
        if(dict == "{}") {
            val jsonObject = JSONObject();
            jsonObject.put(currentScannedText,0)
            preferences.edit().putString("devIdIndex",jsonObject.toString()).apply()
        }
         else {
            val jsonObject = JSONObject(dict);
            jsonObject.put(currentScannedText, allRecords.size - 1)
            preferences.edit().putString("devIdIndex",jsonObject.toString()).apply()
        }
    }

    fun getDeviceIdIndex(deviceId: String?): Int? {
        val dict = preferences.getString("devIdIndex","{}")
        if(dict == "{}") {
            return null
        }
        else {
            val jsonObject = JSONObject(dict);
            return jsonObject.get(deviceId) as Int
        }
    }

    fun getDisplayData(): List<DisplayItem> {

        return loadRecords().map { record ->

            val givenTime = if (record.takenTime.isNotEmpty()) {
                "TAKEN"
            } else {
                "GIVEN"
            }

            DisplayItem(
                deviceId = record.deviceId,
                givenTime = record.givenTime,
                takenTime = record.takenTime,
                sdMissingTime = record.sdCardMissingTime.ifEmpty { "" }
            )
        }
    }

    fun getDeviceDetails(deviceId: String): ScanRecord {
        return loadRecords().get(getDeviceIdIndex(deviceId) as Int)
    }

    companion object {
        private const val FILE_NAME = "scan_log.json"
        private const val PREFS_NAME = "app_prefs"
    }
}