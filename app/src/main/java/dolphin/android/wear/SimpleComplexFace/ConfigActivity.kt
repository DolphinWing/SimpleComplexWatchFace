@file:Suppress("PackageName", "PrivatePropertyName")

package dolphin.android.wear.SimpleComplexFace

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.widget.RecyclerView
import android.support.wear.widget.WearableLinearLayoutManager
import android.support.wear.widget.WearableRecyclerView
import android.support.wearable.activity.WearableActivity
import android.support.wearable.complications.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.concurrent.Executors

class ConfigActivity : WearableActivity(), View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    companion object {
        private const val TAG = "ConfigActivity"
    }

    private var mWearableRecyclerView: WearableRecyclerView? = null
    private var mMyAdapter: MyAdapter? = null
    private lateinit var mWatchFaceComponentName: ComponentName
    private lateinit var mProviderInfoRetriever: ProviderInfoRetriever
    private lateinit var mConfigs: Configs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_config1)
        setContentView(R.layout.activity_config2)

        // Enables Always-on
        setAmbientEnabled()

        mWatchFaceComponentName = ComponentName(this, MyWatchFace::class.java)
        mConfigs = Configs(this)

        mWearableRecyclerView = findViewById(R.id.recycler_config_view)
        mWearableRecyclerView?.let {
            mMyAdapter = MyAdapter(this)
            it.adapter = mMyAdapter
            it.layoutManager = WearableLinearLayoutManager(this, CustomScrollingLayoutCallback())
            it.isEdgeItemsCenteringEnabled = true
            it.setHasFixedSize(true)
        }

        findViewById<View>(R.id.icon1)?.setOnClickListener(this)
        findViewById<View>(R.id.icon2)?.setOnClickListener(this)
        findViewById<View>(R.id.icon3)?.setOnClickListener(this)
        findViewById<CheckBox>(R.id.checkbox1)?.let {
            it.setOnCheckedChangeListener(this)
            it.isChecked = mConfigs.secondHandEnabled
        }
        findViewById<CheckBox>(R.id.checkbox2)?.let {
            it.setOnCheckedChangeListener(this)
            it.isChecked = mConfigs.batteryRingEnabled
        }
        findViewById<CheckBox>(R.id.checkbox3)?.let {
            it.setOnCheckedChangeListener(this)
            it.isChecked = mConfigs.digitalTimeEnabled
        }
        findViewById<CheckBox>(R.id.checkbox4)?.let {
            it.setOnCheckedChangeListener(this)
            it.isChecked = mConfigs.tapComplicationEnabled
        }

        mProviderInfoRetriever = ProviderInfoRetriever(this, Executors.newSingleThreadExecutor())
        mProviderInfoRetriever.init()
        mProviderInfoRetriever.retrieveProviderInfo(object : ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
            override fun onProviderInfoReceived(complicationId: Int, providerInfo: ComplicationProviderInfo?) {
                updateComplicationViews(complicationId, providerInfo)
            }

        }, mWatchFaceComponentName,
                Configs.COMPLICATION_ID_LEFT, Configs.COMPLICATION_ID_RIGHT, Configs.COMPLICATION_ID_BOTTOM)
    }

    override fun onDestroy() {
        super.onDestroy()
        mProviderInfoRetriever.release()
    }

    inner class CustomScrollingLayoutCallback : WearableLinearLayoutManager.LayoutCallback() {
//        /** How much should we scale the icon at most.  */
//        private val MAX_ICON_PROGRESS = 0.65f
//
//        private var mProgressToCenter: Float = 0.toFloat()

        override fun onLayoutFinished(child: View, parent: RecyclerView) {
//            // Figure out % progress from top to bottom
//            val centerOffset = child.height.toFloat() / 2.0f / parent.height.toFloat()
//            val yRelativeToCenterOffset = child.y / parent.height + centerOffset
//
//            // Normalize for center
//            mProgressToCenter = Math.abs(0.5f - yRelativeToCenterOffset)
//            // Adjust to the maximum scale
//            mProgressToCenter = Math.min(mProgressToCenter, MAX_ICON_PROGRESS)
//
//            child.scaleX = 1 - mProgressToCenter
//            child.scaleY = 1 - mProgressToCenter
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.icon1 -> launchComplicationChooser(Configs.COMPLICATION_ID_LEFT)
            R.id.icon2 -> launchComplicationChooser(Configs.COMPLICATION_ID_RIGHT)
            R.id.icon3 -> launchComplicationChooser(Configs.COMPLICATION_ID_BOTTOM)
        }
    }

    override fun onCheckedChanged(view: CompoundButton?, checked: Boolean) {
        when (view?.id) {
            R.id.checkbox1 -> mConfigs.secondHandEnabled = checked
            R.id.checkbox2 -> mConfigs.batteryRingEnabled = checked
            R.id.checkbox3 -> mConfigs.digitalTimeEnabled = checked
            R.id.checkbox4 -> mConfigs.tapComplicationEnabled = checked
        }
    }

    private fun launchComplicationChooser(watchFaceComplicationId: Int) {
        //val watchFace = ComponentName(this, MyWatchFace::class.java)
        startActivityForResult(
                ComplicationHelperActivity.createProviderChooserHelperIntent(
                        this,
                        mWatchFaceComponentName,
                        watchFaceComplicationId,
                        ComplicationData.TYPE_SHORT_TEXT,
                        ComplicationData.TYPE_SMALL_IMAGE),
                watchFaceComplicationId)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: $requestCode $resultCode")
        if (resultCode == Activity.RESULT_OK)
            when (requestCode) {
                Configs.COMPLICATION_ID_LEFT, Configs.COMPLICATION_ID_RIGHT,
                Configs.COMPLICATION_ID_BOTTOM -> {
                    // Retrieves information for selected Complication provider.
                    val complicationProviderInfo = data?.getParcelableExtra<ComplicationProviderInfo>(ProviderChooserIntent.EXTRA_PROVIDER_INFO)
                    Log.d(TAG, "Provider: $complicationProviderInfo")
                    updateComplicationViews(requestCode, complicationProviderInfo)
                }
            }
    }

    private fun updateComplicationViews(watchFaceComplicationId: Int,
                                        complicationProviderInfo: ComplicationProviderInfo?) {
        //try to update adapter
        mMyAdapter?.updateComplicationViews(watchFaceComplicationId, complicationProviderInfo)

        val iconId = when (watchFaceComplicationId) {
            Configs.COMPLICATION_ID_LEFT -> R.id.icon1
            Configs.COMPLICATION_ID_RIGHT -> R.id.icon2
            Configs.COMPLICATION_ID_BOTTOM -> R.id.icon3
            else -> 0
        }
        complicationProviderInfo?.let {
            findViewById<ImageView>(iconId)?.setImageIcon(it.providerIcon)
        }
    }

    class MyAdapter(private val activity: Activity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        companion object {
            const val TYPE_CLOCK = 1
            const val TYPE_SWITCH = 2
            const val TYPE_COLOR = 3
            const val TYPE_TEXT = 4
        }

        private var mClockHolder: ClockHolder? = null
        private val configs = Configs(activity)

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent?.context)
            when (viewType) {
                TYPE_CLOCK -> {
                    mClockHolder = ClockHolder(activity, inflater.inflate(R.layout.holder_clock, parent, false))
                    return mClockHolder as ClockHolder
                }
                TYPE_SWITCH ->
                    return SwitchHolder(activity, inflater.inflate(R.layout.holder_switch, parent, false))
            }
            return TextHolder(inflater.inflate(R.layout.holder_text, parent, false))
        }

        override fun getItemCount(): Int = 6

        override fun getItemViewType(position: Int): Int = when (position) {
            0 -> TYPE_CLOCK
            in 1..4 -> TYPE_SWITCH
            else -> TYPE_TEXT
        }

        //private val SWITCH_TEXT = arrayListOf("clock", "second", "battery", "digital", "tap")

        private fun getItemValue(position: Int): Any? = when (position) {
            1 -> configs.secondHandEnabled
            2 -> configs.batteryRingEnabled
            3 -> configs.digitalTimeEnabled
            4 -> configs.tapComplicationEnabled
            else -> null
        }

        private fun getItemTitle(position: Int): String? = when (position) {
            1 -> activity.getString(R.string.config_title_second_hand)
            2 -> activity.getString(R.string.config_title_battery_ring)
            3 -> activity.getString(R.string.config_title_digital_time)
            4 -> activity.getString(R.string.config_title_enable_tap)
            else -> null
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
            when (position) {
                in 1..4 -> {
                    //val switchHolder = holder as SwitchHolder
                    (holder as SwitchHolder).apply {
                        switch.tag = position
                        switch.isChecked = getItemValue(position) as Boolean
                        text.text = getItemTitle(position)
                    }
                }
                5 -> {
                    val pInfo: PackageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
                    //val textHolder = holder as TextHolder
                    (holder as TextHolder).text.text = pInfo.versionName
                }
            }
        }

        fun updateComplicationViews(watchFaceComplicationId: Int,
                                    complicationProviderInfo: ComplicationProviderInfo?) {
            mClockHolder?.updateComplicationViews(watchFaceComplicationId, complicationProviderInfo)
        }
    }

    class ClockHolder(private val activity: Activity, view: View) : RecyclerView.ViewHolder(view),
            View.OnClickListener {
        private var icon1: ImageView = view.findViewById(R.id.icon1)
        private var icon2: ImageView = view.findViewById(R.id.icon2)
        private var icon3: ImageView = view.findViewById(R.id.icon3)

        init {
            icon1.setOnClickListener(this)
            icon2.setOnClickListener(this)
            icon3.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            when (view?.id) {
                R.id.icon1 -> launchComplicationChooser(Configs.COMPLICATION_ID_LEFT)
                R.id.icon2 -> launchComplicationChooser(Configs.COMPLICATION_ID_RIGHT)
                R.id.icon3 -> launchComplicationChooser(Configs.COMPLICATION_ID_BOTTOM)
            }
        }

        fun updateComplicationViews(watchFaceComplicationId: Int,
                                    complicationProviderInfo: ComplicationProviderInfo?) {
            complicationProviderInfo?.let {
                when (watchFaceComplicationId) {
                    Configs.COMPLICATION_ID_LEFT -> icon1.setImageIcon(it.providerIcon)
                    Configs.COMPLICATION_ID_RIGHT -> icon2.setImageIcon(it.providerIcon)
                    Configs.COMPLICATION_ID_BOTTOM -> icon3.setImageIcon(it.providerIcon)
                }
            }
        }

        private fun launchComplicationChooser(watchFaceComplicationId: Int) {
            val watchFace = ComponentName(activity, MyWatchFace::class.java)
            activity.startActivityForResult(
                    ComplicationHelperActivity.createProviderChooserHelperIntent(
                            activity,
                            watchFace,
                            watchFaceComplicationId,
                            ComplicationData.TYPE_SHORT_TEXT,
                            ComplicationData.TYPE_SMALL_IMAGE),
                    watchFaceComplicationId)
        }
    }

    class SwitchHolder(context: Context, view: View) : RecyclerView.ViewHolder(view),
            CompoundButton.OnCheckedChangeListener {
        private val configs = Configs(context)
        var switch: Switch = view.findViewById(android.R.id.checkbox)
        var text: TextView = view.findViewById(android.R.id.title)

        init {
            switch.setOnCheckedChangeListener(this)
        }

        override fun onCheckedChanged(button: CompoundButton?, checked: Boolean) {
            when (button?.tag) {
                "1", 1 -> configs.secondHandEnabled = checked
                "2", 2 -> configs.batteryRingEnabled = checked
                "3", 3 -> configs.digitalTimeEnabled = checked
                "4", 4 -> configs.tapComplicationEnabled = checked
            }
        }
    }

    class TextHolder(view: View) : RecyclerView.ViewHolder(view) {
        var text: TextView = view.findViewById(android.R.id.title)
    }
}