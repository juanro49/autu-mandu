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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SyncInfo;
import android.content.SyncStatusObserver;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;

import java.util.Date;

import me.kuehle.carreport.BuildConfig;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.query.CarQueries;
import me.kuehle.carreport.gui.util.NewRefuelingSnackbar;
import me.kuehle.carreport.provider.DataProvider;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.util.DemoData;
import me.kuehle.carreport.util.sync.AbstractSyncProvider;
import me.kuehle.carreport.util.sync.Authenticator;
import me.kuehle.carreport.util.sync.SyncProviders;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {
    public interface BackPressedListener {
        boolean onBackPressed();
    }

    private static final int REQUEST_FIRST_START = 10;
    private static final int REQUEST_FROM_DRAWER = 20;
    private static final int REQUEST_ADD_DATA = 30;

    private static final String STATE_TITLE = "title";
    private static final String STATE_NAV_ITEM_INDEX = "nav_item_index";

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;
    private View mNavigationViewTop;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private Fragment mCurrentFragment;
    private int mCurrentNavItemIndex;

    private SyncStatusObserver mSyncStatusObserver;
    private Object mSyncHandle;
    private MenuItem mSyncMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitle = mDrawerTitle = getTitle();
        if (savedInstanceState != null) {
            setTitle(savedInstanceState.getCharSequence(STATE_TITLE, mTitle));
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerLayout.setStatusBarBackground(R.color.primary_dark);

        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        mNavigationViewTop = mNavigationView.inflateHeaderView(R.layout.navigation_view_main_top);
        mNavigationView.setNavigationItemSelectedListener(this);
        updateNavigationViewMenu();
        setSupportActionBar((Toolbar) null);

        mSyncStatusObserver = new SyncStatusObserver() {
            @Override
            public void onStatusChanged(int which) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateSyncMenuItem();
                    }
                });
            }
        };

        final int navItemIndex;
        if (savedInstanceState != null) {
            navItemIndex = savedInstanceState.getInt(STATE_NAV_ITEM_INDEX, 0);
        } else {
            navItemIndex = 0;
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                onNavigationItemSelected(mNavigationView.getMenu().getItem(navItemIndex));
            }
        });

        // When there is no car, show the first start activity.
        if (CarQueries.getCount(this) == 0) {
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

        // Refresh synchronization status
        mSyncStatusObserver.onStatusChanged(0);

        // Watch for synchronization status changes
        mSyncHandle = ContentResolver.addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                        ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE,
                mSyncStatusObserver);
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

            // Cars could have been changed, so the drawer has to be updated.
            updateNavigationViewMenu();

            // If a new refueling has been added, show Snackbar with details.
            if (requestCode % REQUEST_ADD_DATA == DataDetailActivity.EXTRA_EDIT_REFUELING) {
                long newId = data.getLongExtra(DataDetailActivity.EXTRA_NEW_ID, 0);
                if (newId > 0 && mCurrentFragment.getView() != null) {
                    NewRefuelingSnackbar.show(mCurrentFragment.getView(), newId);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (closeFABMenu()) {
            return;
        }

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

        Account account = getCurrentSyncAccount();
        if (account == null) {
            mSyncMenuItem.setVisible(false);
        } else {
            mSyncMenuItem.setVisible(true);
        }

        if (BuildConfig.DEBUG) {
            DemoData.createMenuItem(menu);
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

        switch (item.getItemId()) {
            case R.id.menu_synchronize:
                Account account = getCurrentSyncAccount();
                Bundle settingsBundle = new Bundle();
                settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                ContentResolver.requestSync(account, DataProvider.AUTHORITY, settingsBundle);
                return true;
            default:
                if (item.getIntent() != null) {
                    startActivityForResult(item.getIntent(), REQUEST_ADD_DATA);
                    return true;
                } else {
                    return (BuildConfig.DEBUG && DemoData.onOptionsItemSelected(item))
                            || super.onOptionsItemSelected(item);
                }
        }
    }

    public void onFABAddRefuelingClicked(View fab) {
        handleFABClick(DataDetailActivity.EXTRA_EDIT_REFUELING, -1);
    }

    public void onFABAddOtherExpenditureClicked(View fab) {
        handleFABClick(DataDetailActivity.EXTRA_EDIT_OTHER,
                DataDetailOtherFragment.EXTRA_OTHER_TYPE_EXPENDITURE);
    }

    public void onFABAddOtherIncomeClicked(View fab) {
        handleFABClick(DataDetailActivity.EXTRA_EDIT_OTHER,
                DataDetailOtherFragment.EXTRA_OTHER_TYPE_INCOME);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(mTitle);
        }
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        // All child fragments need to have a toolbar in their layout because of this bug:
        // https://code.google.com/p/android/issues/detail?id=78496
        // https://code.google.com/p/android/issues/detail?id=185736
        if (toolbar != null) {
            super.setSupportActionBar(toolbar);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(mTitle);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerOpened(View drawerView) {
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    mTitle = actionBar.getTitle();
                    actionBar.setTitle(mDrawerTitle);
                }

                invalidateOptionsMenu();
            }

            public void onDrawerClosed(View view) {
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(mTitle);
                }

                invalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mSyncHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncHandle);
            mSyncHandle = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(STATE_TITLE, mTitle);
        outState.putInt(STATE_NAV_ITEM_INDEX, mCurrentNavItemIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        Intent intent = menuItem.getIntent();
        String fragment = intent.getStringExtra("fragment");
        Bundle arguments = intent.getBundleExtra("arguments");
        if (fragment != null) {
            try {
                mCurrentFragment = (Fragment) Class.forName(fragment).newInstance();
                mCurrentFragment.setArguments(arguments);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Menu menu = mNavigationView.getMenu();
            for (int i = 0; i < menu.size(); i++) {
                if (menu.getItem(i) == menuItem) {
                    mCurrentNavItemIndex = i;
                    break;
                }
            }

            FragmentManager fm = getSupportFragmentManager();
            fm.popBackStack();
            fm.beginTransaction().replace(R.id.content_frame, mCurrentFragment).commit();

            menuItem.setChecked(true);
            setTitle(menuItem.getTitle());
        } else if (menuItem.getIntent() != null) {
            startActivityForResult(menuItem.getIntent(), REQUEST_FROM_DRAWER);
        }

        mDrawerLayout.closeDrawer(mNavigationView);

        return true;
    }

    private void updateNavigationViewMenu() {
        // Profile section on top of drawer
        ImageView topImage = (ImageView) mNavigationViewTop.findViewById(android.R.id.icon1);
        TextView topText = (TextView) mNavigationViewTop.findViewById(android.R.id.text1);
        Account account = getCurrentSyncAccount();
        if (account == null) {
            topImage.setVisibility(View.GONE);
            topText.setVisibility(View.GONE);
        } else {
            AbstractSyncProvider syncProvider = SyncProviders.getSyncProviderByAccount(this, account);
            topImage.setVisibility(View.VISIBLE);
            topImage.setImageResource(syncProvider.getIcon());
            topText.setVisibility(View.VISIBLE);
            topText.setText(account.name);
        }

        // Data menu items
        CarCursor car = new CarSelection().query(this.getContentResolver(),
                CarColumns.ALL_COLUMNS, CarColumns.NAME + " COLLATE UNICODE");

        Menu menu = mNavigationView.getMenu();
        menu.clear();

        menu.add(1, Menu.NONE, Menu.NONE, R.string.drawer_reports)
                .setIcon(R.drawable.ic_c_report_24dp)
                .setIntent(new Intent().putExtra("fragment", ReportFragment.class.getName()));
        while (car.moveToNext()) {
            Bundle args = new Bundle();
            args.putLong(DataFragment.EXTRA_CAR_ID, car.getId());

            menu.add(1, Menu.NONE, Menu.NONE, car.getName())
                    .setIcon(R.drawable.ic_list_24dp)
                    .setIntent(new Intent()
                            .putExtra("fragment", DataFragment.class.getName())
                            .putExtra("arguments", args));
        }

        menu.add(1, Menu.NONE, Menu.NONE, R.string.drawer_calculator)
                .setIcon(R.drawable.ic_functions_24dp)
                .setIntent(new Intent().putExtra("fragment", CalculatorFragment.class.getName()));
        menu.add(R.string.drawer_settings).setIntent(new Intent(this, PreferencesActivity.class));
        menu.add(R.string.drawer_help).setIntent(new Intent(this, HelpActivity.class));

        menu.setGroupCheckable(1, true, true);
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

    private void updateSyncMenuItem() {
        Account account = getCurrentSyncAccount();

        boolean isSyncInProgress = false;
        for (SyncInfo syncInfo : ContentResolver.getCurrentSyncs()) {
            if (syncInfo.account.equals(account) &&
                    syncInfo.authority.equals(DataProvider.AUTHORITY)) {
                isSyncInProgress = true;
            }
        }

        if (isSyncInProgress) {
            if (mSyncMenuItem != null) {
                MenuItemCompat.setActionView(mSyncMenuItem,
                        R.layout.actionbar_indeterminate_progress);
            }
        } else {
            if (mSyncMenuItem != null) {
                MenuItemCompat.setActionView(mSyncMenuItem, null);
            }

            // Cars could have changed, so we need to update navigation drawer.
            updateNavigationViewMenu();
        }
    }

    private Account getCurrentSyncAccount() {
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
        if (accounts.length > 0) {
            return accounts[0];
        } else {
            return null;
        }
    }

    private boolean closeFABMenu() {
        FloatingActionMenu floatingActionMenu = (FloatingActionMenu) findViewById(R.id.fab);
        if (floatingActionMenu != null && floatingActionMenu.isOpened()) {
            floatingActionMenu.close(true);
            return true;
        }

        return false;
    }

    private void handleFABClick(final int edit, final int otherType) {
        closeFABMenu();

        Preferences prefs = new Preferences(this);
        CarCursor car = new CarSelection().suspendedSince((Date) null).query(getContentResolver(),
                CarColumns.ALL_COLUMNS, CarColumns.NAME + " COLLATE UNICODE");

        if (car.getCount() == 1 || !prefs.isShowCarMenu()) {
            Intent intent = getDetailActivityIntent(edit, prefs.getDefaultCar(), otherType);
            startActivityForResult(intent, REQUEST_ADD_DATA + edit);
        } else {
            final long[] carIds = new long[car.getCount()];
            final String[] carNames = new String[car.getCount()];
            while (car.moveToNext()) {
                carIds[car.getPosition()] = car.getId();
                carNames[car.getPosition()] = car.getName();
            }

            new AlertDialog.Builder(this)
                    .setItems(carNames, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = getDetailActivityIntent(edit, carIds[which], otherType);
                            startActivityForResult(intent, REQUEST_ADD_DATA + edit);
                        }
                    })
                    .create()
                    .show();
        }
    }

    public static void setSupportActionBar(Fragment fragment) {
        Activity activity = fragment.getActivity();
        if (!(activity instanceof AppCompatActivity) || fragment.getView() == null) {
            return;
        }

        Toolbar toolbar = (Toolbar) fragment.getView().findViewById(R.id.toolbar);
        ((AppCompatActivity) activity).setSupportActionBar(toolbar);
    }

    public static ActionBar getSupportActionBar(Fragment fragment) {
        Activity activity = fragment.getActivity();
        if (activity instanceof AppCompatActivity) {
            return ((AppCompatActivity) activity).getSupportActionBar();
        }

        return null;
    }
}
