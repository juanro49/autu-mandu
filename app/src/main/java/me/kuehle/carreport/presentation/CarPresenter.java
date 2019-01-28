package me.kuehle.carreport.presentation;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

import me.kuehle.carreport.model.CarReportDatabase;
import me.kuehle.carreport.model.entity.Car;
import me.kuehle.carreport.model.entity.FuelType;
import me.kuehle.carreport.model.entity.OtherCost;
import me.kuehle.carreport.model.entity.Refueling;

public class CarPresenter {

    private Context mContext;
    private CarReportDatabase mDB;

    private CarPresenter(Context context) {
        mContext = context;
        mDB = CarReportDatabase.getInstance(mContext);
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

    public int getLatestMileage(long carId) {
        int latestRefuelingMileage = 0;
        int latestOtherCostMileage = 0;

        Car car = mDB.getCarDao().getById(carId);
        if (car == null) {
            return 0;
        }

        Refueling ref = mDB.getRefuelingDao().getLastForCar(carId);
        latestRefuelingMileage = (ref == null ? 0 : ref.getMileage());

        OtherCost oc = mDB.getOtherCostDao().getLastForCar(carId);
        latestOtherCostMileage = (oc == null ? 0 : oc.getMileage());

        return Math.max(car.getInitialMileage(), Math.max(latestOtherCostMileage, latestRefuelingMileage));
    }

    /**
     * @return The count of all cars.
     */
    public int getCount() {
        return mDB.getCarDao().getAll().size();
    }
}
