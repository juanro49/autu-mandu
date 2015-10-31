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

import java.util.HashSet;
import java.util.Set;

import me.kuehle.carreport.provider.othercost.OtherCostColumns;
import me.kuehle.carreport.provider.othercost.OtherCostCursor;
import me.kuehle.carreport.provider.othercost.OtherCostSelection;

public class OtherCostQueries {
    public static String[] getTitles(Context context, boolean expenditures) {
        Set<String> titles = new HashSet<>();

        OtherCostSelection otherCostTitleQuery = new OtherCostSelection();
        if (expenditures) {
            otherCostTitleQuery.priceGt(0);
        } else {
            otherCostTitleQuery.priceLt(0);
        }

        OtherCostCursor otherCost = otherCostTitleQuery.query(context.getContentResolver(),
                new String[]{OtherCostColumns.TITLE},
                OtherCostColumns.TITLE + " COLLATE UNICODE ASC");
        while (otherCost.moveToNext()) {
            titles.add(otherCost.getTitle());
        }

        return titles.toArray(new String[titles.size()]);
    }
}
