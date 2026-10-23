package net.osmand.plus.views.controls.maphudbuttons

import android.content.Context
import android.util.AttributeSet
import net.osmand.plus.R
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState
import net.osmand.plus.views.mapwidgets.configure.buttons.SoundButtonState

/**
 * MineNAV: the Waze sound button. One tap mutes voice guidance, another brings it back.
 */
class SoundButton @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : MapButton(context, attrs, defStyleAttr) {

	private val buttonState: SoundButtonState = app.mapButtonsHelper.soundButtonState

	init {
		setOnClickListener {
			val mute = !settings.VOICE_MUTE.get()
			app.routingHelper.voiceRouter.setMute(mute)
			app.showShortToastMessage(if (mute) R.string.mn_sound_off else R.string.mn_sound_on)
			setInvalidated(true)
			update()
		}
	}

	override fun getButtonState(): MapButtonState = buttonState

	override fun shouldShow(): Boolean {
		// On the Waze home and while driving, out of the way everywhere else
		return !routeDialogOpened && visibilityHelper.shouldShowTopButtons()
				&& (isMineNavHomeVisible() || app.routingHelper.isFollowingMode)
	}
}
