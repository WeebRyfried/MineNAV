package net.osmand.plus.minenav

import android.text.format.DateFormat
import androidx.fragment.app.FragmentManager
import net.osmand.data.ValueHolder
import net.osmand.plus.R
import net.osmand.plus.helpers.TargetPoint
import net.osmand.plus.routing.IRouteInformationListener
import net.osmand.plus.search.ShowQuickSearchMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.util.Algorithms
import java.util.Date

/**
 * Waze-style "Plan a drive": where the trip starts, its stops and destination, how to travel,
 * then "Go now". Replaces OsmAnd's route planner everywhere it would open.
 */
class MineNavRoutePlannerFragment : MineNavPageFragment() {

	private val routeListener = object : IRouteInformationListener {
		override fun newRouteIsCalculated(newRoute: Boolean, showToast: ValueHolder<Boolean>?) {
			app.runInUIThread { if (isResumed) refresh() }
		}

		override fun routeWasCancelled() {
			app.runInUIThread { if (isResumed) refresh() }
		}

		override fun routeWasFinished() {}
	}

	override fun getTitle(): CharSequence =
		getString(if (app.routingHelper.isFollowingMode) R.string.mn_plan_current else R.string.mn_menu_plan_drive)

	override fun onResume() {
		super.onResume()
		app.routingHelper.addListener(routeListener)
	}

	override fun onPause() {
		app.routingHelper.removeListener(routeListener)
		super.onPause()
	}

	override fun onDestroy() {
		// Leaving without a destination ends the planning started for this page
		val routing = app.routingHelper
		if (isRemoving && app.targetPointsHelper.pointToNavigate == null
			&& routing.isRoutePlanningMode && !routing.isFollowingMode) {
			mapActivity?.mapActions?.stopNavigationWithoutConfirm()
		}
		super.onDestroy()
	}

	override fun createSections(): List<Section> {
		val targets = app.targetPointsHelper
		val start = targets.pointToStart
		val destination = targets.pointToNavigate
		val trip = mutableListOf<Row>()
		trip += Row.Open(R.drawable.ic_my_location, R.color.mn_circle_blue, getString(R.string.mn_plan_from),
			subtitle = start?.let { pointName(it) } ?: getString(R.string.mn_plan_my_location)) {
			MineNavActions.chooseRoutePoint(it, ShowQuickSearchMode.START_POINT_SELECTION)
		}
		if (start != null) {
			trip += Row.Open(R.drawable.ic_my_location, R.color.mn_circle_grey, getString(R.string.mn_plan_use_my_location)) {
				targets.clearStartPoint(true)
				refresh()
			}
		}
		targets.intermediatePoints.forEachIndexed { index, point ->
			trip += Row.Removable(R.drawable.ic_action_intermediate, R.color.mn_circle_orange, pointName(point),
				getString(R.string.mn_plan_stop, index + 1)) {
				targets.removeWayPoint(true, index)
				refresh()
			}
		}
		if (destination != null) {
			trip += Row.Open(R.drawable.ic_action_add, R.color.mn_circle_green, getString(R.string.mn_plan_add_stop)) {
				MineNavActions.chooseRoutePoint(it, ShowQuickSearchMode.INTERMEDIATE_SELECTION)
			}
		}
		trip += Row.Open(R.drawable.ic_action_flag, R.color.mn_circle_red, getString(R.string.mn_plan_to),
			subtitle = destination?.let { pointName(it) } ?: getString(R.string.mn_plan_choose_destination)) {
			MineNavActions.chooseRoutePoint(it, ShowQuickSearchMode.DESTINATION_SELECTION)
		}

		val mode = app.routingHelper.appMode
		val sections = mutableListOf(
			Section(null, trip),
			Section(null, listOf(
				Row.Open(mode.getIconRes(), R.color.mn_circle_purple, getString(R.string.mn_plan_travel_mode), mode.toHumanString()) {
					MineNavChoiceFragment.showInstance(it.supportFragmentManager, MineNavChoices.TRAVEL_MODE)
				},
				Row.Open(R.drawable.ic_action_settings, R.color.mn_circle_grey, getString(R.string.route_preview_options)) {
					MineNavRouteOptionsFragment.showInstance(it.supportFragmentManager)
				}
			))
		)
		createTripSummary()?.let { sections += Section(null, listOf(it)) }
		return sections
	}

	private fun createTripSummary(): Row? {
		val routing = app.routingHelper
		if (app.targetPointsHelper.pointToNavigate == null) {
			return null
		}
		if (!routing.isRouteCalculated || routing.isRouteBeingCalculated) {
			return Row.Info(R.drawable.ic_action_time, R.color.mn_circle_teal, "…",
				getString(R.string.route_preview_calculating))
		}
		val leftTime = routing.leftTime
		val distance = OsmAndFormatter.getFormattedDistance(routing.leftDistance.toFloat(), app)
		val arrival = DateFormat.getTimeFormat(app).format(Date(System.currentTimeMillis() + leftTime * 1000L))
		return Row.Info(R.drawable.ic_action_time, R.color.mn_circle_teal,
			OsmAndFormatter.getFormattedDuration(leftTime.toLong(), app),
			getString(R.string.route_preview_details, distance, arrival))
	}

	override fun createBottomButtons(): List<BottomButton> {
		val routing = app.routingHelper
		if (routing.isFollowingMode) {
			return listOf(
				BottomButton(getString(R.string.mn_plan_stop_navigation), false) {
					MineNavStyle.closeAllSettings(it)
					it.mapActions.stopNavigationWithoutConfirm()
				},
				BottomButton(getString(R.string.mn_plan_resume), true) { MineNavStyle.closeAllSettings(it) }
			)
		}
		val ready = app.targetPointsHelper.pointToNavigate != null
				&& routing.isRouteCalculated && !routing.isRouteBeingCalculated
		return listOf(BottomButton(getString(R.string.route_preview_go), true, ready) {
			MineNavStyle.closeAllSettings(it)
			it.mapActions.startNavigation()
		})
	}

	private fun pointName(point: TargetPoint): String {
		val name = point.getPointDescription(app).getSimpleName(app, false)
		return if (Algorithms.isEmpty(name)) point.onlyName else name
	}

	companion object {

		private val TAG: String = MineNavRoutePlannerFragment::class.java.simpleName

		@JvmStatic
		fun showInstance(manager: FragmentManager) {
			// The planner is asked for again whenever a point is picked while it is open
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG, true)) {
				show(manager, MineNavRoutePlannerFragment(), TAG)
			}
		}
	}
}
