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
 * Generates an Association function transaction dataset. The first four attributes are correlated.
 * 
 * @author Ingo Mierswa
 */
public class TransactionDatasetFunction implements TargetFunction {

	private int numberOfExamples;
	private int numberOfAttributes;

	/** Does nothing. */
	@Override
	public void init(RandomGenerator random) {}

	@Override
	public void setTotalNumberOfExamples(int number) {
		this.numberOfExamples = number;
	}

	@Override
	public void setTotalNumberOfAttributes(int number) {
		this.numberOfAttributes = number;
	}

	@Override
	public void setLowerArgumentBound(double lower) {}

	@Override
	public void setUpperArgumentBound(double upper) {}

	@Override
	public Attribute getLabel() {
		return null;
	}

	@Override
	public double calculate(double[] att) throws FunctionException {
		if (att.length < 5) {
			throw new FunctionException("Transaction dataset function", "needs at least 5 attributes!");
		}
		return Double.NaN;
	}

	@Override
	public double[] createArguments(int number, RandomGenerator random) throws FunctionException {
		if (number < 5) {
			throw new FunctionException("Transaction dataset function", "needs at least 5 attributes!");
		}
		double[] args = new double[number];
		for (int i = 0; i < args.length; i++) {
			args[i] = random.nextDouble() < 0.1 ? 1 : 0;
		}
		if ((args[1] == 1) && (random.nextDouble() < 0.8)) {
			args[3] = 1;
		}
		if ((args[0] == 1) && (random.nextDouble() < 0.9)) {
			args[1] = 1;
		}
		if ((args[0] == 1) && (random.nextDouble() < 0.7)) {
			args[2] = 1;
		}
		return args;
	}

	@Override
	public ExampleSetMetaData getGeneratedMetaData() {
		ExampleSetMetaData emd = new ExampleSetMetaData();
		for (int i = 1; i <= numberOfAttributes; i++) {
			AttributeMetaData amd = new AttributeMetaData("att" + i, Ontology.REAL);
			amd.setValueRange(new Range(0, 1d), SetRelation.EQUAL);
			emd.addAttribute(amd);
		}
		emd.setNumberOfExamples(numberOfExamples);
		return emd;
	}

	@Override
	public int getMinNumberOfAttributes() {
		return 5;
	}

	@Override
	public int getMaxNumberOfAttributes() {
		return Integer.MAX_VALUE;
	}
}
