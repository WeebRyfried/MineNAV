package net.osmand.plus.minenav

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.FontCache
import net.osmand.plus.views.controls.PagerSlidingTabStrip

/**
 * MineNAV look for the map download screens: the same rows, round icons, headers and dividers
 * as the MineNAV settings pages.
 */
object MineNavDownloadStyle {

	private const val ICON_INSET_DP = 8f

	@JvmStatic
	fun isNightMode(context: Context): Boolean {
		val app = context.applicationContext as OsmandApplication
		return app.daynightHelper.isNightMode(ThemeUsageContext.APP)
	}

	/** Tabs in the app font, without capitals, with the MineNAV accent under the open tab. */
	@JvmStatic
	fun styleTabs(tabs: PagerSlidingTabStrip, nightMode: Boolean) {
		val colors = MineNavColors(tabs.context, nightMode)
		tabs.setBackgroundColor(colors.row)
		(tabs.parent as? View)?.setBackgroundColor(colors.row)
		tabs.setAllCaps(false)
		tabs.setTypeface(FontCache.getFont(FontCache.getAppTypeface(), 700), 0)
		tabs.setIndicatorColor(colors.color(R.color.mn_primary))
		tabs.setTextColor(colors.text)
		tabs.setTabInactiveTextColor(colors.secondaryText)
		// The strip only ever turns capitals on, so clear them on the tabs it already made
		clearAllCaps(tabs)
	}

	private fun clearAllCaps(view: View) {
		if (view is TextView) {
			view.isAllCaps = false
		} else if (view is ViewGroup) {
			for (i in 0 until view.childCount) {
				clearAllCaps(view.getChildAt(i))
			}
		}
	}

	/** Section titles, spaced like the MineNAV settings headers. */
	@JvmStatic
	fun styleSection(view: View, nightMode: Boolean, first: Boolean) {
		val colors = MineNavColors(view.context, nightMode)
		val resources = view.resources
		val padding = resources.getDimensionPixelSize(R.dimen.content_padding)
		val top = resources.getDimensionPixelSize(if (first) R.dimen.mn_header_first_top else R.dimen.mn_header_top)
		view.setBackgroundColor(colors.screen)
		view.minimumHeight = 0
		view.setPaddingRelative(padding, top, padding, resources.getDimensionPixelSize(R.dimen.mn_header_bottom))
		for (id in intArrayOf(R.id.title, R.id.section_description)) {
			view.findViewById<TextView>(id)?.apply {
				setTextColor(colors.secondaryText)
				setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.mn_header_text_size))
				(layoutParams as? ViewGroup.MarginLayoutParams)?.let {
					if (id == R.id.title) {
						it.marginStart = 0
						it.leftMargin = 0
					}
				}
			}
		}
	}

	/** A region that opens a list: round icon, title and chevron. */
	@JvmStatic
	fun styleGroupRow(view: View, nightMode: Boolean, last: Boolean) {
		val context = view.context
		val colors = MineNavColors(context, nightMode)
		val resources = context.resources
		view.setBackgroundColor(colors.row)
		val title = view.findViewById<TextView>(R.id.title) ?: return
		val icon = title.compoundDrawablesRelative[0] ?: title.compoundDrawables[0]
		val chevron = ContextCompat.getDrawable(context, R.drawable.ic_mn_chevron)?.mutate()?.apply {
			setTint(colors.secondaryText)
		}
		title.setCompoundDrawablesRelativeWithIntrinsicBounds(
			icon?.let { circleIcon(it, colors.color(R.color.mn_circle_teal), resources) }, null, chevron, null)
		title.compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.content_padding)
		title.minHeight = resources.getDimensionPixelSize(R.dimen.mn_settings_row_height)
		title.setTextColor(colors.text)
		title.layoutParams = title.layoutParams.apply { width = ViewGroup.LayoutParams.MATCH_PARENT }
		styleDivider(view, colors, last)
	}

	/** A downloadable map: round icon colored by state, title, details and download button. */
	@JvmStatic
	fun styleItemRow(view: View, nightMode: Boolean, downloaded: Boolean, outdated: Boolean, last: Boolean) {
		val context = view.context
		val colors = MineNavColors(context, nightMode)
		val resources = context.resources
		view.setBackgroundColor(colors.row)
		(view as? ViewGroup)?.getChildAt(0)?.minimumHeight = resources.getDimensionPixelSize(R.dimen.mn_settings_row_height)
		// A visible action button means the map needs a purchase or a plugin first
		val actionButton = view.findViewById<TextView>(R.id.rightButton)
		val locked = actionButton?.visibility == View.VISIBLE
		actionButton?.apply {
			isAllCaps = false
			background = colors.pill(colors.color(R.color.mn_primary))
			setTextColor(Color.WHITE)
			typeface = FontCache.getFont(FontCache.getAppTypeface(), 700)
		}
		view.findViewById<ImageView>(R.id.icon)?.let { icon ->
			val size = resources.getDimensionPixelSize(R.dimen.mn_settings_icon_circle)
			val inset = (ICON_INSET_DP * resources.displayMetrics.density).toInt()
			(icon.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
				it.width = size
				it.height = size
				it.marginEnd = resources.getDimensionPixelSize(R.dimen.content_padding)
				it.rightMargin = it.marginEnd
				icon.layoutParams = it
			}
			icon.setPadding(inset, inset, inset, inset)
			icon.background = colors.circle(colors.color(if (locked) R.color.mn_circle_grey else stateColor(downloaded, outdated)))
			MineNavStyle.tintIcon(icon, Color.WHITE)
		}
		view.findViewById<ImageView>(R.id.secondaryIcon)?.let { button ->
			button.background = null
			MineNavStyle.tintIcon(button, colors.accent)
		}
		styleDivider(view, colors, last)
	}

	@ColorRes
	private fun stateColor(downloaded: Boolean, outdated: Boolean): Int = when {
		downloaded && outdated -> R.color.mn_circle_orange
		downloaded -> R.color.mn_circle_green
		else -> R.color.mn_circle_blue
	}

	private fun styleDivider(view: View, colors: MineNavColors, last: Boolean) {
		view.findViewById<View>(R.id.list_item_divider)?.apply {
			setBackgroundColor(colors.divider)
			visibility = if (last) View.GONE else View.VISIBLE
			(layoutParams as? ViewGroup.MarginLayoutParams)?.let {
				it.marginStart = resources.getDimensionPixelSize(R.dimen.mn_settings_divider_start)
				it.leftMargin = it.marginStart
				it.height = (resources.displayMetrics.density).toInt().coerceAtLeast(1)
				layoutParams = it
			}
		}
	}

	// White glyph on a colored disc, for rows whose icon is a compound drawable
	private fun circleIcon(icon: Drawable, fill: Int, resources: Resources): Drawable {
		val size = resources.getDimensionPixelSize(R.dimen.mn_settings_icon_circle)
		val inset = (ICON_INSET_DP * resources.displayMetrics.density).toInt()
		val circle = GradientDrawable().apply {
			shape = GradientDrawable.OVAL
			setColor(fill)
			setSize(size, size)
		}
		val glyph = (icon.constantState?.newDrawable(resources)?.mutate() ?: icon).apply { setTint(Color.WHITE) }
		return LayerDrawable(arrayOf(circle, glyph)).apply {
			setLayerInset(1, inset, inset, inset, inset)
		}
	}
}
