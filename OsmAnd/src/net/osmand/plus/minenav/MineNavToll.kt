package net.osmand.plus.minenav

import android.content.Context
import net.osmand.plus.R
import net.osmand.plus.routing.RouteCalculationResult
import java.util.Locale

/**
 * Rough toll for a route in Malaysia: the distance driven on toll roads at the class 1 rate of the
 * PLUS North-South Expressway. Fixed-fare plazas of the open toll highways are not counted on their
 * own, so the amount is shown as an estimate.
 */
object MineNavToll {

	// PLUS North-South Expressway, class 1 (cars), after the 2020 reduction
	private const val RATE_RM_PER_KM = 0.1115
	private const val MIN_TOLL_METERS = 500f

	/** The estimated toll in ringgit, or null when the route has no toll roads in Malaysia. */
	@JvmStatic
	fun estimate(route: RouteCalculationResult?): Double? {
		val segments = route?.originalRoute ?: return null
		var tollMeters = 0f
		for (segment in segments) {
			if (segment.`object`?.getValue("toll") != "yes") {
				continue
			}
			val start = segment.startPoint
			if (isInMalaysia(start.latitude, start.longitude)) {
				tollMeters += segment.distance
			}
		}
		return if (tollMeters >= MIN_TOLL_METERS) tollMeters / 1000.0 * RATE_RM_PER_KM else null
	}

	/** "Tolls ≈ RM 27.90", or null without tolls. */
	@JvmStatic
	fun describe(context: Context, route: RouteCalculationResult?): String? {
		val toll = estimate(route) ?: return null
		return context.getString(R.string.mn_toll_estimate, String.format(Locale.US, "RM %.2f", toll))
	}

	private fun isInMalaysia(lat: Double, lon: Double): Boolean {
		val singapore = lat < 1.47 && lon > 103.6 && lon < 104.1
		val peninsula = lat in 1.2..6.8 && lon in 99.6..104.6
		val borneo = lat in 0.85..7.4 && lon in 109.6..119.3
		return (peninsula && !singapore) || borneo
	}
}
