package id.kazera.nocturne.kernelmanager

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(8, 10, 14)
        window.navigationBarColor = Color.rgb(8, 10, 14)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(28), dp(28), dp(28))
            setBackgroundColor(Color.rgb(8, 10, 14))
        }

        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 29f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = getString(R.string.app_subtitle)
            textSize = 14f
            setTextColor(Color.rgb(177, 187, 199))
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }

        val status = TextView(this).apply {
            text = getString(R.string.foundation_status)
            textSize = 12f
            setTextColor(Color.rgb(160, 224, 102))
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(28), dp(12), 0)
        }

        content.addView(title)
        content.addView(subtitle)
        content.addView(status)
        setContentView(content)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
