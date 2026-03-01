package com.example.sync.data

import android.content.Context
import android.content.SharedPreferences

class FactoryLocalStore(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------------- DEVICE ID ----------------

    fun getDeviceId(): String =
        preferences.getString(KEY_DEVICE_ID, DEFAULT_DEVICE_ID) ?: DEFAULT_DEVICE_ID

    fun hasDeviceId(): Boolean =
        getDeviceId() != DEFAULT_DEVICE_ID

    fun saveDeviceId(deviceId: String) {
        preferences.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .apply()
    }

    // ---------------- FACTORIES ----------------

    fun getFactories(): Set<String> =
        preferences.getStringSet(KEY_FACTORIES, emptySet()) ?: emptySet()

    /**
     * Stores factory as factoryName:factoryId
     */
    fun addFactory(factoryName: String, factoryId: String) {

        val updatedFactories = getFactories().toMutableSet()

        // Remove old entry if same factory name already exists
        updatedFactories.removeAll { it.startsWith("$factoryName:") }

        val entry = "$factoryName:$factoryId"
        updatedFactories.add(entry)

        saveFactories(updatedFactories)
    }

    /**
     * Returns factoryId by factoryName
     */
    fun getFactoryId(factoryName: String): String? {

        return getFactories()
            .firstOrNull { it.startsWith("$factoryName:") }
            ?.substringAfter(":")
    }

    /**
     * Returns only factory names (useful for Spinner)
     */
    fun getFactoryNames(): List<String> {
        return getFactories()
            .map { it.substringBefore(":") }
    }

    fun replaceFactories(factories: Collection<String>) {
        saveFactories(factories.toSet())
    }

    private fun saveFactories(factories: Set<String>) {
        preferences.edit()
            .putStringSet(KEY_FACTORIES, factories)
            .apply()
    }

    // ---------------- CURRENT FACTORY ----------------

    fun saveCurrentFactory(factoryName: String?) {
        preferences.edit()
            .putString(KEY_CURRENT_FACTORY, factoryName)
            .apply()
    }

    fun getCurrentFactory(): String {
        return preferences.getString(KEY_CURRENT_FACTORY, null) ?: ""
    }

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_FACTORIES = "factories"
        private const val KEY_CURRENT_FACTORY = "current_factory"
        private const val DEFAULT_DEVICE_ID = "No ID"
    }
}