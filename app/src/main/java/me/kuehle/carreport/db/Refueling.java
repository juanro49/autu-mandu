/*
 * Copyright 2013 Jan Kühle
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

package me.kuehle.carreport.db;

import android.support.annotation.NonNull;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Column.ForeignKeyAction;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.Date;

@Table(name = "refuelings")
public class Refueling extends Model implements Comparable<Refueling> {
    @Column(name = "date", notNull = true)
    public Date date;

    @Column(name = "mileage", notNull = true)
    public int mileage;

    @Column(name = "volume", notNull = true)
    public float volume;

    @Column(name = "price", notNull = true)
    public float price;

    @Column(name = "partial", notNull = true)
    public boolean partial;

    @Column(name = "note", notNull = true)
    public String note;

    @Column(name = "fuel_type", notNull = true, onUpdate = ForeignKeyAction.CASCADE, onDelete = ForeignKeyAction.CASCADE)
    public FuelType fuelType;

    @Column(name = "car", notNull = true, onUpdate = ForeignKeyAction.CASCADE, onDelete = ForeignKeyAction.CASCADE)
    public Car car;

    public boolean guessed = false;

    public boolean valid = true;

    public Refueling() {
        super();
    }

    public Refueling(Date date, int mileage, float volume, float price,
                     boolean partial, String note, FuelType fuelType, Car car) {
        super();
        this.date = date;
        this.mileage = mileage;
        this.volume = volume;
        this.price = price;
        this.partial = partial;
        this.note = note;
        this.fuelType = fuelType;
        this.car = car;
    }

    @Override
    public int compareTo(@NonNull Refueling another) {
        return date.compareTo(another.date);
    }

    public float getFuelPrice() {
        return price / volume;
    }

    public static Refueling getPrevious(Car car, Date date) {
        return new Select().from(Refueling.class)
                .where("car = ? AND date < ?", car.id, date.getTime())
                .orderBy("date DESC")
                .executeSingle();
    }

    public static Refueling getNext(Car car, Date date) {
        return new Select().from(Refueling.class)
                .where("car = ? AND date > ?", car.id, date.getTime())
                .orderBy("date ASC")
                .executeSingle();
    }
}
