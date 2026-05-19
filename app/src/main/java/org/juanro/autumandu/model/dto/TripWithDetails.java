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

package org.juanro.autumandu.model.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import androidx.room.ColumnInfo;

public record TripWithDetails(
        @ColumnInfo(name = "_id") long id,
        @ColumnInfo(name = "car_id") long carId,
        @ColumnInfo(name = "refueling_id") Long refuelingId,
        LocalDate date,
        @ColumnInfo(name = "time_start") LocalTime timeStart,
        @ColumnInfo(name = "time_end") LocalTime timeEnd,
        @ColumnInfo(name = "route_target") String routeTarget,
        String purpose,
        @ColumnInfo(name = "companies_visited") String companiesVisited,
        String driver,
        Integer occupants,
        String cargo,
        @ColumnInfo(name = "km_start") int kmStart,
        @ColumnInfo(name = "km_end") int kmEnd,
        @ColumnInfo(name = "km_business") int kmBusiness,
        @ColumnInfo(name = "km_private") int kmPrivate,
        @ColumnInfo(name = "km_home_work") int kmHomeWork,
        @ColumnInfo(name = "fuel_liters") Double fuelLiters,
        @ColumnInfo(name = "fuel_cost") Double fuelCost,
        @ColumnInfo(name = "other_costs_description") String otherCostsDescription,
        @ColumnInfo(name = "other_costs_amount") Double otherCostsAmount,
        String carName,
        String stationName,
        Double refuelingPrice,
        Double refuelingVolume
) {
    public Integer getTotalDistance() {
        return kmEnd - kmStart;
    }

    public Double getTotalCost() {
        double total = 0.0;
        if (fuelCost != null) total += fuelCost;
        if (otherCostsAmount != null) total += otherCostsAmount;
        return total;
    }
}
