package net.osmand.plus.minenav

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseFullScreenFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.utils.AndroidUtils

/**
 * Base for the Waze-style MineNAV list pages: a toolbar with back, centered title and close,
 * then sections of rows that open pages, toggle options or pick one choice.
 */
abstract class MineNavPageFragment : BaseFullScreenFragment() {

	sealed class Row(@DrawableRes val iconId: Int, @ColorRes val circleColorId: Int, val title: String) {

		// Fill of the round icon when it is not a color resource, e.g. a favorite's own color
		@ColorInt
		open val circleColor: Int? = null

		class Open(@DrawableRes iconId: Int, @ColorRes circleColorId: Int, title: String,
		           val value: String? = null, val subtitle: String? = null,
		           @ColorInt override val circleColor: Int? = null,
		           val action: (MapActivity) -> Unit) : Row(iconId, circleColorId, title)

		class Info(@DrawableRes iconId: Int, @ColorRes circleColorId: Int, title: String,
		           val subtitle: String? = null) : Row(iconId, circleColorId, title)

		class Toggle(@DrawableRes iconId: Int, @ColorRes circleColorId: Int, title: String,
		             val checked: Boolean, val onChange: (MapActivity, Boolean) -> Unit) : Row(iconId, circleColorId, title)

		class Choice(title: String, val selected: Boolean, val action: (MapActivity) -> Unit) : Row(0, 0, title)

		// A row with an X to take it out, like a stop of the route
		class Removable(@DrawableRes iconId: Int, @ColorRes circleColorId: Int, title: String,
		                val subtitle: String? = null, val onRemove: (MapActivity) -> Unit) : Row(iconId, circleColorId, title)
	}

	class Section(val title: String?, val rows: List<Row>)

	class BottomButton(val title: String, val primary: Boolean, val enabled: Boolean = true,
	                   val action: (MapActivity) -> Unit)

	protected lateinit var colors: MineNavColors
	private var list: LinearLayout? = null

	@StringRes
	protected open fun getTitleId(): Int = 0

	protected open fun getTitle(): CharSequence = getString(getTitleId())

	protected abstract fun createSections(): List<Section>

	/** Pill buttons pinned under the list; none by default. */
	protected open fun createBottomButtons(): List<BottomButton> = emptyList()

	override fun getContentStatusBarNightMode(): Boolean = nightMode

	override fun getStatusBarColorId(): Int {
		AndroidUiHelper.setStatusBarContentColor(view, nightMode)
		return if (nightMode) R.color.mn_row_bg_dark else R.color.mn_row_bg_light
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		updateNightMode()
		colors = MineNavColors(requireContext(), nightMode)
		val view = themedInflater.inflate(R.layout.fragment_minenav_settings, container, false)
		view.setBackgroundColor(colors.screen)
		view.findViewById<View>(R.id.settings_toolbar).setBackgroundColor(colors.row)
		view.findViewById<TextView>(R.id.settings_title).apply {
			text = getTitle()
			setTextColor(colors.text)
		}
		view.findViewById<ImageView>(R.id.settings_back).apply {
			imageTintList = ColorStateList.valueOf(colors.text)
			setOnClickListener { goBack() }
		}
		view.findViewById<ImageView>(R.id.settings_close).apply {
			imageTintList = ColorStateList.valueOf(colors.text)
			setOnClickListener { activity?.let { MineNavStyle.closeAllSettings(it) } }
		}
		list = view.findViewById(R.id.settings_items)
		// Values may also change in a sheet opened over this page
		view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
		return view
	}

