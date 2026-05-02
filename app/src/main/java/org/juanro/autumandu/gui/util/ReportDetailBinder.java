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

package org.juanro.autumandu.gui.util;

import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.juanro.autumandu.R;
import org.juanro.autumandu.data.report.AbstractReport;

import java.util.List;

/**
 * Helper class to bind report detail rows.
 */
public class ReportDetailBinder {

    private ReportDetailBinder() {
        // Utility class
    }

    public static void bindDetails(ViewGroup detailsContainer, List<AbstractReport.AbstractListItem> reportData) {
        int currentCount = detailsContainer.getChildCount();
        int targetCount = reportData.size();

        for (int i = 0; i < Math.max(currentCount, targetCount); i++) {
            if (i >= targetCount) {
                hideExtraRow(detailsContainer, i);
                continue;
            }

            AbstractReport.AbstractListItem item = reportData.get(i);
            View rowView = getOrCreateRowView(detailsContainer, i, item);
            bindRowView(rowView, item);
        }
    }

    private static void hideExtraRow(ViewGroup container, int index) {
        if (index < container.getChildCount()) {
            container.getChildAt(index).setVisibility(View.GONE);
        }
    }

    private static View getOrCreateRowView(ViewGroup container, int index, AbstractReport.AbstractListItem item) {
        boolean isSection = item instanceof AbstractReport.Section;
        int layout = isSection ? R.layout.report_row_section : R.layout.report_row_data;
        View rowView = index < container.getChildCount() ? container.getChildAt(index) : null;

        boolean needsInflate = rowView == null || (isSection != (rowView instanceof TextView));
        if (needsInflate) {
            if (rowView != null) {
                container.removeViewAt(index);
            }
            rowView = LayoutInflater.from(container.getContext()).inflate(layout, container, false);
            container.addView(rowView, index);
        } else {
            rowView.setVisibility(View.VISIBLE);
        }
        return rowView;
    }

    private static void bindRowView(View rowView, AbstractReport.AbstractListItem item) {
        DetailViewHolder vh = (DetailViewHolder) rowView.getTag();
        if (vh == null) {
            vh = new DetailViewHolder(rowView);
            rowView.setTag(vh);
        }

        if (item instanceof AbstractReport.Section section) {
            bindSection(vh, section);
        } else {
            bindItem(vh, (AbstractReport.Item) item);
        }
    }

    private static void bindSection(DetailViewHolder vh, AbstractReport.Section section) {
        vh.label.setText(section.getLabel());
        vh.label.setTextColor(section.getColor());
        if (vh.sectionDrawable != null) {
            vh.sectionDrawable.setColorFilter(section.getColor(), PorterDuff.Mode.SRC);
        }
    }

    private static void bindItem(DetailViewHolder vh, AbstractReport.Item dataItem) {
        vh.label.setText(dataItem.getLabel());
        if (vh.value != null) {
            vh.value.setText(dataItem.getValue());
        }
    }

    private static class DetailViewHolder {
        final TextView label;
        @Nullable
        final TextView value;
        @Nullable
        final GradientDrawable sectionDrawable;

        DetailViewHolder(View v) {
            label = v.findViewById(android.R.id.text1);
            value = v.findViewById(android.R.id.text2);
            var drawables = label.getCompoundDrawables();
            if (drawables.length > 3 && drawables[3] instanceof GradientDrawable gd) {
                sectionDrawable = gd;
            } else {
                sectionDrawable = null;
            }
        }
    }
}
