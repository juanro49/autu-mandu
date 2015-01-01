/*
 * Copyright 2014 Jan Kühle
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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.activeandroid.Model;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.gui.MainActivity.DataChangeListener;

public class DataFragment extends Fragment implements DataListCallback,
        AbstractDataDetailFragment.OnItemActionListener, DataChangeListener {
    private class DataListBackStackListener implements OnBackStackChangedListener {
        private boolean mSkipNextIfPop = false;

        @Override
        public void onBackStackChanged() {
            boolean isPop = getChildFragmentManager().getBackStackEntryCount() == 0;
            if (isPop && !mSkipNextIfPop) {
                setNoEntrySelectedTextVisible(true);
                for (Fragment childFragment : getChildFragmentManager().getFragments()) {
                    if (childFragment instanceof DataListListener) {
                        ((DataListListener) childFragment).unselectItem(false);
                    }
                }
            } else if (!isPop) {
                setNoEntrySelectedTextVisible(false);
            }

            mSkipNextIfPop = false;
        }

        public void skipNextIfPop() {
            mSkipNextIfPop = true;
        }
    }

    private class DataListOnPageChangeListener extends SimpleOnPageChangeListener {
        @Override
        public void onPageSelected(int position) {
            onItemUnselected();

            for (Fragment childFragment : getChildFragmentManager().getFragments()) {
                if (childFragment instanceof DataListListener) {
                    ((DataListListener) childFragment).unselectItem(true);
                }
            }

            if (mTxtNoEntrySelected != null) {
                int id = position == 0 ? R.drawable.ic_data_detail_refueling
                        : R.drawable.ic_data_detail_other;
                mTxtNoEntrySelected.setCompoundDrawablesWithIntrinsicBounds(0, id, 0, 0);
            }
        }
    }

    private class DataListPagerAdapter extends FragmentStatePagerAdapter {
        private String[] mTitles;

        public DataListPagerAdapter(FragmentManager fm) {
            super(fm);
            mTitles = getResources().getStringArray(R.array.data_list_page_titles);
        }

        @Override
        public int getCount() {
            return mTitles.length;
        }

        @Override
        public Fragment getItem(int position) {
            AbstractDataListFragment<? extends Model> fragment;
            if (position == 0) {
                fragment = new DataListRefuelingFragment();
            } else {
                fragment = new DataListOtherFragment();
            }

            Bundle args = new Bundle();
            args.putBoolean(AbstractDataListFragment.EXTRA_ACTIVATE_ON_CLICK, mTwoPane);
            args.putLong(AbstractDataListFragment.EXTRA_CAR_ID, mCar.id);
            if (position == 1) {
                args.putInt(DataListOtherFragment.EXTRA_OTHER_TYPE,
                        DataListOtherFragment.EXTRA_OTHER_TYPE_EXPENDITURE);
            } else if (position == 2) {
                args.putInt(DataListOtherFragment.EXTRA_OTHER_TYPE,
                        DataListOtherFragment.EXTRA_OTHER_TYPE_INCOME);
            }

            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }
    }

    public static final String EXTRA_CAR_ID = "car_id";

    private boolean mTwoPane;
    private TextView mTxtNoEntrySelected;
    private Car mCar;
    private DataListBackStackListener mBackStackListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long carId = getArguments().getLong(EXTRA_CAR_ID, 0);
        if (carId == 0) {
            Preferences prefs = new Preferences(getActivity());
            carId = prefs.getDefaultCar();
        }

        mCar = Car.load(Car.class, carId);

        mBackStackListener = new DataListBackStackListener();
        getChildFragmentManager().addOnBackStackChangedListener(mBackStackListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_data, container, false);

        ViewPager mPager = (ViewPager) v.findViewById(R.id.pager);
        mPager.setAdapter(new DataListPagerAdapter(getChildFragmentManager()));
        mPager.setOnPageChangeListener(new DataListOnPageChangeListener());

        PagerTabStrip tabs = (PagerTabStrip) v.findViewById(R.id.pager_tab_strip);
        tabs.setBackgroundResource(R.color.primary);
        tabs.setTabIndicatorColorResource(R.color.accent);
        tabs.setDrawFullUnderline(true);

        mTxtNoEntrySelected = (TextView) v.findViewById(R.id.txt_no_entry_selected);
        if (getChildFragmentManager().findFragmentById(R.id.detail) != null) {
            setNoEntrySelectedTextVisible(false);
        }

        mTwoPane = v.findViewById(R.id.detail) != null;

        return v;
    }

    @Override
    public void onDataChanged() {
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment instanceof DataListListener) {
                ((DataListListener) fragment).updateData();
            }
        }
    }

    @Override
    public void onItemCanceled() {
        onItemUnselected();
    }

    @Override
    public void onItemDeleted() {
        onItemUnselected();
        onDataChanged();
    }

    @Override
    public void onItemSaved() {
        onItemUnselected();
        onDataChanged();
    }

    @Override
    public void onItemSelected(int edit, long id) {
        if (mTwoPane) {
            AbstractDataDetailFragment fragment;
            if (edit == DataDetailActivity.EXTRA_EDIT_REFUELING) {
                fragment = DataDetailRefuelingFragment.newInstance(id, true);
            } else {
                fragment = DataDetailOtherFragment.newInstance(id, true);
            }

            mBackStackListener.skipNextIfPop();

            FragmentManager fm = getChildFragmentManager();
            fm.popBackStack("detail", FragmentManager.POP_BACK_STACK_INCLUSIVE);

            FragmentTransaction ft = fm.beginTransaction().replace(R.id.detail, fragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft.addToBackStack("detail");
            ft.commit();
        } else {
            Intent intent = new Intent(getActivity(), DataDetailActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            intent.putExtra(DataDetailActivity.EXTRA_EDIT, edit);
            intent.putExtra(AbstractDataDetailFragment.EXTRA_ID, id);
            startActivityForResult(intent, 0);
        }
    }

    @Override
    public void onItemUnselected() {
        getChildFragmentManager().popBackStack("detail", FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    private void setNoEntrySelectedTextVisible(boolean visible) {
        if (mTxtNoEntrySelected != null) {
            mTxtNoEntrySelected.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
