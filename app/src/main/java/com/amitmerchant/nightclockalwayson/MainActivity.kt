package com.amitmerchant.nightclockalwayson

import android.content.res.Configuration
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var rootLayout: ViewGroup

    private val handler = Handler(Looper.getMainLooper())

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

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        setContentView(R.layout.activity_main)

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
}