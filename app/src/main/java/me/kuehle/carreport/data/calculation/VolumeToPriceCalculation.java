package me.kuehle.carreport.data.calculation;

import java.util.ArrayList;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.FuelType;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.util.Calculator;
import android.content.Context;

public class VolumeToPriceCalculation extends AbstractCalculation {
	public VolumeToPriceCalculation(Context context) {
		super(context);
	}

	@Override
	public String getName() {
		return context.getString(R.string.calc_option_volume_to_price,
				getInputUnit(), getOutputUnit());
	}

	@Override
	public String getInputUnit() {
		Preferences prefs = new Preferences(context);
		return prefs.getUnitVolume();
	}

	@Override
	public String getOutputUnit() {
		Preferences prefs = new Preferences(context);
		return prefs.getUnitCurrency();
	}

	@Override
	public CalculationItem[] calculate(double input) {
		List<CalculationItem> items = new ArrayList<CalculationItem>();
		for (FuelType fuelType : FuelType.getAll()) {
			List<Refueling> refuelings = fuelType.refuelings();
			if (refuelings.size() > 0) {
				float[] fuelPrices = new float[refuelings.size()];
				for (int i = 0; i < fuelPrices.length; i++) {
					fuelPrices[i] = refuelings.get(i).getFuelPrice();
				}
				
				float avgFuelPrice = Calculator.avg(fuelPrices);
				items.add(new CalculationItem(fuelType.name, input
						/ avgFuelPrice));
			}
		}

		return items.toArray(new CalculationItem[items.size()]);
	}
}