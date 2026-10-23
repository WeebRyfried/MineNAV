package net.osmand.plus.minenav

import net.osmand.plus.activities.MapActivity
import net.osmand.plus.utils.AndroidUtils

/**
 * Map camera moves of the MineNAV screens.
 */
object MineNavMapCamera {

	private const val EDGE_MARGIN_DP = 24f
	private const val ROUTE_BORDER = 0.9f
	private const val FLAT_ELEVATION = 90f

	/**
	 * Shows the whole calculated route, north up and flat, in the map space left free by a sheet
	 * that covers the bottom [coveredBottomPx] pixels of the screen (portrait) or its left
	 * [coveredLeftPx] pixels (landscape), like the Waze overview.
	 */
	@JvmStatic
	fun showWholeRoute(activity: MapActivity, coveredBottomPx: Int, coveredLeftPx: Int) {
		val app = activity.app
		val locations = app.routingHelper.route?.immutableAllLocations
		if (locations.isNullOrEmpty()) {
			return
		}
		var top = -90.0
		var bottom = 90.0
		var left = 180.0
		var right = -180.0
		for (location in locations) {
			top = maxOf(top, location.latitude)
			bottom = minOf(bottom, location.latitude)
			left = minOf(left, location.longitude)
			right = maxOf(right, location.longitude)
		}
		val mapView = activity.mapView
		// Following the position would move the map straight back to it
		app.mapViewTrackingUtilities.setMapLinkedToLocation(false)
		// North up and flat first, so the fit below works on the view it will end with
		mapView.setRotate(0f, true)
		mapView.setElevationAngle(FLAT_ELEVATION)
		val tileBox = mapView.rotatedTileBox
		val margin = AndroidUtils.dpToPx(app, EDGE_MARGIN_DP)
		val visibleTop = AndroidUtils.getStatusBarHeight(app) + margin
		val visibleHeight = tileBox.pixHeight - coveredBottomPx - visibleTop - margin
		val visibleLeft = coveredLeftPx + margin
		val visibleWidth = tileBox.pixWidth - visibleLeft - margin
		if (visibleHeight <= 0 || visibleWidth <= 0) {
			return
		}
		// fitRectToMap puts the route this far from the map's center point
		val offsetX = (visibleLeft + visibleWidth / 2f - tileBox.centerPixelX).toInt()
		val offsetY = (visibleTop + visibleHeight / 2f - tileBox.centerPixelY).toInt()
		mapView.fitRectToMap(tileBox, left, right, top, bottom, visibleWidth, visibleHeight,
			offsetY, offsetX, false, ROUTE_BORDER, false)
	}
}
