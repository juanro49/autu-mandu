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

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractReportChartLineData extends AbstractReportChartData {
    private final Set<Float> mMarkPoints = new HashSet<>();

    public AbstractReportChartLineData(Context context, String name, int color) {
        super(context, name, color);
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

    public boolean isMarked(Float x) {
        return mMarkPoints.contains(x);
    }
}
