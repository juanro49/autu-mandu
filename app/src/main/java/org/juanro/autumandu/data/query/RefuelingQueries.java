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
package org.juanro.autumandu.data.query;

import android.content.Context;

import java.util.Date;

import org.juanro.autumandu.provider.refueling.RefuelingColumns;
import org.juanro.autumandu.provider.refueling.RefuelingCursor;
import org.juanro.autumandu.provider.refueling.RefuelingSelection;

public class RefuelingQueries {
    public static RefuelingCursor getPrevious(Context context, long carId, Date date) {
        return getPrevious(context, carId, date, null);
    }

    public static RefuelingCursor getPrevious(Context context, long carId, Date date, String fuelTypeCategory) {
        RefuelingSelection sel = new RefuelingSelection()
                .carId(carId)
                .and()
                .dateBefore(date);
        String[] columns = RefuelingColumns.ALL_COLUMNS;

        if (fuelTypeCategory != null) {
            sel.and().fuelTypeCategory(fuelTypeCategory);
            columns = null;
        }

        return sel.query(context.getContentResolver(),
                columns, RefuelingColumns.DATE + " DESC");
    }

    public static RefuelingCursor getNext(Context context, long carId, Date date) {
        return new RefuelingSelection()
                .carId(carId)
                .and()
                .dateAfter(date)
                .query(context.getContentResolver(),
                        RefuelingColumns.ALL_COLUMNS,
                        RefuelingColumns.DATE);
    }
}
