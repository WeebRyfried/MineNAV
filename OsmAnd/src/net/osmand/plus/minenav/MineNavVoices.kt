package net.osmand.plus.minenav

import java.util.Locale

/**
 * MineNAV keeps voice guidance to English and Japanese, both for the TTS and the recorded voices.
 */
object MineNavVoices {

	private val LANGUAGES = setOf("en", "ja")

	/** True for a voice file of an allowed language, such as "en-gb_tts.js" or "ja_0.voice.zip". */
	@JvmStatic
	fun isAllowed(fileName: String?): Boolean {
		if (fileName.isNullOrEmpty()) {
			return false
		}
		val language = fileName.lowercase(Locale.US).split('-', '_', '.').first()
		return language in LANGUAGES
	}
}
