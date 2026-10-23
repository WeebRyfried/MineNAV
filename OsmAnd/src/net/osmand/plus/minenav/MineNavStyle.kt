package net.osmand.plus.minenav

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.MetricAffectingSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.widget.CompoundButtonCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID
import net.osmand.plus.R
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.FontCache
import net.osmand.plus.utils.UiUtilities

/**
 * Shared MineNAV (Waze-like) styling applied on top of the original OsmAnd screens, so that
 * every settings page, switch and sheet follows the same look.
 */
object MineNavStyle {

	private val CIRCLE_COLORS = intArrayOf(
		R.color.mn_circle_blue, R.color.mn_circle_cyan, R.color.mn_circle_green, R.color.mn_circle_orange,
		R.color.mn_circle_purple, R.color.mn_circle_teal, R.color.mn_circle_red, R.color.mn_circle_pink
	)

	// Spacers between groups of rows: drawn as the dark gap between Waze cards
	private val GAP_LAYOUTS = setOf(R.layout.simple_divider_item, R.layout.list_item_divider, R.layout.card_bottom_divider)

	// Thin lines between two rows: replaced by the dividers that RowDividers draws on every page
	private val SEPARATOR_LAYOUTS = setOf(R.layout.divider_half_item, R.layout.divider_half_item_with_background)

	private val HEADER_LAYOUTS = setOf(
		R.layout.preference_category_with_descr, R.layout.preference_category_title,
		R.layout.preference_category_with_right_text
	)

	// Device illustrations and long explanations that Waze-style settings do without
	private val HIDDEN_LAYOUTS = setOf(R.layout.settings_device_image_screen, R.layout.preference_info)

	private const val ICON_PADDING_DP = 9f
	private const val COLORED_ICON_PADDING_DP = 6f
	private const val KEEP_ICON_COLORS = "mn_keep_icon_colors"

	// Back stack names of the MineNAV pages are their class names
	private const val MINENAV_PAGE_PREFIX = "MineNav"
	private const val TOOLBAR_TITLE_SP = 20f
	private const val SHEET_TITLE_SP = 20f
	private const val SWITCH_WIDTH_DP = 56f
	private const val SWITCH_HEIGHT_DP = 32f
	private const val SWITCH_KNOB_DP = 24f
	private const val MODE_TOGGLE_INSET_DP = 4f
	private const val FIELD_RADIUS_DP = 16f

	@JvmStatic
	fun hideSettingsDecorations(group: PreferenceGroup) {
		for (i in 0 until group.preferenceCount) {
			val preference = group.getPreference(i)
			val layout = preference.layoutResource
			if (layout in HIDDEN_LAYOUTS || layout in SEPARATOR_LAYOUTS) {
				preference.isVisible = false
			}
			if (preference is PreferenceGroup) {
				hideSettingsDecorations(preference)
			}
		}
	}

	/**
	 * Gives an OsmAnd preference row the exact metrics of a MineNAV row: 64dp height, a 40dp
	 * colored circle icon, text starting at 72dp, a chevron when it opens something.
	 */
	@SuppressLint("RestrictedApi")
	@JvmStatic
	fun styleSettingsRow(adapter: PreferenceGroupAdapter, position: Int, preference: Preference,
	                     holder: PreferenceViewHolder, nightMode: Boolean) {
		val itemView = holder.itemView
		val colors = MineNavColors(itemView.context, nightMode)
		val layout = preference.layoutResource
		when {
			layout in SEPARATOR_LAYOUTS -> setItemHeight(itemView, 0)
			layout in GAP_LAYOUTS -> styleGap(adapter, position, itemView, colors)
			isHeader(preference) -> styleHeader(holder, colors, position == 0)
			else -> styleRow(preference, holder, colors)
		}
	}

	private fun isHeader(preference: Preference): Boolean {
		return preference is PreferenceCategory || preference.layoutResource in HEADER_LAYOUTS
	}

	private fun isRow(preference: Preference): Boolean {
		val layout = preference.layoutResource
		return preference.isVisible && !isHeader(preference) && layout !in GAP_LAYOUTS
				&& layout !in SEPARATOR_LAYOUTS && layout !in HIDDEN_LAYOUTS
	}

	@SuppressLint("RestrictedApi")
	private fun styleGap(adapter: PreferenceGroupAdapter, position: Int, itemView: View, colors: MineNavColors) {
		itemView.setBackgroundColor(colors.screen)
		if (itemView is ViewGroup) {
			for (i in 0 until itemView.childCount) {
				itemView.getChildAt(i).visibility = View.INVISIBLE
			}
		}
		// A header already brings its own space, so only a gap between two rows stays visible
		val previous = adapter.getItem(position - 1)
		val next = adapter.getItem(position + 1)
		val betweenRows = previous != null && next != null && isRow(previous) && isRow(next)
		val gap = itemView.resources.getDimensionPixelSize(R.dimen.mn_section_gap)
		setItemHeight(itemView, if (betweenRows) gap else 0)
	}

	private fun setItemHeight(itemView: View, height: Int) {
		val params = itemView.layoutParams ?: return
		if (params.height != height) {
			params.height = height
			itemView.layoutParams = params
		}
	}

