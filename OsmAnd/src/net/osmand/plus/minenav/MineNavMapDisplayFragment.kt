package net.osmand.plus.minenav

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.dashboard.DashboardType
import net.osmand.plus.helpers.ExternalApiHelper
import net.osmand.plus.render.RendererRegistry
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmandSettings

/**
 * Waze-style "Map display" page with the map options people actually change. Everything else
 * stays reachable through "More map options".
 */
class MineNavMapDisplayFragment : MineNavPageFragment() {

	override fun getTitleId(): Int = R.string.mn_settings_map_display

	override fun createSections(): List<Section> {
		val navMode = ExternalApiHelper.getNavigationProfile(app) ?: ApplicationMode.CAR
		val styleName = RendererRegistry.getRendererName(requireContext(), settings.RENDERER.get())
		return listOf(
			Section(null, listOf(
				Row.Open(R.drawable.ic_action_moon, R.color.mn_circle_purple, getString(R.string.mn_map_dark_mode),
					MineNavChoices.darkModeTitle(app)) {
					MineNavChoiceFragment.showInstance(it.supportFragmentManager, MineNavChoices.DARK_MODE)
				},
				Row.Open(R.drawable.ic_action_map_style, R.color.mn_circle_green, getString(R.string.mn_settings_map_style), styleName) {
					MineNavChoiceFragment.showInstance(it.supportFragmentManager, MineNavChoices.MAP_STYLE)
				},
				Row.Toggle(R.drawable.ic_action_compass, R.color.mn_circle_blue, getString(R.string.mn_map_north_up),
					settings.ROTATE_MAP.getModeValue(navMode) == OsmandSettings.ROTATE_MAP_NONE) { _, on ->
					settings.ROTATE_MAP.setModeValue(navMode,
						if (on) OsmandSettings.ROTATE_MAP_NONE else OsmandSettings.ROTATE_MAP_BEARING)
					app.mapViewTrackingUtilities.updateSettings()
				},
				Row.Toggle(R.drawable.ic_action_ruler, R.color.mn_circle_cyan, getString(R.string.mn_map_auto_zoom),
					settings.AUTO_ZOOM_MAP.getModeValue(navMode)) { _, on ->
					settings.AUTO_ZOOM_MAP.setModeValue(navMode, on)
				}
			)),
			Section(getString(R.string.mn_show_on_map), listOf(
				mapDetailToggle(R.drawable.ic_action_poi_brand, R.color.mn_circle_orange, R.string.mn_map_place_icons, "hideIcons"),
				mapDetailToggle(R.drawable.ic_action_poi_name, R.color.mn_circle_teal, R.string.mn_map_place_names, "hidePOILabels"),
				mapDetailToggle(R.drawable.ic_action_building, R.color.mn_circle_grey, R.string.mn_map_buildings, "hideBuildings"),
				mapDetailToggle(R.drawable.ic_action_building_number, R.color.mn_circle_pink, R.string.mn_map_house_numbers, "hideHouseNumbers"),
				Row.Toggle(R.drawable.ic_action_speed, R.color.mn_circle_red, getString(R.string.mn_map_speedometer),
					settings.SHOW_SPEEDOMETER.getModeValue(navMode)) { _, on ->
					settings.SHOW_SPEEDOMETER.setModeValue(navMode, on)
				}
			)),
			Section(null, listOf(
				Row.Open(R.drawable.ic_action_layers, R.color.mn_circle_blue, getString(R.string.mn_map_more)) {
					MineNavStyle.closeAllSettings(it)
					it.dashboard.setDashboardVisibility(true, DashboardType.CONFIGURE_MAP)
				}
			))
		)
	}

	// The map style hides details with "hide..." switches; the page shows them as "show" toggles
	private fun mapDetailToggle(@DrawableRes iconId: Int, @ColorRes colorId: Int, @StringRes titleId: Int,
	                            hideAttrName: String): Row.Toggle {
		val pref = settings.getCustomRenderBooleanProperty(hideAttrName)
		return Row.Toggle(iconId, colorId, getString(titleId), !pref.get()) { activity, on ->
			for (mode in ApplicationMode.allPossibleValues()) {
				pref.setModeValue(mode, !on)
			}
			activity.refreshMapComplete()
			activity.updateLayers()
		}
	}

	companion object {

		@JvmStatic
		fun showInstance(manager: FragmentManager) {
			show(manager, MineNavMapDisplayFragment(), MineNavMapDisplayFragment::class.java.simpleName)
		}
	}
}
