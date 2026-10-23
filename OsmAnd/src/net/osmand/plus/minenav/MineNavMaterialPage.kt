package net.osmand.plus.minenav

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.FragmentActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import net.osmand.plus.R
import net.osmand.plus.widgets.ui.GroupFooterView
import net.osmand.plus.widgets.ui.GroupHeaderView

/**
 * MineNAV look for OsmAnd's Material 3 screens (net.osmand.plus.widgets.ui): the MineNAV toolbar
 * with a close button, flat rows on the screen background and Waze-colored sliders and switches.
 */
object MineNavMaterialPage {

	private const val CLOSE_ITEM_ORDER = 1000
	private const val SLIDER_TRACK_DP = 6f
	private const val SLIDER_THUMB_DP = 24f

	@JvmStatic
	fun styleToolbar(root: View, activity: FragmentActivity, nightMode: Boolean) {
		val colors = MineNavColors(root.context, nightMode)
		root.setBackgroundColor(colors.screen)
		root.findViewById<View?>(R.id.appbar)?.setBackgroundColor(colors.row)
		val toolbar = root.findViewById<MaterialToolbar?>(R.id.toolbar) ?: return
		toolbar.setBackgroundColor(colors.row)
		toolbar.setTitleTextColor(colors.text)
		toolbar.setNavigationIconTint(colors.text)
		for (i in 0 until toolbar.menu.size()) {
			toolbar.menu.getItem(i).icon?.setTint(colors.text)
		}
		if (toolbar.menu.findItem(R.id.mn_settings_close) == null) {
			toolbar.menu.add(0, R.id.mn_settings_close, CLOSE_ITEM_ORDER, R.string.mn_close).apply {
				icon = ContextCompat.getDrawable(root.context, R.drawable.ic_action_close)?.mutate()?.apply { setTint(colors.text) }
				setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
				setOnMenuItemClickListener {
					MineNavStyle.closeAllSettings(activity)
					true
				}
			}
		}
		MineNavStyle.centerToolbarTitle(toolbar, nightMode)
	}

	/** Restyles the rows; call it again whenever the screen rebinds them. */
	@JvmStatic
	fun styleContent(content: View, nightMode: Boolean) {
		styleView(content, MineNavColors(content.context, nightMode))
	}

	private fun styleView(view: View, colors: MineNavColors) {
		when (view) {
			is GroupFooterView, is GroupHeaderView -> styleTexts(view, colors.secondaryText)
			is RangeSlider -> styleSlider(view, colors)
			is Slider -> styleSlider(view, colors)
			is SwitchCompat -> MineNavStyle.styleSwitch(view, colors.field)
			is TextView -> view.setTextColor(if (view.id == R.id.title) colors.text else colors.secondaryText)
			is ViewGroup -> {
				// The rounded segments of a group become flat MineNAV rows
				if (view.background != null && view.id != R.id.root && view.id != R.id.appbar) {
					view.setBackgroundColor(colors.row)
				}
				for (i in 0 until view.childCount) {
					styleView(view.getChildAt(i), colors)
				}
			}
		}
	}

	private fun styleTexts(view: View, color: Int) {
		if (view is TextView) {
			view.setTextColor(color)
		} else if (view is ViewGroup) {
			for (i in 0 until view.childCount) {
				styleTexts(view.getChildAt(i), color)
			}
		}
	}

	private fun styleSlider(slider: View, colors: MineNavColors) {
		val accent = ColorStateList.valueOf(colors.color(R.color.mn_primary))
		val inactive = ColorStateList.valueOf(colors.field)
		val halo = ColorStateList.valueOf(ColorUtils.setAlphaComponent(colors.color(R.color.mn_primary), 0x33))
		val noTicks = ColorStateList.valueOf(Color.TRANSPARENT)
		val density = slider.resources.displayMetrics.density
		val track = (SLIDER_TRACK_DP * density).toInt()
		val thumb = (SLIDER_THUMB_DP * density).toInt()
		// Waze-like: a thin track and a round white-edged knob instead of the Material 3 bar
		when (slider) {
			is RangeSlider -> slider.apply {
				trackHeight = track
				thumbWidth = thumb
				thumbHeight = thumb
				thumbTrackGapSize = 0
				trackStopIndicatorSize = 0
				thumbStrokeColor = ColorStateList.valueOf(Color.WHITE)
				thumbStrokeWidth = 2 * density
				trackActiveTintList = accent
				trackInactiveTintList = inactive
				thumbTintList = accent
				haloTintList = halo
				tickActiveTintList = noTicks
				tickInactiveTintList = noTicks
			}
			is Slider -> slider.apply {
				trackHeight = track
				thumbWidth = thumb
				thumbHeight = thumb
				thumbTrackGapSize = 0
				trackStopIndicatorSize = 0
				thumbStrokeColor = ColorStateList.valueOf(Color.WHITE)
				thumbStrokeWidth = 2 * density
				trackActiveTintList = accent
				trackInactiveTintList = inactive
				thumbTintList = accent
				haloTintList = halo
				tickActiveTintList = noTicks
				tickInactiveTintList = noTicks
			}
		}
	}
}
