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

    public static void bindDetails(ViewGroup detailsContainer, List<AbstractReport.AbstractListItem> reportData) {
        int currentCount = detailsContainer.getChildCount();
        int targetCount = reportData.size();

        for (int i = 0; i < Math.max(currentCount, targetCount); i++) {
            if (i >= targetCount) {
                if (i < detailsContainer.getChildCount()) detailsContainer.getChildAt(i).setVisibility(View.GONE);
                continue;
            }
            var item = reportData.get(i);
            boolean isSection = item instanceof AbstractReport.Section;
            int layout = isSection ? R.layout.report_row_section : R.layout.report_row_data;
            View rowView = i < currentCount ? detailsContainer.getChildAt(i) : null;
            boolean needsInflate = rowView == null || (isSection != (rowView instanceof TextView));
            if (needsInflate) {
                if (rowView != null) detailsContainer.removeViewAt(i);
                rowView = LayoutInflater.from(detailsContainer.getContext()).inflate(layout, detailsContainer, false);
                detailsContainer.addView(rowView, i);
            } else {
                rowView.setVisibility(View.VISIBLE);
            }
            DetailViewHolder vh = (DetailViewHolder) rowView.getTag();
            if (vh == null) {
                vh = new DetailViewHolder(rowView);
                rowView.setTag(vh);
            }
            if (item instanceof AbstractReport.Section section) {
                vh.label.setText(section.getLabel());
                vh.label.setTextColor(section.getColor());
                if (vh.sectionDrawable != null) vh.sectionDrawable.setColorFilter(section.getColor(), PorterDuff.Mode.SRC);
            } else {
                var dataItem = (AbstractReport.Item) item;
                vh.label.setText(dataItem.getLabel());
                if (vh.value != null) vh.value.setText(dataItem.getValue());
            }
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
