package me.kuehle.carreport.data.calculation;

import android.content.Context;

public abstract class AbstractCalculation {
	protected Context context;

	public AbstractCalculation(Context context) {
		this.context = context;
	}

	public abstract String getName();

	public abstract String getInputUnit();

	public abstract String getOutputUnit();

	public abstract CalculationItem[] calculate(double input);
}
