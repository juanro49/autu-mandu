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
package org.juanro.autumandu.data.report;

import android.content.Context;
import android.graphics.Color;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.PointValue;

public abstract class AbstractReportChartLineData extends AbstractReportChartData {
    public static class PointValueWithTooltip extends PointValue {
        private String mTooltip;

        public PointValueWithTooltip(float x, float y) {
            super(x, y);
        }

        public String getTooltip() {
            return mTooltip;
        }

        public void setTooltip(String tooltip) {
            mTooltip = tooltip;
        }
    }

    private final Set<Float> mMarkPoints = new HashSet<>();

    public AbstractReportChartLineData(Context context, String name, int color) {
        super(context, name, color);
    }

    public Line getLine() {
        int markColor = Color.argb(63, Color.red(getColor()), Color.green(getColor()),
                Color.blue(getColor()));

        List<PointValue> points = mDataPoints.stream()
                .map(p -> {
                    PointValueWithTooltip point = new PointValueWithTooltip(p.x, p.y);
                    point.setTooltip(p.tooltip);
                    if (mMarkPoints.contains(p.x)) {
                        point.setColor(markColor);
                    }
                    return (PointValue) point;
                })
                .collect(Collectors.toList());

        Line line = new Line(points);
        line.setColor(getColor());
        return line;
    }

    protected void add(Float x, Float y, String tooltip, boolean marked) {
        super.add(x, y, tooltip);
        if (marked) {
            mMarkPoints.add(x);
        }
    }

    protected void set(int index, Float x, Float y, String tooltip, boolean marked) {
        if (index >= 0 && index < mDataPoints.size()) {
            mMarkPoints.remove(mDataPoints.get(index).x);
        }

        super.set(index, x, y, tooltip);
        if (marked) {
            mMarkPoints.add(x);
        }
    }
}
