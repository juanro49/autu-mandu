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
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import me.kuehle.carreport.R;
import me.kuehle.carreport.data.query.FuelTypeQueries;
import me.kuehle.carreport.gui.dialog.EditFuelTypeDialogFragment;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment;
import me.kuehle.carreport.provider.fueltype.FuelTypeColumns;
import me.kuehle.carreport.provider.fueltype.FuelTypeSelection;

public class PreferencesFuelTypesFragment extends ListFragment implements
        MessageDialogFragment.MessageDialogFragmentListener,
        EditFuelTypeDialogFragment.EditFuelTypeDialogFragmentListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    private class FuelTypesMultiChoiceModeListener implements MultiChoiceModeListener {
        private ActionMode mActionMode;

        public void deleteSelectedFuelTypes() {
            long[] ids = getListView().getCheckedItemIds();
            for (long id : ids) {
                new FuelTypeSelection().id(id).delete(getActivity().getContentResolver());
            }
        }

        public void finishActionMode() {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_delete:
                    long[] ids = getListView().getCheckedItemIds();
                    for (long id : ids) {
                        if (FuelTypeQueries.isUsed(getActivity(), id)) {
                            MessageDialogFragment.newInstance(null, 0, R.string.alert_delete_title,
                                    getString(R.string.alert_cannot_delete_fuel_type),
                                    android.R.string.ok, null)
                                    .show(getFragmentManager(), null);
                            return true;
                        }
                    }

                    MessageDialogFragment.newInstance(PreferencesFuelTypesFragment.this, REQUEST_DELETE,
                            R.string.alert_delete_title,
                            getString(R.string.alert_delete_fuel_types_message,
                                    getListView().getCheckedItemCount()),
                            android.R.string.yes, android.R.string.no)
                            .show(getFragmentManager(), null);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.edit_fuel_types_cab, menu);
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

    private static final int REQUEST_DELETE = 1;
    private static final int REQUEST_ADD = 2;
    private static final int REQUEST_EDIT = 3;

    private FuelTypesMultiChoiceModeListener mMultiChoiceModeListener;

    private SimpleCursorAdapter mListAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mMultiChoiceModeListener = new FuelTypesMultiChoiceModeListener();

        getListView().setMultiChoiceModeListener(mMultiChoiceModeListener);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        mListAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_activated_2,
                null, new String[]{FuelTypeColumns.NAME, FuelTypeColumns.CATEGORY},
                new int[]{android.R.id.text1, android.R.id.text2}, 0);
        setListAdapter(mListAdapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_fuel_types, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_fuel_type:
                EditFuelTypeDialogFragment.newInstance(PreferencesFuelTypesFragment.this,
                        REQUEST_ADD, 0).show(getFragmentManager(), null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        EditFuelTypeDialogFragment.newInstance(PreferencesFuelTypesFragment.this, REQUEST_EDIT,
                id).show(getFragmentManager(), null);
    }

    @Override
    public void onDialogPositiveClick(int requestCode) {
        if (requestCode == REQUEST_DELETE) {
            mMultiChoiceModeListener.deleteSelectedFuelTypes();
            mMultiChoiceModeListener.finishActionMode();
        }
    }

    @Override
    public void onDialogNegativeClick(int requestCode) {
    }

    @Override
    public void onStop() {
        super.onStop();
        mMultiChoiceModeListener.finishActionMode();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        FuelTypeSelection sel = new FuelTypeSelection();
        return new CursorLoader(getActivity(), sel.uri(), FuelTypeColumns.ALL_COLUMNS, sel.sel(),
                sel.args(), FuelTypeColumns.NAME + " COLLATE UNICODE");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mListAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mListAdapter.changeCursor(null);
    }
}