package me.kuehle.carreport;

import me.kuehle.carreport.db.Car;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

public class ViewDataActivity extends Activity implements
		AbstractEditFragment.OnItemActionListener {
	public static final int ADD_REFUELING_REQUEST_CODE = 0;
	public static final int ADD_OTHER_REQUEST_CODE = 1;
	public static final int EDIT_REQUEST_CODE = 2;

	private ViewDataListFragment list;

	private Car[] cars;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_data);

		list = (ViewDataListFragment) getFragmentManager().findFragmentById(
				R.id.list);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_dropdown_item);
		cars = Car.getAll();
		for (Car car : cars) {
			adapter.add(car.getName());
		}

		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(adapter, listNavigationCallback);

		Preferences prefs = new Preferences(this);
		int defaultCar = prefs.getDefaultCar();
		for (int pos = 0; pos < cars.length; pos++) {
			if (cars[pos].getId() == defaultCar) {
				actionBar.setSelectedNavigationItem(pos);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_data, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_refueling:
			Intent intent = new Intent(this, EditFragmentActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			intent.putExtra(EditFragmentActivity.EXTRA_FINISH_ON_BIG_SCREEN,
					false);
			intent.putExtra(EditFragmentActivity.EXTRA_EDIT,
					EditFragmentActivity.EXTRA_EDIT_REFUELING);
			startActivityForResult(intent, ADD_REFUELING_REQUEST_CODE);
			return true;
		case R.id.menu_add_other:
			Intent intent1 = new Intent(this, EditFragmentActivity.class);
			intent1.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			intent1.putExtra(EditFragmentActivity.EXTRA_FINISH_ON_BIG_SCREEN,
					false);
			intent1.putExtra(EditFragmentActivity.EXTRA_EDIT,
					EditFragmentActivity.EXTRA_EDIT_OTHER);
			startActivityForResult(intent1, ADD_OTHER_REQUEST_CODE);
			return true;
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ((requestCode == ADD_REFUELING_REQUEST_CODE && resultCode == Activity.RESULT_OK)
				|| (requestCode == ADD_OTHER_REQUEST_CODE && resultCode == Activity.RESULT_OK)
				|| (requestCode == EDIT_REQUEST_CODE && resultCode == Activity.RESULT_OK)) {
			list.updateLists();
		}
	}

	@Override
	public void itemSaved() {
		list.updateLists();
	}

	@Override
	public void itemCanceled() {
		list.updateLists();
	}

	@Override
	public void itemDeleted() {
		list.updateLists();
	}

	private OnNavigationListener listNavigationCallback = new OnNavigationListener() {
		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			list.setCar(cars[itemPosition]);
			return true;
		}
	};
}
