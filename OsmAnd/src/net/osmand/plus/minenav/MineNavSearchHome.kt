package net.osmand.plus.minenav

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.ShapeAppearanceModel
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.SpecialPointType
import net.osmand.osm.AbstractPoiType
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment
import net.osmand.plus.search.dialogs.QuickSearchListFragment
import net.osmand.plus.utils.AndroidUtils
import net.osmand.search.core.SearchCoreFactory.SearchAmenityTypesAPI

/**
 * Waze-style face for the search screen: a "Where to?" pill, category tiles, Home and Work,
 * and recent places. Typing still uses the full search engine and its result list.
 */
class MineNavSearchHome private constructor(
	private val fragment: QuickSearchDialogFragment,
	private val root: View,
	nightMode: Boolean
) {

	companion object {

		private const val MAX_RECENT_ITEMS = 8

		@JvmStatic
		fun install(fragment: QuickSearchDialogFragment, root: View, nightMode: Boolean): MineNavSearchHome {
			return MineNavSearchHome(fragment, root, nightMode).also { it.setup() }
		}
	}

	private val app = root.context.applicationContext as OsmandApplication
	private val palette = MineNavColors(root.context, nightMode)
	private val inflater = LayoutInflater.from(root.context)

	private fun setup() {
		root.setBackgroundColor(palette.row)
		root.findViewById<View>(R.id.search_view)?.setBackgroundColor(palette.row)
		root.findViewById<View>(R.id.button_toolbar_layout)?.setBackgroundColor(palette.row)
		root.findViewById<View>(R.id.tab_toolbar_layout)?.visibility = View.GONE
		root.findViewById<View>(R.id.pager)?.visibility = View.GONE
		styleSearchBar()

		val tabs = root.findViewById<LinearLayout>(R.id.tabs_view)
		tabs.setBackgroundColor(palette.row)
		val home = inflater.inflate(R.layout.mn_search_home, tabs, false)
		tabs.addView(home, 0, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
		home.findViewById<TextView>(R.id.mn_search_recent_title).setTextColor(palette.secondaryText)
		setupTiles(home.findViewById(R.id.mn_search_tiles))
		// Round floating buttons, like the map buttons
		val round = ShapeAppearanceModel.builder().setAllCornerSizes(RelativeCornerSize(0.5f)).build()
		for (id in intArrayOf(R.id.fab, R.id.show_on_map_fab)) {
			root.findViewById<FloatingActionButton>(id)?.shapeAppearanceModel = round
		}
		refresh()
	}

	private fun styleSearchBar() {
		val context = root.context
		val toolbar = root.findViewById<Toolbar>(R.id.toolbar)
		toolbar.setBackgroundColor(palette.row)
		toolbar.navigationIcon = null
		toolbar.layoutParams = toolbar.layoutParams.apply {
			height = context.resources.getDimensionPixelSize(R.dimen.mn_search_toolbar_height)
		}

		val bar = root.findViewById<LinearLayout>(R.id.search_container)
		val margin = context.resources.getDimensionPixelSize(R.dimen.content_padding)
		val params = bar.layoutParams as ViewGroup.MarginLayoutParams
		params.height = context.resources.getDimensionPixelSize(R.dimen.mn_search_bar_height)
		params.setMargins(margin, 0, margin, 0)
		bar.layoutParams = params
		bar.background = palette.pill(palette.field)

		val back = ImageView(context).apply {
			setImageResource(R.drawable.ic_arrow_back)
			imageTintList = ColorStateList.valueOf(palette.secondaryText)
			val padding = AndroidUtils.dpToPx(context, 14f)
			setPadding(padding, padding, padding / 2, padding)
			contentDescription = context.getString(R.string.shared_string_back)
			setOnClickListener { fragment.onMineNavBackPressed() }
		}
		bar.addView(back, 0, LinearLayout.LayoutParams(params.height, params.height))

		bar.findViewById<EditText>(R.id.searchEditText).apply {
			setTextColor(palette.text)
			setHintTextColor(palette.secondaryText)
			setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
		}
		bar.findViewById<ImageView>(R.id.clearButton).imageTintList = ColorStateList.valueOf(palette.secondaryText)
	}

	private fun setupTiles(tiles: LinearLayout) {
		// Saved places start a drive, so they are left out while a route point is being picked
		if (!fragment.searchType.isTargetPoint) {
			addTile(tiles, R.drawable.ic_action_bookmark_filled, R.color.mn_circle_orange, R.string.where_to_saved) {
				withMapActivity {
					fragment.dismiss()
					MineNavActions.openSavedPlaces(it)
				}
			}
		}
		addTile(tiles, R.drawable.mm_amenity_fuel, R.color.mn_circle_green, R.string.mn_search_gas) {
			searchCategory("fuel")
		}
		addTile(tiles, R.drawable.mm_amenity_restaurant, R.color.mn_circle_orange, R.string.mn_search_food) {
			searchCategory("sustenance", "restaurant")
		}
		addTile(tiles, R.drawable.mm_amenity_parking, R.color.mn_circle_blue, R.string.mn_search_parking) {
			searchCategory("parking")
		}
	}

	private fun addTile(parent: LinearLayout, @DrawableRes iconId: Int, @ColorRes iconColor: Int,
	                    @StringRes titleId: Int, action: () -> Unit) {
		val tile = inflater.inflate(R.layout.mn_search_tile, parent, false)
		tile.background = GradientDrawable().apply {
			cornerRadius = root.resources.getDimension(R.dimen.mn_search_tile_radius)
			setColor(palette.field)
		}
		tile.findViewById<ImageView>(R.id.tile_icon).apply {
			setImageResource(iconId)
			imageTintList = ColorStateList.valueOf(palette.color(iconColor))
		}
		tile.findViewById<TextView>(R.id.tile_title).apply {
			setText(titleId)
			setTextColor(palette.text)
		}
		tile.setOnClickListener { action() }
		parent.addView(tile)
	}

	/** Rebuilds Home, Work and recent places. */
	fun refresh() {
		val places = root.findViewById<LinearLayout>(R.id.mn_search_places) ?: return
		places.removeAllViews()
		addSpecialPlace(places, SpecialPointType.HOME, R.drawable.ic_action_home_dark, R.color.mn_icon_home, R.string.home_button)
		addSpecialPlace(places, SpecialPointType.WORK, R.drawable.ic_action_work, R.color.mn_icon_work, R.string.work_button)

		val recent = root.findViewById<LinearLayout>(R.id.mn_search_recent)
		recent.removeAllViews()
		// The history can hold the same place more than once, e.g. found by two kinds of search
		val entries = app.searchHistoryHelper.getHistoryEntries(true)
			.sortedByDescending { it.lastAccessTime }
			.distinctBy { it.name.getSimpleName(root.context, false) }
			.take(MAX_RECENT_ITEMS)
		AndroidUiHelper.updateVisibility(root.findViewById(R.id.mn_search_recent_title), entries.isNotEmpty())
		entries.forEachIndexed { index, entry ->
			val name = entry.name
			addRow(recent, R.drawable.ic_action_history, palette.field, palette.text, name.getSimpleName(root.context, false),
				name.typeName.orEmpty(), index < entries.lastIndex) {
				closeAndNavigate(LatLon(entry.lat, entry.lon), name)
			}
		}
	}

	private fun addSpecialPlace(parent: LinearLayout, type: SpecialPointType, @DrawableRes iconId: Int,
	                            @ColorRes iconColor: Int, @StringRes nameId: Int) {
		val point = app.favoritesHelper.getSpecialPoint(type)
		val name = root.context.getString(nameId)
		val subtitle = if (point == null) {
			root.context.getString(R.string.mn_set_place, name.lowercase())
		} else {
			point.address?.takeIf { it.isNotEmpty() } ?: point.getPointDescription(app).getSimpleName(root.context, false)
		}
		addRow(parent, iconId, palette.color(iconColor), Color.WHITE, name, subtitle, type == SpecialPointType.HOME) {
			if (point != null && fragment.searchType.isTargetPoint) {
				closeAndNavigate(LatLon(point.latitude, point.longitude), point.getPointDescription(app))
				return@addRow
			}
			withMapActivity { activity ->
				fragment.dismiss()
				MineNavActions.navigateToSpecialPoint(activity, type, nameId)
			}
		}
	}

	// Round icon like the rest of MineNAV: colored for Home and Work, neutral for recent places
	private fun addRow(parent: LinearLayout, @DrawableRes iconId: Int, @ColorInt circleColor: Int, @ColorInt iconColor: Int,
	                   title: String, subtitle: String, divider: Boolean, action: () -> Unit) {
		val row = inflater.inflate(R.layout.mn_place_row, parent, false)
		row.findViewById<ImageView>(R.id.place_icon).apply {
			setImageResource(iconId)
			background = palette.circle(circleColor)
			imageTintList = ColorStateList.valueOf(iconColor)
		}
		row.findViewById<TextView>(R.id.place_title).apply {
			text = title
			setTextColor(palette.text)
		}
		row.findViewById<TextView>(R.id.place_subtitle).apply {
			text = subtitle
			setTextColor(palette.secondaryText)
			AndroidUiHelper.updateVisibility(this, subtitle.isNotEmpty())
		}
		row.findViewById<View>(R.id.place_divider).apply {
			setBackgroundColor(palette.divider)
			AndroidUiHelper.updateVisibility(this, divider)
		}
		row.findViewById<View>(R.id.place_content).setOnClickListener { action() }
		parent.addView(row)
	}

	private fun searchCategory(vararg keys: String) {
		val core = app.searchUICore.core
		val results = core.shallowSearch(SearchAmenityTypesAPI::class.java, "", null)?.currentSearchResults ?: return
		val match = keys.firstNotNullOfOrNull { key ->
			results.firstOrNull { (it.`object` as? AbstractPoiType)?.keyName == key }
		}
		if (match != null) {
			fragment.completeQueryWithObject(match)
		}
	}

	// Drives there, or uses the place as the start, stop or destination being picked
	private fun closeAndNavigate(latLon: LatLon, description: PointDescription) {
		withMapActivity { activity ->
			if (fragment.searchType.isTargetPoint) {
				val zoom = app.settings.lastKnownMapZoom
				QuickSearchListFragment.showOnMap(activity, fragment, latLon.latitude, latLon.longitude, zoom, description, null)
			} else {
				fragment.dismiss()
				MineNavActions.navigateTo(activity, latLon, description)
			}
		}
	}

	private fun withMapActivity(action: (MapActivity) -> Unit) {
		(fragment.activity as? MapActivity)?.let(action)
	}
}
