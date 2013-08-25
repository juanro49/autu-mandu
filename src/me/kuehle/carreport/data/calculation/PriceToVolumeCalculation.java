package me.kuehle.carreport.data.calculation;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.FuelType;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.util.Calculator;
import android.content.Context;

public class PriceToVolumeCalculation extends AbstractCalculation {
	public PriceToVolumeCalculation(Context context) {
		super(context);
	}

	@Override
	public String getName() {
		return context.getString(R.string.calc_option_price_to_volume,
				getInputUnit(), getOutputUnit());
	}

	@Override
	public String getInputUnit() {
		Preferences prefs = new Preferences(context);
		return prefs.getUnitCurrency();
	}

	@Override
	public String getOutputUnit() {
		Preferences prefs = new Preferences(context);
		return prefs.getUnitVolume();
	}

	@Override
	public CalculationItem[] calculate(double input) {
		List<CalculationItem> items = new ArrayList<CalculationItem>();
		for (FuelType fuelType : FuelType.getAll()) {
			Vector<Double> fuelPrices = new Vector<Double>();
			for (Refueling refueling : fuelType.refuelings()) {
				fuelPrices.add((double) refueling.getFuelPrice());
			}

			if (fuelPrices.size() > 0) {
				double avgFuelPrice = Calculator.avg(fuelPrices);
				items.add(new CalculationItem(fuelType.name, input
						/ avgFuelPrice));
			}
		}

		return items.toArray(new CalculationItem[items.size()]);
	}
}
