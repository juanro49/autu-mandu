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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;
import android.widget.TextView;

import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.dialog.SupportMessageDialogFragment;
import me.kuehle.carreport.gui.dialog.SupportMessageDialogFragment.SupportMessageDialogFragmentListener;

public abstract class AbstractDataListFragment extends
        ListFragment implements SupportMessageDialogFragmentListener,
        DataListListener, LoaderManager.LoaderCallbacks<Cursor> {
    public static final String EXTRA_ACTIVATE_ON_CLICK = "activate_on_click";
    public static final boolean EXTRA_ACTIVATE_ON_CLICK_DEFAULT = false;
    public static final String EXTRA_CAR_ID = "car_id";

    private static final String STATE_CURRENT_ITEM = "current_item";
    private static final int REQUEST_DELETE = 0;

    protected long mCarId;

    private DataListAdapter mListAdapter;
    private int mCurrentItem = ListView.INVALID_POSITION;
    private DataListCallback mDataListCallback;
    private boolean mActivateOnClick = false;
    private ActionMode mActionMode = null;
    private boolean mDontStartActionMode = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.edit_message_no_entries_available));

        mListAdapter = new DataListAdapter(getActivity(), null);
        setListAdapter(mListAdapter);

        getListView().setMultiChoiceModeListener(new DataListMultiChoiceModeListener());
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        mDataListCallback.onViewCreated(getListView());

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState != null) {
            setCurrentPosition(savedInstanceState.getInt(STATE_CURRENT_ITEM,
                    ListView.INVALID_POSITION));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            if (getParentFragment() != null) {
                mDataListCallback = (DataListCallback) getParentFragment();
            } else {
                mDataListCallback = (DataListCallback) activity;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement OnItemSelectionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mActivateOnClick = args.getBoolean(EXTRA_ACTIVATE_ON_CLICK,
                EXTRA_ACTIVATE_ON_CLICK_DEFAULT);

        mCarId = args.getLong(EXTRA_CAR_ID);
    }

    @Override
    public void onDialogNegativeClick(int requestCode) {
    }

    @Override
    public void onDialogPositiveClick(int requestCode) {
        if (requestCode == REQUEST_DELETE) {
            long[] ids = getListView().getCheckedItemIds();
            for (long id : ids) {
                deleteItem(id);
            }

            mActionMode.finish();
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mActionMode != null) {
            return;
        }

        setCurrentPosition(position);
        mDataListCallback.onItemSelected(getExtraEdit(), id);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_ITEM, mCurrentItem);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        setCurrentPosition(ListView.INVALID_POSITION);
        mListAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mListAdapter.changeCursor(null);
    }

    @Override
    public void unselectItem(boolean finishActionMode) {
        if (mActionMode != null && finishActionMode) {
            mActionMode.finish();
        }

        setCurrentPosition(ListView.INVALID_POSITION);
    }

    private void setCurrentPosition(int position) {
        if (mActionMode != null) {
            return;
        }

        getListView().setItemChecked(mCurrentItem, false);
        if (position != ListView.INVALID_POSITION && mActivateOnClick) {
            mDontStartActionMode = true;
            getListView().setItemChecked(position, true);
            mDontStartActionMode = false;
        }

        mCurrentItem = position;
    }

    protected abstract int getAlertDeleteManyMessage();

    protected abstract int getExtraEdit();

    protected abstract SparseArray<String> getItemData(Cursor cursor);

    protected abstract boolean isMissingData(Cursor cursor);

    protected abstract boolean isInvalidData(Cursor cursor);

    protected abstract void deleteItem(long id);

    private class DataListAdapter extends CursorAdapter {
        private final int[] fields = {R.id.title, R.id.subtitle, R.id.date,
                R.id.data1, R.id.data1_calculated, R.id.data2,
                R.id.data2_calculated, R.id.data3, R.id.data3_calculated};

        public DataListAdapter(Context context, Cursor c) {
            super(context, c, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            if (getItemViewType(cursor) == 0) {
                return getActivity().getLayoutInflater().inflate(
                        R.layout.list_item_data_missing, parent, false);
            } else {
                return getActivity().getLayoutInflater().inflate(
                        R.layout.list_item_data, parent, false);
            }
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            SparseArray<String> item = getItemData(cursor);
            if (getItemViewType(cursor) == 0) {
                TextView textView = (TextView) view.findViewById(R.id.title);
                textView.setText(item.get(R.id.title));
            } else {
                for (int field : fields) {
                    TextView textView = (TextView) view.findViewById(field);
                    String value = item.get(field);
                    if (value != null) {
                        textView.setText(value);
                        textView.setVisibility(View.VISIBLE);
                    } else if (field == R.id.subtitle) {
                        textView.setVisibility(View.GONE);
                    } else {
                        textView.setVisibility(View.INVISIBLE);
                    }
                }

                View invalidDataView = view.findViewById(R.id.data_invalid);
                if (isInvalidData(cursor)) {
                    invalidDataView.setVisibility(View.VISIBLE);
                } else {
                    invalidDataView.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            Cursor cursor = (Cursor) getItem(position);
            return getItemViewType(cursor);
        }

        public int getItemViewType(Cursor cursor) {
            if (cursor != null) {
                return isMissingData(cursor) ? 0 : 1;
            } else {
                return 0;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEnabled(int position) {
            Cursor cursor = (Cursor) getItem(position);
            return cursor == null || !isMissingData(cursor);
        }
    }

    private class DataListMultiChoiceModeListener implements MultiChoiceModeListener {
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_delete:
                    String message = String.format(getString(getAlertDeleteManyMessage()),
                            getListView().getCheckedItemCount());
                    SupportMessageDialogFragment.newInstance(
                            AbstractDataListFragment.this, REQUEST_DELETE,
                            R.string.alert_delete_title, message,
                            android.R.string.yes, android.R.string.no).show(
                            getFragmentManager(), null);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (mDontStartActionMode) {
                return false;
            }

            mCurrentItem = ListView.INVALID_POSITION;
            mDataListCallback.onItemUnselected();

            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.view_data_cab, menu);

            mActionMode = mode;
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
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
}