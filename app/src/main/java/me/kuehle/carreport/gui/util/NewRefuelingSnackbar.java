/*
 * Copyright 2016 Jan Kühle
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
package me.kuehle.carreport.gui.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;

import me.kuehle.carreport.FuelConsumption;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.query.RefuelingQueries;
import me.kuehle.carreport.provider.refueling.RefuelingCursor;
import me.kuehle.carreport.provider.refueling.RefuelingSelection;

public class NewRefuelingSnackbar {
    public static void show(@NonNull View view, long id) {
        Context context = view.getContext();
        FuelConsumption fuelConsumption = new FuelConsumption(context);

        RefuelingCursor refueling = new RefuelingSelection().id(id).query(
                context.getContentResolver());
        if (!refueling.moveToFirst()) {
            return;
        }

        RefuelingCursor previousRefueling = RefuelingQueries.getPrevious(context,
                refueling.getCarId(), refueling.getDate());
        if (previousRefueling.moveToFirst() && !refueling.getPartial()) {
            float consumption = getFuelConsumptionToPreviousRefueling(fuelConsumption,
                    refueling.getVolume(), refueling.getMileage(), previousRefueling);
            if (consumption > 0) {
                String consumptionChange = "";
                if (!previousRefueling.isAfterLast()) {
                    float prevVolume = previousRefueling.getVolume();
                    int prevMileage = previousRefueling.getMileage();
                    previousRefueling.moveToNext();

                    float prevConsumption = getFuelConsumptionToPreviousRefueling(fuelConsumption,
                            prevVolume, prevMileage, previousRefueling);
                    if (prevConsumption > 0) {
                        consumptionChange = prevConsumption > consumption ? "\u2193" : "\u2191";
                    }
                }

                String message = context.getString(R.string.toast_new_refueling, consumption,
                        fuelConsumption.getUnitLabel(), consumptionChange);
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Calculates the fuel consumption for the specified volume and mileage relative to the
     * specified previous refueling. This modifies the previousRefueling cursor. It will have
     * one of the following results:
     * <p/>
     * Calculation was successfull:
     * Returns fuel consumption.
     * Cursor previousRefueling points to the previous full refueling.
     * Calculation failed:
     * Returns -1.
     * Cursor previousRefueling points after the last entry.
     *
     * @param fuelConsumption   The fuel consumption helper.
     * @param thisVolume        The volume of the current refueling.
     * @param thisMileage       The mileage of the current refueling.
     * @param previousRefueling A cursor of all previous refuelings sorted by date descending and
     *                          currently pointing to the next refueling relative to the current.
     * @return The fuel consumption or -1.
     */
    private static float getFuelConsumptionToPreviousRefueling(@NonNull FuelConsumption fuelConsumption, float thisVolume, int thisMileage, @NonNull RefuelingCursor previousRefueling) {
        float volumeSinceFullRefueling = thisVolume;
        while (!previousRefueling.isAfterLast() && previousRefueling.getPartial()) {
            volumeSinceFullRefueling += previousRefueling.getVolume();
            previousRefueling.moveToNext();
        }

        if (!previousRefueling.isAfterLast()) {
            int mileageOfLastFullRefueling = previousRefueling.getMileage();

            return fuelConsumption.computeFuelConsumption(
                    volumeSinceFullRefueling,
                    thisMileage - mileageOfLastFullRefueling);
        } else {
            return -1;
        }
    }
}
