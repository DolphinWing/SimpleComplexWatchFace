@file:Suppress("PackageName")

package dolphin.android.wear.SimpleComplexFace

import android.content.*
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.preference.PreferenceManager
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.SystemProviders
import android.support.wearable.complications.rendering.ComplicationDrawable
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.util.SparseArray
import android.view.Gravity
import android.view.SurfaceHolder
import java.lang.ref.WeakReference
import java.util.*


/**
 * Updates rate in milliseconds for interactive mode. We update once a second to advance the
 * second hand.
 */
private const val INTERACTIVE_UPDATE_RATE_MS = 1000

/**
 * Handler message id for updating the time periodically in interactive mode.
 */
private const val MSG_UPDATE_TIME = 0

private const val HOUR_STROKE_WIDTH = 8f
private const val MINUTE_STROKE_WIDTH = 5f
private const val SECOND_TICK_STROKE_WIDTH = 3f

private const val CENTER_GAP_AND_CIRCLE_RADIUS = 4f

private const val SHADOW_RADIUS = 6f

private const val TAG = "MyWatchFace"

//private const val BACKGROUND_COMPLICATION = 9000
//private const val LEFT_COMPLICATION = 9001
//private const val RIGHT_COMPLICATION = 9002
//private const val BOTTOM_COMPLICATION = 9003

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 *
 *
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
class MyWatchFace : CanvasWatchFaceService() {

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: MyWatchFace.Engine) : Handler() {
        private val mWeakReference: WeakReference<MyWatchFace.Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            mWeakReference.get()?.let {
                when (msg.what) {
                    MSG_UPDATE_TIME -> it.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private lateinit var mCalendar: Calendar
        private lateinit var mConfigs: Configs

        private var mRegisteredTimeZoneReceiver = false
        private var mMuteMode: Boolean = false
        private var mCenterX: Float = 0F
        private var mCenterY: Float = 0F

        private var mSecondHandLength: Float = 0F
        private var sMinuteHandLength: Float = 0F
        private var sHourHandLength: Float = 0F

        /* Colors for all hands (hour, minute, seconds, ticks) based on photo loaded. */
        private var mWatchHandColor: Int = 0
        private var mWatchHandHighlightColor: Int = 0
        private var mWatchHandShadowColor: Int = 0

        private lateinit var mHourPaint: Paint
        private lateinit var mMinutePaint: Paint
        private lateinit var mSecondPaint: Paint
        private lateinit var mTickAndCirclePaint: Paint
        private lateinit var mBatteryLevelPaint: Paint
        private lateinit var mBatteryInnerPaint: Paint

        private lateinit var mBackgroundPaint: Paint
        //private lateinit var mBackgroundBitmap: Bitmap
        //private lateinit var mGrayBackgroundBitmap: Bitmap
        private val mComplicationDrawable = SparseArray<ComplicationDrawable>()
        private var mBatteryLevel = 0f

        private var mAmbient: Boolean = false
        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false
        private var mEnableBatteryLevel = true
        private var mEnableTap = false
        private var mEnableSecondHand = true
        private var mEnableDigitalClock = true

        /* Handler to update the time once a second in interactive mode. */
        private val mUpdateTimeHandler = EngineHandler(this)

        private val mTimeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(WatchFaceStyle.Builder(this@MyWatchFace)
                    .setShowUnreadCountIndicator(true)
                    .setStatusBarGravity(Gravity.CENTER_HORIZONTAL + Gravity.TOP)
                    .setAcceptsTapEvents(true)
                    .build())

            mCalendar = Calendar.getInstance()
            mConfigs = Configs(applicationContext)
            mEnableBatteryLevel = mConfigs.batteryRingEnabled
            mEnableDigitalClock = mConfigs.digitalTimeEnabled
            mEnableSecondHand = mConfigs.secondHandEnabled
            mEnableTap = mConfigs.tapComplicationEnabled

            initializeComplicationsAndBackground()
            initializeWatchFace()
        }

        private fun initializeComplicationsAndBackground() {
            mBackgroundPaint = Paint().apply {
                color = Color.BLACK
            }
//            mBackgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.bg)
//
//            /* Extracts colors from background image to improve watchface style. */
//            Palette.from(mBackgroundBitmap).generate {
//                it?.let {
//                    mWatchHandHighlightColor = it.getVibrantColor(Color.RED)
//                    mWatchHandColor = it.getLightVibrantColor(Color.WHITE)
//                    mWatchHandShadowColor = it.getDarkMutedColor(Color.BLACK)
//                    updateWatchHandStyle()
//                }
//            }

            setDefaultSystemComplicationProvider(Configs.COMPLICATION_ID_BACKGROUND,
                    SystemProviders.WATCH_BATTERY, ComplicationData.TYPE_RANGED_VALUE)
            setDefaultSystemComplicationProvider(Configs.COMPLICATION_ID_LEFT,
                    SystemProviders.DATE, ComplicationData.TYPE_SHORT_TEXT)
            setDefaultSystemComplicationProvider(Configs.COMPLICATION_ID_RIGHT,
                    SystemProviders.STEP_COUNT, ComplicationData.TYPE_SHORT_TEXT)
            mComplicationDrawable.put(Configs.COMPLICATION_ID_BOTTOM,
                    ComplicationDrawable(applicationContext))
            mComplicationDrawable.put(Configs.COMPLICATION_ID_LEFT,
                    ComplicationDrawable(applicationContext))
            mComplicationDrawable.put(Configs.COMPLICATION_ID_RIGHT,
                    ComplicationDrawable(applicationContext))
            setComplicationsActiveAndAmbientColors(Color.DKGRAY)
            setActiveComplications(Configs.COMPLICATION_ID_BACKGROUND,
                    Configs.COMPLICATION_ID_LEFT, Configs.COMPLICATION_ID_RIGHT,
                    Configs.COMPLICATION_ID_BOTTOM)
        }

        private fun initializeWatchFace() {
            /* Set defaults for colors */
            mWatchHandColor = Color.WHITE
            mWatchHandHighlightColor = Color.RED
            mWatchHandShadowColor = Color.BLACK

            mHourPaint = Paint().apply {
                color = mWatchHandColor
                strokeWidth = HOUR_STROKE_WIDTH
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                setShadowLayer(
                        SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor)
            }

            mMinutePaint = Paint().apply {
                color = mWatchHandColor
                strokeWidth = MINUTE_STROKE_WIDTH
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                setShadowLayer(
                        SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor)
            }

            mSecondPaint = Paint().apply {
                color = mWatchHandHighlightColor
                strokeWidth = SECOND_TICK_STROKE_WIDTH
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                setShadowLayer(
                        SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor)
            }

            mTickAndCirclePaint = Paint().apply {
                color = Color.argb(120, 128, 128, 128) //mWatchHandColor
                textSize = 128f
                strokeWidth = SECOND_TICK_STROKE_WIDTH
                isAntiAlias = true
                //style = Paint.Style.STROKE
                typeface = Typeface.MONOSPACE
                setShadowLayer(
                        SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor)
            }

            mBatteryLevelPaint = Paint().apply {
                color = Color.argb(255, 80, 205, 80)
                isAntiAlias = true
            }

            mBatteryInnerPaint = Paint().apply {
                color = Color.BLACK
            }
        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            mLowBitAmbient = properties.getBoolean(
                    WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false)
            mBurnInProtection = properties.getBoolean(
                    WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false)
            for (i in 0 until mComplicationDrawable.size()) {
                mComplicationDrawable.valueAt(i).setLowBitAmbient(mLowBitAmbient)
                mComplicationDrawable.valueAt(i).setBurnInProtection(mBurnInProtection)
            }
            //Toast.makeText(applicationContext, "onPropertiesChanged", Toast.LENGTH_SHORT).show()
            mEnableBatteryLevel = mConfigs.batteryRingEnabled
            mEnableDigitalClock = mConfigs.digitalTimeEnabled
            mEnableSecondHand = mConfigs.secondHandEnabled
            mEnableTap = mConfigs.tapComplicationEnabled
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            mAmbient = inAmbientMode

            updateWatchHandStyle()

            for (i in 0 until mComplicationDrawable.size()) {
                mComplicationDrawable.valueAt(i).setInAmbientMode(mAmbient)
            }

            // Check and trigger whether or not timer should be running (only
            // in active mode).
            updateTimer()
        }

        private fun updateWatchHandStyle() {
            if (mAmbient) {
                mHourPaint.color = Color.WHITE
                mMinutePaint.color = Color.WHITE
                mSecondPaint.color = Color.WHITE
                //mTickAndCirclePaint.color = Color.WHITE

                mHourPaint.isAntiAlias = false
                mMinutePaint.isAntiAlias = false
                mSecondPaint.isAntiAlias = false
                mTickAndCirclePaint.isAntiAlias = false

                mHourPaint.clearShadowLayer()
                mMinutePaint.clearShadowLayer()
                mSecondPaint.clearShadowLayer()
                mTickAndCirclePaint.clearShadowLayer()

            } else {
                mHourPaint.color = mWatchHandColor
                mMinutePaint.color = mWatchHandColor
                mSecondPaint.color = mWatchHandHighlightColor
                //mTickAndCirclePaint.color = mWatchHandColor

                mHourPaint.isAntiAlias = true
                mMinutePaint.isAntiAlias = true
                mSecondPaint.isAntiAlias = true
                mTickAndCirclePaint.isAntiAlias = true

                mHourPaint.setShadowLayer(
                        SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor)
                mMinutePaint.setShadowLayer(
                        SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor)
                mSecondPaint.setShadowLayer(
                        SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor)
                mTickAndCirclePaint.setShadowLayer(
                        SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor)
            }
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            super.onInterruptionFilterChanged(interruptionFilter)
            val inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode
                mHourPaint.alpha = if (inMuteMode) 100 else 255
                mMinutePaint.alpha = if (inMuteMode) 100 else 255
                mSecondPaint.alpha = if (inMuteMode) 80 else 255
                invalidate()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f
            mCenterY = height / 2f

            mTickAndCirclePaint.textSize = mCenterX / 2 + 20

            /*
             * Calculate lengths of different hands based on watch screen size.
             */
            mSecondHandLength = (mCenterX * 0.85).toFloat()
            sMinuteHandLength = (mCenterX * 0.75).toFloat()
            sHourHandLength = (mCenterX * 0.4).toFloat()

            // For most Wear devices, width and height are the same, so we just chose one (width).
            val sizeOfComplication = width / 5
            val midpointOfScreen = width / 2

            val horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2
            val verticalOffset = midpointOfScreen - sizeOfComplication / 2
            mComplicationDrawable.get(Configs.COMPLICATION_ID_LEFT).bounds = Rect(
                    horizontalOffset,
                    verticalOffset + sizeOfComplication,
                    horizontalOffset + sizeOfComplication,
                    verticalOffset + sizeOfComplication * 2)
            mComplicationDrawable.get(Configs.COMPLICATION_ID_RIGHT).bounds = Rect(
                    horizontalOffset + midpointOfScreen,
                    verticalOffset + sizeOfComplication,
                    horizontalOffset + midpointOfScreen + sizeOfComplication,
                    verticalOffset + sizeOfComplication * 2)
//            mComplicationDrawable.get(BACKGROUND_COMPLICATION).bounds = Rect(
//                    0, 0, width, height
//            )
            mComplicationDrawable.get(Configs.COMPLICATION_ID_BOTTOM).bounds = Rect(
                    (mCenterX - sizeOfComplication / 2).toInt(),
                    (verticalOffset + sizeOfComplication * 1.5f).toInt(),
                    (mCenterX + sizeOfComplication / 2).toInt(),
                    (verticalOffset + sizeOfComplication * 2.5f).toInt())

//            /* Scale loaded background image (more efficient) if surface dimensions change. */
//            val scale = width.toFloat() / mBackgroundBitmap.width.toFloat()
//
//            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
//                    (mBackgroundBitmap.width * scale).toInt(),
//                    (mBackgroundBitmap.height * scale).toInt(), true)

//            /*
//             * Create a gray version of the image only if it will look nice on the device in
//             * ambient mode. That means we don't want devices that support burn-in
//             * protection (slight movements in pixels, not great for images going all the way to
//             * edges) and low ambient mode (degrades image quality).
//             *
//             * Also, if your watch face will know about all images ahead of time (users aren't
//             * selecting their own photos for the watch face), it will be more
//             * efficient to create a black/white version (png, etc.) and load that when you need it.
//             */
//            if (!mBurnInProtection && !mLowBitAmbient) {
//                initGrayBackgroundBitmap()
//            }
        }

//        private fun initGrayBackgroundBitmap() {
//            mGrayBackgroundBitmap = Bitmap.createBitmap(
//                    mBackgroundBitmap.width,
//                    mBackgroundBitmap.height,
//                    Bitmap.Config.ARGB_8888)
//            val canvas = Canvas(mGrayBackgroundBitmap)
//            val grayPaint = Paint()
//            val colorMatrix = ColorMatrix()
//            colorMatrix.setSaturation(0f)
//            val filter = ColorMatrixColorFilter(colorMatrix)
//            grayPaint.colorFilter = filter
//            canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, grayPaint)
//        }

        /**
         * Captures tap event (and tap type). The [WatchFaceService.TAP_TYPE_TAP] case can be
         * used for implementing specific logic to handle the gesture.
         */
        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
                    // The user has started touching the screen.
                }
                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
                    // The user has started a different gesture or otherwise cancelled the tap.
                }
                WatchFaceService.TAP_TYPE_TAP -> {
                    // The user has completed the tap gesture.
                    if (mEnableTap) {
                        for (i in 0 until mComplicationDrawable.size()) {
                            if (mComplicationDrawable.valueAt(i).onTap(x, y)) {
                                return
                            }
                        }
                    }

//                    // TODO: Add code to handle the tap gesture.
//                    Toast.makeText(applicationContext, R.string.message, Toast.LENGTH_SHORT)
//                            .show()
                }
            }
            invalidate()
        }


        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            mCalendar.timeInMillis = now

            drawBackground(canvas)
            drawComplications(canvas, now)
            drawWatchFace(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)
            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
