package com.amitmerchant.nightclockalwayson

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.*
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatRadioButton
import java.text.SimpleDateFormat
import java.util.*
import android.view.OrientationEventListener
import android.content.pm.ActivityInfo

class MainActivity : AppCompatActivity() {

    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var rootLayout: ViewGroup
    private val key24HourFormat = "use24HourFormat"

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var infoIcon: ImageView
    private val idleTimeout = 5000L // 5 seconds
    private val idleHandler = android.os.Handler()
    private lateinit var settingsIcon: ImageView
    private lateinit var orientationListener: OrientationEventListener
    private val driftHandler = Handler(Looper.getMainLooper())

    companion object {
        const val PREFS_NAME = "clock_settings"
        const val KEY_CLOCK_COLOR = "clock_color"
        const val KEY_SHOW_DATE = "show_date"
    }

    private val driftRunnable = object : Runnable {
        override fun run() {
            applyPixelDrift()
            driftHandler.postDelayed(this, 5 * 60 * 1000) // Every 5 minutes
        }
    }

    private fun applyPixelDrift() {
        val driftRange = -5..5

        listOf(clockText, dateText).forEach { view ->
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(
                driftRange.random(),
                driftRange.random(),
                0,
                0
            )
            view.layoutParams = params
        }
    }

    private val idleRunnable = Runnable {
        if (::infoIcon.isInitialized && ::settingsIcon.isInitialized) {
            infoIcon.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    infoIcon.visibility = View.GONE
                }
                .start()

            settingsIcon.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    settingsIcon.visibility = View.GONE
                }
                .start()
        }
    }

    private val updateClock = object : Runnable {
        override fun run() {
            val now = Calendar.getInstance()

            val use24Hour = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(key24HourFormat, false)

            val timeFormat = SimpleDateFormat(
                if (use24Hour) "HH:mm" else "hh:mm a",
                Locale.getDefault()
            )

            clockText.text = timeFormat.format(now.time)

            dateText.text = getFormattedDate(now)

            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        setContentView(R.layout.activity_main)

        orientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return

                // Decide orientation based on degree ranges
                if (orientation in 60..140) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                } else if (orientation in 220..300) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else if (orientation in 310..360 || orientation in 0..50) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                // (You could add reverse portrait if you want.)
            }
        }

        orientationListener.enable()

        infoIcon = findViewById(R.id.infoIcon)
        infoIcon.setOnClickListener { v: View? -> showInfoDialog() }

        settingsIcon = findViewById(R.id.settingsIcon)

        settingsIcon.setOnClickListener {
            showSettingsDialog()
        }

        infoIcon.setOnClickListener {
            showInfoDialog()
        }

        val settingsIcon = findViewById<ImageView>(R.id.settingsIcon)
        settingsIcon.setOnClickListener {
            showSettingsDialog()
        }

        // Start idle timer
        resetIdleTimer()

        clockText = findViewById(R.id.clockText)
        dateText = findViewById(R.id.dateText)
        rootLayout = findViewById(R.id.rootLayout)

        loadSettings()
        applyClockSettings()

        updateClock.run()
        driftRunnable.run()
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateClock)
        driftHandler.removeCallbacks(driftRunnable)
        super.onDestroy()
    }

    private fun getFormattedDate(calendar: Calendar): String {
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val suffix = when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        val dateFormat = SimpleDateFormat("MMM, yyyy", Locale.getDefault())
        return "$day$suffix ${dateFormat.format(calendar.time)}"
    }

    private fun showInfoDialog() {
        val titleView = TextView(this).apply {
            text = "Night Clock (Always On)"
            setPadding(50, 40, 50, 20)
            textSize = 20f
            setTextColor(Color.BLACK)
        }

        titleView.setTypeface(null, Typeface.BOLD)

        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setCustomTitle(titleView)
            .setMessage(
                Html.fromHtml(
                    """
                <font color='#000000'>
                A minimal, distraction-free clock for your bedside.<br><br>
                ✨ Developed by <b><a href='https://amitmerchant.com'>Amit Merchant</a></b><br><br>
                ☕ <a href='https://buymeacoffee.com/amitmerchant'>Buy Me a Coffee</a><br><br>
                ⭐️ <a href='https://play.google.com/store/apps/details?id=com.amitmerchant.nightclockalwayson'>Rate this app</a><br><br>
                👨‍💻 <a href='https://github.com/amitmerchant1990/night-clock'>Source Code</a><br><br>
                </font>
                """.trimIndent(),
                    Html.FROM_HTML_MODE_LEGACY
                )
            )
            .setPositiveButton("Close", null)

        val dialog = builder.create()
        dialog.show()

        // Set the message text and links color
        dialog.findViewById<TextView>(android.R.id.message)?.let { messageView ->
            messageView.movementMethod = LinkMovementMethod.getInstance()
            messageView.setTextColor(Color.BLACK)
        }

        // 🔥 Set the Close button color to Black after dialog shows
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (::infoIcon.isInitialized && ::settingsIcon.isInitialized) {
            if (infoIcon.visibility != View.VISIBLE || settingsIcon.visibility != View.VISIBLE) {
                infoIcon.alpha = 0f
                infoIcon.visibility = View.VISIBLE
                infoIcon.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()

                settingsIcon.alpha = 0f
                settingsIcon.visibility = View.VISIBLE
                settingsIcon.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
            resetIdleTimer()
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun resetIdleTimer() {
        idleHandler.removeCallbacks(idleRunnable)
        idleHandler.postDelayed(idleRunnable, idleTimeout)
    }

    private val prefs by lazy { getSharedPreferences("night_clock_prefs", MODE_PRIVATE) }

    private fun saveSettings(color: String, showDate: Boolean) {
        prefs.edit()
            .putString("clockColor", color)
            .putBoolean("showDate", showDate)
            .apply()
    }

    private fun loadSettings() {
        val color = prefs.getString("clockColor", "#00FF00") ?: "#00FF00"
        val showDate = prefs.getBoolean("showDate", true)

        clockText.setTextColor(Color.parseColor(color))
        dateText.visibility = if (showDate) View.VISIBLE else View.GONE
    }

    private fun showSettingsDialog() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentColor = sharedPreferences.getInt(KEY_CLOCK_COLOR, android.graphics.Color.GREEN)
        val showDate = sharedPreferences.getBoolean(KEY_SHOW_DATE, true)

        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)

        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.colorOptions)
        val showDateCheckbox = dialogView.findViewById<CheckBox>(R.id.showDateCheckbox)
        val use24HourFormatCheckbox = dialogView.findViewById<CheckBox>(R.id.use24HourFormatCheckbox)
        use24HourFormatCheckbox.isChecked = sharedPreferences.getBoolean(key24HourFormat, false)


        // Pre-select the current color
        when (currentColor) {
            android.graphics.Color.GREEN -> radioGroup.check(R.id.radioGreen)
            android.graphics.Color.MAGENTA -> radioGroup.check(R.id.radioPink)
            android.graphics.Color.YELLOW -> radioGroup.check(R.id.radioYellow)
            android.graphics.Color.WHITE -> radioGroup.check(R.id.radioWhite)
        }

        showDateCheckbox.isChecked = showDate

        val titleView = TextView(this).apply {
            text = "Settings"
            setPadding(50, 40, 50, 20)
            textSize = 20f
            setTextColor(Color.BLACK)
        }

        titleView.setTypeface(null, Typeface.BOLD)

        val builder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setCustomTitle(titleView)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val selectedColor = when (radioGroup.checkedRadioButtonId) {
                    R.id.radioGreen -> android.graphics.Color.GREEN
                    R.id.radioPink -> android.graphics.Color.MAGENTA
                    R.id.radioYellow -> android.graphics.Color.YELLOW
                    R.id.radioWhite -> android.graphics.Color.WHITE
                    else -> android.graphics.Color.GREEN
                }

                val isShowDateChecked = showDateCheckbox.isChecked

                with(sharedPreferences.edit()) {
                    putInt(KEY_CLOCK_COLOR, selectedColor)
                    putBoolean(KEY_SHOW_DATE, isShowDateChecked)
                    putBoolean(key24HourFormat, use24HourFormatCheckbox.isChecked)
                    apply()
                }

                applyClockSettings()
            }
            .setNegativeButton("Cancel", null)

        val dialog = builder.create()
        dialog.show()

        val blackColor = android.graphics.Color.BLACK
        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(blackColor, blackColor)
        )

        // Change title color
        val alertTitleId = resources.getIdentifier("alertTitle", "id", "android")
        val alertTitle = dialog.findViewById<TextView>(alertTitleId)
        alertTitle?.setTextColor(android.graphics.Color.BLACK)

        // Change radio buttons text color
        for (i in 0 until radioGroup.childCount) {
            val radioButton = radioGroup.getChildAt(i) as? RadioButton
            radioButton?.setTextColor(android.graphics.Color.BLACK)
        }

        // Change checkbox text color
        showDateCheckbox.setTextColor(android.graphics.Color.BLACK)
        use24HourFormatCheckbox.setTextColor(android.graphics.Color.BLACK)

        // Change buttons text color
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(android.graphics.Color.BLACK)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(android.graphics.Color.BLACK)

        // Apply to RadioButtons
        for (i in 0 until radioGroup.childCount) {
            val rb = radioGroup.getChildAt(i)
            if (rb is AppCompatRadioButton) {
                rb.buttonTintList = colorStateList
            }
        }

        // Apply to CheckBox
        if (showDateCheckbox is AppCompatCheckBox) {
            showDateCheckbox.buttonTintList = colorStateList
            use24HourFormatCheckbox.buttonTintList = colorStateList
        }
    }

    private fun applyClockSettings() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val selectedColor = sharedPreferences.getInt(KEY_CLOCK_COLOR, android.graphics.Color.GREEN)
        val showDate = sharedPreferences.getBoolean(KEY_SHOW_DATE, true)

        clockText.setTextColor(selectedColor)
        dateText.setTextColor(selectedColor)
        infoIcon.setColorFilter(selectedColor)
        settingsIcon.setColorFilter(selectedColor)

        dateText.visibility = if (showDate) View.VISIBLE else View.GONE
    }
}