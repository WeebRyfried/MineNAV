package net.osmand.plus.feedback;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.aidlapi.OsmAndCustomizationConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;

public class CrashBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	private static final String TAG = OsmAndCustomizationConstants.FRAGMENT_CRASH_ID;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		View titleView = inflate(R.layout.crash_title);
		items.add(new SimpleBottomSheetItem.Builder().setCustomView(titleView).create());
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_send;
	}

	@Override
	protected void onRightBottomButtonClick() {
		OsmandApplication app = this.app;
		app.getFeedbackHelper().sendCrashReport(sent -> {
			app.showToastMessage(sent ? R.string.crash_report_sent : R.string.crash_report_send_failed);
			return true;
		});
		dismiss();
	}

	public static boolean shouldShow(@Nullable OsmandSettings settings, @NonNull MapActivity activity) {
		// MineNAV is a personal build: crash reports must not be sent to the OsmAnd team
		return false;
	}

	public static void showInstance(@NonNull OsmandApplication app, @NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			app.getFeedbackHelper().markSystemCrashesShown();
			CrashBottomSheetDialogFragment fragment = new CrashBottomSheetDialogFragment();
			fragment.show(fragmentManager, TAG);
		}
	}
}
