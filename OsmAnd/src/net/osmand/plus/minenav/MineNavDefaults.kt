package net.osmand.plus.minenav

import net.osmand.plus.OsmandApplication
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmandSettings

/**
 * One-time resets that bring an existing install to the current MineNAV defaults.
 */
object MineNavDefaults {

	private const val CLEAN_MAP_VERSION = 2

	// The map details a Waze-like map leaves out; each can be turned back on in Map display
	private val CLEAN_MAP_HIDDEN = arrayOf("hideIcons", "hidePOILabels", "hideHouseNumbers", "hideBuildings", "noAdminboundaries")

	private const val DEFAULT_VOICE = "en-tts"

	@JvmStatic
	fun apply(app: OsmandApplication) {
		applyVoice(app)
		applyCleanMap(app)
	}

	// Voices are English and Japanese only; any other one chosen before goes back to English
	private fun applyVoice(app: OsmandApplication) {
		val pref = app.settings.VOICE_PROVIDER
		for (mode in ApplicationMode.allPossibleValues()) {
			val voice = pref.getModeValue(mode)
			if (!voice.isNullOrEmpty() && voice != OsmandSettings.VOICE_PROVIDER_NOT_USE && !MineNavVoices.isAllowed(voice)) {
				pref.setModeValue(mode, DEFAULT_VOICE)
			}
		}
	}

	private fun applyCleanMap(app: OsmandApplication) {
		val settings = app.settings
		val version = settings.registerIntPreference("mn_clean_map_version", 0).makeGlobal()
		if (version.get() >= CLEAN_MAP_VERSION) {
			return
		}
		for (mode in ApplicationMode.allPossibleValues()) {
			for (attrName in CLEAN_MAP_HIDDEN) {
				settings.getCustomRenderBooleanProperty(attrName).setModeValue(mode, true)
			}
		}
		version.set(CLEAN_MAP_VERSION)
	}
}
