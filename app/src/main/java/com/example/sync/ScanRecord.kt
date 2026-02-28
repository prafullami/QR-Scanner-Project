package com.example.sync

data class ScanRecord(
    val deviceId: String,
    var givenById: String = "",
    var givenTime: String = "",
    var takenById: String = "",
    var takenTime: String = "",
    var sdCardMissingTime: String = "",
    var factoryId: String = "",
    var recordingDate: String = ""
)