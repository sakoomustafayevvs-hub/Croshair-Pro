package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.CrosshairConfig
import com.example.model.CrosshairPreset
import com.example.model.CrosshairStyle
import org.json.JSONArray
import org.json.JSONObject

class CrosshairPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("crosshair_prefs", Context.MODE_PRIVATE)

    companion object {
        const val ACTION_CROSSHAIR_UPDATED = "com.example.ACTION_CROSSHAIR_UPDATED"
        private const val KEY_STYLE = "style"
        private const val KEY_COLOR = "color"
        private const val KEY_SIZE = "size"
        private const val KEY_STROKE_WIDTH = "stroke_width"
        private const val KEY_GAP = "gap"
        private const val KEY_DOT_SIZE = "dot_size"
        private const val KEY_SHOW_DOT = "show_dot"
        private const val KEY_HAS_OUTLINE = "has_outline"
        private const val KEY_OUTLINE_COLOR = "outline_color"
        private const val KEY_OPACITY = "opacity"
        private const val KEY_OFFSET_X = "offset_x"
        private const val KEY_OFFSET_Y = "offset_y"
        private const val KEY_SHOW_FLOATING_SQUARE = "show_floating_square"
        private const val KEY_SQUARE_X = "square_x"
        private const val KEY_SQUARE_Y = "square_y"
        private const val KEY_PRESETS_JSON = "custom_presets_json"
    }

    fun getConfig(): CrosshairConfig {
        val styleName = prefs.getString(KEY_STYLE, CrosshairStyle.CLASSIC_CROSS.name) ?: CrosshairStyle.CLASSIC_CROSS.name
        val style = try {
            CrosshairStyle.valueOf(styleName)
        } catch (e: Exception) {
            CrosshairStyle.CLASSIC_CROSS
        }

        return CrosshairConfig(
            style = style,
            color = prefs.getLong(KEY_COLOR, 0xFF00FF66L),
            sizeDp = prefs.getFloat(KEY_SIZE, 36f),
            strokeWidthDp = prefs.getFloat(KEY_STROKE_WIDTH, 3f),
            gapDp = prefs.getFloat(KEY_GAP, 6f),
            dotSizeDp = prefs.getFloat(KEY_DOT_SIZE, 4f),
            showDot = prefs.getBoolean(KEY_SHOW_DOT, true),
            hasOutline = prefs.getBoolean(KEY_HAS_OUTLINE, true),
            outlineColor = prefs.getLong(KEY_OUTLINE_COLOR, 0xFF000000L),
            opacity = prefs.getFloat(KEY_OPACITY, 1.0f),
            offsetX = prefs.getInt(KEY_OFFSET_X, 0),
            offsetY = prefs.getInt(KEY_OFFSET_Y, 0),
            showFloatingSquare = prefs.getBoolean(KEY_SHOW_FLOATING_SQUARE, true),
            squareX = prefs.getInt(KEY_SQUARE_X, 40),
            squareY = prefs.getInt(KEY_SQUARE_Y, 200)
        )
    }

    fun saveConfig(config: CrosshairConfig) {
        prefs.edit()
            .putString(KEY_STYLE, config.style.name)
            .putLong(KEY_COLOR, config.color)
            .putFloat(KEY_SIZE, config.sizeDp)
            .putFloat(KEY_STROKE_WIDTH, config.strokeWidthDp)
            .putFloat(KEY_GAP, config.gapDp)
            .putFloat(KEY_DOT_SIZE, config.dotSizeDp)
            .putBoolean(KEY_SHOW_DOT, config.showDot)
            .putBoolean(KEY_HAS_OUTLINE, config.hasOutline)
            .putLong(KEY_OUTLINE_COLOR, config.outlineColor)
            .putFloat(KEY_OPACITY, config.opacity)
            .putInt(KEY_OFFSET_X, config.offsetX)
            .putInt(KEY_OFFSET_Y, config.offsetY)
            .putBoolean(KEY_SHOW_FLOATING_SQUARE, config.showFloatingSquare)
            .putInt(KEY_SQUARE_X, config.squareX)
            .putInt(KEY_SQUARE_Y, config.squareY)
            .apply()
    }

    fun getDefaultPresets(): List<CrosshairPreset> {
        return listOf(
            CrosshairPreset(
                id = "pubg_sniper",
                name = "PUBG Snayper",
                gameName = "PUBG Mobile",
                config = CrosshairConfig(
                    style = CrosshairStyle.SNIPER,
                    color = 0xFFFF0055L, // Red
                    sizeDp = 42f,
                    strokeWidthDp = 2.5f,
                    gapDp = 8f,
                    dotSizeDp = 4f,
                    showDot = true,
                    hasOutline = true
                )
            ),
            CrosshairPreset(
                id = "csgo_classic",
                name = "CS2 / CS:GO Klasik",
                gameName = "Counter-Strike",
                config = CrosshairConfig(
                    style = CrosshairStyle.CLASSIC_CROSS,
                    color = 0xFF00FF66L, // Green
                    sizeDp = 32f,
                    strokeWidthDp = 3f,
                    gapDp = 6f,
                    dotSizeDp = 0f,
                    showDot = false,
                    hasOutline = true
                )
            ),
            CrosshairPreset(
                id = "valorant_dot",
                name = "Valorant Nöqtə",
                gameName = "Valorant Mobile",
                config = CrosshairConfig(
                    style = CrosshairStyle.DOT,
                    color = 0xFF00E5FFL, // Cyan
                    sizeDp = 24f,
                    strokeWidthDp = 2f,
                    gapDp = 0f,
                    dotSizeDp = 8f,
                    showDot = true,
                    hasOutline = true
                )
            ),
            CrosshairPreset(
                id = "apex_halo",
                name = "Apex Halo Ring",
                gameName = "Apex Legends",
                config = CrosshairConfig(
                    style = CrosshairStyle.HALO_RING,
                    color = 0xFFFFCC00L, // Yellow
                    sizeDp = 40f,
                    strokeWidthDp = 3f,
                    gapDp = 10f,
                    dotSizeDp = 5f,
                    showDot = true,
                    hasOutline = true
                )
            ),
            CrosshairPreset(
                id = "freefire_tactical",
                name = "Free Fire Taktiki",
                gameName = "Free Fire",
                config = CrosshairConfig(
                    style = CrosshairStyle.CIRCLE_CROSS,
                    color = 0xFFFF6D00L, // Orange
                    sizeDp = 38f,
                    strokeWidthDp = 2.5f,
                    gapDp = 7f,
                    dotSizeDp = 4f,
                    showDot = true,
                    hasOutline = true
                )
            ),
            CrosshairPreset(
                id = "cod_chevron",
                name = "COD Mobile Chevron",
                gameName = "Call of Duty",
                config = CrosshairConfig(
                    style = CrosshairStyle.TRIANGLE,
                    color = 0xFFFF0033L, // Neon Red
                    sizeDp = 34f,
                    strokeWidthDp = 3f,
                    gapDp = 4f,
                    dotSizeDp = 3f,
                    showDot = true,
                    hasOutline = true
                )
            )
        )
    }

    fun getCustomPresets(): List<CrosshairPreset> {
        val jsonStr = prefs.getString(KEY_PRESETS_JSON, null) ?: return emptyList()
        val list = mutableListOf<CrosshairPreset>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val gameName = obj.optString("gameName", "Xüsusi")
                val cfgObj = obj.getJSONObject("config")

                val config = CrosshairConfig(
                    style = CrosshairStyle.valueOf(cfgObj.optString("style", CrosshairStyle.CLASSIC_CROSS.name)),
                    color = cfgObj.optLong("color", 0xFF00FF66L),
                    sizeDp = cfgObj.optDouble("sizeDp", 36.0).toFloat(),
                    strokeWidthDp = cfgObj.optDouble("strokeWidthDp", 3.0).toFloat(),
                    gapDp = cfgObj.optDouble("gapDp", 6.0).toFloat(),
                    dotSizeDp = cfgObj.optDouble("dotSizeDp", 4.0).toFloat(),
                    showDot = cfgObj.optBoolean("showDot", true),
                    hasOutline = cfgObj.optBoolean("hasOutline", true),
                    outlineColor = cfgObj.optLong("outlineColor", 0xFF000000L),
                    opacity = cfgObj.optDouble("opacity", 1.0).toFloat()
                )
                list.add(CrosshairPreset(id, name, gameName, config))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveCustomPreset(preset: CrosshairPreset) {
        val list = getCustomPresets().toMutableList()
        list.removeAll { it.id == preset.id }
        list.add(0, preset)

        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("gameName", item.gameName)

            val cfgObj = JSONObject()
            cfgObj.put("style", item.config.style.name)
            cfgObj.put("color", item.config.color)
            cfgObj.put("sizeDp", item.config.sizeDp)
            cfgObj.put("strokeWidthDp", item.config.strokeWidthDp)
            cfgObj.put("gapDp", item.config.gapDp)
            cfgObj.put("dotSizeDp", item.config.dotSizeDp)
            cfgObj.put("showDot", item.config.showDot)
            cfgObj.put("hasOutline", item.config.hasOutline)
            cfgObj.put("outlineColor", item.config.outlineColor)
            cfgObj.put("opacity", item.config.opacity)

            obj.put("config", cfgObj)
            jsonArray.put(obj)
        }

        prefs.edit().putString(KEY_PRESETS_JSON, jsonArray.toString()).apply()
    }

    fun deleteCustomPreset(presetId: String) {
        val list = getCustomPresets().filterNot { it.id == presetId }
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("gameName", item.gameName)

            val cfgObj = JSONObject()
            cfgObj.put("style", item.config.style.name)
            cfgObj.put("color", item.config.color)
            cfgObj.put("sizeDp", item.config.sizeDp)
            cfgObj.put("strokeWidthDp", item.config.strokeWidthDp)
            cfgObj.put("gapDp", item.config.gapDp)
            cfgObj.put("dotSizeDp", item.config.dotSizeDp)
            cfgObj.put("showDot", item.config.showDot)
            cfgObj.put("hasOutline", item.config.hasOutline)
            cfgObj.put("outlineColor", item.config.outlineColor)
            cfgObj.put("opacity", item.config.opacity)

            obj.put("config", cfgObj)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PRESETS_JSON, jsonArray.toString()).apply()
    }
}
