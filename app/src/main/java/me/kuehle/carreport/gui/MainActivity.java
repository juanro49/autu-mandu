/*
 * Copyright 2013 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.kuehle.carreport.gui;

import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.util.backup.AbstractSynchronizationProvider;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements
		AbstractSynchronizationProvider.OnSynchronizeListener {
	public static interface BackPressedListener {
		public boolean onBackPressed();
	}

	public static interface DataChangeListener {
		public void onDataChanged();
	}

	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position);
		}
	}

	private static final int REQUEST_FIRST_START = 0;
	private static final int REQUEST_ADD_DATA = 1;
	private static final int REQUEST_SETTINGS = 2;

	private static final String STATE_TITLE = "title";

	private String[] mMainViews;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private ListView mDrawerList;
	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	private Fragment mCurrentFragment;
	private MenuItem mSyncMenuItem;

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_FIRST_START && resultCode == RESULT_CANCELED) {
			finish();
		} else {
			// Rebuild the menu, so a change in the show_car_menu option will take effect.
			if (requestCode == REQUEST_SETTINGS) {
				invalidateOptionsMenu();
			}

			if (mCurrentFragment instanceof DataChangeListener) {
				((DataChangeListener) mCurrentFragment).onDataChanged();
			}
		}
	}

	@Override
	public void onBackPressed() {
		if (mCurrentFragment != null && mCurrentFragment instanceof BackPressedListener) {
			if (((BackPressedListener) mCurrentFragment).onBackPressed()) {
				return;
			}
		}

		// Currently the back stack of child fragment does not pop, when
		// pressing the back button. This works around the issue.
		// Bug report: http://code.google.com/p/android/issues/detail?id=40323
		if (mCurrentFragment != null
				&& mCurrentFragment.getChildFragmentManager().getBackStackEntryCount() > 0) {
			mCurrentFragment.getChildFragmentManager().popBackStack();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTitle = mDrawerTitle = getTitle();
		if (savedInstanceState != null) {
			setTitle(savedInstanceState.getCharSequence(STATE_TITLE, mTitle));
		}

		mMainViews = getResources().getStringArray(R.array.main_views);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		mDrawerList.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_drawer, mMainViews));
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open,
				R.string.drawer_close) {
			public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
				invalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView) {
				mTitle = getSupportActionBar().getTitle();
                getSupportActionBar().setTitle(mDrawerTitle);
				invalidateOptionsMenu();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState == null) {
			selectItem(0);
		}

		// When there is no car, show the first start activity.
		if (Car.getCount() == 0) {
			Intent intent = new Intent(this, FirstStartActivity.class);
			startActivityForResult(intent, REQUEST_FIRST_START);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		mSyncMenuItem = menu.findItem(R.id.menu_synchronize);

		AbstractSynchronizationProvider provider = AbstractSynchronizationProvider.getCurrent(this);
		if (provider == null) {
			mSyncMenuItem.setVisible(false);
		} else {
			mSyncMenuItem.setVisible(provider.isAuthenticated());
			if (AbstractSynchronizationProvider.isSynchronisationInProgress()) {
                MenuItemCompat.setActionView(mSyncMenuItem,
                        R.layout.actionbar_indeterminate_progress);
			}
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Pass the event to ActionBarDrawerToggle, if it returns
		// true, then it has handled the app icon touch event.
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		// DemoData.addDemoData();
		// if (mCurrentFragment instanceof DataChangeListener) {
		// ((DataChangeListener) mCurrentFragment).onDataChanged();
		// }

		switch (item.getItemId()) {
		case R.id.menu_synchronize:
			AbstractSynchronizationProvider.getCurrent(this).synchronize();
			return true;
		case R.id.menu_settings:
			Intent intentPrefs = new Intent(this, PreferencesActivity.class);
			startActivityForResult(intentPrefs, REQUEST_SETTINGS);
			return true;
		case R.id.menu_help:
			Intent intentHelp = new Intent(this, HelpActivity.class);
			startActivity(intentHelp);
			return true;
		default:
			if (item.getIntent() != null) {
				startActivityForResult(item.getIntent(), REQUEST_ADD_DATA);
				return true;
			} else {
				return super.onOptionsItemSelected(item);
			}
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO: If the navigation drawer is open, hide action items related to the content view.
		// boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		// menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);

		// Show available cars in submenu of "Add refueling" and "Add other cost" option, if the
		// setting is enabled.
		Preferences prefs = new Preferences(this);
		List<Car> cars = Car.getAll();
		for (int i = cars.size() - 1; i >= 0; i--) {
			if (cars.get(i).isSuspended()) {
				cars.remove(i);
			}
		}

		MenuItem[] items = { menu.findItem(R.id.menu_add_refueling),
				menu.findItem(R.id.menu_add_other) };
		int extraEdit[] = { DataDetailActivity.EXTRA_EDIT_REFUELING,
				DataDetailActivity.EXTRA_EDIT_OTHER };

		for (int i = 0; i < items.length; i++) {
			if (items[i] == null) {
				continue;
			}

			SubMenu subMenu = items[i].getSubMenu();
			subMenu.clear();

			if (cars.size() == 1 || !prefs.isShowCarMenu()) {
				items[i].setIntent(getDetailActivityIntent(extraEdit[i], prefs.getDefaultCar()));
			} else {
				items[i].setIntent(null);
				for (Car car : cars) {
					subMenu.add(car.name).setIntent(getDetailActivityIntent(extraEdit[i], car.id));
				}
			}
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onSynchronizationFinished(boolean result) {
		if (mSyncMenuItem != null) {
            MenuItemCompat.setActionView(mSyncMenuItem, null);
		}

		if (result) {
			if (mCurrentFragment instanceof DataChangeListener) {
				((DataChangeListener) mCurrentFragment).onDataChanged();
			}
		} else {
			Toast.makeText(MainActivity.this,
                    R.string.toast_synchronization_failed, Toast.LENGTH_SHORT)
					.show();
		}
	}

	@Override
	public void onSynchronizationStarted() {
		if (mSyncMenuItem != null) {
            MenuItemCompat.setActionView(mSyncMenuItem, R.layout.actionbar_indeterminate_progress);
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
        getSupportActionBar().setTitle(mTitle);
	}

	private Intent getDetailActivityIntent(int edit, long carId) {
		Intent intent = new Intent(this, DataDetailActivity.class);
		intent.putExtra(DataDetailActivity.EXTRA_EDIT, edit);
		intent.putExtra(AbstractDataDetailFragment.EXTRA_CAR_ID, carId);
		return intent;
	}

	private void selectItem(int position) {
		if (position == 0) {
			mCurrentFragment = new ReportFragment();
		} else if (position == 1) {
			mCurrentFragment = new DataFragment();
		} else if (position == 2) {
			mCurrentFragment = new CalculatorFragment();
		} else {
			return;
		}

		FragmentManager fm = getSupportFragmentManager();
		fm.popBackStack();
		fm.beginTransaction().replace(R.id.content_frame, mCurrentFragment).commit();

		mDrawerList.setItemChecked(position, true);
		setTitle(mMainViews[position]);
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	@Override
	protected void onPause() {
		super.onPause();
		AbstractSynchronizationProvider.setSynchronisationCallback(null);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	protected void onResume() {
		super.onResume();

		AbstractSynchronizationProvider provider = AbstractSynchronizationProvider.getCurrent(this);
		if (mSyncMenuItem != null) {
			mSyncMenuItem.setVisible(provider != null && provider.isAuthenticated());
		}

		AbstractSynchronizationProvider.setSynchronisationCallback(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putCharSequence(STATE_TITLE, mTitle);
		super.onSaveInstanceState(outState);
	}

    public static ActionBar getSupportActionBar(Fragment fragment) {
        Activity activity = fragment.getActivity();
        if (activity instanceof ActionBarActivity) {
            return ((ActionBarActivity) activity).getSupportActionBar();
        }

        return null;
    }
}
