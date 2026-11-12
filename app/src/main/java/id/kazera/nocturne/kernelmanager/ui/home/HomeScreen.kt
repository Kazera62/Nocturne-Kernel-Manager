package id.kazera.nocturne.kernelmanager.ui.home

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import id.kazera.nocturne.kernelmanager.R

/**
 * Initial presentation only. This screen does not read or modify device state.
 */
object HomeScreen {

    fun create(context: Context): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(context.dp(28), context.dp(28), context.dp(28), context.dp(28))
            setBackgroundColor(context.getColor(R.color.nocturne_background))
        }

        val title = TextView(context).apply {
            text = context.getString(R.string.app_name)
            textSize = 29f
            setTextColor(context.getColor(R.color.nocturne_text_primary))
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(context).apply {
            text = context.getString(R.string.app_subtitle)
            textSize = 14f
            setTextColor(context.getColor(R.color.nocturne_text_secondary))
            gravity = Gravity.CENTER
            setPadding(0, context.dp(12), 0, 0)
        }

        val status = TextView(context).apply {
            text = context.getString(R.string.foundation_status)
            textSize = 12f
            setTextColor(context.getColor(R.color.nocturne_accent))
            gravity = Gravity.CENTER
            setPadding(context.dp(12), context.dp(28), context.dp(12), 0)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)
        return root
    }

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
