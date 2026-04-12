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

package org.juanro.autumandu.util;

import android.content.Context;

import androidx.annotation.NonNull;

import org.juanro.autumandu.model.AutuManduDatabase;

/**
 * Utility class for mileage calculations.
 */
public final class MileageUtil {

    private MileageUtil() {
        // Utility class
    }

    /**
     * Calculates the latest mileage of a car based on its initial mileage,
     * refuelings and other costs.
     *
     * @param context the context to access the database.
     * @param carId   the ID of the car.
     * @return the latest mileage.
     */
    public static int getLatestMileage(@NonNull Context context, long carId) {
        AutuManduDatabase db = AutuManduDatabase.getInstance(context);
        return db.getCarDao().getLatestMileage(carId);
    }
}
