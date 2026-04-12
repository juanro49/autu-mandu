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
package org.juanro.autumandu.gui.util;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;

import org.juanro.autumandu.FuelConsumption;
import org.juanro.autumandu.R;
import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.model.dto.RefuelingWithDetails;

import java.util.List;

/**
 * Utility class to show a snackbar after a new refueling has been added.
 * Optimized with Room DTOs and proper accessibility of fields.
 */
public class NewRefuelingSnackbar {
    public static void show(@NonNull View view, long id) {
        Context context = view.getContext().getApplicationContext();
        FuelConsumption fuelConsumption = new FuelConsumption(context);

        AutuManduDatabase.DB_EXECUTOR.execute(() -> {
            AutuManduDatabase db = AutuManduDatabase.getInstance(context);
            // Usamos la versión síncrona del DAO ya que estamos dentro de DB_EXECUTOR
            RefuelingWithDetails refueling = db.getRefuelingDao().getByIdWithDetails(id);
            if (refueling == null) {
                return;
            }

            List<RefuelingWithDetails> previousRefuelings = db.getRefuelingDao()
                    .getWithDetailsForCarAndCategory(refueling.carId(), refueling.fuelTypeCategory());

            int currentIndex = -1;
            for (int i = 0; i < previousRefuelings.size(); i++) {
                if (previousRefuelings.get(i).id() == id) {
                    currentIndex = i;
                    break;
                }
            }

            if (currentIndex > 0 && !refueling.partial()) {
                float consumption = getFuelConsumptionToPreviousRefueling(fuelConsumption,
                        refueling.volume(), refueling.mileage(), previousRefuelings, currentIndex - 1);
                if (consumption > 0) {
                    String finalConsumptionChange = "";

                    if (currentIndex > 1) {
                        RefuelingWithDetails prevRefueling = previousRefuelings.get(currentIndex - 1);
                        float prevConsumption = getFuelConsumptionToPreviousRefueling(fuelConsumption,
                                prevRefueling.volume(), prevRefueling.mileage(), previousRefuelings, currentIndex - 2);
                        if (prevConsumption > 0) {
                            finalConsumptionChange = prevConsumption > consumption ? "↓" : "↑";
                        }
                    }

                    final String consumptionChange = finalConsumptionChange;
                    view.post(() -> {
                        String message = context.getString(R.string.toast_new_refueling, consumption,
                                fuelConsumption.getUnitLabel(), consumptionChange);
                        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    /**
     * Calculates the fuel consumption for the specified volume and mileage relative to the
     * specified previous refueling. This modifies the previousRefueling cursor. It will have
     * one of the following results:
     * <p/>
     * Calculation was successful:
     * Returns fuel consumption.
     * Calculation failed:
     * Returns -1.
     *
     * @param fuelConsumption   The fuel consumption helper.
     * @param thisVolume        The volume of the current refueling.
     * @param thisMileage       The mileage of the current refueling.
     * @param previousRefuelings A list of all previous refuelings sorted by date descending
     * @return The fuel consumption or -1.
     */
    private static float getFuelConsumptionToPreviousRefueling(@NonNull FuelConsumption fuelConsumption, float thisVolume, int thisMileage, @NonNull List<RefuelingWithDetails> previousRefuelings, int startIndex) {
        float volumeSinceFullRefueling = thisVolume;
        int i = startIndex;
        while (i >= 0 && previousRefuelings.get(i).partial()) {
            volumeSinceFullRefueling += previousRefuelings.get(i).volume();
            i--;
        }

        if (i >= 0) {
            int mileageOfLastFullRefueling = previousRefuelings.get(i).mileage();

            return fuelConsumption.computeFuelConsumption(
                    volumeSinceFullRefueling,
                    thisMileage - mileageOfLastFullRefueling);
        } else {
            return -1;
        }
    }
}
