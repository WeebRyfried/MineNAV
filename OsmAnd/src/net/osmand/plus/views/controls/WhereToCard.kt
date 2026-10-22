package net.osmand.plus.views.controls

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import net.osmand.data.FavouritePoint
import net.osmand.data.LatLon
import net.osmand.data.SpecialPointType
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.dashboard.DashboardType
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.search.ShowQuickSearchMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.widgets.FrameLayoutEx

/**
 * Waze-style "Where to?" card shown at the bottom of the map while browsing:
 * the mascot opens the main menu, the search pill opens search, and the chips
 * plan a route to Home or Work or open the saved places.
 */
class WhereToCard @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayoutEx(context, attrs, defStyleAttr) {

	private val app = context.applicationContext as OsmandApplication
	private var mapActivity: MapActivity? = null
	private var nightMode: Boolean? = null

	fun setMapActivity(activity: MapActivity?) {
		mapActivity = activity
		if (activity == null) {
			return
		}
		findViewById<View>(R.id.where_to_menu).setOnClickListener { openMenu(it) }
		findViewById<View>(R.id.where_to_search).setOnClickListener { openSearch() }
		setupChip(R.id.where_to_home, R.drawable.ic_action_home_dark, R.string.home_button) {
			navigateTo(SpecialPointType.HOME, R.string.home_button)
		}
		setupChip(R.id.where_to_work, R.drawable.ic_action_work, R.string.work_button) {
			navigateTo(SpecialPointType.WORK, R.string.work_button)
		}
		setupChip(R.id.where_to_saved, R.drawable.ic_action_favorite, R.string.shared_string_favorites) {
			openSavedPlaces()
		}
	}

	fun update(show: Boolean, nightMode: Boolean) {
		if (this.nightMode != nightMode) {
			this.nightMode = nightMode
			applyColors(nightMode)
		}
		AndroidUiHelper.updateVisibility(this, show)
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
		val text = color(if (nightMode) R.color.mn_text_dark else R.color.mn_text_light)
		val stroke = color(if (nightMode) R.color.mn_chip_stroke_dark else R.color.mn_chip_stroke_light)
		val radius = resources.getDimension(R.dimen.where_to_card_radius)

		findViewById<MaterialCardView>(R.id.where_to_surface).setCardBackgroundColor(surface)
		findViewById<View>(R.id.where_to_search).background = roundRect(field, 0, radius)
		findViewById<ImageView>(R.id.where_to_search_icon).imageTintList = ColorStateList.valueOf(text)
		findViewById<TextView>(R.id.where_to_search_text).setTextColor(text)

		val avatar = GradientDrawable().apply {
			shape = GradientDrawable.OVAL
			setColor(color(R.color.mn_lavender))
		}
		findViewById<View>(R.id.where_to_menu).background = avatar

		for (id in intArrayOf(R.id.where_to_home, R.id.where_to_work, R.id.where_to_saved)) {
			val chip = findViewById<View>(id)
			chip.background = roundRect(surface, stroke, radius)
			chip.findViewById<ImageView>(R.id.chip_icon).imageTintList = ColorStateList.valueOf(text)
			chip.findViewById<TextView>(R.id.chip_text).setTextColor(text)
		}
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

	private fun color(@ColorRes id: Int): Int = ContextCompat.getColor(context, id)

	private fun openMenu(view: View) {
		val activity = mapActivity ?: return
		MapActivity.clearPrevActivityIntent()
		if (app.settings.SHOW_DASHBOARD_ON_MAP_SCREEN.get()) {
			activity.dashboard.setDashboardVisibility(true, DashboardType.DASHBOARD,
				AndroidUtils.getCenterViewCoordinates(view))
		} else {
			activity.openDrawer()
		}
	}

	private fun openSearch() {
		val activity = mapActivity ?: return
		val fragmentsHelper = activity.fragmentsHelper
		fragmentsHelper.dismissCardDialog()
		fragmentsHelper.showQuickSearch(ShowQuickSearchMode.NEW_IF_EXPIRED, false)
	}

	private fun openSavedPlaces() {
		val activity = mapActivity ?: return
		val intent = Intent(activity, app.appCustomization.myPlacesActivity)
		intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		activity.startActivity(intent)
	}

	private fun navigateTo(type: SpecialPointType, @StringRes nameId: Int) {
		val activity = mapActivity ?: return
		val point: FavouritePoint? = app.favoritesHelper.getSpecialPoint(type)
		if (point == null) {
			// Route planning has Home and Work cards where the user can set them
			app.showShortToastMessage(app.getString(R.string.where_to_set_place, app.getString(nameId)))
			activity.mapActions.doRoute()
			return
		}
		val latLon = LatLon(point.latitude, point.longitude)
		app.targetPointsHelper.navigateToPoint(latLon, true, -1, point.getPointDescription(app))
		activity.mapActions.enterRoutePlanningModeGivenGpx(null, null, null, true, true)
	}
}
