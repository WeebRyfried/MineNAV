package net.osmand.plus.minenav

import androidx.annotation.StringRes
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.dialogs.SelectMapStyleBottomSheetDialogFragment
import net.osmand.plus.render.RendererRegistry
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.enums.DayNightMode

/**
 * Single-choice lists shown by [MineNavChoiceFragment], identified by a key.
 */
object MineNavChoices {

	const val MAP_STYLE = "map_style"
	const val DARK_MODE = "dark_mode"
	const val TRAVEL_MODE = "travel_mode"

	private val MAP_STYLES = listOf(
		RendererRegistry.MINENAV_RENDER, RendererRegistry.DEFAULT_RENDER, RendererRegistry.TOURING_VIEW,
		RendererRegistry.TOPO_RENDER, RendererRegistry.OSM_CARTO_RENDER, RendererRegistry.LIGHTRS_RENDER,
		RendererRegistry.UNIRS_RENDER, RendererRegistry.DESERT_RENDER
	)

	private val DARK_MODES = listOf(
		DayNightMode.APP_THEME to R.string.mn_dark_follow_phone,
		DayNightMode.AUTO to R.string.mn_dark_at_night,
		DayNightMode.DAY to R.string.mn_dark_always_light,
		DayNightMode.NIGHT to R.string.mn_dark_always_dark
	)

	class Option(val id: String, val title: String)

	class Choice(
		@StringRes val titleId: Int,
		val options: List<Option>,
		val selectedId: String,
		val onSelect: (MapActivity, String) -> Unit
	)

	@JvmStatic
	fun create(key: String, app: OsmandApplication): Choice? = when (key) {
		MAP_STYLE -> Choice(
			R.string.mn_settings_map_style,
			MAP_STYLES.map { Option(it, RendererRegistry.getRendererName(app, it)) },
			app.settings.RENDERER.get()
		) { activity, id -> SelectMapStyleBottomSheetDialogFragment.setStyle(activity, id) }

		DARK_MODE -> Choice(
			R.string.mn_map_dark_mode,
			DARK_MODES.map { (mode, titleId) -> Option(mode.name, app.getString(titleId)) },
			app.settings.DAYNIGHT_MODE.get().name
		) { activity, id ->
			val mode = DayNightMode.valueOf(id)
			// One switch for every profile, like Waze
			for (appMode in ApplicationMode.allPossibleValues()) {
				app.settings.DAYNIGHT_MODE.setModeValue(appMode, mode)
			}
			activity.refreshMapComplete()
		}

		TRAVEL_MODE -> Choice(
			R.string.mn_plan_travel_mode,
			ApplicationMode.values(app).filter { it != ApplicationMode.DEFAULT }.map { Option(it.stringKey, it.toHumanString()) },
			app.routingHelper.appMode.stringKey
		) { activity, id ->
			ApplicationMode.valueOfStringKey(id, null)?.let { MineNavActions.setTravelMode(activity, it) }
		}

		else -> null
	}

	@JvmStatic
	fun darkModeTitle(app: OsmandApplication): String {
		val current = app.settings.DAYNIGHT_MODE.get()
		val titleId = DARK_MODES.firstOrNull { it.first == current }?.second ?: R.string.mn_dark_follow_phone
		return app.getString(titleId)
	}
}
