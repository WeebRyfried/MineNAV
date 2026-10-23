package net.osmand.plus.views.controls

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.routing.RouteCalculationResult
import net.osmand.plus.minenav.MineNavColors
import net.osmand.plus.minenav.MineNavMapCamera
import net.osmand.plus.minenav.MineNavRouteOptionsFragment
import net.osmand.plus.minenav.MineNavStyle
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.widgets.FrameLayoutEx
import java.util.Date

/**
 * Waze-style route preview: trip time, distance and arrival time with "Go now", over a map
 * that shows the whole route. "Options" opens the MineNAV route options.
 */
class RoutePreviewCard @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayoutEx(context, attrs, defStyleAttr) {

	private val app = context.applicationContext as OsmandApplication
	private var mapActivity: MapActivity? = null
	private var colors: MineNavColors? = null
	private var overviewRoute: RouteCalculationResult? = null

	fun setMapActivity(activity: MapActivity?) {
		mapActivity = activity
		if (activity == null) {
			return
		}
		findViewById<View>(R.id.route_preview_close).setOnClickListener {
			activity.mapActions.stopNavigationWithoutConfirm()
			activity.mapRouteInfoMenu.resetRouteCalculation()
		}
		findViewById<View>(R.id.route_preview_options).setOnClickListener {
			MineNavRouteOptionsFragment.showInstance(activity.supportFragmentManager)
		}
		findViewById<View>(R.id.route_preview_go).setOnClickListener {
			if (app.routingHelper.isRouteCalculated) {
				activity.mapActions.startNavigation()
			}
		}
	}

	/**
	 * The preview replaces OsmAnd's planner once a destination is set; without one the planner
	 * is still needed to pick it.
	 */
	fun canReplaceRouteMenu(): Boolean {
		return app.targetPointsHelper.pointToNavigate != null && !app.routingHelper.isPublicTransportMode
	}

	fun update(show: Boolean, nightMode: Boolean) {
		if (colors?.nightMode != nightMode) {
			applyColors(MineNavColors(context, nightMode))
		}
		if (show) {
			updateInfo()
		}
		MineNavStyle.animateSheet(this, show)
	}

	private fun updateInfo() {
		val target = app.targetPointsHelper.pointToNavigate
		val name = target?.getPointDescription(context)?.getSimpleName(context, false).orEmpty()
		findViewById<TextView>(R.id.route_preview_to).text = context.getString(R.string.route_preview_to, name)

		val routingHelper = app.routingHelper
		val calculated = routingHelper.isRouteCalculated && !routingHelper.isRouteBeingCalculated
		val time = findViewById<TextView>(R.id.route_preview_time)
		val details = findViewById<TextView>(R.id.route_preview_details)
		if (calculated) {
			val leftTime = routingHelper.leftTime
			val distance = OsmAndFormatter.getFormattedDistance(routingHelper.leftDistance.toFloat(), app)
			val arrival = DateFormat.getTimeFormat(context).format(Date(System.currentTimeMillis() + leftTime * 1000L))
			time.text = OsmAndFormatter.getFormattedDuration(leftTime.toLong(), app)
			details.text = context.getString(R.string.route_preview_details, distance, arrival)
			val route = routingHelper.route
			if (route !== overviewRoute) {
				overviewRoute = route
				// After layout, so the sheet's height is known
				post { showWholeRoute() }
			}
		} else {
			time.text = "…"
			details.setText(R.string.route_preview_calculating)
		}
		AndroidUiHelper.updateVisibility(findViewById(R.id.route_preview_progress), !calculated)
		findViewById<View>(R.id.route_preview_go).apply {
			isEnabled = calculated
			alpha = if (calculated) 1f else 0.5f
		}
	}

	private fun showWholeRoute() {
		val activity = mapActivity ?: return
		val location = IntArray(2)
		val surface = findViewById<View>(R.id.route_preview_surface)
		surface.getLocationInWindow(location)
		if (!AndroidUiHelper.isOrientationPortrait(activity) && width < rootView.width) {
			// Landscape: the sheet stands at the side, the route goes beside it
			MineNavMapCamera.showWholeRoute(activity, 0, location[0] + surface.width)
		} else {
			val sheetTop = location[1] - translationY
			MineNavMapCamera.showWholeRoute(activity, (rootView.height - sheetTop).toInt(), 0)
		}
	}

	private fun applyColors(colors: MineNavColors) {
		this.colors = colors
		val sheetRadius = resources.getDimension(R.dimen.where_to_sheet_radius)
		MineNavStyle.setSheetBackground(this, findViewById(R.id.route_preview_surface), GradientDrawable().apply {
			setColor(colors.row)
			cornerRadii = floatArrayOf(sheetRadius, sheetRadius, sheetRadius, sheetRadius, 0f, 0f, 0f, 0f)
		})
		findViewById<View>(R.id.route_preview_handle).background =
			colors.pill(colors.color(if (colors.nightMode) R.color.mn_handle_dark else R.color.mn_handle_light))
		findViewById<TextView>(R.id.route_preview_to).setTextColor(colors.secondaryText)
		findViewById<TextView>(R.id.route_preview_time).setTextColor(colors.text)
		findViewById<TextView>(R.id.route_preview_details).setTextColor(colors.secondaryText)
		findViewById<ImageView>(R.id.route_preview_close).apply {
			background = colors.circle(colors.field)
			imageTintList = ColorStateList.valueOf(colors.text)
		}
		findViewById<ProgressBar>(R.id.route_preview_progress).indeterminateTintList =
			ColorStateList.valueOf(colors.color(R.color.mn_primary))
		findViewById<TextView>(R.id.route_preview_options).apply {
			background = colors.pill(colors.field)
			setTextColor(colors.text)
		}
		findViewById<TextView>(R.id.route_preview_go).apply {
			background = colors.pill(colors.color(R.color.mn_primary))
			setTextColor(colors.color(R.color.mn_text_dark))
		}
	}
}
