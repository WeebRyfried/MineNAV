package net.osmand.plus.views.controls

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.SpecialPointType
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.minenav.MineNavActions
import net.osmand.plus.minenav.MineNavStyle
import net.osmand.plus.search.ShowQuickSearchMode
import net.osmand.plus.search.history.HistoryEntry
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.widgets.FrameLayoutEx
import kotlin.math.abs

/**
 * Waze-style bottom sheet shown on the map while browsing: the search pill (with voice input)
 * opens search, the tiles plan a route to Home or Work or save a new place, and the last
 * searched places can be driven to with one tap. Pulling the sheet down folds it to the search
 * pill alone; pulling it up opens it again.
 */
class WhereToCard @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayoutEx(context, attrs, defStyleAttr) {

	companion object {
		private const val MAX_RECENT_ITEMS = 2
		private const val FOLD_DURATION_MS = 220L
	}

	private val app = context.applicationContext as OsmandApplication
	private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
	private var mapActivity: MapActivity? = null
	private var nightMode: Boolean? = null
	private var recentKey: List<String> = emptyList()

	private var expanded = true
	private var foldAnimator: ValueAnimator? = null
	private var downX = 0f
	private var downY = 0f
	private var dragging = false

	fun setMapActivity(activity: MapActivity?) {
		mapActivity = activity
		if (activity == null) {
			return
		}
		findViewById<View>(R.id.where_to_search).setOnClickListener { openSearch() }
		findViewById<View>(R.id.where_to_voice).setOnClickListener { MineNavActions.startVoiceSearch(activity) }
		findViewById<View>(R.id.where_to_handle).setOnClickListener { setExpanded(!expanded, true) }
		setupChip(R.id.where_to_home, R.drawable.ic_mn_home_color, R.string.home_button) {
			navigateToSpecialPoint(SpecialPointType.HOME, R.string.home_button)
		}
		setupChip(R.id.where_to_work, R.drawable.ic_mn_work_color, R.string.work_button) {
			navigateToSpecialPoint(SpecialPointType.WORK, R.string.work_button)
		}
		setupChip(R.id.where_to_new, R.drawable.ic_action_plus, R.string.mn_new_place) {
			MineNavActions.saveNewPlace(activity)
		}
	}

	fun update(show: Boolean, nightMode: Boolean) {
		if (this.nightMode != nightMode) {
			this.nightMode = nightMode
			applyColors(nightMode)
			recentKey = emptyList()
		}
		if (show) {
			updateRecent(nightMode)
		}
		MineNavStyle.animateSheet(this, show)
	}

