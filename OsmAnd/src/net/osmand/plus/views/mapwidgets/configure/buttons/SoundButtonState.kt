package net.osmand.plus.views.mapwidgets.configure.buttons

import net.osmand.aidlapi.OsmAndCustomizationConstants.HUD_BTN_ID_SCHEME
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.minenav.MineNavButtons
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.shared.grid.ButtonPositionSize
import net.osmand.shared.grid.ButtonPositionSize.Companion.POS_RIGHT
import net.osmand.shared.grid.ButtonPositionSize.Companion.POS_TOP

/**
 * MineNAV: the Waze sound button, top right, which mutes or unmutes voice guidance.
 */
class SoundButtonState(app: OsmandApplication) : MapButtonState(app, SOUND_HUD_ID) {

	companion object {
		const val SOUND_HUD_ID = HUD_BTN_ID_SCHEME + "minenav_sound"
	}

	private val visibilityPref: CommonPreference<Boolean> =
		addPreference(settings.registerBooleanPreference("${id}_state", true)).makeProfile()

	override fun getName(): String = app.getString(R.string.mn_sound_button)

	override fun getDescription(): String = app.getString(R.string.mn_sound_button_descr)

	override fun isEnabled(): Boolean = visibilityPref.get()

	override fun getVisibilityPref(): CommonPreference<Boolean> = visibilityPref

	override fun getDefaultLayoutId(): Int = R.layout.map_sound_button

	override fun getDefaultSize(): Int = MineNavButtons.WAZE_BUTTON_SIZE_DP

	override fun getDefaultIconName(nightMode: Boolean?): String =
		if (settings.VOICE_MUTE.get()) "ic_action_volume_mute" else "ic_action_volume_up"

	override fun setupButtonPosition(position: ButtonPositionSize): ButtonPositionSize =
		setupButtonPosition(position, POS_RIGHT, POS_TOP, false, true)
}
