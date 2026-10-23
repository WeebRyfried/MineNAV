package net.osmand.plus.minenav

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R

/**
 * Waze-style single choice page: options with a check mark next to the selected one.
 */
class MineNavChoiceFragment : MineNavPageFragment() {

	private val choiceKey: String
		get() = requireArguments().getString(KEY_CHOICE).orEmpty()

	override fun getTitleId(): Int = MineNavChoices.create(choiceKey, app)?.titleId ?: R.string.shared_string_settings

	override fun createSections(): List<Section> {
		val choice = MineNavChoices.create(choiceKey, app) ?: return emptyList()
		val rows = choice.options.map { option ->
			Row.Choice(option.title, option.id == choice.selectedId) { activity ->
				choice.onSelect(activity, option.id)
				refresh()
			}
		}
		return listOf(Section(null, rows))
	}

	companion object {

		private const val KEY_CHOICE = "choice_key"

		@JvmStatic
		fun showInstance(manager: FragmentManager, choiceKey: String) {
			val fragment = MineNavChoiceFragment().apply {
				arguments = Bundle().apply { putString(KEY_CHOICE, choiceKey) }
			}
			show(manager, fragment, MineNavChoiceFragment::class.java.simpleName + choiceKey)
		}
	}
}
