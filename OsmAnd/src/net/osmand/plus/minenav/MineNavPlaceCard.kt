package net.osmand.plus.minenav

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import net.osmand.plus.R
import net.osmand.plus.utils.FontCache

/**
 * Waze-style look for the OsmAnd place card (map context menu): one rounded sheet with a handle,
 * a bold title, round action buttons and pill buttons, like the MineNAV route preview.
 */
object MineNavPlaceCard {

	private const val TITLE_SIZE_SP = 22f
	private const val ACTION_TEXT_SP = 13f
	private const val BUTTON_TEXT_SP = 16f

	@JvmStatic
	fun style(root: View, nightMode: Boolean) {
		val colors = MineNavColors(root.context, nightMode)
		(root.findViewById<View>(R.id.context_menu_top_shadow_all) as? LinearLayout)?.let { MineNavStyle.addHandle(it, colors) }
		root.findViewById<TextView>(R.id.context_menu_line1)?.apply {
			typeface = FontCache.getFont(FontCache.getAppTypeface(), 800)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_SIZE_SP)
			setTextColor(colors.text)
		}
		root.findViewById<TextView>(R.id.context_menu_line2)?.setTextColor(colors.secondaryText)
		// One plain sheet: no lines between the header, the actions and the buttons
		for (id in intArrayOf(R.id.buttons_top_border, R.id.buttons_bottom_border)) {
			root.findViewById<View>(id)?.setBackgroundColor(colors.row)
		}
		for (id in intArrayOf(R.id.context_menu_buttons, R.id.context_menu_bottom_scroll, R.id.context_menu_bottom_view)) {
			root.findViewById<View>(id)?.setBackgroundColor(colors.row)
		}
		(root.findViewById<View>(R.id.context_menu_bottom_buttons) as? LinearLayout)?.let { styleBottomButtons(it, colors) }
		// Bar shown when the card is pulled up to full screen, like the MineNAV page toolbars
		root.findViewById<View>(R.id.context_menu_toolbar)?.setBackgroundColor(colors.row)
		root.findViewById<TextView>(R.id.context_menu_toolbar_text)?.apply {
			setTextColor(colors.text)
			typeface = FontCache.getFont(FontCache.getAppTypeface(), 700)
		}
		root.findViewById<ImageView>(R.id.context_menu_toolbar_back)?.imageTintList = ColorStateList.valueOf(colors.text)
	}

	/** Round buttons with a label below, rebuilt every time the actions change. */
	@JvmStatic
	fun styleActions(buttons: LinearLayout, nightMode: Boolean) {
		val context = buttons.context
		val colors = MineNavColors(context, nightMode)
		val size = context.resources.getDimensionPixelSize(R.dimen.mn_round_button_size)
		val padding = context.resources.getDimensionPixelSize(R.dimen.content_padding_small)
		buttons.setBackgroundColor(colors.row)
		for (i in 0 until buttons.childCount) {
			val item = buttons.getChildAt(i)
			item.findViewById<ImageView>(R.id.icon)?.apply {
				layoutParams = layoutParams.apply {
					width = size
					height = size
				}
				setPadding(padding, padding, padding, padding)
				background = colors.circle(colors.field)
				MineNavStyle.tintIcon(this, colors.text)
			}
			item.findViewById<TextView>(R.id.text)?.apply {
				setTextColor(colors.secondaryText)
				setTextSize(TypedValue.COMPLEX_UNIT_SP, ACTION_TEXT_SP)
			}
			item.findViewById<View>(R.id.item)?.setPadding(0, padding, 0, padding / 2)
		}
	}

	/** The card's "Actions" sheet: the same round buttons on a rounded sheet, closed with a pill. */
	@JvmStatic
	fun styleActionsSheet(root: View, nightMode: Boolean) {
		val context = root.context
		val colors = MineNavColors(context, nightMode)
		val resources = context.resources
		val size = resources.getDimensionPixelSize(R.dimen.mn_round_button_size)
		val small = resources.getDimensionPixelSize(R.dimen.content_padding_small)
		val padding = resources.getDimensionPixelSize(R.dimen.content_padding)
		root.findViewById<LinearLayout>(R.id.context_menu_items_container)?.let { items ->
			MineNavStyle.addHandle(items, colors)
			styleGridItems(items, colors, size, small)
		}
		root.findViewById<View>(R.id.divider)?.visibility = View.GONE
		root.findViewById<View>(R.id.cancel_row_background)?.setBackgroundColor(colors.row)
		root.findViewById<ViewGroup>(R.id.cancel_row)?.let { row ->
			row.background = colors.pill(colors.field)
			(row.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
				params.height = resources.getDimensionPixelSize(R.dimen.route_preview_button_height)
				params.setMargins(padding, small, padding, padding)
				row.layoutParams = params
			}
			(row.getChildAt(0) as? TextView)?.apply {
				isAllCaps = false
				setTextColor(colors.text)
				typeface = FontCache.getFont(FontCache.getAppTypeface(), 700)
				setTextSize(TypedValue.COMPLEX_UNIT_SP, BUTTON_TEXT_SP)
			}
		}
	}

	/** The Actions sheet's own background: rounded on top, square once it fills the screen. */
	@JvmStatic
	fun actionsSheetBackground(view: View, nightMode: Boolean, fullScreen: Boolean): Drawable {
		val colors = MineNavColors(view.context, nightMode)
		val radius = if (fullScreen) 0f else view.resources.getDimension(R.dimen.where_to_sheet_radius)
		return GradientDrawable().apply {
			setColor(colors.row)
			cornerRadii = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
		}
	}

	private fun styleGridItems(group: ViewGroup, colors: MineNavColors, size: Int, padding: Int) {
		for (i in 0 until group.childCount) {
			val child = group.getChildAt(i)
			when {
				child is ImageView && child.id == R.id.icon -> {
					child.layoutParams = child.layoutParams.apply {
						width = size
						height = size
					}
					child.setPadding(padding, padding, padding, padding)
					child.background = colors.circle(colors.field)
					MineNavStyle.tintIcon(child, colors.text)
				}
				child is TextView && child.id == R.id.title -> {
					child.setTextColor(colors.secondaryText)
					child.setTextSize(TypedValue.COMPLEX_UNIT_SP, ACTION_TEXT_SP)
				}
				child is ViewGroup -> styleGridItems(child, colors, size, padding)
			}
		}
	}

	// "Details" as the grey pill and the main action as the colored pill, side by side
	private fun styleBottomButtons(bar: LinearLayout, colors: MineNavColors) {
		val resources = bar.resources
		val padding = resources.getDimensionPixelSize(R.dimen.content_padding)
		bar.setBackgroundColor(colors.row)
		bar.layoutParams = bar.layoutParams.apply { height = ViewGroup.LayoutParams.WRAP_CONTENT }
		bar.setPadding(padding, padding / 2, padding, padding)
		bar.gravity = Gravity.CENTER_VERTICAL
		bar.findViewById<TextView>(R.id.context_menu_details_button)?.let {
			stylePill(it, colors.field, colors.text, colors, 0)
		}
		bar.findViewById<TextView>(R.id.context_menu_directions_button)?.let {
			stylePill(it, colors.color(R.color.mn_primary), Color.WHITE, colors,
				resources.getDimensionPixelSize(R.dimen.content_padding_small))
		}
	}

	private fun stylePill(button: TextView, fill: Int, textColor: Int, colors: MineNavColors, marginStart: Int) {
		val height = button.resources.getDimensionPixelSize(R.dimen.route_preview_button_height)
		button.isAllCaps = false
		button.background = colors.pill(fill)
		button.setTextColor(textColor)
		button.gravity = Gravity.CENTER
		button.typeface = FontCache.getFont(FontCache.getAppTypeface(), 700)
		button.setTextSize(TypedValue.COMPLEX_UNIT_SP, BUTTON_TEXT_SP)
		button.setPaddingRelative(0, 0, 0, 0)
		// Copies, because the icons come from the shared OsmAnd icon cache
		val icons = button.compoundDrawables.map { icon ->
			icon?.constantState?.newDrawable(button.resources)?.mutate()?.apply { setTint(textColor) } ?: icon
		}
		button.setCompoundDrawablesWithIntrinsicBounds(icons[0], icons[1], icons[2], icons[3])
		(button.layoutParams as? LinearLayout.LayoutParams)?.let {
			it.width = 0
			it.weight = 1f
			it.height = height
			it.marginStart = marginStart
			button.layoutParams = it
		}
	}
}
