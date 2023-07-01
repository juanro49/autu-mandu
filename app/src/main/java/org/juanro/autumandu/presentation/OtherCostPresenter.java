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
package org.juanro.autumandu.presentation;

import android.content.Context;

import java.util.List;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dao.OtherCostDAO;

public class OtherCostPresenter {

    private AutuManduDatabase mDB;

    private OtherCostPresenter(Context context){
        mDB = AutuManduDatabase.getInstance(context);
    }

    public static OtherCostPresenter getInstance(Context context) {
        return new OtherCostPresenter(context);
    }

    public String[] getTitles(boolean expenditures) {
        OtherCostDAO ocDAO = mDB.getOtherCostDao();
        List<String> titles;
        if (expenditures) {
            titles = ocDAO.getPositiveCostTitles();
        } else {
            titles = ocDAO.getNegativeCostTitles();
        }
        return titles.toArray(new String[0]);
    }
}
