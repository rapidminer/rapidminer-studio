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

import com.rapidminer.example.Attributes;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.container.Range;

import java.util.Set;


/**
 * 
 * Abstract superclass of all clustering functions of the ExampleSetGenerator
 * 
 * @author Sebastian Land
 * 
 */
public abstract class ClusterFunction implements TargetFunction {

	/** The number of attributes. */
	protected int numberOfAttributes = 2;
	protected int numberOfExamples = 0;
	/** The lower bound for the dataset. */
	protected double lowerBound = -10.0d;
	/** The upper bound for the dataset. */
	protected double upperBound = 10.0d;

	public ClusterFunction() {
		super();
	}

	/** Since circles are used the upper and lower bounds must be the same. */
	@Override
	public void setLowerArgumentBound(double lower) {
		this.lowerBound = lower;
	}

	@Override
	public void setUpperArgumentBound(double upper) {
		this.upperBound = upper;
	}

	/** Does nothing. */
	@Override
	public void setTotalNumberOfExamples(int number) {
		this.numberOfExamples = number;
	}

	/** Sets the total number of attributes. */
	@Override
	public void setTotalNumberOfAttributes(int number) {
		this.numberOfAttributes = number;
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
			amd.setValueRange(new Range(lowerBound, upperBound), SetRelation.EQUAL);
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

	protected abstract Set<String> getClusterSet();
}
