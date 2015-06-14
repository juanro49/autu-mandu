package me.kuehle.carreport.data.calculation;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;

public class PriceToVolumeCalculation extends AbstractPriceVolumeCalculation {
    public PriceToVolumeCalculation(Context context) {
        super(context);
    }

    @Override
    public String getName() {
        return mContext.getString(R.string.calc_option_price_to_volume,
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
        return prefs.getUnitVolume();
    }

    @Override
    protected CalculationItem[] onCalculate(double input) {
        List<CalculationItem> items = new ArrayList<>();

        for (int i = 0; i < mNames.size(); i++) {
            String name = mNames.get(i);
            double avgFuelPrice = mAvgFuelPrices.get(i);

            items.add(new CalculationItem(name, input / avgFuelPrice));
        }

        return items.toArray(new CalculationItem[items.size()]);
    }
}
