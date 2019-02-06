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

import java.util.HashSet;
import java.util.Set;

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
 * Generates a data set for two attributes. The data build three rings which are around each other.
 * 
 * @author Ingo Mierswa
 */
public class RingClusteringFunction extends ClusterFunction {

	private double bound = ExampleSetGenerator.DEFAULT_SINGLE_BOUND;

	private Attribute label = AttributeFactory.createAttribute("label", Ontology.NOMINAL);

	private RandomGenerator random;

	public RingClusteringFunction() {
		label.getMapping().mapString("core");
		label.getMapping().mapString("first_ring");
		label.getMapping().mapString("second_ring");
	}

	/** Does nothing. */
	@Override
	public void init(RandomGenerator random) {
		this.random = random;
	}

	/** Since circles are used the upper and lower bounds must be the same. */
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
		return label;
	}

	@Override
	public double calculate(double[] att) throws FunctionException {
		if (att.length != 2) {
			throw new FunctionException("Ring clustering function", "must have 2 attributes!");
		}
		if (random.nextDouble() < 0.05) {
			int type = random.nextInt(3);
			switch (type) {
				case 0:
					return getLabel().getMapping().mapString("core");
				case 1:
					return getLabel().getMapping().mapString("first_ring");
				case 2:
					return getLabel().getMapping().mapString("second_ring");
				default:
					return getLabel().getMapping().mapString("core");
			}
		} else {
			double radius = Math.sqrt(att[0] * att[0] + att[1] * att[1]);
			if (radius < bound / 3.0d) {
				return getLabel().getMapping().mapString("core");
			} else if (radius < 2 * bound / 3.0d) {
				return getLabel().getMapping().mapString("first_ring");
			} else {
				return getLabel().getMapping().mapString("second_ring");
			}
		}
	}

	@Override
	public double[] createArguments(int number, RandomGenerator random) throws FunctionException {
		if (number != 2) {
			throw new FunctionException("Ring clustering function", "must have 2 attributes!");
		}
		double[] args = new double[number];
		int type = random.nextInt(3);
		double radius = 0.0d;
		switch (type) {
			case 0:
				radius = random.nextGaussian();
				break;
			case 1:
				radius = bound / 2.0d + random.nextGaussian();
				break;
			case 2:
				radius = bound + random.nextGaussian();
				break;
			default:
				radius = random.nextGaussian();
				break;
		}
		double angle = random.nextDouble() * 2 * Math.PI;
		args[0] = radius * Math.cos(angle);
		args[1] = radius * Math.sin(angle);
		return args;
	}

	@Override
	protected Set<String> getClusterSet() {
		HashSet<String> set = new HashSet<String>();
		set.add("core");
		set.add("first_ring");
		set.add("second_ring");
		return set;
	}

	@Override
	public int getMinNumberOfAttributes() {
		return 2;
	}

	@Override
	public int getMaxNumberOfAttributes() {
		return 2;
	}

	@Override
	public ExampleSetMetaData getGeneratedMetaData() {
		ExampleSetMetaData emd = new ExampleSetMetaData();
		// label
		AttributeMetaData amd = new AttributeMetaData("label", Ontology.NOMINAL, Attributes.LABEL_NAME);
		amd.setValueSet(getClusterSet(), SetRelation.EQUAL);
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
