/*
 * Copyright 2026 Juanro49
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.DataDetailActivity;
import org.juanro.autumandu.model.entity.Reminder;
import org.juanro.autumandu.model.dto.ReminderWithCar;
import org.juanro.autumandu.util.TimeSpan;
import org.juanro.autumandu.viewmodel.RemindersViewModel;

import java.util.Locale;
import java.util.Objects;

public class ReminderListFragment extends Fragment {

    private RemindersViewModel viewModel;
    private ReminderAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_reminder_list, container, false);

        RecyclerView recyclerView = v.findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReminderAdapter();
        recyclerView.setAdapter(adapter);

        v.findViewById(R.id.fab_add_reminder).setOnClickListener(view -> openReminderDetail(AbstractDataDetailFragment.EXTRA_ID_DEFAULT));

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(view.findViewById(R.id.toolbar));

        viewModel = new ViewModelProvider(this).get(RemindersViewModel.class);
        viewModel.getReminders().observe(getViewLifecycleOwner(), reminders -> adapter.submitList(reminders));
    }

    private void openReminderDetail(long id) {
        Intent intent = new Intent(requireActivity(), DataDetailActivity.class);
        intent.putExtra(DataDetailActivity.EXTRA_EDIT, DataDetailActivity.EXTRA_EDIT_REMINDER);
        intent.putExtra(AbstractDataDetailFragment.EXTRA_ID, id);
        startActivity(intent);
    }

    private class ReminderAdapter extends ListAdapter<ReminderWithCar, ReminderViewHolder> {

        public ReminderAdapter() {
            super(new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull ReminderWithCar oldItem, @NonNull ReminderWithCar newItem) {
                    return Objects.equals(oldItem.reminder().getId(), newItem.reminder().getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ReminderWithCar oldItem, @NonNull ReminderWithCar newItem) {
                    Reminder r1 = oldItem.reminder();
                    Reminder r2 = newItem.reminder();
                    return Objects.equals(r1.getTitle(), r2.getTitle()) &&
                            Objects.equals(r1.getAfterDistance(), r2.getAfterDistance()) &&
                            Objects.equals(r1.getAfterTimeSpanUnit(), r2.getAfterTimeSpanUnit()) &&
                            Objects.equals(r1.getAfterTimeSpanCount(), r2.getAfterTimeSpanCount()) &&
                            Objects.equals(r1.getStartDate(), r2.getStartDate()) &&
                            r1.getStartMileage() == r2.getStartMileage() &&
                            r1.isNotificationDismissed() == r2.isNotificationDismissed() &&
                            Objects.equals(r1.getSnoozedUntil(), r2.getSnoozedUntil()) &&
                            r1.getCarId() == r2.getCarId() &&
                            oldItem.isDue() == newItem.isDue() &&
                            oldItem.isSnoozed() == newItem.isSnoozed();
                }
            });
        }

        @Override
        public ReminderWithCar getItem(int position) {
            return super.getItem(position);
        }

        @NonNull
        @Override
        public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_reminder, parent, false);
            return new ReminderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
            holder.bind(getItem(position));
        }
    }

    private class ReminderViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView car;
        private final TextView afterDistance;
        private final TextView afterTime;
        private final TextView status;
        private final ImageButton btnDone;
        private final ImageButton btnSnooze;

        private final java.text.DateFormat dateFormat;
        private final String unitDistance;

        public ReminderViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txt_title);
            car = itemView.findViewById(R.id.txt_car);
            afterDistance = itemView.findViewById(R.id.txt_after_distance);
            afterTime = itemView.findViewById(R.id.txt_after_time);
            status = itemView.findViewById(R.id.txt_status);
            btnDone = itemView.findViewById(R.id.btn_done);
            btnSnooze = itemView.findViewById(R.id.btn_snooze);

            dateFormat = DateFormat.getDateFormat(itemView.getContext());
            unitDistance = new Preferences(itemView.getContext()).getUnitDistance();

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    openReminderDetail(adapter.getItem(position).reminder().getId());
                }
            });
        }

        public void bind(ReminderWithCar item) {
            Context context = itemView.getContext();
            title.setText(item.reminder().getTitle());
            car.setText(item.carName());

            btnDone.setOnClickListener(v -> viewModel.markAsDone(item.reminder().getId()));
            btnSnooze.setOnClickListener(v -> viewModel.snooze(item.reminder().getId()));

            if (item.reminder().getAfterDistance() != null) {
                afterDistance.setText(String.format(Locale.getDefault(), "%d %s", item.reminder().getAfterDistance(), unitDistance));
                afterDistance.setVisibility(View.VISIBLE);
            } else {
                afterDistance.setVisibility(View.GONE);
            }

            if (item.reminder().getAfterTimeSpanUnit() != null) {
                Integer count = item.reminder().getAfterTimeSpanCount();
                TimeSpan span = new TimeSpan(item.reminder().getAfterTimeSpanUnit(),
                        count == null ? 1 : count);
                afterTime.setText(span.toLocalizedString(context));
                afterTime.setVisibility(View.VISIBLE);
            } else {
                afterTime.setVisibility(View.GONE);
            }

            if (item.isDue()) {
                status.setTextColor(MaterialColors.getColor(status, R.attr.colorError));
                if (item.reminder().isNotificationDismissed()) {
                    status.setText(R.string.description_reminder_status_due_dismissed);
                } else if (item.isSnoozed()) {
                    java.util.Date snoozedUntil = item.reminder().getSnoozedUntil();
                    if (snoozedUntil != null) {
                        status.setText(context.getString(R.string.description_reminder_status_due_snoozed, dateFormat.format(snoozedUntil)));
                    } else {
                        status.setText(R.string.description_reminder_status_due);
                    }
                } else {
                    status.setText(R.string.description_reminder_status_due);
                }
            } else {
                status.setTextColor(MaterialColors.getColor(status, R.attr.colorOnSurfaceVariant));
                if (item.reminder().getAfterDistance() != null && item.reminder().getAfterTimeSpanUnit() != null) {
                    status.setText(context.getString(R.string.description_reminder_status_distance_and_time,
                            String.format(Locale.getDefault(), "%d %s", item.getDistanceToDue(), unitDistance),
                            TimeSpan.fromMillis(item.getTimeToDue()).toLocalizedString(context)));
                } else if (item.reminder().getAfterDistance() != null) {
                    status.setText(context.getString(R.string.description_reminder_status_distance,
                            String.format(Locale.getDefault(), "%d %s", item.getDistanceToDue(), unitDistance)));
                } else {
                    status.setText(context.getString(R.string.description_reminder_status_time,
                            TimeSpan.fromMillis(item.getTimeToDue()).toLocalizedString(context)));
                }
            }
        }
    }
}
