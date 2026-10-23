package net.osmand.plus.views.controls

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.AndroidUtils

/**
 * Waze-style pill with the name of the street you are on, floating over the map just above
 * the "Where to?" sheet while browsing.
 */
class MineNavStreetPill @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

	private val app = context.applicationContext as OsmandApplication
	private var nightMode: Boolean? = null

	fun update(show: Boolean, nightMode: Boolean) {
		if (this.nightMode != nightMode) {
			this.nightMode = nightMode
			background = GradientDrawable().apply {
				cornerRadius = AndroidUtils.dpToPx(context, 100f).toFloat()
				setColor(ContextCompat.getColor(context, if (nightMode) R.color.mn_street_pill_dark else R.color.mn_street_pill_light))
			}
			setTextColor(ContextCompat.getColor(context, if (nightMode) R.color.mn_text_dark else R.color.mn_text_light))
		}
		val name = if (show) currentStreetName() else null
		if (!name.isNullOrEmpty() && name != text.toString()) {
			text = name
		}
		val visibility = if (name.isNullOrEmpty()) View.GONE else View.VISIBLE
		if (this.visibility != visibility) {
			this.visibility = visibility
		}
	}

	private fun currentStreetName(): String? {
		val provider = app.locationProvider
		val location = provider.lastKnownLocation ?: return null
		val road = provider.lastKnownRouteSegment ?: return null
		val settings = app.settings
		val locale = settings.MAP_PREFERRED_LOCALE.get()
		val transliterate = settings.MAP_TRANSLITERATE_NAMES.get()
		val name = road.getName(locale, transliterate)
		return if (!name.isNullOrEmpty()) name else road.getRef(locale, transliterate, road.bearingVsRouteDirection(location))
	}
}
