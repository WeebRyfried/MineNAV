package net.osmand.plus.views.controls

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.SpecialPointType
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.minenav.MineNavActions
import net.osmand.plus.minenav.MineNavMenuFragment
import net.osmand.plus.minenav.MineNavStyle
import net.osmand.plus.search.ShowQuickSearchMode
import net.osmand.plus.search.history.HistoryEntry
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.widgets.FrameLayoutEx

/**
 * Waze-style bottom sheet shown on the map while browsing: the mascot opens the main menu,
 * the search pill opens search, the chips plan a route to Home or Work or open saved places,
 * and the last searched places can be driven to with one tap.
 */
class WhereToCard @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayoutEx(context, attrs, defStyleAttr) {

	companion object {
		private const val MAX_RECENT_ITEMS = 2
	}

	private val app = context.applicationContext as OsmandApplication
	private var mapActivity: MapActivity? = null
	private var nightMode: Boolean? = null
	private var recentKey: List<String> = emptyList()

	fun setMapActivity(activity: MapActivity?) {
		mapActivity = activity
		if (activity == null) {
			return
		}
		findViewById<View>(R.id.where_to_menu).setOnClickListener { openMenu() }
		findViewById<View>(R.id.where_to_search).setOnClickListener { openSearch() }
		setupChip(R.id.where_to_home, R.drawable.ic_action_home_dark, R.string.home_button) {
			navigateToSpecialPoint(SpecialPointType.HOME, R.string.home_button)
		}
		setupChip(R.id.where_to_work, R.drawable.ic_action_work, R.string.work_button) {
			navigateToSpecialPoint(SpecialPointType.WORK, R.string.work_button)
		}
		setupChip(R.id.where_to_saved, R.drawable.ic_action_bookmark_filled, R.string.where_to_saved) {
			openSavedPlaces()
		}
	}

	fun update(show: Boolean, nightMode: Boolean) {
		if (this.nightMode != nightMode) {
			this.nightMode = nightMode
			applyColors(nightMode)
			recentKey = emptyList()
		}
		if (show) {
			updateRecent(nightMode)
		}
		MineNavStyle.animateSheet(this, show)
	}

	private fun setupChip(id: Int, @DrawableRes iconId: Int, @StringRes textId: Int, action: () -> Unit) {
		val chip = findViewById<View>(id)
		chip.findViewById<ImageView>(R.id.chip_icon).setImageResource(iconId)
		chip.findViewById<TextView>(R.id.chip_text).setText(textId)
		chip.setOnClickListener { action() }
	}

	private fun applyColors(nightMode: Boolean) {
		val surface = color(if (nightMode) R.color.mn_card_bg_dark else R.color.mn_card_bg_light)
		val field = color(if (nightMode) R.color.mn_field_bg_dark else R.color.mn_field_bg_light)
		val text = textColor(nightMode)
		val secondary = secondaryColor(nightMode)
		val stroke = color(if (nightMode) R.color.mn_chip_stroke_dark else R.color.mn_chip_stroke_light)
		val radius = resources.getDimension(R.dimen.where_to_card_radius)
		val sheetRadius = resources.getDimension(R.dimen.where_to_sheet_radius)

		MineNavStyle.setSheetBackground(this, findViewById(R.id.where_to_surface), GradientDrawable().apply {
			setColor(surface)
			cornerRadii = floatArrayOf(sheetRadius, sheetRadius, sheetRadius, sheetRadius, 0f, 0f, 0f, 0f)
		})
		findViewById<View>(R.id.where_to_handle).background =
			roundRect(color(if (nightMode) R.color.mn_handle_dark else R.color.mn_handle_light), 0, radius)
		findViewById<View>(R.id.where_to_search).background = roundRect(field, 0, radius)
		findViewById<ImageView>(R.id.where_to_search_icon).imageTintList = ColorStateList.valueOf(secondary)
		findViewById<TextView>(R.id.where_to_search_text).setTextColor(secondary)
		findViewById<TextView>(R.id.where_to_recent_title).setTextColor(secondary)
		findViewById<View>(R.id.where_to_menu).background = GradientDrawable().apply {
			shape = GradientDrawable.OVAL
			setColor(color(R.color.mn_lavender))
		}

		val chipIconColors = mapOf(
			R.id.where_to_home to color(R.color.mn_icon_home),
			R.id.where_to_work to color(R.color.mn_icon_work),
			R.id.where_to_saved to color(R.color.mn_primary)
		)
		for ((id, iconColor) in chipIconColors) {
			val chip = findViewById<View>(id)
			chip.background = roundRect(surface, stroke, radius)
			chip.findViewById<ImageView>(R.id.chip_icon).imageTintList = ColorStateList.valueOf(iconColor)
			chip.findViewById<TextView>(R.id.chip_text).setTextColor(text)
		}
	}

