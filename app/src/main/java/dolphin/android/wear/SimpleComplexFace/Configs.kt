@file:Suppress("PackageName")

package dolphin.android.wear.SimpleComplexFace

import android.content.Context
import android.graphics.Color
import android.preference.PreferenceManager

/**
 * Configurations
 */
class Configs(private val context: Context) {
    companion object {
        const val COMPLICATION_ID_BACKGROUND = 9000
        const val COMPLICATION_ID_LEFT = 9001
        const val COMPLICATION_ID_RIGHT = 9002
        const val COMPLICATION_ID_BOTTOM = 9003
        const val COMPLICATION_ID_TOP = 9004

        private const val KEY_SECOND_HAND = "second_hand"
        private const val KEY_BATTERY_RING = "battery_ring"
        private const val KEY_BATTERY_TEXT = "battery_text"
        private const val KEY_ANALOG_TICK = "analog_ticker"
        private const val KEY_DIGITAL_TIME = "digital_time"
        private const val KEY_ENABLE_TAP = "enable_tap"
        private const val KEY_COLOR = "main_tap"
        private const val KEY_VIBRATE_LOW_BATTERY = "vibrate_low_battery"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    var secondHandEnabled: Boolean
        get() = prefs.getBoolean(KEY_SECOND_HAND, true)
        set(value) = prefs.edit().putBoolean(KEY_SECOND_HAND, value).apply()

    var batteryRingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_RING, true)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_RING, value).apply()

    var batteryTextEnabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_TEXT, true)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_TEXT, value).apply()

    var analogTickEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANALOG_TICK, false)
        set(value) = prefs.edit().putBoolean(KEY_ANALOG_TICK, value).apply()

    var digitalTimeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DIGITAL_TIME, true)
        set(value) = prefs.edit().putBoolean(KEY_DIGITAL_TIME, value).apply()

    var tapComplicationEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_TAP, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_TAP, value).apply()

    val COLOR_WHITE: Int
        get() = Color.WHITE

    val COLOR_RED: Int
        get() = context.getColor(R.color.oval_color_red)

    val COLOR_GREEN: Int
        get() = context.getColor(R.color.oval_color_green)

    val COLOR_BLUE: Int
        get() = context.getColor(R.color.oval_color_blue)

    val COLOR_ORANGE: Int
        get() = context.getColor(R.color.oval_color_orange)

    val COLOR_PURPLE: Int
        get() = context.getColor(R.color.oval_color_purple)

    fun getColorDrawable(color: Int) = when (color) {
        COLOR_RED -> R.drawable.color_oval_red
        COLOR_GREEN -> R.drawable.color_oval_green
        COLOR_BLUE -> R.drawable.color_oval_blue
        COLOR_PURPLE -> R.drawable.color_oval_purple
        COLOR_ORANGE -> R.drawable.color_oval_orange
        else -> R.drawable.color_oval_white
    }

    var clockMainColor: Int
        get() = prefs.getInt(KEY_COLOR, COLOR_WHITE)
        set(value) = prefs.edit().putInt(KEY_COLOR, value).apply()

    var vibratorEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE_LOW_BATTERY, false)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE_LOW_BATTERY, value).apply()
}