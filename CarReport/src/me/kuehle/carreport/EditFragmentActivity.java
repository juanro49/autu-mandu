package me.kuehle.carreport;

import android.app.Activity;
import android.os.Bundle;

public class EditFragmentActivity extends Activity implements
		AbstractEditFragment.OnItemActionListener {
	public static final String EXTRA_FINISH_ON_BIG_SCREEN = "finishOnBigScreen";
	public static final String EXTRA_EDIT = "edit";

	public static final int EXTRA_EDIT_REFUELING = 0;
	public static final int EXTRA_EDIT_OTHER = 1;

	public static final int MAX_SCREEN_WIDTH = 800;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_fragment_holder);

		boolean finishOnBigScreen = getIntent().getBooleanExtra(
				EXTRA_FINISH_ON_BIG_SCREEN, false);
		int edit = getIntent().getIntExtra(EXTRA_EDIT, EXTRA_EDIT_REFUELING);

		if (isBigScreen() && finishOnBigScreen) {
			finish();
			return;
		}

		if (savedInstanceState == null) {
			// During initial setup, plug in the details fragment.
			AbstractEditFragment fragment;
			if (edit == EXTRA_EDIT_REFUELING) {
				fragment = new EditRefuelingFragment();
			} else {
				fragment = new EditOtherCostFragment();
			}
			fragment.setArguments(getIntent().getExtras());

			getFragmentManager().beginTransaction()
					.add(R.id.fragment_holder, fragment).commit();
		}
	}

	@Override
	public void itemSaved() {
		setResult(RESULT_OK);
		finish();
	}

	@Override
	public void itemCanceled() {
		setResult(RESULT_CANCELED);
		finish();
	}

	@Override
	public void itemDeleted() {
		setResult(RESULT_OK);
		finish();
	}

	private boolean isBigScreen() {
		return getResources().getConfiguration().screenWidthDp > MAX_SCREEN_WIDTH;
	}
}
