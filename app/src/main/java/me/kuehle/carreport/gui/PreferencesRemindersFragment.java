/*
 * Copyright 2015 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.query.ReminderQueries;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment;
import me.kuehle.carreport.provider.reminder.ReminderColumns;
import me.kuehle.carreport.provider.reminder.ReminderCursor;
import me.kuehle.carreport.provider.reminder.ReminderSelection;
import me.kuehle.carreport.util.TimeSpan;
import me.kuehle.carreport.util.reminder.ReminderService;

public class PreferencesRemindersFragment extends ListFragment implements
        MessageDialogFragment.MessageDialogFragmentListener,
        LoaderManager.LoaderCallbacks<Cursor> {
    private class ReminderAdapter extends CursorAdapter {
        private java.text.DateFormat mDateFormat;
        private String mUnitDistance;

        public ReminderAdapter() {
            super(getActivity(), null, 0);

            mDateFormat = DateFormat.getDateFormat(getActivity());

            Preferences prefs = new Preferences(getActivity());
            mUnitDistance = prefs.getUnitDistance();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.list_item_reminder,
                    parent, false);

            ReminderViewHolder holder = new ReminderViewHolder();
            holder.title = (TextView) view.findViewById(R.id.txt_title);
            holder.car = (TextView) view.findViewById(R.id.txt_car);
            holder.afterDistance = (TextView) view.findViewById(R.id.txt_after_distance);
            holder.afterTime = (TextView) view.findViewById(R.id.txt_after_time);
            holder.status = (TextView) view.findViewById(R.id.txt_status);

            View btnDone = view.findViewById(R.id.btn_done);
            View btnSnooze = view.findViewById(R.id.btn_snooze);

            btnDone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getListView().getPositionForView(v);
                    long id = getItemId(position);
                    ReminderService.markRemindersDone(getActivity(), id);
                    notifyDataSetChanged();
                }
            });

            btnSnooze.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getListView().getPositionForView(v);
                    long id = getItemId(position);
                    ReminderService.snoozeReminders(getActivity(), id);
                    notifyDataSetChanged();
                }
            });

            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ReminderCursor reminder = new ReminderCursor(cursor);
            ReminderQueries queries = new ReminderQueries(getActivity(), reminder);
            ReminderViewHolder holder = (ReminderViewHolder) view.getTag();

            holder.title.setText(reminder.getTitle());
            holder.car.setText(reminder.getCarName());
            if (reminder.getAfterDistance() != null) {
                holder.afterDistance.setText(String.format("%d %s", reminder.getAfterDistance(),
                        mUnitDistance));
                holder.afterDistance.setVisibility(View.VISIBLE);
            } else {
                holder.afterDistance.setVisibility(View.INVISIBLE);
            }

            if (reminder.getAfterTimeSpanUnit() != null) {
                TimeSpan span = new TimeSpan(reminder.getAfterTimeSpanUnit(),
                        reminder.getAfterTimeSpanCount() == null ? 1 : reminder.getAfterTimeSpanCount());
                holder.afterTime.setText(span.toLocalizedString(getActivity()));
                holder.afterTime.setVisibility(View.VISIBLE);
            } else {
                holder.afterTime.setVisibility(View.INVISIBLE);
            }

            if (queries.isDue()) {
                holder.status.setTextColor(getResources().getColor(R.color.accent));
                if (reminder.getNotificationDismissed()) {
                    holder.status.setText(R.string.description_reminder_status_due_dismissed);
                } else if (queries.isSnoozed()) {
                    holder.status.setText(getString(
                            R.string.description_reminder_status_due_snoozed,
                            mDateFormat.format(reminder.getSnoozedUntil())));
                } else {
                    holder.status.setText(R.string.description_reminder_status_due);
                }
            } else {
                holder.status.setTextColor(getResources().getColor(R.color.secondary_text));
                if (reminder.getAfterDistance() != null && reminder.getAfterTimeSpanUnit() != null) {
                    holder.status.setText(getString(
                            R.string.description_reminder_status_distance_and_time,
                            String.format("%d %s", queries.getDistanceToDue(), mUnitDistance),
                            TimeSpan.fromMillis(queries.getTimeToDue()).toLocalizedString(getActivity())));
                } else if (reminder.getAfterDistance() != null) {
                    holder.status.setText(getString(
                            R.string.description_reminder_status_distance,
                            String.format("%d %s", queries.getDistanceToDue(), mUnitDistance)));
                } else {
                    holder.status.setText(getString(
                            R.string.description_reminder_status_time,
                            TimeSpan.fromMillis(queries.getTimeToDue()).toLocalizedString(getActivity())));
                }
            }
        }
    }

    private static class ReminderViewHolder {
        public TextView title;
        public TextView car;
        public TextView afterDistance;
        public TextView afterTime;
        public TextView status;
    }

    private class ReminderMultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {
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
                    String message = getString(R.string.alert_delete_reminders_message,
                            getListView().getCheckedItemCount());
                    MessageDialogFragment.newInstance(PreferencesRemindersFragment.this,
                            REQUEST_DELETE, R.string.alert_delete_title, message,
                            android.R.string.yes, android.R.string.no)
                            .show(getFragmentManager(), null);
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            this.mActionMode = mode;

            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.edit_reminders_cab, menu);

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

    private ReminderAdapter mReminderAdapter;
    private ReminderMultiChoiceModeListener mMultiChoiceModeListener;
    private boolean mReminderEditInProgress = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mReminderAdapter = new ReminderAdapter();
        mMultiChoiceModeListener = new ReminderMultiChoiceModeListener();

        getListView().setMultiChoiceModeListener(mMultiChoiceModeListener);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        setListAdapter(mReminderAdapter);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        openReminderDetailFragment(id);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_reminders, menu);
    }

    @Override
    public void onDialogNegativeClick(int requestCode) {
    }

    @Override
    public void onDialogPositiveClick(int requestCode) {
        if (requestCode == REQUEST_DELETE) {
            long[] checkedIds = getListView().getCheckedItemIds();
            for (long id : checkedIds) {
                new ReminderSelection().id(id).delete(getActivity().getContentResolver());
            }

            mMultiChoiceModeListener.finishActionMode();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_reminder:
                openReminderDetailFragment(AbstractDataDetailFragment.EXTRA_ID_DEFAULT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mReminderEditInProgress) {
            mReminderEditInProgress = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mMultiChoiceModeListener.finishActionMode();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        ReminderSelection sel = new ReminderSelection();
        return new CursorLoader(getActivity(), sel.uri(), null, sel.sel(), sel.args(), ReminderColumns.TITLE + " COLLATE UNICODE");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mReminderAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mReminderAdapter.changeCursor(null);
    }

    private void openReminderDetailFragment(long id) {
        Intent intent = new Intent(getActivity(), DataDetailActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtra(DataDetailActivity.EXTRA_EDIT, DataDetailActivity.EXTRA_EDIT_REMINDER);
        intent.putExtra(AbstractDataDetailFragment.EXTRA_ID, id);
        startActivityForResult(intent, 0);

        mReminderEditInProgress = true;
    }
}
