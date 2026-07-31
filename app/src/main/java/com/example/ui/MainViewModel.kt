package com.example.ui

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import com.example.data.CrosshairPreferences
import com.example.model.CrosshairConfig
import com.example.model.CrosshairPreset
import com.example.service.CrosshairOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = CrosshairPreferences(application)

    private val _config = MutableStateFlow(prefs.getConfig())
    val config: StateFlow<CrosshairConfig> = _config.asStateFlow()

    private val _hasOverlayPermission = MutableStateFlow(checkOverlayPermission())
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _presets = MutableStateFlow<List<CrosshairPreset>>(emptyList())
    val presets: StateFlow<List<CrosshairPreset>> = _presets.asStateFlow()

    init {
        loadPresets()
    }

    fun refreshPermission() {
        _hasOverlayPermission.value = checkOverlayPermission()
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(getApplication())
    }

    fun updateConfig(newConfig: CrosshairConfig) {
        _config.value = newConfig
        prefs.saveConfig(newConfig)

        // If service is running, send intent to update live overlay
        if (_isServiceRunning.value) {
            val intent = Intent(getApplication(), CrosshairOverlayService::class.java)
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    getApplication<Application>().startForegroundService(intent)
                } else {
                    getApplication<Application>().startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleOverlayService() {
        val app = getApplication<Application>()
        if (_isServiceRunning.value) {
            val intent = Intent(app, CrosshairOverlayService::class.java).apply {
                action = CrosshairOverlayService.ACTION_STOP
            }
            try {
                app.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isServiceRunning.value = false
        } else {
            if (!checkOverlayPermission()) {
                _hasOverlayPermission.value = false
                return
            }
            try {
                val intent = Intent(app, CrosshairOverlayService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    app.startForegroundService(intent)
                } else {
                    app.startService(intent)
                }
                _isServiceRunning.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _isServiceRunning.value = false
            }
        }
    }

    fun loadPresets() {
        val defaults = prefs.getDefaultPresets()
        val customs = prefs.getCustomPresets()
        _presets.value = defaults + customs
    }

    fun applyPreset(preset: CrosshairPreset) {
        updateConfig(preset.config)
    }

    fun saveCustomPreset(name: String, gameName: String) {
        val newPreset = CrosshairPreset(
            id = "custom_" + System.currentTimeMillis(),
            name = name,
            gameName = if (gameName.isBlank()) "Xüsusi Oyun" else gameName,
            config = _config.value
        )
        prefs.saveCustomPreset(newPreset)
        loadPresets()
    }

    fun deleteCustomPreset(presetId: String) {
        prefs.deleteCustomPreset(presetId)
        loadPresets()
    }
}
