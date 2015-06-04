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
package me.kuehle.carreport.provider.fueltype;

import me.kuehle.carreport.provider.base.BaseModel;

import java.util.Date;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * A fuel type.
 */
public interface FuelTypeModel extends BaseModel {

    /**
     * Name of the fuel type, e.g. Diesel.
     * Cannot be {@code null}.
     */
    @NonNull
    String getName();

    /**
     * An optional category like fuel or gas. Fuel types may be grouped by this category in reports.
     * Can be {@code null}.
     */
    @Nullable
    String getCategory();
}
