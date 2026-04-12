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

package org.juanro.autumandu.gui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import com.google.android.material.tabs.TabLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.pref.PreferencesActivity;
import org.juanro.autumandu.gui.pref.PreferencesStationsFragment;
import org.juanro.autumandu.gui.util.FloatingActionButtonRevealer;
import org.juanro.autumandu.gui.util.FragmentUtils;
import org.juanro.autumandu.gui.DataDetailActivity;

public class DataFragment extends Fragment implements DataListCallback,
        AbstractDataDetailFragment.OnItemActionListener {
    private class DataListBackStackListener implements OnBackStackChangedListener {
        private boolean skipNextIfPop = false;

        @Override
        public void onBackStackChanged() {
            var isPop = getChildFragmentManager().getBackStackEntryCount() == 0;
            if (isPop && !skipNextIfPop) {
                setNoEntrySelectedTextVisible(true);
                for (var childFragment : getChildFragmentManager().getFragments()) {
                    if (childFragment instanceof DataListListener) {
                        ((DataListListener) childFragment).unselectItem(false);
                    }
                }
            } else if (!isPop) {
                setNoEntrySelectedTextVisible(false);
            }

            skipNextIfPop = false;
        }

        public void skipNextIfPop() {
            skipNextIfPop = true;
        }
    }

    private class DataListOnPageChangeListener extends SimpleOnPageChangeListener {
        @Override
        public void onPageSelected(int position) {
            onItemUnselected();

            var fragments = getChildFragmentManager().getFragments();
            for (var childFragment : fragments) {
                if (childFragment instanceof DataListListener) {
                    ((DataListListener) childFragment).unselectItem(true);
                }
            }

            if (txtNoEntrySelected != null) {
                int id = switch (position) {
                    case 0, 4 -> R.drawable.ic_c_refueling_128dp;
                    case 1 -> R.drawable.ic_c_tire_128dp;
                    default -> R.drawable.ic_c_other_128dp;
                };

                txtNoEntrySelected.setCompoundDrawablesWithIntrinsicBounds(0, id, 0, 0);
            }
        }
    }

    private class DataListPagerAdapter extends FragmentStatePagerAdapter {
        private final String[] titles;

        public DataListPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            titles = getResources().getStringArray(R.array.data_list_page_titles);
        }

        @Override
        public int getCount() {
            return titles.length;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            AbstractDataListFragment<?> fragment = switch (position) {
                case 0 -> new DataListRefuelingFragment();
                case 1 -> new DataListTireFragment();
                case 4 -> new DataListStationFragment();
                default -> new DataListOtherFragment();
            };

            fragment.setArguments(getArgumentsForPage(position));
            return fragment;
        }

        private Bundle getArgumentsForPage(int position) {
            var args = new Bundle();
            args.putBoolean(AbstractDataListFragment.EXTRA_ACTIVATE_ON_CLICK, twoPane);
            args.putLong(AbstractDataListFragment.EXTRA_CAR_ID, carId);
            if (position == 2) {
                args.putInt(DataListOtherFragment.EXTRA_OTHER_TYPE,
                        DataListOtherFragment.EXTRA_OTHER_TYPE_EXPENDITURE);
            } else if (position == 3) {
                args.putInt(DataListOtherFragment.EXTRA_OTHER_TYPE,
                        DataListOtherFragment.EXTRA_OTHER_TYPE_INCOME);
            }
            return args;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            // Do nothing here! This is a fix for a NullPointerException described here:
            // http://stackoverflow.com/questions/18642890/
        }
    }

    public static final String EXTRA_CAR_ID = "car_id";

    private boolean twoPane;
    private TextView txtNoEntrySelected;
    private long carId;
    private DataListBackStackListener backStackListener;
    private FloatingActionMenu fab;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var arguments = getArguments();
        if (arguments != null) {
            carId = arguments.getLong(EXTRA_CAR_ID, 0);
        }
        if (carId == 0) {
            var prefs = new Preferences(requireActivity());
            carId = prefs.getDefaultCar();
        }

        backStackListener = new DataListBackStackListener();
        getChildFragmentManager().addOnBackStackChangedListener(backStackListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        var v = inflater.inflate(R.layout.fragment_data, container, false);

        var pagerAdapter = new DataListPagerAdapter(getChildFragmentManager());

        var pager = (ViewPager) v.findViewById(R.id.pager);
        pager.setAdapter(pagerAdapter);
        pager.addOnPageChangeListener(new DataListOnPageChangeListener());

        var tabs = (TabLayout) v.findViewById(R.id.tab_layout);
        tabs.setupWithViewPager(pager);

        txtNoEntrySelected = v.findViewById(R.id.txt_no_entry_selected);
        if (getChildFragmentManager().findFragmentById(R.id.detail) != null) {
            setNoEntrySelectedTextVisible(false);
        }

        twoPane = v.findViewById(R.id.detail) != null;

        fab = v.findViewById(R.id.fab);

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(view.findViewById(R.id.toolbar));
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
    public void onViewCreated(@NonNull RecyclerView recyclerView) {
        FloatingActionButtonRevealer.setup(fab, recyclerView);
    }

    @Override
    public void onItemSelected(int edit, long id) {
        if (edit == DataDetailActivity.EXTRA_EDIT_STATION) {
            startActivity(createStationIntent());
            return;
        }

        if (twoPane) {
            showDetailFragment(edit, id);
        } else {
            startActivity(createDetailIntent(edit, id));
        }
    }

    private Intent createStationIntent() {
        var intent = new Intent(getActivity(), PreferencesActivity.class);
        intent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT, PreferencesStationsFragment.class.getName());
        intent.putExtra(PreferencesActivity.EXTRA_SHOW_FRAGMENT_TITLE, R.string.pref_title_header_stations);
        return intent;
    }

    private Intent createDetailIntent(int edit, long id) {
        var intent = new Intent(getActivity(), DataDetailActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtra(DataDetailActivity.EXTRA_EDIT, edit);
        intent.putExtra(AbstractDataDetailFragment.EXTRA_ID, id);
        return intent;
    }

    private void showDetailFragment(int edit, long id) {
        AbstractDataDetailFragment fragment = switch (edit) {
            case DataDetailActivity.EXTRA_EDIT_REFUELING -> DataDetailRefuelingFragment.newInstance(id);
            case DataDetailActivity.EXTRA_EDIT_TIRE -> DataDetailTireFragment.newInstance(id);
            default -> DataDetailOtherFragment.newInstance(id);
        };

        backStackListener.skipNextIfPop();

        var fm = getChildFragmentManager();

        // Disable the fragment animations, when we are just replacing an existing detail
        // fragment.
        if (fm.getBackStackEntryCount() > 0)
        {
            FragmentUtils.disableAnimations();
        }

        fm.popBackStack("detail", FragmentManager.POP_BACK_STACK_INCLUSIVE);

        fm.beginTransaction()
                .replace(R.id.detail, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack("detail")
                .commit();
    }

    @Override
    public void onItemUnselected() {
        getChildFragmentManager().popBackStack("detail", FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    private void setNoEntrySelectedTextVisible(boolean visible) {
        if (txtNoEntrySelected != null) {
            txtNoEntrySelected.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
