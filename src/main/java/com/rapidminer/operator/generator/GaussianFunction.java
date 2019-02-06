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
import com.rapidminer.example.Attributes;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.container.Range;


/**
 * Generates a gaussian distribution for all attributes.
 * 
 * @author Ingo Mierswa
 */
public class GaussianFunction extends RegressionFunction {

	private double bound = ExampleSetGenerator.DEFAULT_SINGLE_BOUND;

	@Override
	public void setLowerArgumentBound(double lower) {
		this.bound = lower;
	}

	@Override
	public void setUpperArgumentBound(double upper) {
		// not used
		this.bound = upper;
	}

	@Override
	public Attribute getLabel() {
		return AttributeFactory.createAttribute("label", Ontology.REAL);
	}

	@Override
	public double calculate(double[] att) throws FunctionException {
		return 0.0d;
	}

	@Override
	public double[] createArguments(int number, RandomGenerator random) {
		double[] args = new double[number];
		for (int i = 0; i < args.length; i++) {
			args[i] = random.nextGaussian() * bound;
		}
		return args;
	}

	@Override
	public ExampleSetMetaData getGeneratedMetaData() {
		ExampleSetMetaData emd = new ExampleSetMetaData();
		// label
		AttributeMetaData amd = new AttributeMetaData("label", Ontology.REAL, Attributes.LABEL_NAME);
		amd.setValueRange(new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), SetRelation.SUBSET);
		emd.addAttribute(amd);

		// attributes
		for (int i = 0; i < numberOfAttributes; i++) {
			amd = new AttributeMetaData("att" + (i + 1), Ontology.REAL);
			amd.setValueRange(new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), SetRelation.SUBSET);
			emd.addAttribute(amd);
		}
		emd.setNumberOfExamples(numberOfExamples);
		return emd;
	}
}
