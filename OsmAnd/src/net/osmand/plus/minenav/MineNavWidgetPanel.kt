package net.osmand.plus.minenav

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import net.osmand.plus.R
import net.osmand.plus.utils.AndroidUtils

/**
 * Waze-style bottom panel while driving: the street name floats in a pill over the map, above a
 * sheet with rounded top corners that holds the trip info and runs down to the screen's edge.
 */
object MineNavWidgetPanel {

	private const val SHEET_RADIUS_DP = 24f
	private const val BUTTON_SIZE_DP = 40f
	private const val WAYPOINT_RADIUS_DP = 16f

	/** Gives the bottom widget panel its sheet, which starts at the first row that is not the street name. */
	@JvmStatic
	fun styleBottomPanel(panel: ViewGroup, nightMode: Boolean) {
		val sheet = panel.background as? SheetBackground
			?: SheetBackground(AndroidUtils.dpToPx(panel.context, SHEET_RADIUS_DP).toFloat()).also {
				panel.background = it
			}
		sheet.color = MineNavColors(panel.context, nightMode).row
		ViewCompat.setElevation(panel, 0f)
		updateSheetTop(panel)
	}

	@JvmStatic
	fun updateSheetTop(panel: ViewGroup) {
		val sheet = panel.background as? SheetBackground ?: return
		var top = panel.height
		for (i in 0 until panel.childCount) {
			val row = panel.getChildAt(i)
			if (row.visibility == View.VISIBLE && row.height > 0 && row.findViewById<View?>(R.id.street_name_widget) == null) {
				top = row.top
				break
			}
		}
		sheet.sheetTop = top
	}

	/** The street name (or the next waypoint) in a pill of its own, centered over the map. */
	@JvmStatic
	fun styleStreetName(widget: View, nightMode: Boolean) {
		val colors = MineNavColors(widget.context, nightMode)
		val resources = widget.resources
		val horizontal = resources.getDimensionPixelSize(R.dimen.content_padding)
		val vertical = resources.getDimensionPixelSize(R.dimen.content_padding_small)
		widget.background = null
		val name = (widget as? ViewGroup)?.getChildAt(0) as? LinearLayout ?: return
		name.background = colors.pill(colors.row)
		name.setPadding(horizontal, vertical, horizontal, vertical)
		(name.layoutParams as? FrameLayout.LayoutParams)?.let { params ->
			params.width = ViewGroup.LayoutParams.WRAP_CONTENT
			params.gravity = Gravity.CENTER
			params.topMargin = vertical
			params.bottomMargin = vertical
			name.layoutParams = params
		}
		widget.findViewById<TextView?>(R.id.map_address_text)?.setTextColor(colors.text)
		widget.findViewById<View?>(R.id.waypoint_info_bar)?.let { waypoint ->
			waypoint.background = GradientDrawable().apply {
				cornerRadius = AndroidUtils.dpToPx(widget.context, WAYPOINT_RADIUS_DP).toFloat()
				setColor(colors.row)
			}
			(waypoint.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
				params.setMargins(horizontal, vertical, horizontal, vertical)
				waypoint.layoutParams = params
			}
		}
	}

	/** The button that opens the trip details: round, like the map buttons. */
	@JvmStatic
	fun styleRouteInfoButton(button: View, nightMode: Boolean) {
		val colors = MineNavColors(button.context, nightMode)
		button.background = RippleDrawable(ColorStateList.valueOf(colors.divider), colors.circle(colors.field), null)
		((button as? ViewGroup)?.getChildAt(0) as? ImageView)?.imageTintList = ColorStateList.valueOf(colors.text)
		val container = button.parent as? View ?: return
		val size = AndroidUtils.dpToPx(button.context, BUTTON_SIZE_DP)
		val params = container.layoutParams
		if (params.width != size || params.height != size) {
			params.width = size
			params.height = size
			container.layoutParams = params
		}
	}

	private class SheetBackground(private val radius: Float) : Drawable() {

		private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
		private val rect = RectF()

		var color: Int
			get() = paint.color
			set(value) {
				if (paint.color != value) {
					paint.color = value
					invalidateSelf()
				}
			}

		var sheetTop = 0
			set(value) {
				if (field != value) {
					field = value
					invalidateSelf()
				}
			}

		override fun draw(canvas: Canvas) {
			val top = bounds.top + sheetTop
			if (top >= bounds.bottom) {
				return
			}
			// The bottom corners stay below the screen's edge
			rect.set(bounds.left.toFloat(), top.toFloat(), bounds.right.toFloat(), bounds.bottom + radius)
			canvas.drawRoundRect(rect, radius, radius, paint)
		}

		override fun setAlpha(alpha: Int) {
			paint.alpha = alpha
			invalidateSelf()
		}

		override fun setColorFilter(colorFilter: ColorFilter?) {
			paint.colorFilter = colorFilter
			invalidateSelf()
		}

		@Deprecated("Deprecated in Java")
		override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
	}
}
