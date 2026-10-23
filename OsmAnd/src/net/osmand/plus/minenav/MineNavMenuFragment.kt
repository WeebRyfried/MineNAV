package net.osmand.plus.minenav

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.Version
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.BaseFullScreenFragment
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.AndroidUtils

/**
 * Full-screen main menu in the style of Waze: mascot header with the active profile,
 * then a short list of big items.
 */
class MineNavMenuFragment : BaseFullScreenFragment() {

	private lateinit var colors: MineNavColors

	override fun getContentStatusBarNightMode(): Boolean = nightMode

	override fun getStatusBarColorId(): Int {
		AndroidUiHelper.setStatusBarContentColor(view, nightMode)
		return if (nightMode) R.color.mn_row_bg_dark else R.color.mn_row_bg_light
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
		updateNightMode()
		colors = MineNavColors(requireContext(), nightMode)
		val view = themedInflater.inflate(R.layout.fragment_minenav_menu, container, false)
		view.setBackgroundColor(colors.row)

		view.findViewById<ImageView>(R.id.menu_close).apply {
			background = colors.circle(colors.field)
			imageTintList = ColorStateList.valueOf(colors.text)
			setOnClickListener { dismiss() }
		}
		view.findViewById<TextView>(R.id.menu_title).setTextColor(colors.text)
		setupProfilePill(view.findViewById(R.id.menu_profile))
		view.findViewById<View>(R.id.menu_divider).setBackgroundColor(colors.divider)
		view.findViewById<TextView>(R.id.menu_version).apply {
			text = Version.getFullVersion(app)
			setTextColor(colors.secondaryText)
		}

		val items = view.findViewById<LinearLayout>(R.id.menu_items)
		addItem(items, R.drawable.ic_action_plan_route, R.string.mn_menu_plan_drive) {
			dismiss()
			it.mapActions.doRoute()
		}
		addItem(items, R.drawable.ic_action_folder_favorites, R.string.mn_menu_saved_places) {
			MineNavSavedPlacesFragment.showInstance(it.supportFragmentManager)
		}
		addItem(items, R.drawable.ic_action_import, R.string.mn_menu_download_maps) {
			dismiss()
			it.startActivity(Intent(it, app.appCustomization.downloadActivity))
		}
		addItem(items, R.drawable.ic_action_layers, R.string.mn_menu_map_display) {
			MineNavMapDisplayFragment.showInstance(it.supportFragmentManager)
		}
		addItem(items, R.drawable.ic_action_settings, R.string.shared_string_settings) {
			MineNavSettingsFragment.showInstance(it.supportFragmentManager)
		}
		addItem(items, R.drawable.ic_action_help, R.string.mn_menu_help) {
			MineNavAboutFragment.showInstance(it.supportFragmentManager)
		}
		return view
	}

	private fun setupProfilePill(pill: TextView) {
		pill.background = colors.pill(colors.field)
		pill.setTextColor(colors.accent)
		pill.text = settings.applicationMode.toHumanString()
		val arrow = uiUtilities.getPaintedIcon(R.drawable.ic_action_arrow_drop_down, colors.accent)
		pill.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, arrow, null)
		pill.setOnClickListener { showProfiles(pill) }
	}

	private fun showProfiles(anchor: TextView) {
		val modes = ApplicationMode.values(app)
		val popup = PopupMenu(requireContext(), anchor)
		modes.forEachIndexed { index, mode -> popup.menu.add(0, index, index, mode.toHumanString()) }
		popup.setOnMenuItemClickListener { item ->
			val mode = modes[item.itemId]
			settings.setApplicationMode(mode)
			anchor.text = mode.toHumanString()
			true
		}
		popup.show()
	}

	private fun addItem(parent: LinearLayout, @DrawableRes iconId: Int, @StringRes titleId: Int,
	                    action: (MapActivity) -> Unit) {
		val item = themedInflater.inflate(R.layout.minenav_menu_item, parent, false)
		item.findViewById<ImageView>(R.id.item_icon).apply {
			setImageResource(iconId)
			imageTintList = ColorStateList.valueOf(colors.text)
		}
		item.findViewById<TextView>(R.id.item_title).apply {
			setText(titleId)
			setTextColor(colors.text)
		}
		item.setOnClickListener { mapActivity?.let(action) }
		parent.addView(item)
	}

	private fun dismiss() {
		val manager = activity?.supportFragmentManager ?: return
		if (!manager.isStateSaved) {
			manager.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
		}
	}

	companion object {

		val TAG: String = MineNavMenuFragment::class.java.simpleName

		@JvmStatic
		fun showInstance(manager: FragmentManager) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				manager.beginTransaction()
					.setCustomAnimations(R.anim.mn_enter_from_left, R.anim.mn_exit_to_left,
						R.anim.mn_enter_from_left, R.anim.mn_exit_to_left)
					.replace(R.id.fragmentContainer, MineNavMenuFragment(), TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss()
			}
		}
	}
}
