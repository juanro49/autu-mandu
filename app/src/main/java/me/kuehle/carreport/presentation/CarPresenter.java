package me.kuehle.carreport.presentation;

import android.content.Context;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.kuehle.carreport.provider.car.CarColumns;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.provider.fueltype.FuelTypeColumns;
import me.kuehle.carreport.provider.othercost.OtherCostColumns;
import me.kuehle.carreport.provider.othercost.OtherCostCursor;
import me.kuehle.carreport.provider.othercost.OtherCostSelection;
import me.kuehle.carreport.provider.refueling.RefuelingColumns;
import me.kuehle.carreport.provider.refueling.RefuelingCursor;
import me.kuehle.carreport.provider.refueling.RefuelingSelection;

public class CarPresenter {

    private Context mContext;

    private CarPresenter(Context context) {
        mContext = context;
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

        RefuelingCursor refueling = new RefuelingSelection().carId(carId).query(
                mContext.getContentResolver(),
                new String[]{FuelTypeColumns.CATEGORY},
                FuelTypeColumns.CATEGORY + " COLLATE UNICODE ASC");
        while (refueling.moveToNext()) {
            categories.add(refueling.getFuelTypeCategory());
        }

        return categories;
    }

    public int getLatestMileage(long carId) {
        int latestRefuelingMileage = 0;
        int latestOtherCostMileage = 0;

        CarCursor car = new CarSelection().id(carId).query(mContext.getContentResolver());
        car.moveToNext();

        RefuelingCursor refueling = new RefuelingSelection().carId(carId).limit(1).query(
                mContext.getContentResolver(),
                new String[]{RefuelingColumns.MILEAGE},
                RefuelingColumns.MILEAGE + " DESC");
        if (refueling.moveToNext()) {
            latestRefuelingMileage = refueling.getMileage();
        }

        OtherCostCursor otherCost = new OtherCostSelection().carId(carId).limit(1).query(
                mContext.getContentResolver(),
                new String[]{OtherCostColumns.MILEAGE},
                OtherCostColumns.MILEAGE + " DESC");
        if (otherCost.moveToNext() && otherCost.getMileage() != null) {
            latestOtherCostMileage = otherCost.getMileage();
        }

        return Math.max(car.getInitialMileage(), Math.max(latestOtherCostMileage, latestRefuelingMileage));
    }

    /**
     * @return The count of all cars.
     */
    public int getCount() {
        CarCursor cursor = new CarSelection().query(mContext.getContentResolver(), new String[]{CarColumns._ID});
        return cursor.getCount();
    }
}
