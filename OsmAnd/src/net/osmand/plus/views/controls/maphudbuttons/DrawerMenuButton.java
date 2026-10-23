package net.osmand.plus.views.controls.maphudbuttons;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.minenav.MineNavActions;
import net.osmand.plus.views.mapwidgets.configure.buttons.DrawerMenuButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;

public class DrawerMenuButton extends MapButton {

	private final DrawerMenuButtonState buttonState;

	public DrawerMenuButton(@NonNull Context context) {
		this(context, null);
	}

	public DrawerMenuButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DrawerMenuButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		buttonState = app.getMapButtonsHelper().getDrawerMenuButtonState();

		// MineNAV: the Waze menu button opens the MineNAV menu
		setOnClickListener(v -> MineNavActions.openMenu(mapActivity));
	}

	@Nullable
	@Override
	public MapButtonState getButtonState() {
		return buttonState;
	}

	@Override
	protected boolean shouldShow() {
		return isMineNavHomeVisible();
	}
}