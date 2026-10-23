package net.osmand.plus.minenav

import android.content.Intent
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.helpers.ExternalApiHelper
import net.osmand.plus.render.RendererRegistry
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.fragments.BaseSettingsFragment
import net.osmand.plus.settings.fragments.SettingsScreenType

/**
 * Waze-style settings hub: short sections of rows with colored round icons. Each row opens
 * the page that holds the real options.
 */
class MineNavSettingsFragment : MineNavPageFragment() {

	override fun getTitleId(): Int = R.string.shared_string_settings

	override fun createSections(): List<Section> {
		val navMode = ExternalApiHelper.getNavigationProfile(app) ?: ApplicationMode.CAR
		val screen = { type: SettingsScreenType, mode: ApplicationMode? ->
			{ activity: MapActivity -> BaseSettingsFragment.showInstance(activity, type, mode); Unit }
		}
		val styleName = RendererRegistry.getRendererName(requireContext(), settings.RENDERER.get())
		return listOf(
			Section(null, listOf(
				Row.Open(R.drawable.ic_action_settings, R.color.mn_circle_cyan, getString(R.string.mn_settings_general),
					action = screen(SettingsScreenType.GLOBAL_SETTINGS, null)),
				Row.Open(R.drawable.ic_action_map_style, R.color.mn_circle_green, getString(R.string.mn_settings_map_display)) {
					MineNavMapDisplayFragment.showInstance(it.supportFragmentManager)
				},
				Row.Open(R.drawable.ic_action_volume_up, R.color.mn_circle_blue, getString(R.string.mn_settings_voice),
					action = screen(SettingsScreenType.VOICE_ANNOUNCES, navMode))
			)),
			Section(getString(R.string.mn_settings_driving), listOf(
				Row.Open(R.drawable.ic_action_gdirections_dark, R.color.mn_circle_blue, getString(R.string.mn_settings_navigation),
					action = screen(SettingsScreenType.NAVIGATION, navMode)),
				Row.Open(R.drawable.ic_action_avoid_motorways, R.color.mn_circle_purple, getString(R.string.mn_settings_route_options),
					action = screen(SettingsScreenType.ROUTE_PARAMETERS, navMode)),
				Row.Open(R.drawable.ic_action_car_dark, R.color.mn_circle_cyan, getString(R.string.mn_settings_vehicle),
					action = screen(SettingsScreenType.VEHICLE_PARAMETERS, navMode)),
				Row.Open(R.drawable.ic_action_alert, R.color.mn_circle_orange, getString(R.string.mn_settings_alerts),
					action = screen(SettingsScreenType.SCREEN_ALERTS, navMode))
			)),
			Section(getString(R.string.mn_settings_maps), listOf(
				Row.Open(R.drawable.ic_action_import, R.color.mn_circle_teal, getString(R.string.mn_menu_download_maps)) {
					it.startActivity(Intent(it, app.appCustomization.downloadActivity))
				},
				Row.Open(R.drawable.ic_action_layers, R.color.mn_circle_green, getString(R.string.mn_settings_map_style), styleName) {
					MineNavChoiceFragment.showInstance(it.supportFragmentManager, MineNavChoices.MAP_STYLE)
				}
			)),
			Section(getString(R.string.mn_settings_app), listOf(
				Row.Open(R.drawable.ic_action_ruler_unit, R.color.mn_circle_purple, getString(R.string.mn_settings_units),
					action = screen(SettingsScreenType.GENERAL_PROFILE, settings.applicationMode)),
				Row.Open(R.drawable.ic_action_manage_profiles, R.color.mn_circle_pink, getString(R.string.mn_settings_profiles),
					action = screen(SettingsScreenType.MAIN_SETTINGS, null)),
				Row.Open(R.drawable.ic_action_folder_favorites, R.color.mn_circle_grey, getString(R.string.mn_settings_storage),
					action = screen(SettingsScreenType.DATA_STORAGE, null)),
				Row.Open(R.drawable.ic_action_help, R.color.mn_circle_red, getString(R.string.mn_menu_help)) {
					MineNavAboutFragment.showInstance(it.supportFragmentManager)
				}
			))
		)
	}

	companion object {

		val TAG: String = MineNavSettingsFragment::class.java.simpleName

		@JvmStatic
		fun showInstance(manager: FragmentManager) {
			show(manager, MineNavSettingsFragment(), TAG)
		}
	}
}
