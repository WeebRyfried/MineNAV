package net.osmand.plus.minenav

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import net.osmand.data.FavouritePoint
import net.osmand.data.LatLon
import net.osmand.data.SpecialPointType
import net.osmand.plus.R

/**
 * Waze-style "Saved places": Home, Work and the saved favorites. Tapping a place opens the
 * "Go now" route preview; editing stays in the full My Places screen.
 */
class MineNavSavedPlacesFragment : MineNavPageFragment() {

	override fun getTitleId(): Int = R.string.mn_menu_saved_places

	override fun createSections(): List<Section> {
		val places = listOf(
			specialPlaceRow(SpecialPointType.HOME, R.drawable.ic_action_home_dark, R.color.mn_icon_home, R.string.home_button),
			specialPlaceRow(SpecialPointType.WORK, R.drawable.ic_action_work, R.color.mn_icon_work, R.string.work_button)
		)
		val favorites = app.favoritesHelper.favouritePoints
			.filter { !it.isSpecialPoint }
			.sortedBy { it.getDisplayName(app).lowercase() }
			.map { favoriteRow(it) }
		val saved = favorites.ifEmpty {
			listOf(Row.Open(R.drawable.ic_action_favorite, R.color.mn_circle_grey,
				getString(R.string.mn_saved_empty)) { MineNavActions.openMyPlaces(it) })
		}
		val manage = Row.Open(R.drawable.ic_action_folder_favorites, R.color.mn_circle_blue,
			getString(R.string.mn_saved_manage)) { MineNavActions.openMyPlaces(it) }
		return listOf(
			Section(null, places),
			Section(getString(R.string.mn_saved_favorites), saved),
			Section(null, listOf(manage))
		)
	}

	private fun specialPlaceRow(type: SpecialPointType, @DrawableRes iconId: Int, @ColorRes colorId: Int,
	                            @StringRes nameId: Int): Row {
		val point = app.favoritesHelper.getSpecialPoint(type)
		val name = getString(nameId)
		val subtitle = if (point != null) {
			point.address?.takeIf { it.isNotEmpty() } ?: point.getPointDescription(app).getSimpleName(app, false)
		} else {
			getString(R.string.mn_set_place, name.lowercase())
		}
		return Row.Open(iconId, colorId, name, subtitle = subtitle) {
			MineNavActions.navigateToSpecialPoint(it, type, nameId)
		}
	}

	private fun favoriteRow(point: FavouritePoint): Row {
		val defaultColor = ContextCompat.getColor(app, R.color.mn_primary)
		val color = app.favoritesHelper.getColorWithCategory(point, defaultColor)
		val subtitle = point.address?.takeIf { it.isNotEmpty() } ?: point.getCategoryDisplayName(app)
		return Row.Open(point.getOverlayIconId(app), 0, point.getDisplayName(app),
			subtitle = subtitle, circleColor = color) {
			MineNavActions.navigateTo(it, LatLon(point.latitude, point.longitude), point.getPointDescription(app))
		}
	}

	companion object {

		@JvmStatic
		fun showInstance(manager: FragmentManager) {
			show(manager, MineNavSavedPlacesFragment(), MineNavSavedPlacesFragment::class.java.simpleName)
		}
	}
}
