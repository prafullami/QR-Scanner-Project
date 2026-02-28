package com.example.sync.data

import android.content.Context
import android.content.SharedPreferences

class FactoryLocalStore(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeviceId(): String = preferences.getString(KEY_DEVICE_ID, DEFAULT_DEVICE_ID) ?: DEFAULT_DEVICE_ID

    fun saveDeviceId(deviceId: String) {
        preferences.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .apply()
    }

    fun getFactories(): Set<String> = preferences.getStringSet(KEY_FACTORIES, emptySet()) ?: emptySet()

    fun addFactory(factoryName: String) {
        val updatedFactories = getFactories().toMutableSet()
        updatedFactories.add(factoryName)
        saveFactories(updatedFactories)
    }

    fun replaceFactories(factories: Collection<String>) {
        saveFactories(factories.toSet())
    }

    fun saveCurrentFactory(factoryName: String?) {
        preferences.edit()
            .putString(KEY_CURRENT_FACTORY, factoryName)
            .apply()
    }

    fun getCurrentFactory(): String {
        return preferences.getString(KEY_CURRENT_FACTORY, null)?: ""
    }

    private fun saveFactories(factories: Set<String>) {
        preferences.edit()
            .putStringSet(KEY_FACTORIES, factories)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_FACTORIES = "factories"
        private const val DEFAULT_DEVICE_ID = "No ID"
        private const val KEY_CURRENT_FACTORY = "current_factory"
    }
}
