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

package org.juanro.autumandu.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "trip",
    foreignKeys = {
        @ForeignKey(
            entity = Car.class,
            parentColumns = "_id",
            childColumns = "car_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Refueling.class,
            parentColumns = "_id",
            childColumns = "refueling_id",
            onDelete = ForeignKey.SET_NULL
        )
    },
    indices = {
        @Index("car_id"),
        @Index("date"),
        @Index("refueling_id"),
        @Index(value = {"car_id", "date"}, name = "idx_car_date")
    }
)
public class Trip {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private long id;

    @ColumnInfo(name = "car_id")
    private long carId;

    @ColumnInfo(name = "refueling_id")
    private Long refuelingId;

    // === FECHA Y HORA (Obligatorios) ===
    @NonNull
    @ColumnInfo(name = "date")
    private LocalDate date;

    @NonNull
    @ColumnInfo(name = "date_end")
    private LocalDate dateEnd;

    @NonNull
    @ColumnInfo(name = "time_start")
    private LocalTime timeStart;

    @NonNull
    @ColumnInfo(name = "time_end")
    private LocalTime timeEnd;

    // === RUTA Y PROPÓSITO (Obligatorios) ===
    @NonNull
    @ColumnInfo(name = "route_target")
    private String routeTarget = "";

    @NonNull
    @ColumnInfo(name = "purpose")
    private String purpose = "";

    // === INFORMACIÓN ADICIONAL (Opcional) ===
    @ColumnInfo(name = "companies_visited")
    private String companiesVisited;

    @ColumnInfo(name = "driver")
    private String driver;

    @ColumnInfo(name = "occupants")
    private Integer occupants;

    @ColumnInfo(name = "cargo")
    private String cargo;

    // === ODÓMETRO (Obligatorios) ===
    @ColumnInfo(name = "km_start")
    private int kmStart;

    @ColumnInfo(name = "km_end")
    private int kmEnd;

    // === CATEGORIZACIÓN DE KM (Obligatorios para fiscal) ===
    @ColumnInfo(name = "km_business")
    private int kmBusiness = 0;

    @ColumnInfo(name = "km_private")
    private int kmPrivate = 0;

    @ColumnInfo(name = "km_home_work")
    private int kmHomeWork = 0;

    // === GEOLOCALIZACIÓN (Opcional) ===
    @ColumnInfo(name = "start_lat")
    private Double startLat;

    @ColumnInfo(name = "start_lon")
    private Double startLon;

    @ColumnInfo(name = "end_lat")
    private Double endLat;

    @ColumnInfo(name = "end_lon")
    private Double endLon;

    // === COSTOS DURANTE VIAJE (Opcionales) ===
    @ColumnInfo(name = "fuel_liters")
    private Double fuelLiters;

    @ColumnInfo(name = "fuel_cost")
    private Double fuelCost;

    @ColumnInfo(name = "other_costs_description")
    private String otherCostsDescription;

    @ColumnInfo(name = "other_costs_amount")
    private Double otherCostsAmount;

    // === AUDITORÍA (Sistema) ===
    @NonNull
    @ColumnInfo(name = "created_at")
    private LocalDateTime createdAt;

    @NonNull
    @ColumnInfo(name = "updated_at")
    private LocalDateTime updatedAt;

    public Trip() {
        this.date = LocalDate.now();
        this.dateEnd = LocalDate.now();
        this.timeStart = LocalTime.now();
        this.timeEnd = LocalTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCarId() {
        return carId;
    }

    public void setCarId(long carId) {
        this.carId = carId;
    }

    public Long getRefuelingId() {
        return refuelingId;
    }

    public void setRefuelingId(Long refuelingId) {
        this.refuelingId = refuelingId;
    }

    @NonNull
    public LocalDate getDate() {
        return date;
    }

    public void setDate(@NonNull LocalDate date) {
        this.date = date;
    }

    @NonNull
    public LocalDate getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(@NonNull LocalDate dateEnd) {
        this.dateEnd = dateEnd;
    }

    @NonNull
    public LocalTime getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(@NonNull LocalTime timeStart) {
        this.timeStart = timeStart;
    }

    @NonNull
    public LocalTime getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(@NonNull LocalTime timeEnd) {
        this.timeEnd = timeEnd;
    }

    @NonNull
    public String getRouteTarget() {
        return routeTarget;
    }

    public void setRouteTarget(@NonNull String routeTarget) {
        this.routeTarget = routeTarget;
    }

    @NonNull
    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(@NonNull String purpose) {
        this.purpose = purpose;
    }

    public String getCompaniesVisited() {
        return companiesVisited;
    }

    public void setCompaniesVisited(String companiesVisited) {
        this.companiesVisited = companiesVisited;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public Integer getOccupants() {
        return occupants;
    }

    public void setOccupants(Integer occupants) {
        this.occupants = occupants;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public int getKmStart() {
        return kmStart;
    }

    public void setKmStart(int kmStart) {
        this.kmStart = kmStart;
    }

    public int getKmEnd() {
        return kmEnd;
    }

    public void setKmEnd(int kmEnd) {
        this.kmEnd = kmEnd;
    }

    public int getKmBusiness() {
        return kmBusiness;
    }

    public void setKmBusiness(int kmBusiness) {
        this.kmBusiness = kmBusiness;
    }

    public int getKmPrivate() {
        return kmPrivate;
    }

    public void setKmPrivate(int kmPrivate) {
        this.kmPrivate = kmPrivate;
    }

    public int getKmHomeWork() {
        return kmHomeWork;
    }

    public void setKmHomeWork(int kmHomeWork) {
        this.kmHomeWork = kmHomeWork;
    }

    public Double getStartLat() {
        return startLat;
    }

    public void setStartLat(Double startLat) {
        this.startLat = startLat;
    }

    public Double getStartLon() {
        return startLon;
    }

    public void setStartLon(Double startLon) {
        this.startLon = startLon;
    }

    public Double getEndLat() {
        return endLat;
    }

    public void setEndLat(Double endLat) {
        this.endLat = endLat;
    }

    public Double getEndLon() {
        return endLon;
    }

    public void setEndLon(Double endLon) {
        this.endLon = endLon;
    }

    public Double getFuelLiters() {
        return fuelLiters;
    }

    public void setFuelLiters(Double fuelLiters) {
        this.fuelLiters = fuelLiters;
    }

    public Double getFuelCost() {
        return fuelCost;
    }

    public void setFuelCost(Double fuelCost) {
        this.fuelCost = fuelCost;
    }

    public String getOtherCostsDescription() {
        return otherCostsDescription;
    }

    public void setOtherCostsDescription(String otherCostsDescription) {
        this.otherCostsDescription = otherCostsDescription;
    }

    public Double getOtherCostsAmount() {
        return otherCostsAmount;
    }

    public void setOtherCostsAmount(Double otherCostsAmount) {
        this.otherCostsAmount = otherCostsAmount;
    }

    @NonNull
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NonNull LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @NonNull
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(@NonNull LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

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
