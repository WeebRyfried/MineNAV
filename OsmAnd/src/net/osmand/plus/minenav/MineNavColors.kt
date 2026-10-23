package net.osmand.plus.minenav

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import net.osmand.plus.R
import net.osmand.plus.utils.AndroidUtils

/**
 * Waze-like light and dark palette shared by the MineNAV screens.
 */
class MineNavColors(private val context: Context, val nightMode: Boolean) {

	@ColorInt
	val screen = color(if (nightMode) R.color.mn_screen_bg_dark else R.color.mn_screen_bg_light)

	@ColorInt
	val row = color(if (nightMode) R.color.mn_row_bg_dark else R.color.mn_row_bg_light)

	@ColorInt
	val field = color(if (nightMode) R.color.mn_field_bg_dark else R.color.mn_field_bg_light)

	@ColorInt
	val text = color(if (nightMode) R.color.mn_text_dark else R.color.mn_text_light)

	@ColorInt
	val secondaryText = color(if (nightMode) R.color.mn_text_secondary_dark else R.color.mn_text_secondary_light)

	@ColorInt
	val divider = color(if (nightMode) R.color.mn_divider_dark else R.color.mn_divider_light)

	// Lighter in dark mode so accent text and check marks stay readable
	@ColorInt
	val accent = color(if (nightMode) R.color.mn_lilac else R.color.mn_primary)

	@ColorInt
	fun color(id: Int): Int = ContextCompat.getColor(context, id)

	fun circle(@ColorInt fill: Int): GradientDrawable = GradientDrawable().apply {
		shape = GradientDrawable.OVAL
		setColor(fill)
	}

	fun pill(@ColorInt fill: Int): GradientDrawable = GradientDrawable().apply {
		cornerRadius = AndroidUtils.dpToPx(context, 100f).toFloat()
		setColor(fill)
	}
}
