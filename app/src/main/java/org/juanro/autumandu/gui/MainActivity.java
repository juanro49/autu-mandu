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

package org.juanro.autumandu.gui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.github.clans.fab.FloatingActionMenu;
import com.google.android.material.navigation.NavigationView;

import org.juanro.autumandu.BuildConfig;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.fragment.AbstractDataDetailFragment;
import org.juanro.autumandu.gui.fragment.CalculatorFragment;
import org.juanro.autumandu.gui.fragment.DataDetailOtherFragment;
import org.juanro.autumandu.gui.fragment.DataFragment;
import org.juanro.autumandu.gui.fragment.ReportFragment;
import org.juanro.autumandu.gui.pref.PreferencesActivity;
import org.juanro.autumandu.util.backup.AutoBackupWorker;
import org.juanro.autumandu.gui.util.NewRefuelingSnackbar;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.util.DemoData;
import org.juanro.autumandu.util.reminder.ReminderWorker;
import org.juanro.autumandu.util.sync.AbstractSyncProvider;
import org.juanro.autumandu.util.sync.Authenticator;
import org.juanro.autumandu.util.sync.SyncManager;
import org.juanro.autumandu.util.sync.SyncProviders;
import org.juanro.autumandu.viewmodel.MainViewModel;

import java.util.List;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {
    public interface BackPressedListener {
        boolean onBackPressed();
    }

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

    private MainViewModel mViewModel;

    private MenuItem mSyncMenuItem;
    private List<WorkInfo> mOnceWorkInfos;
    private List<WorkInfo> mPeriodicWorkInfos;

    private final ActivityResultLauncher<Intent> mFirstStartLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_CANCELED) {
                    finish();
                } else {
                    mViewModel.getCars().removeObservers(this);
                    mViewModel.getCars().observe(this, this::updateNavigationViewMenu);
                }
            }
    );

    private final ActivityResultLauncher<Intent> mAddDataLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                mViewModel.getCars().removeObservers(this);
                mViewModel.getCars().observe(this, this::updateNavigationViewMenu);
                if (result.getResultCode() == RESULT_OK && result.getData() != null && mCurrentFragment != null) {
                    long newId = result.getData().getLongExtra(DataDetailActivity.EXTRA_NEW_ID, 0);
                    View view = mCurrentFragment.getView();
                    if (newId > 0 && view != null) {
                        NewRefuelingSnackbar.show(view, newId);
                    }
                }
            }
    );

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        mTitle = mDrawerTitle = getTitle();
        if (savedInstanceState != null) {
            setTitle(savedInstanceState.getCharSequence(STATE_TITLE, mTitle));
        }

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerLayout.setStatusBarBackground(R.color.primary_dark);

        mNavigationView = findViewById(R.id.navigation_view);
        mNavigationViewTop = mNavigationView.inflateHeaderView(R.layout.navigation_view_main_top);
        mNavigationView.setNavigationItemSelectedListener(this);
        mViewModel.getCars().observe(this, this::updateNavigationViewMenu);

        WorkManager wm = WorkManager.getInstance(this);
        wm.getWorkInfosForUniqueWorkLiveData(SyncManager.SYNC_WORK_NAME_ONCE)
                .observe(this, workInfos -> {
                    mOnceWorkInfos = workInfos;
                    updateSyncMenuItem();
                });
        wm.getWorkInfosForUniqueWorkLiveData(SyncManager.SYNC_WORK_NAME_PERIODIC)
                .observe(this, workInfos -> {
                    mPeriodicWorkInfos = workInfos;
                    updateSyncMenuItem();
                });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (closeFABMenu()) {
                    return;
                }

                if (mCurrentFragment instanceof BackPressedListener) {
                    if (((BackPressedListener) mCurrentFragment).onBackPressed()) {
                        return;
                    }
                }

                if (mCurrentFragment != null
                        && mCurrentFragment.getChildFragmentManager().getBackStackEntryCount() > 0) {
                    mCurrentFragment.getChildFragmentManager().popBackStack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        // When there is no car, show the first start activity.
        mViewModel.getCarCount().observe(this, count -> {
            if (count != null && count == 0) {
                Intent intent = new Intent(this, FirstStartActivity.class);
                mFirstStartLauncher.launch(intent);
            }
        });

        // Update reminders and schedule periodic update
        ReminderWorker.enqueueUpdate(this);
        ReminderWorker.schedulePeriodicUpdate(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh synchronization status
        updateSyncMenuItem();

        AutoBackupWorker.enqueue(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FROM_DRAWER)
        {
            invalidateOptionsMenu();
        }

        // Cars could have been changed, so the drawer has to be updated.
        mViewModel.getCars().removeObservers(this);
        mViewModel.getCars().observe(this, this::updateNavigationViewMenu);

        // If a new refueling has been added, show Snackbar with details.
        if (requestCode % REQUEST_ADD_DATA == DataDetailActivity.EXTRA_EDIT_REFUELING
            && resultCode == RESULT_OK
            && data != null
            && mCurrentFragment != null)
        {
            long newId = data.getLongExtra(DataDetailActivity.EXTRA_NEW_ID, 0);
            View view = mCurrentFragment.getView();
            if (newId > 0 && view != null)
            {
                NewRefuelingSnackbar.show(view, newId);
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        mSyncMenuItem = menu.findItem(R.id.menu_synchronize);

        Account account = getCurrentSyncAccount();
        if (mSyncMenuItem != null) {
            mSyncMenuItem.setVisible(account != null);
        }

        if (BuildConfig.DEBUG) {
            DemoData.createMenuItem(menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event.
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int itemId = item.getItemId();
        if (itemId == R.id.menu_synchronize) {
            SyncManager.runSyncOnce(this);
            return true;
        } else {
            if (item.getIntent() != null) {
                mAddDataLauncher.launch(item.getIntent());
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

    public void onFABAddTiresClicked(View fab) {
        handleFABClick(DataDetailActivity.EXTRA_EDIT_TIRE, -1);
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
        super.setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(mTitle);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerOpened(View drawerView) {
                ActionBar ab = getSupportActionBar();
                if (ab != null) {
                    mTitle = ab.getTitle();
                    ab.setTitle(mDrawerTitle);
                }

                invalidateOptionsMenu();
            }

            public void onDrawerClosed(View view) {
                ActionBar ab = getSupportActionBar();
                if (ab != null) {
                    ab.setTitle(mTitle);
                }

                invalidateOptionsMenu();
            }
        };

        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putCharSequence(STATE_TITLE, mTitle);
        outState.putInt(STATE_NAV_ITEM_INDEX, mCurrentNavItemIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        mDrawerLayout.closeDrawer(mNavigationView);

        Intent intent = menuItem.getIntent();
        if (intent == null) return false;

        String fragment = intent.getStringExtra("fragment");
        Bundle arguments = intent.getBundleExtra("arguments");
        if (fragment != null) {
            try {
                mCurrentFragment = (Fragment) Class.forName(fragment).newInstance();
                mCurrentFragment.setArguments(arguments);
            } catch (Exception e) {
                Log.e("MainActivity", "Error instantiating fragment", e);
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

            setTitle(menuItem.getTitle());

            // Update ActionBar icon
            updateActionBarIcon();
            return true;
        } else {
            if (intent.getComponent() != null &&
                    (intent.getComponent().getClassName().equals(PreferencesActivity.class.getName()) ||
                     intent.getComponent().getClassName().equals(HelpActivity.class.getName()))) {
                startActivity(intent);
                return true;
            }
            return false;
        }
    }

    private void updateActionBarIcon() {
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            boolean isHome = (mCurrentFragment instanceof ReportFragment || mCurrentFragment instanceof DataFragment);
            if (mDrawerToggle != null) {
                mDrawerToggle.setDrawerIndicatorEnabled(isHome);
            }
            ab.setDisplayHomeAsUpEnabled(!isHome);
            ab.setHomeButtonEnabled(!isHome);
        }
    }

    private void updateNavigationViewMenu(List<Car> cars) {
        // Profile section on top of drawer
        ImageView topImage = mNavigationViewTop.findViewById(android.R.id.icon1);
        TextView topText = mNavigationViewTop.findViewById(android.R.id.text1);
        Account account = getCurrentSyncAccount();
        if (account == null) {
            topImage.setVisibility(View.GONE);
            topText.setVisibility(View.GONE);
        } else {
            AbstractSyncProvider syncProvider = SyncProviders.getSyncProviderByAccount(this, account);
            topImage.setVisibility(View.VISIBLE);
            if (syncProvider != null) {
                topImage.setImageResource(syncProvider.getIcon());
            }
            topText.setVisibility(View.VISIBLE);
            topText.setText(account.name);
        }

        // Data menu items
        Menu menu = mNavigationView.getMenu();
        menu.clear();

        int groupId = Menu.FIRST;
        int itemId = Menu.FIRST;

        menu.add(groupId, itemId++, Menu.NONE, R.string.drawer_reports)
                .setIcon(R.drawable.ic_c_report_24dp)
                .setIntent(new Intent().putExtra("fragment", ReportFragment.class.getName()));
        for (Car car : cars) {
            Bundle args = new Bundle();
            args.putLong(DataFragment.EXTRA_CAR_ID, car.getId());

            menu.add(groupId, itemId++, Menu.NONE, car.getName())
                    .setIcon(R.drawable.ic_list_24dp)
                    .setIntent(new Intent()
                            .putExtra("fragment", DataFragment.class.getName())
                            .putExtra("arguments", args));
        }

        menu.add(groupId, itemId, Menu.NONE, R.string.drawer_calculator)
                .setIcon(R.drawable.ic_functions_24dp)
                .setIntent(new Intent().putExtra("fragment", CalculatorFragment.class.getName()));

        menu.add(R.string.drawer_settings).setIntent(new Intent(this, PreferencesActivity.class));
        menu.add(R.string.drawer_help).setIntent(new Intent(this, HelpActivity.class));

        menu.setGroupCheckable(groupId, true, true);

        // Initialize the first page of the navigation drawer only if no fragment is currently shown.
        if (mCurrentFragment == null) {
            mNavigationView.getMenu().performIdentifierAction(Menu.FIRST, 0);
        }
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
        boolean isSyncInProgress = false;

        if (mOnceWorkInfos != null) {
            for (WorkInfo workInfo : mOnceWorkInfos) {
                if (workInfo.getState() == WorkInfo.State.RUNNING || workInfo.getState() == WorkInfo.State.ENQUEUED) {
                    isSyncInProgress = true;
                    break;
                }
            }
        }

        if (!isSyncInProgress && mPeriodicWorkInfos != null) {
            for (WorkInfo workInfo : mPeriodicWorkInfos) {
                if (workInfo.getState() == WorkInfo.State.RUNNING) {
                    isSyncInProgress = true;
                    break;
                }
            }
        }

        if (isSyncInProgress) {
            if (mSyncMenuItem != null) {
                mSyncMenuItem.setActionView(R.layout.actionbar_indeterminate_progress);
            }
        } else {
            if (mSyncMenuItem != null) {
                mSyncMenuItem.setActionView(null);
            }

            // Cars could have changed, so we need to update navigation drawer.
            mViewModel.getCars().removeObservers(this);
            mViewModel.getCars().observe(this, this::updateNavigationViewMenu);
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
        FloatingActionMenu floatingActionMenu = findViewById(R.id.fab);
        if (floatingActionMenu != null && floatingActionMenu.isOpened()) {
            floatingActionMenu.close(true);
            return true;
        }

        return false;
    }

    private void handleFABClick(final int edit, final int otherType) {
        closeFABMenu();

        Preferences prefs = new Preferences(this);
        // Usamos observeForever y removeObserver para asegurar que el clic sea una acción única
        // y no acumule observadores que disparen múltiples diálogos.
        androidx.lifecycle.Observer<List<Car>> observer = new androidx.lifecycle.Observer<>() {
            @Override
            public void onChanged(List<Car> cars) {
                mViewModel.getNotSuspendedCars().removeObserver(this);
                if (cars == null || cars.isEmpty()) return;

                if (cars.size() == 1 || !prefs.isShowCarMenu()) {
                    long carId = cars.size() == 1 ? cars.get(0).getId() : prefs.getDefaultCar();
                    Intent intent = getDetailActivityIntent(edit, carId, otherType);
                    mAddDataLauncher.launch(intent);
                } else {
                    final long[] carIds = new long[cars.size()];
                    final String[] carNames = new String[cars.size()];
                    for (int i = 0; i < cars.size(); i++) {
                        Car car = cars.get(i);
                        carIds[i] = car.getId();
                        carNames[i] = car.getName();
                    }

                    new AlertDialog.Builder(MainActivity.this)
                            .setItems(carNames, (dialog, which) -> {
                                Intent intent = getDetailActivityIntent(edit, carIds[which], otherType);
                                mAddDataLauncher.launch(intent);
                            })
                            .create()
                            .show();
                }
            }
        };
        mViewModel.getNotSuspendedCars().observeForever(observer);
    }

}