//                canvas.drawColor(Color.BLACK)
            } else if (mAmbient) {
//                canvas.drawBitmap(mGrayBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            } else {
//                canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, mBackgroundPaint)
                if (mEnableBatteryLevel) {
                    canvas.drawArc(3f, 3f, mCenterX * 2 - 3, mCenterY * 2 - 3,
                            0f, 36 * mBatteryLevel, true, mBatteryLevelPaint)
                    canvas.drawArc(10f, 10f, mCenterX * 2 - 10, mCenterY * 2 - 10,
                            0f, 36 * mBatteryLevel, true, mBatteryInnerPaint)
                }
            }
        }

        private fun drawComplications(canvas: Canvas, currentTimeMillis: Long) {
            if (!mAmbient) {
                for (i in 0 until mComplicationDrawable.size()) {
                    mComplicationDrawable.valueAt(i).draw(canvas, currentTimeMillis)
                }
            }
        }

        private fun drawWatchFace(canvas: Canvas) {

            /*
             * Draw ticks. Usually you will want to bake this directly into the photo, but in
             * cases where you want to allow users to select their own photos, this dynamically
             * creates them on top of the photo.
             */
            val innerTickRadius = mCenterX - 10
            val outerTickRadius = mCenterX
            for (tickIndex in 0..11) {
                val tickRot = (tickIndex.toDouble() * Math.PI * 2.0 / 12).toFloat()
                val innerX = Math.sin(tickRot.toDouble()).toFloat() * innerTickRadius
                val innerY = (-Math.cos(tickRot.toDouble())).toFloat() * innerTickRadius
                val outerX = Math.sin(tickRot.toDouble()).toFloat() * outerTickRadius
                val outerY = (-Math.cos(tickRot.toDouble())).toFloat() * outerTickRadius
                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mMinutePaint)
            }

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            val seconds =
                    mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f
            val secondsRotation = seconds * 6f

            val minHandOffset = seconds / 10
            val minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f + minHandOffset

            val hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f
            val hoursRotation = mCalendar.get(Calendar.HOUR) * 30 + hourHandOffset + seconds / 60

            if (!mAmbient && mEnableDigitalClock) {
                val bounds = Rect()
                val hours = String.format("%02d", mCalendar.get(Calendar.HOUR_OF_DAY))
                val minutes = String.format("%02d", mCalendar.get(Calendar.MINUTE))
                mTickAndCirclePaint.getTextBounds(hours, 0, hours.length, bounds)
                canvas.drawText(hours, mCenterX - bounds.width() - 20,
                        mCenterY + bounds.height() / 2, mTickAndCirclePaint)
                mTickAndCirclePaint.getTextBounds(minutes, 0, minutes.length, bounds)
                canvas.drawText(minutes, mCenterX + 4, mCenterY + bounds.height() / 2, mTickAndCirclePaint)
            }
            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save()

            canvas.rotate(hoursRotation, mCenterX, mCenterY)
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS * 4,
                    mCenterX,
                    mCenterY - sHourHandLength,
                    mHourPaint)

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY)
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS * 2,
                    mCenterX,
                    mCenterY - sMinuteHandLength,
                    mMinutePaint)

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
            if (!mAmbient && mEnableSecondHand) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY)
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mSecondPaint)
            }
