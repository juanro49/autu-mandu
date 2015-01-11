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

import me.kuehle.carreport.BuildConfig;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.gui.util.DrawerListAdapter;
import me.kuehle.carreport.gui.util.DrawerListItem;
import me.kuehle.carreport.util.DemoData;
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
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
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
			selectDrawerItem(position);
		}
	}

	private static final int REQUEST_FIRST_START = 0;
	private static final int REQUEST_ADD_DATA = 1;
	private static final int REQUEST_FROM_DRAWER = 2;

	private static final String STATE_TITLE = "title";

	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private ListView mDrawerList;
    private DrawerListAdapter mDrawerAdapter;
	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	private Fragment mCurrentFragment;
    private int mCurrentDrawerPosition;
	private MenuItem mSyncMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar((Toolbar) findViewById(R.id.action_bar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mTitle = mDrawerTitle = getTitle();
        if (savedInstanceState != null) {
            setTitle(savedInstanceState.getCharSequence(STATE_TITLE, mTitle));
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawerAdapter = new DrawerListAdapter(this, buildDrawerItems());

        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open,
                R.string.drawer_close) {
            public void onDrawerOpened(View drawerView) {
                mTitle = getSupportActionBar().getTitle();
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu();
            }

            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            selectDrawerItem(1);
        }

        // When there is no car, show the first start activity.
        if (Car.getCount() == 0) {
            Intent intent = new Intent(this, FirstStartActivity.class);
            startActivityForResult(intent, REQUEST_FIRST_START);
        }
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
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_FIRST_START && resultCode == RESULT_CANCELED) {
			finish();
		} else {
			// Rebuild the menu, so a change in the show_car_menu option will take effect.
			if (requestCode == REQUEST_FROM_DRAWER) {
				invalidateOptionsMenu();
			}

            dataChanged();
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
			} else {
                MenuItemCompat.setActionView(mSyncMenuItem, null);
            }
		}

        if (BuildConfig.DEBUG) {
            DemoData.createMenuItem(menu);
        }

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
        Preferences prefs = new Preferences(this);
        List<Car> cars = Car.getAllActive();
        MenuItem[] items = {
                menu.findItem(R.id.menu_add_other_expenditure),
                menu.findItem(R.id.menu_add_other_income)
        };
        int[] otherTypes = {
                DataDetailOtherFragment.EXTRA_OTHER_TYPE_EXPENDITURE,
                DataDetailOtherFragment.EXTRA_OTHER_TYPE_INCOME
        };

        for (int i = 0; i < items.length; i++) {
            if (items[i] == null) {
                continue;
            }

            SubMenu subMenu = items[i].getSubMenu();
            subMenu.clear();
            if (cars.size() == 1 || !prefs.isShowCarMenu()) {
                items[i].setIntent(getDetailActivityIntent(DataDetailActivity.EXTRA_EDIT_OTHER,
                        prefs.getDefaultCar(), otherTypes[i]));
            } else {
                items[i].setIntent(null);
                for (Car car : cars) {
                    subMenu.add(car.name).setIntent(getDetailActivityIntent(
                            DataDetailActivity.EXTRA_EDIT_OTHER, car.id, otherTypes[i]));
                }
            }
        }

		return super.onPrepareOptionsMenu(menu);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.menu_synchronize:
                AbstractSynchronizationProvider.getCurrent(this).synchronize();
                return true;
            default:
                if (item.getIntent() != null) {
                    startActivityForResult(item.getIntent(), REQUEST_ADD_DATA);
                    return true;
                } else if (BuildConfig.DEBUG && DemoData.onOptionsItemSelected(item)) {
                    return true;
                } else {
                    return super.onOptionsItemSelected(item);
                }
        }
    }

    public void onFABClicked(View fab) {
        Preferences prefs = new Preferences(this);
        List<Car> cars = Car.getAllActive();

        if (cars.size() == 1 || !prefs.isShowCarMenu()) {
            Intent intent = getDetailActivityIntent(DataDetailActivity.EXTRA_EDIT_REFUELING,
                    prefs.getDefaultCar());
            startActivityForResult(intent, REQUEST_ADD_DATA);
        } else {
            PopupMenu popup = new PopupMenu(this, fab);
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    startActivityForResult(menuItem.getIntent(), REQUEST_ADD_DATA);
                    return true;
                }
            });

            Menu menu = popup.getMenu();
            for (Car car : cars) {
                menu.add(car.name).setIntent(getDetailActivityIntent(
                        DataDetailActivity.EXTRA_EDIT_REFUELING, car.id));
            }

            popup.show();
        }
    }

    @Override
    public void onSynchronizationStarted() {
        if (mSyncMenuItem != null) {
            MenuItemCompat.setActionView(mSyncMenuItem, R.layout.actionbar_indeterminate_progress);
        }
    }

	@Override
	public void onSynchronizationFinished(boolean result) {
		if (mSyncMenuItem != null) {
            MenuItemCompat.setActionView(mSyncMenuItem, null);
		}

		if (result) {
            dataChanged();
		} else {
			Toast.makeText(MainActivity.this, R.string.toast_synchronization_failed,
                    Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
        getSupportActionBar().setTitle(mTitle);
	}

	@Override
	protected void onPause() {
		super.onPause();
		AbstractSynchronizationProvider.setSynchronisationCallback(null);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putCharSequence(STATE_TITLE, mTitle);
		super.onSaveInstanceState(outState);
	}

    private DrawerListItem[] buildDrawerItems() {
        List<Car> cars = Car.getAll();

        DrawerListItem[] items = new DrawerListItem[6 + cars.size()];
        int i = 0;

        // Profile section on top of drawer.
        AbstractSynchronizationProvider syncProvider = AbstractSynchronizationProvider
                .getCurrent(this);
        if (syncProvider == null) {
            items[i++] = new DrawerListItem();
        } else {
            items[i++] = new DrawerListItem(syncProvider.getAccountName(), syncProvider.getIcon());
        }

        items[i++] = new DrawerListItem(getString(R.string.drawer_reports), R.drawable.ic_reports,
                new ReportFragment());

        for (Car car : cars) {
            Bundle args = new Bundle();
            args.putLong(DataFragment.EXTRA_CAR_ID, car.id);

            DataFragment fragment = new DataFragment();
            fragment.setArguments(args);

            items[i++] = new DrawerListItem(car.name, R.drawable.ic_list, fragment);
        }

        items[i++] = new DrawerListItem(getString(R.string.drawer_calculator),
                R.drawable.ic_functions, new CalculatorFragment());
        items[i++] = new DrawerListItem();
        items[i++] = new DrawerListItem(getString(R.string.drawer_settings),
                new Intent(this, PreferencesActivity.class));
        items[i] = new DrawerListItem(getString(R.string.drawer_help),
                new Intent(this, HelpActivity.class));

        return items;
    }

    private void dataChanged() {
        if (mCurrentFragment instanceof DataChangeListener) {
            ((DataChangeListener) mCurrentFragment).onDataChanged();
        }

        // Cars could have been changed, so the drawer has to be updated.
        mDrawerAdapter.setItems(buildDrawerItems());
    }

    private Intent getDetailActivityIntent(int edit, long carId) {
        return getDetailActivityIntent(edit, carId, -1);
    }

    private Intent getDetailActivityIntent(int edit, long carId, int otherType) {
        Intent intent = new Intent(this, DataDetailActivity.class);
        intent.putExtra(DataDetailActivity.EXTRA_EDIT, edit);
        intent.putExtra(AbstractDataDetailFragment.EXTRA_CAR_ID, carId);
        if (otherType >= 0) {
            intent.putExtra(DataDetailOtherFragment.EXTRA_OTHER_TYPE, otherType);
        }

        return intent;
    }

    private void selectDrawerItem(int position) {
        DrawerListItem selected = (DrawerListItem) mDrawerAdapter.getItem(position);
        if (selected.getFragment() != null) {
            mCurrentFragment = selected.getFragment();
            mCurrentDrawerPosition = position;

            FragmentManager fm = getSupportFragmentManager();
            fm.popBackStack();
            fm.beginTransaction().replace(R.id.content_frame, mCurrentFragment).commit();
        } else if (selected.getIntent() != null) {
            startActivityForResult(selected.getIntent(), REQUEST_FROM_DRAWER);
        }

        mDrawerList.setItemChecked(mCurrentDrawerPosition, true);
        DrawerListItem current = (DrawerListItem) mDrawerAdapter.getItem(mCurrentDrawerPosition);
        setTitle(current.getText());
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    public static ActionBar getSupportActionBar(Fragment fragment) {
        Activity activity = fragment.getActivity();
        if (activity instanceof ActionBarActivity) {
            return ((ActionBarActivity) activity).getSupportActionBar();
        }

        return null;
    }
}
