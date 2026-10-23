package net.osmand.plus.minenav

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import net.osmand.plus.R

/**
 * Colors of the MineNAV first-run screen: one surface, the suggested map in a rounded card,
 * round icon buttons and a grey pill to skip.
 */
object MineNavFirstRun {

	private const val CARD_RADIUS_DP = 20f

	@JvmStatic
	fun style(root: View, nightMode: Boolean) {
		val context = root.context
		val colors = MineNavColors(context, nightMode)
		val primary = colors.color(R.color.mn_primary)
		root.setBackgroundColor(colors.row)
		root.findViewById<TextView?>(R.id.welcome_title)?.setTextColor(colors.text)
		root.findViewById<TextView?>(R.id.wizard_description)?.setTextColor(colors.secondaryText)
		root.findViewById<TextView?>(R.id.wizard_title)?.setTextColor(colors.text)
		root.findViewById<TextView?>(R.id.terms_of_service_description)?.setTextColor(colors.secondaryText)
		root.findViewById<View?>(R.id.wizard_card)?.background = GradientDrawable().apply {
			cornerRadius = context.resources.displayMetrics.density * CARD_RADIUS_DP
			setColor(colors.field)
		}
		root.findViewById<ImageView?>(R.id.wizard_icon)?.apply {
			background = colors.circle(primary)
			imageTintList = ColorStateList.valueOf(colors.color(R.color.mn_text_dark))
		}
		for (id in intArrayOf(R.id.actions_button, R.id.location_button)) {
			root.findViewById<ImageView?>(id)?.apply {
				background = colors.circle(colors.row)
				imageTintList = ColorStateList.valueOf(colors.text)
			}
		}
		val accent = ColorStateList.valueOf(primary)
		root.findViewById<ProgressBar?>(R.id.wizard_progress_bar)?.progressTintList = accent
		root.findViewById<ProgressBar?>(R.id.wizard_progress_bar_icon)?.indeterminateTintList = accent
		root.findViewById<TextView?>(R.id.skip_button)?.apply {
			background = colors.pill(colors.field)
			setTextColor(colors.text)
		}
	}
}