//            canvas.drawCircle(
//                    mCenterX,
//                    mCenterY,
//                    CENTER_GAP_AND_CIRCLE_RADIUS,
//                    mTickAndCirclePaint)

            /* Restore the canvas' original orientation. */
            canvas.restore()
        }

        private fun setComplicationsActiveAndAmbientColors(primaryComplicationColor: Int) {
            for (i in 0 until mComplicationDrawable.size()) {
                // Active mode colors.
                mComplicationDrawable.valueAt(i).setBorderColorActive(primaryComplicationColor)
                mComplicationDrawable.valueAt(i).setRangedValuePrimaryColorActive(primaryComplicationColor)

                // Ambient mode colors.
                mComplicationDrawable.valueAt(i).setBorderColorAmbient(Color.WHITE)
                mComplicationDrawable.valueAt(i).setRangedValuePrimaryColorAmbient(Color.WHITE)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                setComplicationsActiveAndAmbientColors(Color.DKGRAY)
                registerReceiver()
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer()
        }

        private fun registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@MyWatchFace.registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = false
            this@MyWatchFace.unregisterReceiver(mTimeZoneReceiver)
        }

        /**
         * Starts/stops the [.mUpdateTimeHandler] timer based on the state of the watch face.
         */
        private fun updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running. The timer
         * should only run in active mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !mAmbient
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }

//        private lateinit var leftComplicationDrawable: ComplicationDrawable
//        private lateinit var rightComplicationDrawable: ComplicationDrawable

        override fun onComplicationDataUpdate(watchFaceComplicationId: Int, data: ComplicationData?) {
            //Log.d(TAG, "onComplicationDataUpdate $watchFaceComplicationId")
            //super.onComplicationDataUpdate(watchFaceComplicationId, data)
            if (watchFaceComplicationId == Configs.COMPLICATION_ID_BACKGROUND) {
                Log.d(TAG, " type: ${data?.type}")
                Log.d(TAG, "value: ${data?.value}")
                Log.d(TAG, "  min: ${data?.minValue}")
                Log.d(TAG, "  max: ${data?.maxValue}")
                mBatteryLevel = data?.value ?: 0f
                mBatteryLevelPaint.color = when (mBatteryLevel) {
                    in 1..15 -> Color.argb(255, 180, 85, 80)
                    in 16..30 -> Color.argb(255, 185, 185, 60)
                    in 31..100 -> Color.argb(255, 80, 185, 80)
                    else -> Color.BLACK
                }
            } else {
                mComplicationDrawable[watchFaceComplicationId].setComplicationData(data)
            }
            invalidate()
        }
    }
}