	private val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
		if (hasFocus && isResumed) {
			refresh()
		}
	}

	override fun onDestroyView() {
		view?.viewTreeObserver?.removeOnWindowFocusChangeListener(focusListener)
		list = null
		super.onDestroyView()
	}

	override fun onResume() {
		super.onResume()
		// Values may have changed on a page opened from here
		refresh()
	}

	protected fun refresh() {
		val list = list ?: return
		list.removeAllViews()
		createSections().forEachIndexed { index, section -> addSection(list, section, index == 0) }
		updateBottomButtons()
	}

	private fun updateBottomButtons() {
		val bar = view?.findViewById<View>(R.id.settings_bottom_bar) ?: return
		val buttons = createBottomButtons()
		bar.setBackgroundColor(colors.row)
		AndroidUiHelper.updateVisibility(bar, buttons.isNotEmpty())
		val primary = buttons.firstOrNull { it.primary }
		val secondary = buttons.firstOrNull { !it.primary }
		bindBottomButton(bar.findViewById(R.id.settings_bottom_primary), primary,
			colors.color(R.color.mn_primary), colors.color(R.color.mn_text_dark))
		bindBottomButton(bar.findViewById(R.id.settings_bottom_secondary), secondary, colors.field, colors.text)
	}

	private fun bindBottomButton(view: TextView, button: BottomButton?, @ColorInt fill: Int, @ColorInt textColor: Int) {
		AndroidUiHelper.updateVisibility(view, button != null)
		if (button == null) {
			return
		}
		view.text = button.title
		view.background = colors.pill(fill)
		view.setTextColor(textColor)
		view.isEnabled = button.enabled
		view.alpha = if (button.enabled) 1f else 0.5f
		view.setOnClickListener { mapActivity?.let(button.action) }
	}

	protected fun goBack() {
		val manager = activity?.supportFragmentManager ?: return
		if (!manager.isStateSaved) {
			manager.popBackStack()
		}
	}

	// Same spacing as the OsmAnd pages restyled by MineNavStyle.styleSettingsRow
	private fun addSection(list: LinearLayout, section: Section, first: Boolean) {
		val padding = resources.getDimensionPixelSize(R.dimen.content_padding)
		if (section.title != null) {
			val top = resources.getDimensionPixelSize(if (first) R.dimen.mn_header_first_top else R.dimen.mn_header_top)
			val header = TextView(requireContext()).apply {
				setPadding(padding, top, padding, resources.getDimensionPixelSize(R.dimen.mn_header_bottom))
				setTextColor(colors.secondaryText)
				setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.mn_header_text_size))
				text = section.title
			}
			list.addView(header)
		} else if (!first) {
			list.addView(Space(requireContext()), LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.mn_section_gap)))
		}
		val group = LinearLayout(requireContext()).apply {
			orientation = LinearLayout.VERTICAL
			setBackgroundColor(colors.row)
		}
		section.rows.forEachIndexed { index, row -> group.addView(createRow(group, row, index == section.rows.lastIndex)) }
		list.addView(group)
	}

	private fun createRow(parent: ViewGroup, row: Row, last: Boolean): View {
		val view = themedInflater.inflate(R.layout.minenav_settings_row, parent, false)
		val icon = view.findViewById<ImageView>(R.id.row_icon)
		if (row.iconId != 0) {
			icon.setImageResource(row.iconId)
			icon.background = colors.circle(row.circleColor ?: colors.color(row.circleColorId))
			icon.imageTintList = ColorStateList.valueOf(Color.WHITE)
		} else {
			icon.visibility = View.GONE
		}
		view.findViewById<TextView>(R.id.row_title).apply {
			text = row.title
			setTextColor(colors.text)
		}
		view.findViewById<View>(R.id.row_divider).apply {
			setBackgroundColor(colors.divider)
			AndroidUiHelper.updateVisibility(this, !last)
			if (row.iconId == 0) {
				(layoutParams as ViewGroup.MarginLayoutParams).marginStart =
					resources.getDimensionPixelSize(R.dimen.content_padding)
			}
		}
		val value = view.findViewById<TextView>(R.id.row_value)
		val switch = view.findViewById<SwitchCompat>(R.id.row_switch)
		val check = view.findViewById<ImageView>(R.id.row_check)
		val chevron = view.findViewById<ImageView>(R.id.row_chevron)
		chevron.imageTintList = ColorStateList.valueOf(colors.secondaryText)
		val content = view.findViewById<View>(R.id.row_content)
		when (row) {
			is Row.Open -> {
				value.text = row.value
				value.setTextColor(colors.secondaryText)
				AndroidUiHelper.updateVisibility(value, !row.value.isNullOrEmpty())
				bindSubtitle(view, row.subtitle)
				content.setOnClickListener { mapActivity?.let(row.action) }
			}
			is Row.Info -> {
				chevron.visibility = View.GONE
				bindSubtitle(view, row.subtitle)
				content.background = null
			}
			is Row.Removable -> {
				bindSubtitle(view, row.subtitle)
				content.background = null
				chevron.apply {
					val inset = resources.getDimensionPixelSize(R.dimen.content_padding_half) / 2
					setImageResource(R.drawable.ic_action_close)
					contentDescription = getString(R.string.shared_string_remove)
					background = colors.circle(colors.field)
					setPadding(inset, inset, inset, inset)
					setOnClickListener { mapActivity?.let(row.onRemove) }
				}
			}
			is Row.Toggle -> {
				chevron.visibility = View.GONE
				switch.visibility = View.VISIBLE
				MineNavStyle.styleSwitch(switch, colors.color(if (nightMode) R.color.mn_handle_dark else R.color.mn_handle_light))
				switch.isChecked = row.checked
				content.setOnClickListener {
					switch.isChecked = !switch.isChecked
					mapActivity?.let { row.onChange(it, switch.isChecked) }
				}
			}
			is Row.Choice -> {
				chevron.visibility = View.GONE
				check.imageTintList = ColorStateList.valueOf(colors.accent)
				AndroidUiHelper.updateVisibility(check, row.selected)
				content.setOnClickListener { mapActivity?.let(row.action) }
			}
		}
		return view
	}

	private fun bindSubtitle(row: View, subtitle: String?) {
		row.findViewById<TextView>(R.id.row_subtitle).apply {
			text = subtitle
			setTextColor(colors.secondaryText)
			AndroidUiHelper.updateVisibility(this, !subtitle.isNullOrEmpty())
		}
	}

	companion object {

		/**
		 * Slides the page in from the right. [add] keeps the current page alive underneath,
		 * for pages that report back to it.
		 */
		@JvmStatic
		@JvmOverloads
		fun show(manager: FragmentManager, fragment: Fragment, tag: String, add: Boolean = false) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, tag)) {
				val transaction = MineNavStyle.setForwardAnimations(manager.beginTransaction())
				if (add) {
					transaction.add(R.id.fragmentContainer, fragment, tag)
				} else {
					transaction.replace(R.id.fragmentContainer, fragment, tag)
				}
				transaction.addToBackStack(tag).commitAllowingStateLoss()
			}
		}
	}
}
