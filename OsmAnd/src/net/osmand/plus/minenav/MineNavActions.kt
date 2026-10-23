package net.osmand.plus.minenav

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
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

	const val VOICE_SEARCH_REQUEST = 2601

	// How long a "New" place search may take before its place card no longer opens the editor
	private const val NEW_PLACE_TIMEOUT_MS = 5 * 60 * 1000L
	private var newPlaceRequestTime = 0L

	/** Opens the MineNAV menu, from the menu button on the map. */
	@JvmStatic
	fun openMenu(activity: MapActivity) {
		MapActivity.clearPrevActivityIntent()
		MineNavMenuFragment.showInstance(activity.supportFragmentManager)
	}

	/** Asks the system speech recognizer for a destination; the result opens the search. */
	@JvmStatic
	fun startVoiceSearch(activity: MapActivity) {
		val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
			putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
			putExtra(RecognizerIntent.EXTRA_PROMPT, activity.getString(R.string.where_to_hint))
		}
		try {
			activity.startActivityForResult(intent, VOICE_SEARCH_REQUEST)
		} catch (e: ActivityNotFoundException) {
			activity.app.showShortToastMessage(R.string.mn_voice_search_unavailable)
		}
	}

	/** Handles the speech recognizer's answer; returns true when the result was ours. */
	@JvmStatic
	fun onActivityResult(activity: MapActivity, requestCode: Int, resultCode: Int, data: Intent?): Boolean {
		if (requestCode != VOICE_SEARCH_REQUEST) {
			return false
		}
		val text = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
		if (resultCode == Activity.RESULT_OK && !text.isNullOrBlank()) {
			activity.fragmentsHelper.showQuickSearch(text)
		}
		return true
	}

	/** "New" on the sheet: find a place, and its card opens straight in the save editor. */
	@JvmStatic
	fun saveNewPlace(activity: MapActivity) {
		newPlaceRequestTime = System.currentTimeMillis()
		activity.app.showShortToastMessage(R.string.mn_new_place_hint)
		activity.fragmentsHelper.showQuickSearch(ShowQuickSearchMode.NEW, false)
	}

	/** Called when a place card opens: true once after "New", when the card should go to the editor. */
	@JvmStatic
	fun consumeNewPlaceRequest(): Boolean {
		val pending = newPlaceRequestTime > 0 && System.currentTimeMillis() - newPlaceRequestTime < NEW_PLACE_TIMEOUT_MS
		newPlaceRequestTime = 0
		return pending
	}

	/** The search was left without choosing a place: "New" is over. */
	@JvmStatic
	fun cancelNewPlaceRequest() {
		newPlaceRequestTime = 0
	}

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
