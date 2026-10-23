package net.osmand.plus.minenav

import android.content.Intent
import androidx.annotation.StringRes
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.SpecialPointType
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.search.ShowQuickSearchMode
import net.osmand.plus.settings.backend.ApplicationMode

/**
 * Actions shared by the MineNAV sheet, search and menu, so they all behave the same way.
 */
object MineNavActions {

	/** Opens the "Go now" route preview to [latLon]. */
	@JvmStatic
	fun navigateTo(activity: MapActivity, latLon: LatLon, description: PointDescription?) {
		val app = activity.app
		app.targetPointsHelper.navigateToPoint(latLon, true, -1, description)
		activity.mapActions.enterRoutePlanningModeGivenGpx(null, null, null, true, true)
	}

	/** Drives to Home or Work, or opens the search to set them first. */
	@JvmStatic
	fun navigateToSpecialPoint(activity: MapActivity, type: SpecialPointType, @StringRes nameId: Int) {
		val app = activity.app
		val point = app.favoritesHelper.getSpecialPoint(type)
		if (point == null) {
			app.showShortToastMessage(app.getString(R.string.mn_set_place_hint, app.getString(nameId).lowercase()))
			val mode = if (type == SpecialPointType.WORK) {
				ShowQuickSearchMode.WORK_POINT_SELECTION
			} else {
				ShowQuickSearchMode.HOME_POINT_SELECTION
			}
			activity.fragmentsHelper.showQuickSearch(mode, false)
			return
		}
		navigateTo(activity, LatLon(point.latitude, point.longitude), point.getPointDescription(app))
	}

	/** Opens the search to pick the start, a stop or the destination of the planned route. */
	@JvmStatic
	fun chooseRoutePoint(activity: MapActivity, mode: ShowQuickSearchMode) {
		activity.fragmentsHelper.showQuickSearch(mode, false)
	}

	/** Recalculates the planned route for another travel mode, as OsmAnd's planner does. */
	@JvmStatic
	fun setTravelMode(activity: MapActivity, mode: ApplicationMode) {
		val app = activity.app
		val routing = app.routingHelper
		routing.appMode = mode
		if (app.settings.applicationMode != ApplicationMode.DEFAULT) {
			app.settings.setApplicationMode(mode, false)
		}
		app.initVoiceCommandPlayer(activity, mode, null, true, false, true, true)
		routing.onSettingsChanged(true)
	}

	/** Opens the Waze-style list of Home, Work and favorites. */
	@JvmStatic
	fun openSavedPlaces(activity: MapActivity) {
		MineNavSavedPlacesFragment.showInstance(activity.supportFragmentManager)
	}

	/** Opens the full My Places screen, where saved places are edited. */
	@JvmStatic
	fun openMyPlaces(activity: MapActivity) {
		val intent = Intent(activity, activity.app.appCustomization.myPlacesActivity)
		intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		activity.startActivity(intent)
	}
}