	private fun styleHeader(holder: PreferenceViewHolder, colors: MineNavColors, first: Boolean) {
		val itemView = holder.itemView
		val resources = itemView.resources
		itemView.setBackgroundColor(colors.screen)
		itemView.minimumHeight = 0
		setItemHeight(itemView, ViewGroup.LayoutParams.WRAP_CONTENT)
		val top = resources.getDimensionPixelSize(if (first) R.dimen.mn_header_first_top else R.dimen.mn_header_top)
		itemView.setPaddingRelative(itemView.paddingStart, top, itemView.paddingEnd,
			resources.getDimensionPixelSize(R.dimen.mn_header_bottom))
		(holder.findViewById(android.R.id.icon) as? ImageView)?.visibility = View.GONE

		val title = holder.findViewById(android.R.id.title) as? TextView
		title?.apply {
			setTextColor(colors.secondaryText)
			setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.mn_header_text_size))
			isAllCaps = false
			setPaddingRelative(paddingStart, 0, paddingEnd, 0)
		}
		val summary = (holder.findViewById(android.R.id.summary) ?: itemView.findViewById<View?>(R.id.summary)) as? TextView
		summary?.apply {
			setTextColor(colors.secondaryText)
			val belowTitle = (parent as? LinearLayout)?.orientation == LinearLayout.VERTICAL
			setPaddingRelative(paddingStart, if (belowTitle) AndroidUtils.dpToPx(context, 2f) else 0, paddingEnd, 0)
		}
	}

	private fun styleRow(preference: Preference, holder: PreferenceViewHolder, colors: MineNavColors) {
		val itemView = holder.itemView
		val context = itemView.context
		val resources = context.resources
		val rowHeight = resources.getDimensionPixelSize(R.dimen.mn_settings_row_height)
		itemView.setBackgroundColor(colors.row)
		// Some OsmAnd rows have a fixed 48dp height that clips the larger MineNAV row
		if ((itemView.layoutParams?.height ?: 0) > 0) {
			setItemHeight(itemView, ViewGroup.LayoutParams.WRAP_CONTENT)
		}
		itemView.minimumHeight = 0
		val selectable = itemView.findViewById<View>(R.id.selectable_list_item)
		selectable?.minimumHeight = rowHeight
		if (preference.isSelectable) {
			selectable?.background = UiUtilities.getColoredSelectableDrawable(context, colors.secondaryText, 0.3f)
		}
		val enabled = preference.isEnabled
		(holder.findViewById(android.R.id.title) as? TextView)?.setTextColor(if (enabled) colors.text else colors.secondaryText)
		(holder.findViewById(android.R.id.summary) as? TextView)?.setTextColor(colors.secondaryText)
		itemView.findViewById<TextView>(R.id.summary)?.setTextColor(colors.secondaryText)

		val icon = findRowIcon(itemView)
		if (icon != null && icon.visibility != View.GONE) {
			placeRowIcon(icon, icon.parent === selectable)
			if (icon.drawable == null || icon.visibility != View.VISIBLE) {
				icon.background = null
			} else if (preference.peekExtras()?.getBoolean(KEEP_ICON_COLORS) == true) {
				styleColoredIcon(icon, colors, enabled)
			} else {
				styleCircleIcon(icon, colors.color(circleColorFor(preference)), enabled)
			}
		}

		val switchView = itemView.findViewById<View>(R.id.switchWidget)
		if (switchView != null) {
			hideSwitchSeparator(switchView)
		}
		val checkBox = itemView.findViewById<View>(android.R.id.checkbox)
		if (checkBox is CompoundButton) {
			styleCheckMark(checkBox, colors)
		}
		val hasWidget = switchView != null || checkBox != null
		val showChevron = preference.isSelectable && !hasWidget
		val chevron = if (showChevron) findOrAddChevron(itemView) else itemView.findViewById<ImageView?>(R.id.mn_chevron)
		chevron?.apply {
			setImageResource(R.drawable.ic_mn_chevron)
			imageTintList = ColorStateList.valueOf(colors.secondaryText)
			visibility = if (showChevron) View.VISIBLE else View.GONE
		}
	}

	private fun findRowIcon(itemView: View): ImageView? {
		return (itemView.findViewById<View?>(android.R.id.icon) ?: itemView.findViewById<View?>(R.id.icon)) as? ImageView
	}

	// 16dp + 40dp circle + the text's own 16dp start padding = text at 72dp, as on MineNAV pages
	private fun placeRowIcon(icon: ImageView, inRow: Boolean) {
		val resources = icon.resources
		val size = resources.getDimensionPixelSize(R.dimen.mn_settings_icon_circle)
		val start = resources.getDimensionPixelSize(R.dimen.content_padding)
		val vertical = if (inRow) resources.getDimensionPixelSize(R.dimen.mn_settings_icon_margin) else 0
		val params = icon.layoutParams as? ViewGroup.MarginLayoutParams ?: return
		if (params.width != size || params.height != size || params.marginEnd != 0 || params.topMargin != vertical) {
			params.width = size
			params.height = size
			params.leftMargin = start
			params.rightMargin = 0
			params.marginStart = start
			params.marginEnd = 0
			params.topMargin = vertical
			params.bottomMargin = vertical
			icon.layoutParams = params
		}
	}

	private fun findOrAddChevron(itemView: View): ImageView? {
		itemView.findViewById<ImageView>(R.id.mn_chevron)?.let { return it }
		val row = itemView.findViewById<View>(R.id.selectable_list_item) as? LinearLayout ?: return null
		if (row.orientation != LinearLayout.HORIZONTAL) {
			return null
		}
		val context = itemView.context
		val size = context.resources.getDimensionPixelSize(R.dimen.standard_icon_size)
		val chevron = ImageView(context).apply {
			id = R.id.mn_chevron
			importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
		}
		row.addView(chevron, LinearLayout.LayoutParams(size, size).apply {
			gravity = Gravity.CENTER_VERTICAL
			marginEnd = context.resources.getDimensionPixelSize(R.dimen.content_padding)
		})
		return chevron
	}

	// Waze marks the chosen option with a check mark instead of a radio circle
	private fun styleCheckMark(button: CompoundButton, colors: MineNavColors) {
		val context = button.context
		val check = ContextCompat.getDrawable(context, R.drawable.ic_mn_check)?.mutate() ?: return
		check.setTint(colors.accent)
		// An invisible copy keeps the same size, so the text does not move when the check moves
		val empty = ContextCompat.getDrawable(context, R.drawable.ic_mn_check)?.mutate()?.apply { alpha = 0 } ?: return
		CompoundButtonCompat.setButtonTintList(button, null)
		button.buttonDrawable = StateListDrawable().apply {
			addState(intArrayOf(android.R.attr.state_checked), check)
			addState(intArrayOf(), empty)
		}
	}

	private fun circleColorFor(preference: Preference): Int {
		return circleColorFor(preference.key ?: preference.title?.toString() ?: "")
	}

	private fun circleColorFor(key: String): Int = CIRCLE_COLORS[Math.floorMod(key.hashCode(), CIRCLE_COLORS.size)]

	/**
	 * The dashboard screens shown over the map (Configure map and the screens it opens) as a
	 * MineNAV page: a solid toolbar with a centered title and an X, and no map above the list.
	 */
	@JvmStatic
	fun styleDashboardPage(dashboard: View, nightMode: Boolean, onClose: Runnable) {
		val colors = MineNavColors(dashboard.context, nightMode)
		val resources = dashboard.resources
		val toolbar = dashboard.findViewById<View?>(R.id.dashboard_toolbar_container) ?: return
		dashboard.setBackgroundColor(colors.screen)
		dashboard.findViewById<View?>(R.id.animateContent)?.setBackgroundColor(colors.screen)
		toolbar.translationY = 0f
		toolbar.setBackgroundColor(colors.row)
		dashboard.findViewById<View?>(R.id.map_part_dashboard)?.let { part ->
			val height = resources.getDimensionPixelSize(R.dimen.dashboard_map_toolbar)
			if (part.layoutParams.height != height) {
				part.layoutParams.height = height
				part.requestLayout()
			}
			part.setBackgroundColor(colors.row)
			// The map download button and the shadow drawn over the map
			if (part is ViewGroup) {
				for (i in 0 until part.childCount) {
					part.getChildAt(i).visibility = View.GONE
				}
			}
		}
		dashboard.findViewById<View?>(R.id.shadow_on_map)?.visibility = View.GONE
		dashboard.findViewById<ImageView?>(R.id.toolbar_back)?.imageTintList = ColorStateList.valueOf(colors.text)
		val title = dashboard.findViewById<TextView?>(R.id.toolbar_text)
		title?.apply {
			setTextColor(colors.text)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, TOOLBAR_TITLE_SP)
		}
		val actions = dashboard.findViewById<View?>(R.id.toolbar_settings)?.parent as? LinearLayout
		if (actions != null && actions.findViewById<View?>(R.id.mn_settings_close) == null) {
			val size = resources.getDimensionPixelSize(R.dimen.toolbar_height)
			val padding = resources.getDimensionPixelSize(R.dimen.content_padding)
			actions.addView(ImageView(dashboard.context).apply {
				id = R.id.mn_settings_close
				setImageResource(R.drawable.ic_action_close)
				imageTintList = ColorStateList.valueOf(colors.text)
				setPadding(padding, padding, padding, padding)
				contentDescription = dashboard.context.getString(R.string.mn_close)
				background = AppCompatSelectable.borderless(dashboard.context)
				setOnClickListener { onClose.run() }
			}, LinearLayout.LayoutParams(size, size))
		}
		if (title != null && title.getTag(R.id.mn_title_centered) == null) {
			title.setTag(R.id.mn_title_centered, true)
			toolbar.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> centerInParentWidth(title, toolbar) }
		}
	}

	// Shifts a title so it is centered on [frame], whatever sits before or after it
	private fun centerInParentWidth(title: View, frame: View) {
		val titleLocation = IntArray(2)
		val frameLocation = IntArray(2)
		title.getLocationInWindow(titleLocation)
		frame.getLocationInWindow(frameLocation)
		val left = titleLocation[0] - title.translationX - frameLocation[0]
		title.translationX = (frame.width - title.width) / 2f - left
	}

	/** A Configure map row as a MineNAV row: round colored icon, inset divider. */
	@JvmStatic
	fun styleMenuRow(view: View, nightMode: Boolean) {
		val colors = MineNavColors(view.context, nightMode)
		view.setBackgroundColor(colors.row)
		val title = view.findViewById<TextView?>(R.id.title)
		title?.setTextColor(colors.text)
		view.findViewById<TextView?>(R.id.description)?.setTextColor(colors.secondaryText)
		view.findViewById<ImageView?>(R.id.icon)?.let { icon ->
			if (icon.visibility == View.VISIBLE && icon.drawable != null) {
				placeRowIcon(icon, false)
				styleCircleIcon(icon, colors.color(circleColorFor(title?.text?.toString().orEmpty())), view.isEnabled)
			}
		}
		view.findViewById<ImageView?>(R.id.secondary_icon)?.let { tintIcon(it, colors.secondaryText) }
		view.findViewById<View?>(R.id.divider)?.let { divider ->
			divider.setBackgroundColor(colors.divider)
			(divider.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
				it.marginStart = view.resources.getDimensionPixelSize(R.dimen.mn_settings_divider_start)
				it.leftMargin = it.marginStart
				divider.layoutParams = it
			}
		}
	}

	/** OsmAnd's dotted split mark before a toggle becomes a chevron: the row opens its options. */
	@JvmStatic
	fun styleOptionsMark(icon: ImageView, nightMode: Boolean) {
		val colors = MineNavColors(icon.context, nightMode)
		icon.setImageResource(R.drawable.ic_mn_chevron)
		icon.imageTintList = ColorStateList.valueOf(colors.secondaryText)
		(icon.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
			val gap = icon.resources.getDimensionPixelSize(R.dimen.content_padding_small)
			if (params.marginEnd != gap) {
				params.marginEnd = gap
				params.rightMargin = gap
				icon.layoutParams = params
			}
		}
	}

	/** A profile in OsmAnd's profile strip as a round button; the chosen one is filled. */
	@JvmStatic
	fun styleModeToggle(toggle: View, icon: ImageView, selection: View?, checked: Boolean, nightMode: Boolean) {
		val colors = MineNavColors(toggle.context, nightMode)
		val inset = AndroidUtils.dpToPx(toggle.context, MODE_TOGGLE_INSET_DP)
		val fill = if (checked) colors.color(R.color.mn_primary) else colors.field
		toggle.background = RippleDrawable(ColorStateList.valueOf(colors.divider), InsetDrawable(colors.circle(fill), inset), null)
		icon.setImageDrawable(icon.drawable?.constantState?.newDrawable()?.mutate())
		icon.imageTintList = ColorStateList.valueOf(if (checked) colors.color(R.color.mn_text_dark) else colors.text)
		selection?.visibility = View.INVISIBLE
		val strip = toggle.parent as? ViewGroup ?: return
		val padding = toggle.resources.getDimensionPixelSize(R.dimen.content_padding) - inset
		if (strip.paddingStart != padding) {
			strip.setPaddingRelative(padding, inset, padding, inset)
		}
	}

	/** A Configure map category as a MineNAV section header over its rows. */
	@JvmStatic
	fun styleMenuCategory(view: View, nightMode: Boolean) {
		val colors = MineNavColors(view.context, nightMode)
		val resources = view.resources
		view.setBackgroundColor(colors.screen)
		// The first child is the shadowed gap between categories
		((view as? ViewGroup)?.getChildAt(0) as? ViewGroup)?.let { gap ->
			gap.setBackgroundColor(colors.screen)
			for (i in 0 until gap.childCount) {
				gap.getChildAt(i).visibility = View.INVISIBLE
			}
		}
		view.findViewById<View?>(R.id.button_container)?.let { (it.parent as? View)?.setBackgroundColor(colors.screen) }
		view.findViewById<TextView?>(R.id.title)?.apply {
			setTextColor(colors.secondaryText)
			setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.mn_header_text_size))
		}
		view.findViewById<TextView?>(R.id.description)?.setTextColor(colors.secondaryText)
		view.findViewById<ImageView?>(R.id.explicit_indicator)?.imageTintList = ColorStateList.valueOf(colors.secondaryText)
		view.findViewById<View?>(R.id.divider)?.setBackgroundColor(colors.screen)
		view.findViewById<View?>(R.id.items_container)?.let { (it.parent as? View)?.setBackgroundColor(colors.row) }
	}

	private fun styleCircleIcon(icon: ImageView, @ColorInt circleColor: Int, enabled: Boolean) {
		val padding = AndroidUtils.dpToPx(icon.context, ICON_PADDING_DP)
		icon.setPadding(padding, padding, padding, padding)
		icon.background = GradientDrawable().apply {
			shape = GradientDrawable.OVAL
			setColor(circleColor)
		}
		tintIcon(icon, Color.WHITE)
		icon.alpha = if (enabled) 1f else 0.45f
	}

	/** Marks an icon that has its own colors, like a road sign, so rows keep them. */
	@JvmStatic
	fun keepIconColors(preference: Preference) {
		preference.extras.putBoolean(KEEP_ICON_COLORS, true)
	}

	// Multi-colored icons go untinted on a neutral circle
	private fun styleColoredIcon(icon: ImageView, colors: MineNavColors, enabled: Boolean) {
		val padding = AndroidUtils.dpToPx(icon.context, COLORED_ICON_PADDING_DP)
		icon.setPadding(padding, padding, padding, padding)
		icon.background = colors.circle(colors.field)
		icon.imageTintList = null
		icon.alpha = if (enabled) 1f else 0.45f
	}

	/**
	 * Tints a copy of the icon: OsmAnd caches its colored icons and shares one instance between
	 * screens, so tinting the shared drawable would recolor the icon everywhere.
	 */
	@JvmStatic
	fun tintIcon(icon: ImageView, @ColorInt color: Int) {
		val copy = icon.drawable?.constantState?.newDrawable(icon.resources)?.mutate()
		if (copy != null) {
			icon.setImageDrawable(copy)
		}
		icon.imageTintList = ColorStateList.valueOf(color)
	}

	/**
	 * Draws the thin line between two rows of the same group, inset like the MineNAV rows.
	 */
	@JvmStatic
	fun createRowDividers(context: Context, nightMode: Boolean): RecyclerView.ItemDecoration =
		RowDividers(context, MineNavColors(context, nightMode).divider)

	private class RowDividers(context: Context, @ColorInt color: Int) : RecyclerView.ItemDecoration() {

		private val paint = Paint().apply { this.color = color }
		private val height = AndroidUtils.dpToPx(context, 1f)
		private val iconInset = context.resources.getDimensionPixelSize(R.dimen.mn_settings_divider_start)
		private val textInset = context.resources.getDimensionPixelSize(R.dimen.content_padding)

		@SuppressLint("RestrictedApi")
		override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
			val adapter = parent.adapter as? PreferenceGroupAdapter ?: return
			val rtl = parent.layoutDirection == View.LAYOUT_DIRECTION_RTL
			for (i in 0 until parent.childCount) {
				val child = parent.getChildAt(i)
				val position = parent.getChildAdapterPosition(child)
				if (position == RecyclerView.NO_POSITION) {
					continue
				}
				val current = adapter.getItem(position)
				val next = adapter.getItem(position + 1)
				if (current == null || next == null || !isRow(current) || !isRow(next)) {
					continue
				}
				val icon = findRowIcon(child)
				val inset = if (icon != null && icon.visibility != View.GONE) iconInset else textInset
				val bottom = child.bottom + child.translationY
				val left = if (rtl) 0f else inset.toFloat()
				val right = if (rtl) (parent.width - inset).toFloat() else parent.width.toFloat()
				canvas.drawRect(left, bottom - height, right, bottom, paint)
			}
		}
	}

	// The thin vertical line OsmAnd draws before a switch; Waze rows have none
	private fun hideSwitchSeparator(switchView: View) {
		val parent = switchView.parent as? ViewGroup ?: return
		val index = parent.indexOfChild(switchView)
		if (index > 0) {
			val previous = parent.getChildAt(index - 1)
			if (previous.javaClass == View::class.java) {
				previous.visibility = View.GONE
			}
		}
	}

	/** Search results as plain Waze rows instead of OsmAnd cards. */
	@JvmStatic
	fun styleSearchRow(view: View, nightMode: Boolean) {
		val colors = MineNavColors(view.context, nightMode)
		view.setBackgroundColor(colors.row)
		val title = view.findViewById<TextView?>(R.id.title) ?: view.findViewById<TextView?>(R.id.item_title)?.apply {
			// Cities and addresses come with OsmAnd's smaller title
			setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.mn_place_title_size))
		}
		title?.apply {
			setTextColor(colors.text)
			typeface = FontCache.getFont(FontCache.getAppTypeface(), 800)
			isAllCaps = false
			// Font spans (OsmAnd's match highlight) would make some names thinner than others
			val name = text
			if (name is Spanned && name.getSpans(0, name.length, MetricAffectingSpan::class.java).isNotEmpty()) {
				val plain = SpannableString(name)
				plain.getSpans(0, plain.length, MetricAffectingSpan::class.java).forEach { plain.removeSpan(it) }
				text = plain
			}
		}
		view.findViewById<TextView>(R.id.secondary_button_title)?.isAllCaps = false
		for (id in intArrayOf(R.id.subtitle, R.id.item_type, R.id.address, R.id.distance_to_city)) {
			view.findViewById<TextView?>(id)?.setTextColor(colors.secondaryText)
		}
		view.findViewById<View?>(R.id.divider)?.let { divider ->
			divider.setBackgroundColor(colors.divider)
			// Inset like the icon rows; a divider the adapter drew edge to edge stays that way
			val params = divider.layoutParams as? ViewGroup.MarginLayoutParams
			if (params != null && params.marginStart > 0) {
				params.marginStart = view.resources.getDimensionPixelSize(R.dimen.mn_settings_divider_start)
				params.leftMargin = params.marginStart
				divider.layoutParams = params
			}
		}
		// Waze puts the place icon (or photo) in a round tile at the start of the row
		val container = view.findViewById<ViewGroup?>(R.id.image_container)
		if (container != null) {
			moveToRowStart(container)
			val shield = container.findViewById<View?>(R.id.shieldSign)?.visibility == View.VISIBLE
			container.background = if (shield) null else colors.circle(colors.field)
			container.clipToOutline = !shield
		} else {
			view.findViewById<ImageView?>(R.id.imageView)?.let { styleSearchIcon(it, colors) }
		}
	}

	/** OsmAnd's card shadows and gaps between search groups become plain space on the flat list. */
	@JvmStatic
	fun styleSearchSeparator(view: View, nightMode: Boolean) {
		view.setBackgroundColor(MineNavColors(view.context, nightMode).row)
		hideImages(view)
	}

	private fun hideImages(view: View) {
		if (view is ImageView) {
			view.visibility = View.GONE
		} else if (view is ViewGroup) {
			for (i in 0 until view.childCount) {
				hideImages(view.getChildAt(i))
			}
		}
	}

	private fun moveToRowStart(container: ViewGroup) {
		val row = container.parent as? LinearLayout ?: return
		if (row.orientation != LinearLayout.HORIZONTAL) {
			return
		}
		val toggle = row.findViewById<View?>(R.id.toggle_item)
		val index = if (toggle != null && toggle.parent === row) row.indexOfChild(toggle) + 1 else 0
		if (row.indexOfChild(container) == index) {
			return
		}
		val resources = container.resources
		val size = resources.getDimensionPixelSize(R.dimen.mn_settings_icon_circle)
		row.removeView(container)
		row.addView(container, index, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT).apply {
			gravity = Gravity.CENTER_VERTICAL
			marginEnd = resources.getDimensionPixelSize(R.dimen.content_padding)
		})
		container.minimumWidth = size
		container.minimumHeight = size
	}

	// Rows whose icon is not in a tile: draw the circle around the icon itself
	private fun styleSearchIcon(icon: ImageView, colors: MineNavColors) {
		val resources = icon.resources
		val size = resources.getDimensionPixelSize(R.dimen.mn_settings_icon_circle)
		val inset = resources.getDimensionPixelSize(R.dimen.content_padding_half)
		val params = icon.layoutParams as? ViewGroup.MarginLayoutParams ?: return
		if (params.width != size) {
			params.width = size
			params.height = size
			params.marginEnd = resources.getDimensionPixelSize(R.dimen.content_padding)
			params.rightMargin = params.marginEnd
			icon.layoutParams = params
			// The text column was spaced for the old 24dp icon
			val row = icon.parent as? ViewGroup
			val next = row?.getChildAt(row.indexOfChild(icon) + 1)
			(next?.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
				it.marginStart = 0
				it.leftMargin = 0
				next.layoutParams = it
			}
		}
		icon.setPadding(inset, inset, inset, inset)
		icon.background = colors.circle(colors.field)
	}

	/** Search distances as plain grey text, without OsmAnd's colored direction arrow. */
	@JvmStatic
	fun styleSearchDistance(direction: ImageView?, distance: TextView?) {
		direction?.visibility = View.GONE
		distance ?: return
		val colors = MineNavColors(distance.context, MineNavDownloadStyle.isNightMode(distance.context))
		distance.setTextColor(colors.secondaryText)
		(distance.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
			if (it.marginStart != 0) {
				it.marginStart = 0
				it.leftMargin = 0
				distance.layoutParams = it
			}
		}
	}

	@JvmStatic
	fun styleSettingsToolbar(root: View, nightMode: Boolean, onClose: Runnable) {
		val container = root.findViewById<LinearLayout>(R.id.toolbar_container) ?: return
		val context = root.context
		val colors = MineNavColors(context, nightMode)
		root.findViewById<View>(R.id.appbar)?.setBackgroundColor(colors.row)
		container.setBackgroundColor(colors.row)
		container.findViewById<ImageView>(R.id.close_button)?.imageTintList = ColorStateList.valueOf(colors.text)

		val title = container.findViewById<TextView>(R.id.toolbar_title)
		(title?.parent as? LinearLayout)?.apply {
			gravity = Gravity.CENTER_HORIZONTAL
			background = null
		}
		title?.apply {
			setTextColor(colors.text)
			gravity = Gravity.CENTER
			setTextSize(TypedValue.COMPLEX_UNIT_SP, TOOLBAR_TITLE_SP)
		}
		container.findViewById<TextView>(R.id.toolbar_subtitle)?.apply {
			setTextColor(colors.secondaryText)
			gravity = Gravity.CENTER
		}
		if (container.findViewById<View>(R.id.mn_settings_close) == null) {
			val size = context.resources.getDimensionPixelSize(R.dimen.toolbar_height)
			val padding = AndroidUtils.dpToPx(context, 16f)
			val close = ImageView(context).apply {
				id = R.id.mn_settings_close
				setImageResource(R.drawable.ic_action_close)
				imageTintList = ColorStateList.valueOf(colors.text)
				setPadding(padding, padding, padding, padding)
				contentDescription = context.getString(R.string.mn_close)
				background = AppCompatSelectable.borderless(context)
				setOnClickListener { onClose.run() }
			}
			container.addView(close, LinearLayout.LayoutParams(size, size))
		}
	}

	/**
	 * The On/Off bar at the top of some OsmAnd settings pages, drawn as a plain MineNAV row with
	 * a divider below instead of a bar in the profile color.
	 */
	@JvmStatic
	fun styleToolbarSwitchBar(button: CompoundButton, nightMode: Boolean) {
		var parent = button.parent as? View
		while (parent != null && parent.id != R.id.toolbar_switch_container) {
			parent = parent.parent as? View
		}
		val bar = parent ?: return
		val colors = MineNavColors(bar.context, nightMode)
		bar.background = LayerDrawable(arrayOf(ColorDrawable(colors.divider), ColorDrawable(colors.row))).apply {
			setLayerInset(1, 0, 0, 0, AndroidUtils.dpToPx(bar.context, 1f))
		}
		bar.findViewById<TextView>(R.id.switchButtonText)?.setTextColor(colors.text)
	}

	/**
	 * Centers the title of a standard toolbar, like the MineNAV pages, without touching what
	 * sets it: the title view is shifted after every layout, staying clear of the buttons.
	 */
	@JvmStatic
	fun centerToolbarTitle(toolbar: Toolbar, nightMode: Boolean) {
		val colors = MineNavColors(toolbar.context, nightMode)
		toolbar.setTitleTextColor(colors.text)
		if (toolbar.getTag(R.id.mn_title_centered) == true) {
			return
		}
		toolbar.setTag(R.id.mn_title_centered, true)
		toolbar.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> centerTitle(toolbar) }
	}

	/** OsmAnd's place editors: the MineNAV toolbar and rounded fields without the underline. */
	@JvmStatic
	fun styleEditor(root: View, nightMode: Boolean) {
		val colors = MineNavColors(root.context, nightMode)
		root.findViewById<Toolbar?>(R.id.toolbar)?.let { toolbar ->
			toolbar.setBackgroundColor(colors.row)
			(toolbar.parent as? View)?.setBackgroundColor(colors.row)
			centerToolbarTitle(toolbar, nightMode)
		}
		for (id in intArrayOf(R.id.name_caption, R.id.address_caption, R.id.description_caption)) {
			root.findViewById<TextInputLayout?>(id)?.let { styleTextField(it, colors) }
		}
	}

	@JvmStatic
	fun styleTextField(field: TextInputLayout, colors: MineNavColors) {
		val radius = AndroidUtils.dpToPx(field.context, FIELD_RADIUS_DP).toFloat()
		field.boxBackgroundColor = colors.field
		field.setBoxCornerRadii(radius, radius, radius, radius)
		field.boxStrokeWidth = 0
		field.boxStrokeWidthFocused = 0
		field.defaultHintTextColor = ColorStateList.valueOf(colors.secondaryText)
		field.hintTextColor = ColorStateList.valueOf(colors.accent)
		field.editText?.setTextColor(colors.text)
	}

	private fun centerTitle(toolbar: Toolbar) {
		var title: TextView? = null
		var start = 0
		var end = toolbar.width
		for (i in 0 until toolbar.childCount) {
			val child = toolbar.getChildAt(i)
			if (child.visibility != View.VISIBLE) {
				continue
			}
			if (child is TextView && child.text == toolbar.title) {
				title = child
			} else if (child.right <= toolbar.width / 2) {
				start = maxOf(start, child.right)
			} else if (child.left >= toolbar.width / 2) {
				end = minOf(end, child.left)
			}
		}
		val view = title ?: return
		val centered = (toolbar.width - view.width) / 2
		val target = centered.coerceAtMost(end - view.width).coerceAtLeast(start)
		view.translationX = if (end - start >= view.width) (target - view.left).toFloat() else 0f
		view.setTextSize(TypedValue.COMPLEX_UNIT_SP, TOOLBAR_TITLE_SP)
	}

	/**
	 * Closes every settings and menu screen at once and returns to the map, like the X in Waze:
	 * pops the back stack from the first MineNAV page or OsmAnd settings screen.
	 */
	@JvmStatic
	fun closeAllSettings(activity: FragmentActivity) {
		val manager = activity.supportFragmentManager
		if (manager.isStateSaved) {
			return
		}
		for (i in 0 until manager.backStackEntryCount) {
			val entry = manager.getBackStackEntryAt(i)
			val name = entry.name ?: continue
			if (name.startsWith(MINENAV_PAGE_PREFIX) || name == DRAWER_SETTINGS_ID) {
				manager.popBackStack(entry.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
				return
			}
		}
	}

	@JvmStatic
	fun setForwardAnimations(transaction: FragmentTransaction): FragmentTransaction {
		return transaction.setCustomAnimations(
			R.anim.mn_enter_from_right, R.anim.mn_exit_to_left,
			R.anim.mn_enter_from_left, R.anim.mn_exit_to_right
		)
	}

	@JvmStatic
	fun setRiseAnimations(transaction: FragmentTransaction): FragmentTransaction {
		return transaction.setCustomAnimations(
			R.anim.mn_enter_from_bottom, R.anim.mn_fade_out,
			R.anim.mn_fade_in, R.anim.mn_exit_to_bottom
		)
	}

	/**
	 * Waze-like pill switch: white knob on a grey track that takes the MineNAV accent when on.
	 */
	@JvmStatic
	fun styleSwitch(switch: SwitchCompat, @ColorInt offTrackColor: Int) {
		val context = switch.context
		val onColor = ContextCompat.getColor(context, R.color.mn_primary)
		switch.thumbDrawable = createThumb(context)
		switch.trackDrawable = createTrack(context, onColor, offTrackColor)
		switch.thumbTintList = null
		switch.trackTintList = null
		// SwitchCompat makes the track at least twice the knob plus its margins: 2 * 24 + 2 * 4 = 56dp
		switch.switchMinWidth = AndroidUtils.dpToPx(context, SWITCH_WIDTH_DP)
	}

	// The 24dp knob is centered in a transparent 32dp high column. Insets would be read by
	// SwitchCompat as a shadow and shrink the track, leaving the knob flush with its ends.
	private fun createThumb(context: Context): Drawable {
		val knobSize = AndroidUtils.dpToPx(context, SWITCH_KNOB_DP)
		val height = AndroidUtils.dpToPx(context, SWITCH_HEIGHT_DP)
		val margin = (height - knobSize) / 2
		val column = GradientDrawable().apply {
			setColor(Color.TRANSPARENT)
			setSize(knobSize, height)
		}
		val knob = GradientDrawable().apply {
			shape = GradientDrawable.OVAL
			setColor(Color.WHITE)
			setSize(knobSize, knobSize)
		}
		return LayerDrawable(arrayOf(column, knob)).apply {
			setLayerInset(1, 0, margin, 0, margin)
		}
	}

	// The track's side padding keeps the knob 4dp away from both ends
	private fun createTrack(context: Context, @ColorInt onColor: Int, @ColorInt offColor: Int): Drawable {
		val width = AndroidUtils.dpToPx(context, SWITCH_WIDTH_DP)
		val height = AndroidUtils.dpToPx(context, SWITCH_HEIGHT_DP)
		val margin = (height - AndroidUtils.dpToPx(context, SWITCH_KNOB_DP)) / 2
		fun pill(@ColorInt color: Int) = GradientDrawable().apply {
			cornerRadius = height / 2f
			setColor(color)
			setSize(width, height)
		}
		val states = StateListDrawable().apply {
			addState(intArrayOf(-android.R.attr.state_enabled), pill(Color.argb(90, Color.red(offColor), Color.green(offColor), Color.blue(offColor))))
			addState(intArrayOf(android.R.attr.state_checked), pill(onColor))
			addState(intArrayOf(), pill(offColor))
		}
		return LayerDrawable(arrayOf(states)).apply {
			setPadding(margin, 0, margin, 0)
		}
	}

	/**
	 * Waze-like OsmAnd bottom sheet: a drag handle on top and a bold title.
	 */
	@JvmStatic
	fun styleSheet(mainView: View, items: List<BaseBottomSheetItem>, nightMode: Boolean) {
		val colors = MineNavColors(mainView.context, nightMode)
		mainView.findViewById<LinearLayout?>(R.id.main_container)?.let { addHandle(it, colors) }
		for (item in items) {
			if (item is TitleItem) {
				(item.view as? TextView)?.apply {
					typeface = FontCache.getFont(FontCache.getAppTypeface(), 800)
					setTextSize(TypedValue.COMPLEX_UNIT_SP, SHEET_TITLE_SP)
				}
			}
		}
	}

	/** The small bar at the top of MineNAV sheets and cards. */
	@JvmStatic
	fun addHandle(container: LinearLayout, colors: MineNavColors) {
		if (container.findViewById<View>(R.id.mn_card_handle) != null) {
			return
		}
		val resources = container.resources
		val handle = View(container.context).apply {
			id = R.id.mn_card_handle
			background = colors.pill(colors.color(if (colors.nightMode) R.color.mn_handle_dark else R.color.mn_handle_light))
		}
		val params = LinearLayout.LayoutParams(
			resources.getDimensionPixelSize(R.dimen.where_to_handle_width),
			resources.getDimensionPixelSize(R.dimen.where_to_handle_height)
		).apply {
			gravity = Gravity.CENTER_HORIZONTAL
			topMargin = resources.getDimensionPixelSize(R.dimen.content_padding_half)
		}
		container.addView(handle, 0, params)
	}

	/**
	 * Sets the background of a map sheet and runs it under the gesture bar down to the screen
	 * edge, like the Waze sheets; the sheet's content stays above the bar.
	 */
	@JvmStatic
	fun setSheetBackground(sheet: View, surface: View, background: Drawable) {
		surface.setTag(R.id.mn_sheet_background, background)
		surface.setTag(R.id.mn_sheet_extension, null)
		if (sheet.getTag(R.id.mn_sheet_extension) == null) {
			sheet.setTag(R.id.mn_sheet_extension, true)
			sheet.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> extendToScreenBottom(sheet, surface) }
		}
		extendToScreenBottom(sheet, surface)
	}

	private fun extendToScreenBottom(sheet: View, surface: View) {
		val background = surface.getTag(R.id.mn_sheet_background) as? Drawable ?: return
		val location = IntArray(2)
		surface.getLocationInWindow(location)
		// The slide-in animation moves the sheet; measure where it rests
		val bottom = location[1] - sheet.translationY + surface.height
		val gap = (surface.rootView.height - bottom).toInt().coerceAtLeast(0)
		if (surface.height == 0 || surface.getTag(R.id.mn_sheet_extension) == gap) {
			return
		}
		surface.setTag(R.id.mn_sheet_extension, gap)
		// A background with insets also replaces the view's padding, so keep the sheet's own
		val padding = intArrayOf(surface.paddingLeft, surface.paddingTop, surface.paddingRight, surface.paddingBottom)
		if (gap > 0) {
			allowDrawingBelow(surface)
			surface.background = InsetDrawable(background, 0, 0, 0, -gap)
		} else {
			surface.background = background
		}
		surface.setPadding(padding[0], padding[1], padding[2], padding[3])
	}

	// Each parent up to the map HUD has to let the sheet paint outside its bounds
	private fun allowDrawingBelow(view: View) {
		var parent = view.parent as? ViewGroup
		while (parent != null) {
			parent.clipChildren = false
			parent.clipToPadding = false
			if (parent.id == R.id.map_hud_container) {
				return
			}
			parent = parent.parent as? ViewGroup
		}
	}

	/**
	 * Slides a bottom sheet in or out instead of popping it.
	 */
	@JvmStatic
	fun animateSheet(sheet: View, show: Boolean) {
		val current = sheet.getTag(R.id.mn_sheet_target) as? Boolean ?: (sheet.visibility == View.VISIBLE)
		if (current == show) {
			return
		}
		sheet.setTag(R.id.mn_sheet_target, show)
		val duration = sheet.resources.getInteger(R.integer.mn_sheet_anim_duration).toLong()
		val offset = AndroidUtils.dpToPx(sheet.context, 48f).toFloat()
		sheet.animate().cancel()
		if (show) {
			sheet.alpha = 0f
			sheet.translationY = offset
			sheet.visibility = View.VISIBLE
			sheet.animate().alpha(1f).translationY(0f).setDuration(duration).withEndAction(null).start()
		} else {
			sheet.animate().alpha(0f).translationY(offset).setDuration(duration).withEndAction {
				if (sheet.getTag(R.id.mn_sheet_target) == false) {
					sheet.visibility = View.GONE
					sheet.alpha = 1f
					sheet.translationY = 0f
				}
			}.start()
		}
	}

	private object AppCompatSelectable {
		fun borderless(context: Context): Drawable? {
			val value = TypedValue()
			context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, value, true)
			return ContextCompat.getDrawable(context, value.resourceId)
		}
	}
}
