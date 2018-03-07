@file:Suppress("PackageName")

package dolphin.android.wear.SimpleComplexFace

import android.content.Context
import android.preference.PreferenceManager

/**
 * Configurations
 */
class Configs(context: Context) {
    companion object {
        const val COMPLICATION_ID_BACKGROUND = 9000
        const val COMPLICATION_ID_LEFT = 9001
        const val COMPLICATION_ID_RIGHT = 9002
        const val COMPLICATION_ID_BOTTOM = 9003

        private const val KEY_BATTERY_RING = "battery_ring"
        private const val KEY_DIGITAL_TIME = "digital_time"
        private const val KEY_SECOND_HAND = "second_hand"
        private const val KEY_ENABLE_TAP = "enable_tap"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    var batteryRingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_RING, true)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_RING, value).apply()

    var digitalTimeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIGITAL_TIME, true)
        set(value) = prefs.edit().putBoolean(KEY_DIGITAL_TIME, value).apply()

    var secondHandEnabled: Boolean
        get() = prefs.getBoolean(KEY_SECOND_HAND, true)
        set(value) = prefs.edit().putBoolean(KEY_SECOND_HAND, value).apply()

    var tapComplicationEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_TAP, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_TAP, value).apply()
}