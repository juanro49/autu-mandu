package me.kuehle.carreport.presentation;

import android.content.Context;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.kuehle.carreport.model.CarReportDatabase;
import me.kuehle.carreport.model.dao.CarDAO;
import me.kuehle.carreport.model.dao.CarDAO_Impl;
import me.kuehle.carreport.model.dao.FuelTypeDAO;
import me.kuehle.carreport.model.dao.RefuelingDAO;
import me.kuehle.carreport.model.entity.Car;
import me.kuehle.carreport.model.entity.OtherCost;
import me.kuehle.carreport.model.entity.Refueling;
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

        Set<Long> fueltypeIds = new HashSet<>();
        for (Refueling ref: mDB.getRefuelingDao().getAllForCar(carId)) {
            fueltypeIds.add(ref.getFuelTypeId());
        }

        FuelTypeDAO ftDAO = mDB.getFuelTypeDao();
        for (long ftId: fueltypeIds) {
            categories.add(ftDAO.getById(ftId).getCategory());
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
