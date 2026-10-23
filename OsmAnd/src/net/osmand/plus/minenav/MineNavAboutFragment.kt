package net.osmand.plus.minenav

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.Version
import net.osmand.plus.activities.MapActivity

/**
 * Waze-style "Help and about" page: version, credits for the map data and for OsmAnd, and the
 * user guide.
 */
class MineNavAboutFragment : MineNavPageFragment() {

	override fun getTitleId(): Int = R.string.mn_menu_help

	override fun createSections(): List<Section> {
		return listOf(
			Section(null, listOf(
				Row.Info(R.drawable.ic_action_info_dark, R.color.mn_circle_blue, getString(R.string.mn_about_version),
					Version.getFullVersion(app))
			)),
			Section(getString(R.string.about_minenav_credits_title), listOf(
				Row.Open(R.drawable.ic_action_openstreetmap_logo, R.color.mn_circle_green, getString(R.string.mn_about_map_data),
					subtitle = getString(R.string.mn_about_map_data_descr)) { openLink(it, OSM_COPYRIGHT_URL) },
				Row.Open(R.drawable.ic_action_world_globe, R.color.mn_circle_orange, getString(R.string.mn_about_built_on),
					subtitle = getString(R.string.mn_about_built_on_descr)) { openLink(it, OSMAND_URL) }
			)),
			Section(getString(R.string.shared_string_help), listOf(
				Row.Open(R.drawable.ic_action_book_info, R.color.mn_circle_purple, getString(R.string.mn_about_guide),
					subtitle = getString(R.string.mn_about_guide_descr)) { openLink(it, USER_GUIDE_URL) }
			))
		)
	}

	private fun openLink(activity: MapActivity, url: String) {
		try {
			activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
		} catch (e: ActivityNotFoundException) {
			app.showShortToastMessage(R.string.no_activity_for_intent)
		}
	}

	companion object {

		private const val OSM_COPYRIGHT_URL = "https://www.openstreetmap.org/copyright"
		private const val OSMAND_URL = "https://osmand.net"
		private const val USER_GUIDE_URL = "https://docs.osmand.net/docs/user/"

		@JvmStatic
		fun showInstance(manager: FragmentManager) {
			show(manager, MineNavAboutFragment(), MineNavAboutFragment::class.java.simpleName)
		}
	}
}
