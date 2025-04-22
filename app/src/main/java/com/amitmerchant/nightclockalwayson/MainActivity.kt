package com.amitmerchant.nightclockalwayson

import android.os.*
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var rootLayout: ViewGroup

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var infoIcon: ImageView
    private val idleTimeout = 5000L // 5 seconds
    private val idleHandler = android.os.Handler()

    private val idleRunnable = Runnable {
        if (::infoIcon.isInitialized) {
            infoIcon.animate()
                .alpha(0f)
                .setDuration(500)
                .withEndAction {
                    infoIcon.visibility = View.GONE
                }
                .start()
        }
    }

    private val updateClock = object : Runnable {
        override fun run() {
            val now = Calendar.getInstance()

            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
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

        infoIcon = findViewById(R.id.infoIcon)
        infoIcon.setOnClickListener { v: View? -> showInfoDialog() }

        infoIcon.setOnClickListener {
            showInfoDialog()
        }

        // Start idle timer
        resetIdleTimer()

        clockText = findViewById(R.id.clockText)
        dateText = findViewById(R.id.dateText)
        rootLayout = findViewById(R.id.rootLayout)

        updateClock.run()
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateClock)
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
        val builder = AlertDialog.Builder(this)
            .setTitle("Night Clock (Always On)")
            .setMessage(
                Html.fromHtml(
                    """
                    A minimal, distraction-free clock for your bedside.<br><br>
                    ✨ Developed by Amit Merchant<br><br>
                    ☕ <a href='https://buymeacoffee.com/amitmerchant'>Buy Me a Coffee</a><br><br>
                    🌐 <a href='https://amitmerchant.com'>Website</a>
                    """.trimIndent(),
                    Html.FROM_HTML_MODE_LEGACY
                )
            )
            .setPositiveButton("Close", null)

        val dialog = builder.create()
        dialog.show()

        // Enable clickable links
        val messageView = dialog.findViewById<TextView>(android.R.id.message)
        messageView?.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (::infoIcon.isInitialized) {
            if (infoIcon.visibility != View.VISIBLE) {
                infoIcon.alpha = 0f
                infoIcon.visibility = View.VISIBLE
                infoIcon.animate()
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
}