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
package me.kuehle.carreport.data.report;

import android.content.Context;

import lecho.lib.hellocharts.model.SubcolumnValue;

public abstract class AbstractReportChartColumnData extends AbstractReportChartData {
    public static class SubcolumnValueWithTooltip extends SubcolumnValue {
        private String mTooltip;

        public SubcolumnValueWithTooltip(float value, int color) {
            super(value, color);
        }

        public String getTooltip() {
            return mTooltip;
        }

        public void setTooltip(String tooltip) {
            mTooltip = tooltip;
        }
    }

    public AbstractReportChartColumnData(Context context, String name, int color) {
        super(context, name, color);
    }

    public SubcolumnValue getSubcolumnValue(Float x) {
        int index = indexOf(x);
        if (index > -1) {
            Float y = getYValues().get(index);

            SubcolumnValueWithTooltip value = new SubcolumnValueWithTooltip(y, getColor());
            if (getTooltips().size() > index) {
                value.setTooltip(getTooltips().get(index));
            }

            return value;
        } else {
            return null;
        }
    }
}
