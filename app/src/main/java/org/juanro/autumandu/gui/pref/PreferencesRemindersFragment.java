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

package org.juanro.autumandu.gui.pref;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.ViewModelProvider;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.DataDetailActivity;
import org.juanro.autumandu.gui.fragment.AbstractDataDetailFragment;
import org.juanro.autumandu.gui.dialog.MessageDialogFragment;
import org.juanro.autumandu.gui.util.AbstractPreferenceActivity;
import org.juanro.autumandu.model.dto.ReminderWithCar;
import org.juanro.autumandu.util.TimeSpan;
import org.juanro.autumandu.util.reminder.ReminderService;
import org.juanro.autumandu.viewmodel.RemindersViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PreferencesRemindersFragment extends ListFragment implements
        AbstractPreferenceActivity.OptionsMenuListener {

    private RemindersViewModel viewModel;

    private class ReminderAdapter extends BaseAdapter {
        private final java.text.DateFormat dateFormat;
        private final String unitDistance;
        private List<ReminderWithCar> items = new ArrayList<>();

        public ReminderAdapter() {
            dateFormat = DateFormat.getDateFormat(requireContext());
            var prefs = new Preferences(requireContext());
            unitDistance = prefs.getUnitDistance();
        }

        public void setItems(List<ReminderWithCar> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public ReminderWithCar getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).reminder().getId();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ReminderViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext()).inflate(R.layout.list_item_reminder,
                        parent, false);

                holder = new ReminderViewHolder();
                holder.title = convertView.findViewById(R.id.txt_title);
                holder.car = convertView.findViewById(R.id.txt_car);
                holder.afterDistance = convertView.findViewById(R.id.txt_after_distance);
                holder.afterTime = convertView.findViewById(R.id.txt_after_time);
                holder.status = convertView.findViewById(R.id.txt_status);

                var btnDone = convertView.findViewById(R.id.btn_done);
                var btnSnooze = convertView.findViewById(R.id.btn_snooze);

                btnDone.setOnClickListener(v -> {
                    var pos = getListView().getPositionForView(v);
                    var id = getItemId(pos);
                    ReminderService.markRemindersDone(requireContext(), id);
                });

                btnSnooze.setOnClickListener(v -> {
                    var pos = getListView().getPositionForView(v);
                    var id = getItemId(pos);
                    ReminderService.snoozeReminders(requireContext(), id);
                });

                convertView.setTag(holder);
            } else {
                holder = (ReminderViewHolder) convertView.getTag();
            }

            var item = getItem(position);

            holder.title.setText(item.reminder().getTitle());
            holder.car.setText(item.carName());

            if (item.reminder().getAfterDistance() != null) {
                holder.afterDistance.setText(String.format(
                        Locale.getDefault(),
                        "%d %s",
                        item.reminder().getAfterDistance(),
                        unitDistance));
                holder.afterDistance.setVisibility(View.VISIBLE);
            } else {
                holder.afterDistance.setVisibility(View.INVISIBLE);
            }

            if (item.reminder().getAfterTimeSpanUnit() != null) {
                var span = new TimeSpan(item.reminder().getAfterTimeSpanUnit(),
                        item.reminder().getAfterTimeSpanCount() == null ? 1 : item.reminder().getAfterTimeSpanCount());
                holder.afterTime.setText(span.toLocalizedString(requireContext()));
                holder.afterTime.setVisibility(View.VISIBLE);
            } else {
                holder.afterTime.setVisibility(View.INVISIBLE);
            }

            if (item.isDue()) {
                holder.status.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent));
                if (item.reminder().isNotificationDismissed()) {
                    holder.status.setText(R.string.description_reminder_status_due_dismissed);
                } else if (item.isSnoozed() && item.reminder().getSnoozedUntil() != null) {
                    holder.status.setText(getString(
                            R.string.description_reminder_status_due_snoozed,
                            dateFormat.format(item.reminder().getSnoozedUntil())));
                } else {
                    holder.status.setText(R.string.description_reminder_status_due);
                }
            } else {
                holder.status.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_text));
                if (item.reminder().getAfterDistance() != null && item.reminder().getAfterTimeSpanUnit() != null) {
                    holder.status.setText(getString(
                            R.string.description_reminder_status_distance_and_time,
                            String.format(Locale.getDefault(), "%d %s", item.getDistanceToDue(), unitDistance),
                            TimeSpan.fromMillis(item.getTimeToDue()).toLocalizedString(requireContext())));
                } else if (item.reminder().getAfterDistance() != null) {
                    holder.status.setText(getString(
                            R.string.description_reminder_status_distance,
                            String.format(Locale.getDefault(), "%d %s", item.getDistanceToDue(), unitDistance)));
                } else {
                    holder.status.setText(getString(
                            R.string.description_reminder_status_time,
                            TimeSpan.fromMillis(item.getTimeToDue()).toLocalizedString(requireContext())));
                }
            }

            return convertView;
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
        private ActionMode actionMode;

        public void finishActionMode() {
            if (actionMode != null) {
                actionMode.finish();
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.menu_delete) {
                var message = getString(R.string.alert_delete_reminders_message,
                        getListView().getCheckedItemCount());
                MessageDialogFragment.newInstance(REQUEST_DELETE, R.string.alert_delete_title, message,
                        android.R.string.yes, android.R.string.no)
                        .show(getParentFragmentManager(), null);
                return true;
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            this.actionMode = mode;
            var inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.edit_reminders_cab, menu);
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

    private static final int REQUEST_DELETE = 1;

    private ReminderAdapter reminderAdapter;
    private ReminderMultiChoiceModeListener multiChoiceModeListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RemindersViewModel.class);

        reminderAdapter = new ReminderAdapter();
        multiChoiceModeListener = new ReminderMultiChoiceModeListener();

        getListView().setMultiChoiceModeListener(multiChoiceModeListener);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        setListAdapter(reminderAdapter);

        viewModel.getReminders().observe(getViewLifecycleOwner(), reminders -> reminderAdapter.setItems(reminders));

        getParentFragmentManager().setFragmentResultListener(
                MessageDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
                    int requestCode = result.getInt(MessageDialogFragment.RESULT_REQUEST_CODE);
                    int action = result.getInt(MessageDialogFragment.RESULT_ACTION);
                    if (requestCode == REQUEST_DELETE && action == MessageDialogFragment.ACTION_POSITIVE) {
                        onDialogPositiveClick();
                    }
                });
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        openReminderDetailFragment(id);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.edit_reminders, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_add_reminder) {
            openReminderDetailFragment(AbstractDataDetailFragment.EXTRA_ID_DEFAULT);
            return true;
        }
        return false;
    }

    private void onDialogPositiveClick() {
        var checkedIds = getListView().getCheckedItemIds();
        viewModel.deleteReminders(checkedIds);
        multiChoiceModeListener.finishActionMode();
    }

    @Override
    public void onStop() {
        super.onStop();
        multiChoiceModeListener.finishActionMode();
    }

    private void openReminderDetailFragment(long id) {
        var intent = new Intent(requireActivity(), DataDetailActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        intent.putExtra(DataDetailActivity.EXTRA_EDIT, DataDetailActivity.EXTRA_EDIT_REMINDER);
        intent.putExtra(AbstractDataDetailFragment.EXTRA_ID, id);
        startActivity(intent);
    }
}
