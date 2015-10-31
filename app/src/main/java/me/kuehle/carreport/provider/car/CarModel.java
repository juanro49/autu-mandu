/*
 * Copyright 2015 Jan Kühle
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
package me.kuehle.carreport.provider.car;

import me.kuehle.carreport.provider.base.BaseModel;

import java.util.Date;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A car.
 */
public interface CarModel extends BaseModel {

    /**
     * Name of the car. Only for display purposes.
     * Cannot be {@code null}.
     */
    @NonNull
    String getName();

    /**
     * Color of the car in android color representation.
     */
    int getColor();

    /**
     * Initial mileage of the car, when it starts to be used in the app.
     */
    int getInitialMileage();

    /**
     * When the car has been suspended, this contains the start date.
     * Can be {@code null}.
     */
    @Nullable
    Date getSuspendedSince();
}
