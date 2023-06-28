package me.kuehle.carreport.presentation;

import android.content.Context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import me.kuehle.carreport.model.CarReportDatabase;
import me.kuehle.carreport.model.entity.Car;
import me.kuehle.carreport.model.entity.FuelType;
import me.kuehle.carreport.model.entity.OtherCost;
import me.kuehle.carreport.model.entity.Refueling;
import me.kuehle.carreport.model.entity.Station;

public class CarPresenter {

    private CarReportDatabase mDB;

    private CarPresenter(Context context) {
        mDB = CarReportDatabase.getInstance(context);
    }

    /**
     * Singleton creation method. Uses the parameters only if no instance exists.
     * @param context Context for accessing data.
     * @return The presenter for cars.
     */
    public static CarPresenter getInstance(Context context) {
        return new CarPresenter(context);
    }

    public Set<String> getUsedFuelTypeCategories(long carId) {
        Set<String> categories = new HashSet<>();

        for (FuelType ft: mDB.getFuelTypeDao().getFuelTypesForCar(carId)) {
            categories.add(ft.getCategory());
        }

        return categories;
    }

    public Map<String, Double> getUsedStations(long carId) {
        Map<String, Double> stations = new HashMap<>();
        for (Station s: mDB.getStationDao().getUsedForCar(carId)) {
            double volume = mDB.getStationDao().getVolumeForStationAndCar(carId, s.getId());
            stations.put(s.getName(), volume);
        }

        return stations;
    }

    public int getLatestMileage(long carId) {
        Car car = mDB.getCarDao().getById(carId);
        if (car == null) {
            return 0;
        }

        Refueling ref = mDB.getRefuelingDao().getLastForCar(carId);
        int latestRefuelingMileage = (ref == null ? 0 : ref.getMileage());

        OtherCost oc = mDB.getOtherCostDao().getLastForCar(carId);
        int latestOtherCostMileage = (oc == null ? 0 : oc.getMileage());

        return Math.max(car.getInitialMileage(), Math.max(latestOtherCostMileage, latestRefuelingMileage));
    }

    /**
     * @return The count of all cars.
     */
    public int getCount() {
        return mDB.getCarDao().getAll().size();
    }
}
