package me.kuehle.carreport.data.calculation;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.provider.fueltype.FuelTypeCursor;
import me.kuehle.carreport.provider.fueltype.FuelTypeSelection;
import me.kuehle.carreport.provider.refueling.RefuelingCursor;
import me.kuehle.carreport.provider.refueling.RefuelingSelection;
import me.kuehle.carreport.util.Calculator;

public class VolumeToPriceCalculation extends AbstractCalculation {
    public VolumeToPriceCalculation(Context context) {
        super(context);
    }

    @Override
    public String getName() {
        return mContext.getString(R.string.calc_option_volume_to_price,
                getInputUnit(), getOutputUnit());
    }

    @Override
    public String getInputUnit() {
        Preferences prefs = new Preferences(mContext);
        return prefs.getUnitVolume();
    }

    @Override
    public String getOutputUnit() {
        Preferences prefs = new Preferences(mContext);
        return prefs.getUnitCurrency();
    }

    @Override
    public CalculationItem[] calculate(double input) {
        List<CalculationItem> items = new ArrayList<>();
        FuelTypeCursor fuelType = new FuelTypeSelection().query(mContext.getContentResolver());
        while (fuelType.moveToNext()) {
            RefuelingCursor refueling = new RefuelingSelection().fuelTypeId(fuelType.getId()).query(mContext.getContentResolver());
            if (refueling.getCount() > 0) {
                List<Float> fuelPrices = new ArrayList<>(refueling.getCount());
                while (refueling.moveToNext()) {
                    fuelPrices.add(refueling.getPrice() / refueling.getVolume());
                }

                float avgFuelPrice = Calculator.avg(fuelPrices.toArray(new Float[refueling.getCount()]));
                items.add(new CalculationItem(fuelType.getName(), input * avgFuelPrice));
            }
        }

        return items.toArray(new CalculationItem[items.size()]);
    }
}
