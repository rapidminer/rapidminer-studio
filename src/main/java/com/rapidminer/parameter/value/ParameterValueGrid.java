/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package com.rapidminer.parameter.value;

import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;


/**
 * A grid of numerical parameter values.
 *
 * @author Tobias Malbrecht
 */
public class ParameterValueGrid extends ParameterValues {

	public static final int SCALE_LINEAR = 0;

	public static final int SCALE_QUADRATIC = 1;

	public static final int SCALE_LOGARITHMIC = 2;

	public static final int SCALE_LOGARITHMIC_LEGACY = 3;

	public static final String[] SCALES = { "linear", "quadratic", "logarithmic", "logarithmic (legacy)" };

	public static final int DEFAULT_STEPS = 10;

	public static final int DEFAULT_SCALE = SCALE_LINEAR;

	private String min;

	private String max;

	private String steps;

	private String stepSize;

	private int scale;

	public ParameterValueGrid(Operator operator, ParameterType type, String min, String max) {
		this(operator, type, min, max, Integer.toString(DEFAULT_STEPS), DEFAULT_SCALE);
	}

	public ParameterValueGrid(Operator operator, ParameterType type, String min, String max, String stepSize) {
		super(operator, type);
		this.min = min;
		this.max = max;
		this.steps = null;
		this.stepSize = stepSize;
		this.scale = SCALE_LINEAR;
	}

	public ParameterValueGrid(Operator operator, ParameterType type, String min, String max, String steps, int scale) {
		super(operator, type);
		this.min = min;
		this.max = max;
		this.steps = steps;
		this.scale = scale;
	}

	public ParameterValueGrid(Operator operator, ParameterType type, String min, String max, String steps, String scaleName) {
		super(operator, type);
		this.min = min;
		this.max = max;
		this.steps = steps;
		this.scale = SCALE_LINEAR;
		for (int i = 0; i < SCALES.length; i++) {
			if (scaleName.equals(SCALES[i])) {
				this.scale = i;
				break;
			}
		}
	}

	public void setMin(String min) {
		this.min = min;
	}

	public String getMin() {
		return min;
	}

	public void setMax(String max) {
		this.max = max;
	}

	public String getMax() {
		return max;
	}

	public void setSteps(String steps) {
		this.steps = steps;
	}

	public String getSteps() {
		return steps;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public int getScale() {
		return scale;
	}

	@Override
	public void move(int index, int direction) {}

	@Override
	public String[] getValuesArray() {
		double[] values = getValues();
		String[] valuesArray = new String[values.length];
		if (type instanceof ParameterTypeInt) {
			for (int i = 0; i < values.length; i++) {
				valuesArray[i] = Integer.toString((int) values[i]);
			}
		} else {
			for (int i = 0; i < values.length; i++) {
				valuesArray[i] = Double.toString(values[i]);
			}
		}
		return valuesArray;
	}

	public double[] getValues() {
		double[] values = null;
		if (stepSize != null && steps == null) {
			steps = Integer.toString((int) (Double.valueOf(max) - Double.valueOf(min)) / Integer.parseInt(stepSize));
		}
		switch (scale) {
			case SCALE_LINEAR:
				values = scalePolynomial(Integer.parseInt(steps), 1);
				break;
			case SCALE_QUADRATIC:
				values = scalePolynomial(Integer.parseInt(steps), 2);
				break;
			case SCALE_LOGARITHMIC:
				values = scaleLogarithmic(Integer.parseInt(steps));
				break;
			case SCALE_LOGARITHMIC_LEGACY:
				values = scaleLogarithmicLegacy(Integer.parseInt(steps));
				break;
			default:
				values = scalePolynomial(Integer.parseInt(steps), 1);
		}
		if (type instanceof ParameterTypeInt) {
			if (values.length > 0) {
				for (int i = 0; i < values.length; i++) {
					values[i] = Math.round(values[i]);
				}
				int count = 1;
				for (int i = 1; i < values.length; i++) {
					if (values[i] != values[i - 1]) {
						count++;
					}
				}
				double[] uniqueValues = new double[count];
				uniqueValues[0] = values[0];
				count = 1;
				for (int i = 1; i < values.length; i++) {
					if (values[i] != values[i - 1]) {
						uniqueValues[count] = values[i];
						count++;
					}
				}
				return uniqueValues;
			} else {
				return values;
			}
		}
		return values;
	}

	private double[] scalePolynomial(int steps, double power) {
		double[] values = new double[steps + 1];
		double minValue = Double.parseDouble(min);
		double maxValue = Double.parseDouble(max);
		for (int i = 0; i < steps + 1; i++) {
			values[i] = minValue + Math.pow((double) i / (double) steps, power) * (maxValue - minValue);
		}
		return values;
	}

	private double[] scaleLogarithmic(int steps) {
		double minValue = Double.parseDouble(min);
		double maxValue = Double.parseDouble(max);
		double[] values = new double[steps + 1];
		for (int i = 0; i < steps + 1; i++) {
			values[i] = Math.pow(maxValue / minValue, (double) i / (double) steps) * minValue;
		}
		return values;
	}

	private double[] scaleLogarithmicLegacy(int steps) {
		double minValue = Double.parseDouble(min);
		double maxValue = Double.parseDouble(max);
		double[] values = new double[steps + 1];
		double offset = 1 - minValue;
		for (int i = 0; i < steps + 1; i++) {
			values[i] = Math.pow(maxValue + offset, (double) i / (double) steps) - offset;
		}
		return values;
	}

	@Override
	public int getNumberOfValues() {
		return getValues().length;
	}

	@Override
	public String getValuesString() {
		return "[" + min + ";" + max + ";" + steps + ";" + SCALES[scale] + "]";
	}

	@Override
	public String toString() {
		return "grid: " + min + " - " + max + " (" + steps + ", " + SCALES[scale] + ")";
	}
}
