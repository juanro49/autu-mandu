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

package org.juanro.autumandu.gui.pref;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.ViewModelProvider;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.DataDetailActivity;
import org.juanro.autumandu.gui.fragment.AbstractDataDetailFragment;
import org.juanro.autumandu.gui.dialog.MessageDialogFragment;
import org.juanro.autumandu.gui.util.AbstractPreferenceActivity;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.viewmodel.CarsViewModel;

import java.util.ArrayList;
import java.util.List;

public class PreferencesCarsFragment extends ListFragment implements
        AbstractPreferenceActivity.OptionsMenuListener {

    private CarsViewModel viewModel;

    private class CarAdapter extends BaseAdapter {
        private List<Car> cars = new ArrayList<>();

        @Override
        public int getCount() {
            return cars.size();
        }

        @Override
        public Car getItem(int position) {
            return cars.get(position);
        }

        @Override
        public long getItemId(int position) {
            return cars.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CarViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext()).inflate(
                        R.layout.list_item_car, parent, false);

                holder = new CarViewHolder();
                holder.name = convertView.findViewById(android.R.id.text1);
                holder.suspended = convertView.findViewById(android.R.id.text2);
                holder.color = convertView.findViewById(android.R.id.custom);
                convertView.setTag(holder);
            } else {
                holder = (CarViewHolder) convertView.getTag();
            }

            var car = getItem(position);

            holder.name.setText(car.getName());
            if (car.getSuspendedSince() != null) {
                holder.suspended.setText(getString(
                        R.string.suspended_since,
                        android.text.format.DateFormat.getDateFormat(
                                requireContext()).format(
                                car.getSuspendedSince())));
                holder.suspended.setVisibility(View.VISIBLE);
            } else {
                holder.suspended.setVisibility(View.GONE);
            }

            holder.color.getBackground().setColorFilter(car.getColor(), PorterDuff.Mode.SRC_IN);

            return convertView;
        }

        public void setCars(List<Car> cars) {
            this.cars = cars;
            notifyDataSetChanged();
        }
    }

    private static class CarViewHolder {
        TextView name;
        TextView suspended;
        View color;
    }

    private class CarMultiChoiceModeListener implements MultiChoiceModeListener {
        private ActionMode actionMode;

        public void finishActionMode() {
            if (actionMode != null) {
                actionMode.finish();
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.menu_delete) {
                if (getListView().getCheckedItemCount() == carAdapter.getCount()) {
                    MessageDialogFragment.newInstance(0,
                            R.string.alert_delete_title,
                            getString(R.string.alert_cannot_delete_last_car),
                            android.R.string.ok, null).show(
                            getParentFragmentManager(), null);
                } else {
                    var message = getString(R.string.alert_delete_cars_message,
                            getListView().getCheckedItemCount());
                    MessageDialogFragment.newInstance(
                            DELETE_REQUEST_CODE,
                            R.string.alert_delete_title, message,
                            android.R.string.yes, android.R.string.no).show(
                            getParentFragmentManager(), null);
                }
                return true;
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;
            var inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.edit_cars_cab, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                              boolean checked) {
            var count = getListView().getCheckedItemCount();
            mode.setTitle(String.format(getString(R.string.cab_title_selected), count));
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    }

    private static final int DELETE_REQUEST_CODE = 1;

    private CarAdapter carAdapter;
    private CarMultiChoiceModeListener multiChoiceModeListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getParentFragmentManager().setFragmentResultListener(MessageDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            var action = bundle.getInt(MessageDialogFragment.RESULT_ACTION);
            var requestCode = bundle.getInt(MessageDialogFragment.RESULT_REQUEST_CODE);
            if (action == MessageDialogFragment.ACTION_POSITIVE && requestCode == DELETE_REQUEST_CODE) {
                onDialogPositiveClick();
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CarsViewModel.class);

        carAdapter = new CarAdapter();
        multiChoiceModeListener = new CarMultiChoiceModeListener();

        getListView().setMultiChoiceModeListener(multiChoiceModeListener);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        setListAdapter(carAdapter);

        viewModel.getCars().observe(getViewLifecycleOwner(), cars -> carAdapter.setCars(cars));
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.edit_cars, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_add_car) {
            openCarDetailFragment(AbstractDataDetailFragment.EXTRA_ID_DEFAULT);
            return true;
        }
        return false;
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        openCarDetailFragment(id);
    }

    private void onDialogPositiveClick() {
        var checkedIds = getListView().getCheckedItemIds();
        viewModel.deleteCars(checkedIds);
        multiChoiceModeListener.finishActionMode();
    }

    @Override
    public void onStop() {
        super.onStop();
        multiChoiceModeListener.finishActionMode();
    }

    private void openCarDetailFragment(long id) {
        var intent = new Intent(requireActivity(), DataDetailActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtra(DataDetailActivity.EXTRA_EDIT, DataDetailActivity.EXTRA_EDIT_CAR);
        intent.putExtra(AbstractDataDetailFragment.EXTRA_ID, id);
        startActivity(intent);
    }
}
