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

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment.MessageDialogFragmentListener;
import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;

public class PreferencesCarsFragment extends ListFragment implements
        MessageDialogFragmentListener, LoaderManager.LoaderCallbacks<Cursor> {
    private class CarAdapter extends CursorAdapter {
        public CarAdapter() {
            super(getActivity(), null, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = getActivity().getLayoutInflater().inflate(
                    R.layout.list_item_car, parent, false);

            CarViewHolder holder = new CarViewHolder();
            holder.name = (TextView) view.findViewById(android.R.id.text1);
            holder.suspended = (TextView) view.findViewById(android.R.id.text2);
            holder.color = view.findViewById(android.R.id.custom);
            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            CarCursor car = new CarCursor(cursor);
            CarViewHolder holder = (CarViewHolder) view.getTag();

            holder.name.setText(car.getName());
            if (car.getSuspendedSince() != null) {
                holder.suspended.setText(getString(
                        R.string.suspended_since,
                        android.text.format.DateFormat.getDateFormat(
                                getActivity()).format(
                                car.getSuspendedSince())));
                holder.suspended.setVisibility(View.VISIBLE);
            } else {
                holder.suspended.setVisibility(View.GONE);
            }

            holder.color.getBackground().setColorFilter(car.getColor(), PorterDuff.Mode.SRC);
        }
    }

    private static class CarViewHolder {
        public TextView name;
        public TextView suspended;
        public View color;
    }

    private class CarMultiChoiceModeListener implements MultiChoiceModeListener {
        private ActionMode mActionMode;

        public void finishActionMode() {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_delete:
                    if (getListView().getCheckedItemCount() == mCarAdapter.getCount()) {
                        MessageDialogFragment.newInstance(null, 0,
                                R.string.alert_delete_title,
                                getString(R.string.alert_cannot_delete_last_car),
                                android.R.string.ok, null).show(
                                getFragmentManager(), null);
                    } else {
                        String message = getString(R.string.alert_delete_cars_message,
                                getListView().getCheckedItemCount());
                        MessageDialogFragment.newInstance(
                                PreferencesCarsFragment.this, DELETE_REQUEST_CODE,
                                R.string.alert_delete_title, message,
                                android.R.string.yes, android.R.string.no).show(
                                getFragmentManager(), null);
                    }
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.edit_cars_cab, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                              boolean checked) {
            int count = getListView().getCheckedItemCount();
            mode.setTitle(String.format(getString(R.string.cab_title_selected), count));
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    }

    private static final int DELETE_REQUEST_CODE = 1;

    private CarAdapter mCarAdapter;
    private CarMultiChoiceModeListener mMultiChoiceModeListener;
    private boolean mCarEditInProgress = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mCarAdapter = new CarAdapter();
        mMultiChoiceModeListener = new CarMultiChoiceModeListener();

        getListView().setMultiChoiceModeListener(mMultiChoiceModeListener);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        setListAdapter(mCarAdapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_cars, menu);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        openCarDetailFragment(id);
    }

    @Override
    public void onDialogNegativeClick(int requestCode) {
    }

    @Override
    public void onDialogPositiveClick(int requestCode) {
        if (requestCode == DELETE_REQUEST_CODE) {
            long[] checkedIds = getListView().getCheckedItemIds();
            for (long id : checkedIds) {
                new CarSelection().id(id).delete(getActivity().getContentResolver());
            }

            mMultiChoiceModeListener.finishActionMode();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_car:
                openCarDetailFragment(AbstractDataDetailFragment.EXTRA_ID_DEFAULT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCarEditInProgress) {
            mCarEditInProgress = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mMultiChoiceModeListener.finishActionMode();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CarSelection sel = new CarSelection();
        return new CursorLoader(getActivity(), sel.uri(), CarColumns.ALL_COLUMNS, sel.sel(),
                sel.args(), CarColumns.NAME + " COLLATE UNICODE");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCarAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCarAdapter.changeCursor(null);
    }

    private void openCarDetailFragment(long id) {
        Intent intent = new Intent(getActivity(), DataDetailActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtra(DataDetailActivity.EXTRA_EDIT, DataDetailActivity.EXTRA_EDIT_CAR);
        intent.putExtra(AbstractDataDetailFragment.EXTRA_ID, id);
        startActivityForResult(intent, 0);

        mCarEditInProgress = true;
    }
}