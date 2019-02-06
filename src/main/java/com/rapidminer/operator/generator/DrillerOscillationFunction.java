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
package com.rapidminer.operator.generator;

import com.rapidminer.example.Attribute;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.container.Range;


/**
 * Generator for an artificial audio data set (based on real-world data from drilling processes).
 *
 * @author Sebastian Land
 */
public class DrillerOscillationFunction implements TargetFunction {

	private static final double SKEW_CATASTROPHY_EXPONENT = 1.2d;
	private double TURN_PERIODS = 16d;
	private static double GENERAL_NOISE = 0.02d;
	private static double INITIAL_SKEW_RATE_VARIANCE = 0.2d;
	private static double LAST_SKEW_CHANGE_PROBABILITY = 0.2d;
	private static double MAXIMAL_SKEW_CHANGE = 0.1d;
	private static double CRITICAL_FREQUENCY_SKEW = 2.7d;
	private static double SKEW_FASTER_PREFERENCE = 0.3d;
	private int periodLength;
	private int period = 0;
	private double[] frequencePeriod;
	private double[] skew;
	private double[] skewChangeDirection;
	private double[] crashed;

	private int numberOfAttributes;
	private int numberOfExamples;

	public DrillerOscillationFunction() {}

	@Override
	public double calculate(double[] args) throws FunctionException {
		return 0;
	}

	@Override
	public double[] createArguments(int dimension, RandomGenerator random) throws FunctionException {
		if (dimension < 3) {
			throw new FunctionException("Driller oscillation function", "needs at least 3 attributes!");
		}
		if (skew == null) {
			skew = new double[dimension - 1];
			for (int i = 0; i < dimension - 1; i++) {
				skew[i] = 1d + random.nextDoubleInRange(-INITIAL_SKEW_RATE_VARIANCE, INITIAL_SKEW_RATE_VARIANCE);
			}
			skewChangeDirection = new double[dimension - 1];
			for (int i = 0; i < dimension - 1; i++) {
				skewChangeDirection[i] = random.nextDouble() - SKEW_FASTER_PREFERENCE;
			}
			crashed = new double[dimension];
			frequencePeriod = new double[dimension - 1];
		}
		double[] values = new double[dimension];
		dimension--;
		// with later periods possible change in skewing will be more probable
		double neededProbability = Math.pow((((double) period) / periodLength), 1.5d) * LAST_SKEW_CHANGE_PROBABILITY;

		// now check against needed probability
		for (int i = 0; i < dimension; i++) {
			if (skew[i] > CRITICAL_FREQUENCY_SKEW) {
				skew[i] = Math.pow(skew[i], SKEW_CATASTROPHY_EXPONENT);
				crashed[i] = 1d;
			} else {
				double dice = random.nextDouble();
				if (dice <= neededProbability) {
					// then change skew
					double skewChange = skew[i] * random.nextDoubleInRange(0, MAXIMAL_SKEW_CHANGE);
					if (random.nextDouble() <= skewChangeDirection[i]) {
						skew[i] = skew[i] - skewChange;
					} else {
						skew[i] = skew[i] + skewChange;
					}
				}
			}
		}
		// generating data
		for (int i = 0; i < dimension; i++) {
			frequencePeriod[i] = frequencePeriod[i] + ((Math.PI * skew[i]) / TURN_PERIODS);
			values[i] = Math.sin(frequencePeriod[i]);
			// adding noise
			values[i] = values[i] * random.nextDoubleInRange(1d - GENERAL_NOISE, 1d + GENERAL_NOISE);
		}
		values[1] = skew[0];
		values[dimension] = period;
		period++;
		if (period == periodLength) {
			crashed[dimension] = period;
			return crashed;
		}
		return values;
	}

	@Override
	public Attribute getLabel() {
		return null;
	}

	@Override
	public void init(RandomGenerator random) {}

	@Override
	public void setUpperArgumentBound(double upper) {}

	@Override
	public void setLowerArgumentBound(double lower) {}

	@Override
	public void setTotalNumberOfAttributes(int number) {
		this.numberOfAttributes = number;
	}

	@Override
	public void setTotalNumberOfExamples(int number) {
		this.periodLength = number;
		this.numberOfExamples = number;
	}

	@Override
	public ExampleSetMetaData getGeneratedMetaData() {
		ExampleSetMetaData emd = new ExampleSetMetaData();
		for (int i = 1; i <= numberOfAttributes; i++) {
			AttributeMetaData amd = new AttributeMetaData("att" + i, Ontology.REAL);
			if (i == numberOfAttributes) {
				amd.setValueRange(new Range(0, numberOfExamples), SetRelation.EQUAL);
			} else if (i == 2) {
				amd.setValueRange(new Range(0, Double.POSITIVE_INFINITY), SetRelation.SUBSET);
			} else {
				amd.setValueRange(new Range(-1d - GENERAL_NOISE, 1d + GENERAL_NOISE), SetRelation.EQUAL);
			}
			if (i < numberOfAttributes) {
				amd.getNumberOfMissingValues().increaseByUnknownAmount();
			}
			emd.addAttribute(amd);
		}
		emd.setNumberOfExamples(numberOfExamples);
		return emd;
	}

	@Override
	public int getMinNumberOfAttributes() {
		return 3;
	}

	@Override
	public int getMaxNumberOfAttributes() {
		return Integer.MAX_VALUE;
	}
}
