package me.kuehle.carreport.data.calculation;

public class CalculationItem {
	private String name;
	private double value;

	public CalculationItem(String name, double value) {
		super();
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public double getValue() {
		return value;
	}
}