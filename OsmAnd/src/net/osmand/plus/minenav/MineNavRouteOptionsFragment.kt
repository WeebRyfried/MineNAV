package net.osmand.plus.minenav

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.routing.RoutingHelperUtils
import net.osmand.plus.settings.fragments.BaseSettingsFragment
import net.osmand.plus.settings.fragments.SettingsScreenType
import net.osmand.plus.utils.AndroidUtils
import net.osmand.router.GeneralRouter.RoutingParameterType

/**
 * Waze-style "Options" of the route preview: what the route should avoid and voice guidance.
 * Stops, the start point and every other route setting stay one tap away.
 */
class MineNavRouteOptionsFragment : MineNavPageFragment() {

	private class AvoidOption(val id: String, @DrawableRes val iconId: Int, @ColorRes val colorId: Int)

	override fun getTitleId(): Int = R.string.route_preview_options

	override fun createSections(): List<Section> {
		val mode = app.routingHelper.appMode
		val router = app.getRouter(mode)
		val parameters = if (router != null) RoutingHelperUtils.getParametersForDerivedProfile(mode, router) else emptyMap()
		val avoidRows = AVOID_OPTIONS.mapNotNull { option ->
			val parameter = parameters[option.id]
			if (parameter == null || parameter.type != RoutingParameterType.BOOLEAN) {
				return@mapNotNull null
			}
			val pref = settings.getCustomRoutingBooleanProperty(parameter.id, parameter.defaultBoolean)
			val title = AndroidUtils.getRoutingStringPropertyName(app, parameter.id, parameter.name)
			Row.Toggle(option.iconId, option.colorId, title, pref.getModeValue(mode)) { _, on ->
				pref.setModeValue(mode, on)
				app.routingHelper.onSettingsChanged(mode, true)
			}
		}
		val voiceRouter = app.routingHelper.voiceRouter
		val voice = Row.Toggle(R.drawable.ic_action_volume_up, R.color.mn_circle_blue,
			getString(R.string.mn_route_voice), !voiceRouter.isMuteForMode(mode)) { _, on ->
			voiceRouter.setMuteForMode(mode, !on)
		}
		val sections = mutableListOf<Section>()
		if (avoidRows.isNotEmpty()) {
			sections += Section(getString(R.string.mn_route_avoid), avoidRows)
		}
		sections += Section(null, listOf(voice))
		sections += Section(null, listOf(
			Row.Open(R.drawable.ic_action_plan_route, R.color.mn_circle_purple, getString(R.string.mn_route_stops)) {
				goBack()
				it.mapRouteInfoMenu.showHideMenu()
			},
			Row.Open(R.drawable.ic_action_settings, R.color.mn_circle_grey, getString(R.string.mn_route_all_settings)) {
				BaseSettingsFragment.showInstance(it, SettingsScreenType.ROUTE_PARAMETERS, mode)
			}
		))
		return sections
	}

	companion object {

		// The few avoid options Waze offers; the rest are in the full route settings
		private val AVOID_OPTIONS = listOf(
			AvoidOption("avoid_toll", R.drawable.mm_toll_booth, R.color.mn_circle_orange),
			AvoidOption("avoid_motorway", R.drawable.ic_action_avoid_motorways, R.color.mn_circle_blue),
			AvoidOption("avoid_ferries", R.drawable.mm_amenity_ferry_terminal, R.color.mn_circle_teal),
			AvoidOption("avoid_unpaved", R.drawable.ic_action_road_works_dark, R.color.mn_circle_green)
		)

		@JvmStatic
		fun showInstance(manager: FragmentManager) {
			show(manager, MineNavRouteOptionsFragment(), MineNavRouteOptionsFragment::class.java.simpleName)
		}
	}
}
