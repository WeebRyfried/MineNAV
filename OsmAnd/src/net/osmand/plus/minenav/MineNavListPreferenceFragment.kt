package net.osmand.plus.minenav

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceFragmentCompat
import net.osmand.plus.settings.fragments.OnPreferenceChanged
import net.osmand.plus.settings.preferences.ListPreferenceEx

/**
 * Waze-style page for an OsmAnd list setting: every option is a row and the chosen one has a
 * check mark. Used instead of the OsmAnd single choice bottom sheet.
 */
class MineNavListPreferenceFragment : MineNavPageFragment() {

	@Suppress("DEPRECATION")
	private val preference: ListPreferenceEx?
		get() {
			val key = arguments?.getString(KEY_PREFERENCE) ?: return null
			return (targetFragment as? PreferenceFragmentCompat)?.findPreference(key)
		}

	override fun getTitle(): CharSequence {
		val preference = preference ?: return ""
		return preference.dialogTitle ?: preference.title ?: ""
	}

	override fun createSections(): List<Section> {
		val preference = preference ?: return emptyList()
		val entries = preference.entries ?: return emptyList()
		val values = preference.entryValues ?: return emptyList()
		val selected = preference.findIndexOfValue(preference.value)
		val rows = entries.mapIndexed { index, entry ->
			Row.Choice(entry, index == selected) { select(preference, values[index]) }
		}
		return listOf(Section(preference.description?.takeIf { it.isNotEmpty() }, rows))
	}

	// Same steps as the Apply button of the OsmAnd sheet
	private fun select(preference: ListPreferenceEx, value: Any) {
		if (preference.callChangeListener(value)) {
			preference.value = value
		}
		@Suppress("DEPRECATION")
		(targetFragment as? OnPreferenceChanged)?.onPreferenceChanged(preference.key)
		refresh()
	}

	companion object {

		private const val KEY_PREFERENCE = "preference_key"
		private val TAG: String = MineNavListPreferenceFragment::class.java.simpleName

		@JvmStatic
		fun showInstance(manager: FragmentManager, key: String, target: Fragment) {
			val fragment = MineNavListPreferenceFragment().apply {
				arguments = Bundle().apply { putString(KEY_PREFERENCE, key) }
				@Suppress("DEPRECATION")
				setTargetFragment(target, 0)
			}
			// Added on top: the settings page keeps its view for the change callbacks
			show(manager, fragment, TAG, add = true)
		}
	}
}