	private fun updateRecent(nightMode: Boolean) {
		// In landscape the sheet is a side panel with no room for the recent list
		val maxItems = if (AndroidUiHelper.isOrientationPortrait(context)) MAX_RECENT_ITEMS else 0
		val entries = app.searchHistoryHelper.getHistoryEntries(true)
			.sortedByDescending { it.lastAccessTime }
			.distinctBy { it.name.getSimpleName(context, false) }
			.take(maxItems)
		val key = entries.map { it.serializedName + it.lat + it.lon }
		if (key == recentKey) {
			return
		}
		recentKey = key
		val list = findViewById<LinearLayout>(R.id.where_to_recent_list)
		list.removeAllViews()
		AndroidUiHelper.updateVisibility(findViewById(R.id.where_to_recent_title), entries.isNotEmpty())
		val inflater = LayoutInflater.from(context)
		for (entry in entries) {
			val row = inflater.inflate(R.layout.map_where_to_recent_item, list, false)
			bindRecent(row, entry, nightMode)
			list.addView(row)
		}
	}

	private fun bindRecent(row: View, entry: HistoryEntry, nightMode: Boolean) {
		val name = entry.name
		val title = name.getSimpleName(context, false)
		val subtitle = name.typeName.orEmpty()
		row.findViewById<ImageView>(R.id.recent_icon).imageTintList = ColorStateList.valueOf(secondaryColor(nightMode))
		row.findViewById<TextView>(R.id.recent_title).apply {
			text = title
			setTextColor(textColor(nightMode))
		}
		row.findViewById<TextView>(R.id.recent_subtitle).apply {
			text = subtitle
			setTextColor(secondaryColor(nightMode))
			AndroidUiHelper.updateVisibility(this, subtitle.isNotEmpty())
		}
		row.setOnClickListener { navigateTo(LatLon(entry.lat, entry.lon), name) }
	}

	private fun roundRect(fill: Int, stroke: Int, radius: Float): GradientDrawable {
		return GradientDrawable().apply {
			cornerRadius = radius
			setColor(fill)
			if (stroke != 0) {
				setStroke(AndroidUtils.dpToPx(context, 1f), stroke)
			}
		}
	}

	private fun textColor(nightMode: Boolean): Int =
		color(if (nightMode) R.color.mn_text_dark else R.color.mn_text_light)

	private fun secondaryColor(nightMode: Boolean): Int =
		color(if (nightMode) R.color.mn_text_secondary_dark else R.color.mn_text_secondary_light)

	private fun color(@ColorRes id: Int): Int = ContextCompat.getColor(context, id)

	private fun openMenu() {
		val activity = mapActivity ?: return
		MapActivity.clearPrevActivityIntent()
		MineNavMenuFragment.showInstance(activity.supportFragmentManager)
	}

	private fun openSearch() {
		val activity = mapActivity ?: return
		val fragmentsHelper = activity.fragmentsHelper
		fragmentsHelper.dismissCardDialog()
		fragmentsHelper.showQuickSearch(ShowQuickSearchMode.NEW_IF_EXPIRED, false)
	}

	private fun openSavedPlaces() {
		mapActivity?.let { MineNavActions.openSavedPlaces(it) }
	}

	private fun navigateToSpecialPoint(type: SpecialPointType, @StringRes nameId: Int) {
		mapActivity?.let { MineNavActions.navigateToSpecialPoint(it, type, nameId) }
	}

	private fun navigateTo(latLon: LatLon, description: PointDescription) {
		mapActivity?.let { MineNavActions.navigateTo(it, latLon, description) }
	}
}
