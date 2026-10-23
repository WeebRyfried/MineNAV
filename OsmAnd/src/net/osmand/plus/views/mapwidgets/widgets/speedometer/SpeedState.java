package net.osmand.plus.views.mapwidgets.widgets.speedometer;

import android.content.Context;
import android.graphics.Color;

import net.osmand.plus.R;

public enum SpeedState {
	SAFE(R.color.color_transparent, R.color.color_transparent),
	WARNING(R.color.speedometer_bg_tolerance_day, R.color.speedometer_bg_tolerance_night),
	EXCEED(R.color.speedometer_bg_limit_day, R.color.speedometer_bg_limit_night);

	final int alertColorDay;
	final int alertColorNight;

	SpeedState(int alertColorDay, int alertColorNight) {
		this.alertColorDay = alertColorDay;
		this.alertColorNight = alertColorNight;
	}

	public int getAlertColor(Context context, boolean nightMode) {
		int colorRes = nightMode ? alertColorNight : alertColorDay;
		return context.getColor(colorRes);
	}

	public int getSpeedTextColor(Context context, boolean nightMode) {
		int color = 0;
		switch (this) {
			case SAFE -> color = context.getColor(R.color.mn_speed_text);
			case WARNING -> color = context.getColor(R.color.speed_limit_tolerance_value);
			case EXCEED -> color = context.getColor(R.color.speeding_value);
		}
		return color;
	}

	public int getSpeedUnitTextColor(Context context, boolean nightMode) {
		int color = 0;
		switch (this) {
			case SAFE -> color = context.getColor(R.color.mn_speed_units);
			case WARNING -> color = context.getColor(nightMode ? R.color.speedometer_tolerance_units_color_dark : R.color.speedometer_tolerance_units_color_light);
			case EXCEED -> color = context.getColor(nightMode ? R.color.speedometer_limit_units_color_dark : R.color.speedometer_limit_units_color_light);
		}
		return color;
	}

}