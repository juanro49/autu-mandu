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
package me.kuehle.carreport.data.query;

import android.content.Context;

import java.util.Date;

import me.kuehle.carreport.provider.refueling.RefuelingColumns;
import me.kuehle.carreport.provider.refueling.RefuelingCursor;
import me.kuehle.carreport.provider.refueling.RefuelingSelection;

public class RefuelingQueries {
    public static RefuelingCursor getPrevious(Context context, long carId, Date date) {
        return new RefuelingSelection()
                .carId(carId)
                .and()
                .dateBefore(date)
                .query(context.getContentResolver(),
                        RefuelingColumns.ALL_COLUMNS,
                        RefuelingColumns.DATE + " DESC");
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
