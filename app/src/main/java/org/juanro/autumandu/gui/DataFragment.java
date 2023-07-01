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

package org.juanro.autumandu.gui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;

import java.util.List;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.util.FloatingActionButtonRevealer;
import org.juanro.autumandu.gui.util.FragmentUtils;

public class DataFragment extends Fragment implements DataListCallback,
        AbstractDataDetailFragment.OnItemActionListener {
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

            List<Fragment> fragments = getChildFragmentManager().getFragments();
            if (fragments != null) {
                for (Fragment childFragment : fragments) {
                    if (childFragment instanceof DataListListener) {
                        ((DataListListener) childFragment).unselectItem(true);
                    }
                }
            }

            if (mTxtNoEntrySelected != null) {
                int id = position == 0 ? R.drawable.ic_c_refueling_128dp
                        : R.drawable.ic_c_other_128dp;
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
            AbstractDataListFragment fragment;

            switch(position) {
                case 0:
                    fragment = new DataListRefuelingFragment();
                    break;
                case 3:
                    fragment = new DataListStationFragment();
                    break;
                default:
                    fragment = new DataListOtherFragment();
            }

            Bundle args = new Bundle();
            args.putBoolean(AbstractDataListFragment.EXTRA_ACTIVATE_ON_CLICK, mTwoPane);
            args.putLong(AbstractDataListFragment.EXTRA_CAR_ID, mCarId);
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

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            // Do nothing here! This is a fix for a NullPointerException described here:
            // http://stackoverflow.com/questions/18642890/
        }
    }

    public static final String EXTRA_CAR_ID = "car_id";

    private boolean mTwoPane;
    private TextView mTxtNoEntrySelected;
    private long mCarId;
    private DataListBackStackListener mBackStackListener;
    private FloatingActionMenu mFab;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCarId = getArguments().getLong(EXTRA_CAR_ID, 0);
        if (mCarId == 0) {
            Preferences prefs = new Preferences(getActivity());
            mCarId = prefs.getDefaultCar();
        }

        mBackStackListener = new DataListBackStackListener();
        getChildFragmentManager().addOnBackStackChangedListener(mBackStackListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_data, container, false);

        PagerAdapter pagerAdapter = new DataListPagerAdapter(getChildFragmentManager());

        ViewPager pager = (ViewPager) v.findViewById(R.id.pager);
        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(new DataListOnPageChangeListener());

        TabLayout tabs = (TabLayout) v.findViewById(R.id.tab_layout);
        tabs.setupWithViewPager(pager);

        mTxtNoEntrySelected = (TextView) v.findViewById(R.id.txt_no_entry_selected);
        if (getChildFragmentManager().findFragmentById(R.id.detail) != null) {
            setNoEntrySelectedTextVisible(false);
        }

        mTwoPane = v.findViewById(R.id.detail) != null;

        mFab = (FloatingActionMenu) v.findViewById(R.id.fab);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MainActivity.setSupportActionBar(this);
    }

    @Override
    public void onItemCanceled() {
        onItemUnselected();
    }

    @Override
    public void onItemDeleted() {
        onItemUnselected();
    }

    @Override
    public void onItemSaved(long newId) {
        onItemUnselected();
    }

    @Override
    public void onViewCreated(RecyclerView recyclerView) {
        FloatingActionButtonRevealer.setup(mFab, recyclerView);
    }

    @Override
    public void onItemSelected(int edit, long id) {
        if (mTwoPane) {
            AbstractDataDetailFragment fragment = null;
            boolean editStation = false;

            if (edit == DataDetailActivity.EXTRA_EDIT_REFUELING)
            {
                fragment = DataDetailRefuelingFragment.newInstance(id);
            }
            else if (edit == DataDetailActivity.EXTRA_EDIT_STATION)
            {
                editStation = true;

                Intent intent = new Intent(getActivity(), PreferencesActivity.class);
                intent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT, PreferencesStationsFragment.class.getName());
                intent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT_TITLE, R.string.pref_title_header_stations);
            }
            else
            {
                fragment = DataDetailOtherFragment.newInstance(id);
            }

            if (!editStation)
            {
                mBackStackListener.skipNextIfPop();

                FragmentManager fm = getChildFragmentManager();

                // Disable the fragment animations, when we are just replacing an existing detail
                // fragment.
                if (fm.getBackStackEntryCount() > 0)
                {
                    FragmentUtils.DISABLE_FRAGMENT_ANIMATIONS = 2;
                }

                fm.popBackStack("detail", FragmentManager.POP_BACK_STACK_INCLUSIVE);

                fm.beginTransaction()
                    .replace(R.id.detail, fragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .addToBackStack("detail")
                    .commit();
            }
        } else {
            Intent intent;

            if (edit == DataDetailActivity.EXTRA_EDIT_STATION)
            {
                intent = new Intent(getActivity(), PreferencesActivity.class);
                intent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT, PreferencesStationsFragment.class.getName());
                intent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT_TITLE, R.string.pref_title_header_stations);
            }
            else
            {
                intent = new Intent(getActivity(), DataDetailActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                intent.putExtra(DataDetailActivity.EXTRA_EDIT, edit);
                intent.putExtra(AbstractDataDetailFragment.EXTRA_ID, id);
            }

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