	override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				downX = event.rawX
				downY = event.rawY
				dragging = false
			}
			MotionEvent.ACTION_MOVE -> {
				val dx = event.rawX - downX
				val dy = event.rawY - downY
				// A vertical pull folds or opens the sheet; sideways moves stay with the tiles
				if (abs(dy) > touchSlop && abs(dy) > abs(dx)) {
					dragging = true
					return true
				}
			}
		}
		return super.onInterceptTouchEvent(event)
	}

	override fun onTouchEvent(event: MotionEvent): Boolean {
		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				downX = event.rawX
				downY = event.rawY
				return true
			}
			MotionEvent.ACTION_MOVE -> {
				if (abs(event.rawY - downY) > touchSlop) {
					dragging = true
				}
				return true
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				if (dragging) {
					val dy = event.rawY - downY
					if (abs(dy) > touchSlop) {
						setExpanded(dy < 0, true)
					}
				}
				dragging = false
				return true
			}
		}
		return super.onTouchEvent(event)
	}

	private fun setExpanded(expand: Boolean, animate: Boolean) {
		if (expanded == expand && foldAnimator == null) {
			return
		}
		expanded = expand
		val content = findViewById<View>(R.id.where_to_expandable)
		foldAnimator?.cancel()
		foldAnimator = null
		if (!animate) {
			content.visibility = if (expand) View.VISIBLE else View.GONE
			return
		}
		val start = if (content.visibility == View.VISIBLE) content.height else 0
		val end = if (expand) {
			content.measure(MeasureSpec.makeMeasureSpec(content.width.takeIf { it > 0 } ?: width, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
			content.measuredHeight
		} else {
			0
		}
		content.visibility = View.VISIBLE
		foldAnimator = ValueAnimator.ofInt(start, end).apply {
			duration = FOLD_DURATION_MS
			addUpdateListener {
				val params = content.layoutParams
				params.height = it.animatedValue as Int
				content.layoutParams = params
				content.alpha = if (end > 0) (it.animatedValue as Int).toFloat() / end else
					if (start > 0) (it.animatedValue as Int).toFloat() / start else 0f
			}
			addListener(object : android.animation.AnimatorListenerAdapter() {
				override fun onAnimationEnd(animation: android.animation.Animator) {
					val params = content.layoutParams
					params.height = ViewGroup.LayoutParams.WRAP_CONTENT
					content.layoutParams = params
					content.alpha = 1f
					content.visibility = if (expanded) View.VISIBLE else View.GONE
					foldAnimator = null
				}
			})
			start()
		}
	}

	private fun setupChip(id: Int, @DrawableRes iconId: Int, @StringRes textId: Int, action: () -> Unit) {
		val chip = findViewById<View>(id)
		chip.findViewById<ImageView>(R.id.chip_icon).setImageResource(iconId)
		chip.findViewById<TextView>(R.id.chip_text).setText(textId)
		chip.setOnClickListener { action() }
	}

	private fun applyColors(nightMode: Boolean) {
		val surface = color(if (nightMode) R.color.mn_card_bg_dark else R.color.mn_card_bg_light)
		val field = color(if (nightMode) R.color.mn_field_bg_dark else R.color.mn_field_bg_light)
		val text = textColor(nightMode)
		val secondary = secondaryColor(nightMode)
		val accent = color(if (nightMode) R.color.mn_lilac else R.color.mn_primary)
		val stroke = color(if (nightMode) R.color.mn_chip_stroke_dark else R.color.mn_chip_stroke_light)
		val radius = resources.getDimension(R.dimen.where_to_card_radius)
		val chipRadius = resources.getDimension(R.dimen.where_to_chip_radius)
		val sheetRadius = resources.getDimension(R.dimen.where_to_sheet_radius)

		MineNavStyle.setSheetBackground(this, findViewById(R.id.where_to_surface), GradientDrawable().apply {
			setColor(surface)
			cornerRadii = floatArrayOf(sheetRadius, sheetRadius, sheetRadius, sheetRadius, 0f, 0f, 0f, 0f)
		})
		findViewById<View>(R.id.where_to_handle).background =
			roundRect(color(if (nightMode) R.color.mn_handle_dark else R.color.mn_handle_light), 0, radius)
		findViewById<View>(R.id.where_to_search).background = roundRect(field, 0, radius)
		findViewById<ImageView>(R.id.where_to_search_icon).imageTintList = ColorStateList.valueOf(secondary)
		findViewById<ImageView>(R.id.where_to_voice).imageTintList = ColorStateList.valueOf(text)
		findViewById<TextView>(R.id.where_to_search_text).setTextColor(secondary)
		findViewById<TextView>(R.id.where_to_recent_title).setTextColor(secondary)

		for (id in intArrayOf(R.id.where_to_home, R.id.where_to_work, R.id.where_to_new)) {
			val chip = findViewById<View>(id)
			val isNew = id == R.id.where_to_new
			chip.background = roundRect(surface, stroke, chipRadius)
			// Home and Work keep the colors of their icons, "New" is drawn in the accent color
			chip.findViewById<ImageView>(R.id.chip_icon).imageTintList = if (isNew) ColorStateList.valueOf(accent) else null
			chip.findViewById<TextView>(R.id.chip_text).setTextColor(if (isNew) accent else text)
		}
	}

	private fun updateRecent(nightMode: Boolean) {
		// In landscape the sheet is a side panel with no room for the recent list
		val maxItems = if (AndroidUiHelper.isOrientationPortrait(context)) MAX_RECENT_ITEMS else 0
		val entries = app.searchHistoryHelper.getHistoryEntries(true)
			.sortedByDescending { it.lastAccessTime }
			.distinctBy { it.name.getSimpleName(context, false) }
			.take(maxItems)
		val key = entries.map { it.serializedName + it.lat + it.lon }
		if (key == recentKey) {
			return
		}
		recentKey = key
		val list = findViewById<LinearLayout>(R.id.where_to_recent_list)
		list.removeAllViews()
		AndroidUiHelper.updateVisibility(findViewById(R.id.where_to_recent_title), entries.isNotEmpty())
		val inflater = LayoutInflater.from(context)
		for (entry in entries) {
			val row = inflater.inflate(R.layout.map_where_to_recent_item, list, false)
			bindRecent(row, entry, nightMode)
			list.addView(row)
		}
	}

	private fun bindRecent(row: View, entry: HistoryEntry, nightMode: Boolean) {
		val name = entry.name
		val title = name.getSimpleName(context, false)
		val subtitle = name.typeName.orEmpty()
		row.findViewById<ImageView>(R.id.recent_icon).imageTintList = ColorStateList.valueOf(textColor(nightMode))
		row.findViewById<TextView>(R.id.recent_title).apply {
			text = title
			setTextColor(textColor(nightMode))
		}
		row.findViewById<TextView>(R.id.recent_subtitle).apply {
			text = subtitle
			setTextColor(secondaryColor(nightMode))
			AndroidUiHelper.updateVisibility(this, subtitle.isNotEmpty())
		}
		row.setOnClickListener { navigateTo(LatLon(entry.lat, entry.lon), name) }
	}

	private fun roundRect(fill: Int, stroke: Int, radius: Float): GradientDrawable {
		return GradientDrawable().apply {
			cornerRadius = radius
			setColor(fill)
			if (stroke != 0) {
				setStroke(AndroidUtils.dpToPx(context, 1.5f), stroke)
			}
		}
	}

	private fun textColor(nightMode: Boolean): Int =
		color(if (nightMode) R.color.mn_text_dark else R.color.mn_text_light)

	private fun secondaryColor(nightMode: Boolean): Int =
		color(if (nightMode) R.color.mn_text_secondary_dark else R.color.mn_text_secondary_light)

	private fun color(@ColorRes id: Int): Int = ContextCompat.getColor(context, id)

	private fun openSearch() {
		val activity = mapActivity ?: return
		val fragmentsHelper = activity.fragmentsHelper
		fragmentsHelper.dismissCardDialog()
		fragmentsHelper.showQuickSearch(ShowQuickSearchMode.NEW_IF_EXPIRED, false)
	}

	private fun navigateToSpecialPoint(type: SpecialPointType, @StringRes nameId: Int) {
		mapActivity?.let { MineNavActions.navigateToSpecialPoint(it, type, nameId) }
	}

	private fun navigateTo(latLon: LatLon, description: PointDescription) {
		mapActivity?.let { MineNavActions.navigateTo(it, latLon, description) }
	}
}
