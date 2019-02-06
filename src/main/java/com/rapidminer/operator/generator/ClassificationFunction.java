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

import java.util.HashSet;
import java.util.Set;


/**
 * A target function for classification labels, i.e. non-continous nominal labels.
 * 
 * @author Ingo Mierswa
 */
public abstract class ClassificationFunction implements TargetFunction {

	protected double lower = -10.0d;

	protected double upper = 10.0d;

	private int numberOfExamples = 0;
	private int numberOfAttributes = 0;

	Attribute label = AttributeFactory.createAttribute("label", Ontology.BINOMINAL);

	public ClassificationFunction() {
		label.getMapping().mapString("negative");
		label.getMapping().mapString("positive");
	}

	/** Does nothing. */
	@Override
	public void init(RandomGenerator random) {}

	@Override
	public void setTotalNumberOfExamples(int number) {
		numberOfExamples = number;
	}

	public int getTotalNumberOfExamples() {
		return numberOfExamples;
	}

	@Override
	public void setTotalNumberOfAttributes(int number) {
		numberOfAttributes = number;
	}

	public int getTotalNumberOfAttributes() {
		return numberOfAttributes;
	}

	@Override
	public void setLowerArgumentBound(double lower) {
		this.lower = lower;
	}

	@Override
	public void setUpperArgumentBound(double upper) {
		this.upper = upper;
	}

	@Override
	public Attribute getLabel() {
		return label;
	}

	@Override
	public double[] createArguments(int dimension, RandomGenerator random) {
		double[] args = new double[dimension];
		for (int i = 0; i < args.length; i++) {
			args[i] = random.nextDoubleInRange(lower, upper);
		}
		return args;
	}

	@Override
	public ExampleSetMetaData getGeneratedMetaData() {
		ExampleSetMetaData emd = new ExampleSetMetaData();
		// label
		AttributeMetaData amd = new AttributeMetaData("label", Ontology.BINOMINAL, Attributes.LABEL_NAME);
		Set<String> valueSet = new HashSet<String>();
		valueSet.add("negative");
		valueSet.add("positive");
		amd.setValueSet(valueSet, SetRelation.EQUAL);
		emd.addAttribute(amd);

		// attributes
		for (int i = 0; i < numberOfAttributes; i++) {
			amd = new AttributeMetaData("att" + (i + 1), Ontology.REAL);
			amd.setValueRange(new Range(lower, upper), SetRelation.EQUAL);
			emd.addAttribute(amd);
		}
		emd.setNumberOfExamples(numberOfExamples);
		return emd;
	}

	@Override
	public int getMinNumberOfAttributes() {
		return 1;
	}

	@Override
	public int getMaxNumberOfAttributes() {
		return Integer.MAX_VALUE;
	}
}
