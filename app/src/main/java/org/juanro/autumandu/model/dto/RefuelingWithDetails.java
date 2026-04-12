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

import java.util.Date;

public record RefuelingWithDetails(
        long id,
        Date date,
        int mileage,
        float volume,
        float price,
        boolean partial,
        String note,
        long fuelTypeId,
        long stationId,
        long carId,
        String fuelTypeName,
        String fuelTypeCategory,
        String stationName,
        String carName,
        int carColor,
        int carInitialMileage,
        Date carSuspendedSince,
        double carBuyingPrice,
        int carNumTires
) {
}
