package me.kuehle.carreport.data.calculation;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.balancing.BalancedRefuelingCursor;
import me.kuehle.carreport.data.balancing.RefuelingBalancer;
import me.kuehle.carreport.provider.car.CarCursor;
import me.kuehle.carreport.provider.car.CarSelection;
import me.kuehle.carreport.provider.othercost.OtherCostColumns;
import me.kuehle.carreport.provider.othercost.OtherCostCursor;
import me.kuehle.carreport.provider.othercost.OtherCostSelection;
import me.kuehle.carreport.util.Recurrences;

public class PriceToDistanceCalculation extends AbstractCalculation {
    public PriceToDistanceCalculation(Context context) {
        super(context);
    }

    @Override
    public String getName() {
        return mContext.getString(R.string.calc_option_price_to_distance,
                getInputUnit(), getOutputUnit());
    }

    @Override
    public String getInputUnit() {
        Preferences prefs = new Preferences(mContext);
        return prefs.getUnitCurrency();
    }

    @Override
    public String getOutputUnit() {
        Preferences prefs = new Preferences(mContext);
        return prefs.getUnitDistance();
    }

    @Override
    public CalculationItem[] calculate(double input) {
        List<CalculationItem> items = new ArrayList<>();

        CarCursor car = new CarSelection().query(mContext.getContentResolver());
        while (car.moveToNext()) {
            double totalCosts = 0;
            int startMileage = -1;
            int endMileage = -1;

            RefuelingBalancer balancer = new RefuelingBalancer(mContext);
            BalancedRefuelingCursor refueling = balancer.getBalancedRefuelings(car.getId());
            while (refueling.moveToNext()) {
                if (startMileage == -1) {
                    if (!refueling.getPartial()) {
                        startMileage = refueling.getMileage();
                    }

                    continue;
                }

                totalCosts += refueling.getPrice();
                endMileage = refueling.getMileage();
            }

            OtherCostCursor otherCost = new OtherCostSelection().carId(car.getId()).query(mContext.getContentResolver(),
                    OtherCostColumns.ALL_COLUMNS);
            while (otherCost.moveToNext()) {
                int recurrences;
                if (otherCost.getEndDate() == null) {
                    recurrences = Recurrences.getRecurrencesSince(otherCost.getRecurrenceInterval(),
                            otherCost.getRecurrenceMultiplier(), otherCost.getDate());
                } else {
                    recurrences = Recurrences.getRecurrencesBetween(otherCost.getRecurrenceInterval(),
                            otherCost.getRecurrenceMultiplier(), otherCost.getDate(), otherCost.getEndDate());
                }

                totalCosts += otherCost.getPrice() * recurrences;

                if (otherCost.getMileage() > -1) {
                    startMileage = Math.min(startMileage, otherCost.getMileage());
                    endMileage = Math.max(endMileage, otherCost.getMileage());
                }
            }

            if (totalCosts > 0 && startMileage > -1 && endMileage > startMileage) {
                double avgDistancePrice = totalCosts / (endMileage - startMileage);
                items.add(new CalculationItem(car.getName(), input / avgDistancePrice));
            }
        }

        return items.toArray(new CalculationItem[items.size()]);
    }
}
