package com.example.data.local

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray

/**
 * Manages unit options across the app (Stock screen and Sell screen).
 * Includes standard defaults and persists user-defined custom units via SharedPreferences.
 */
object UnitManager {
    // 2. Include these units: pcs, kg, gram, litre, meter, feet, inch, box, packet, dozen, bag, roll, set, pair, carton, bundle.
    val DEFAULT_UNITS = listOf(
        "pcs",
        "kg",
        "gram",
        "litre",
        "meter",
        "feet",
        "inch",
        "box",
        "packet",
        "dozen",
        "bag",
        "roll",
        "set",
        "pair",
        "carton",
        "bundle"
    )

    private val _customUnits = mutableStateListOf<String>()
    val customUnits: List<String> get() = _customUnits

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences("hardware_store_prefs", Context.MODE_PRIVATE)
        val savedJson = prefs.getString("custom_units_list", null)
        _customUnits.clear()
        if (!savedJson.isNullOrBlank()) {
            try {
                val array = JSONArray(savedJson)
                for (i in 0 until array.length()) {
                    val u = array.getString(i).trim()
                    if (u.isNotBlank() && !DEFAULT_UNITS.any { it.equals(u, ignoreCase = true) } && !_customUnits.contains(u)) {
                        _customUnits.add(u)
                    }
                }
            } catch (_: Exception) {
            }
        }
        isInitialized = true
    }

    /**
     * Returns full list of units: standard defaults + saved custom units + current product unit if any.
     */
    fun getAllUnits(extraUnit: String? = null): List<String> {
        val result = mutableListOf<String>()
        result.addAll(DEFAULT_UNITS)
        for (custom in _customUnits) {
            if (!result.any { it.equals(custom, ignoreCase = true) }) {
                result.add(custom)
            }
        }
        if (!extraUnit.isNullOrBlank()) {
            val trimmed = extraUnit.trim()
            if (!result.any { it.equals(trimmed, ignoreCase = true) }) {
                result.add(trimmed)
            }
        }
        return result
    }

    /**
     * Saves a new custom unit to SharedPreferences and updates the active list for next time.
     */
    fun addCustomUnit(context: Context?, newUnit: String): String {
        val trimmed = newUnit.trim()
        if (trimmed.isBlank()) return trimmed

        val existingDefault = DEFAULT_UNITS.firstOrNull { it.equals(trimmed, ignoreCase = true) }
        if (existingDefault != null) return existingDefault

        val existingCustom = _customUnits.firstOrNull { it.equals(trimmed, ignoreCase = true) }
        if (existingCustom != null) return existingCustom

        _customUnits.add(trimmed)
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences("hardware_store_prefs", Context.MODE_PRIVATE)
                val array = JSONArray()
                for (u in _customUnits) {
                    array.put(u)
                }
                prefs.edit().putString("custom_units_list", array.toString()).apply()
            } catch (_: Exception) {
            }
        }
        return trimmed
    }

    /**
     * Helper to return bilingual display text for dropdown rows.
     */
    fun getDisplayLabel(unit: String, isBangla: Boolean): String {
        val bn = getBanglaUnit(unit)
        return if (isBangla) {
            if (bn.equals(unit, ignoreCase = true)) unit else "$unit ($bn)"
        } else {
            unit
        }
    }

    fun getBanglaUnit(unit: String): String {
        return when (unit.lowercase().trim()) {
            "pcs", "pc", "piece", "pieces" -> "টি"
            "kg" -> "কেজি"
            "gram", "gm" -> "গ্রাম"
            "litre", "liter", "l" -> "লিটার"
            "meter", "m" -> "মিটার"
            "feet", "ft" -> "ফুট"
            "inch", "in" -> "ইঞ্চি"
            "box" -> "বক্স"
            "packet", "pkt" -> "প্যাকেট"
            "dozen", "dz" -> "ডজন"
            "bag" -> "ব্যাগ"
            "roll" -> "রোল"
            "set" -> "সেট"
            "pair" -> "জোড়া"
            "carton", "ctn" -> "কার্টন"
            "bundle" -> "বান্ডিল"
            "cans", "can" -> "ক্যান"
            else -> unit
        }
    }
}
