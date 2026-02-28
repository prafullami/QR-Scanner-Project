package com.example.sync

data class ScanRecord(
    val qr: String,
    var given: Boolean = false,
    var taken: Boolean = false,
    var sdMissing: Boolean = false
)