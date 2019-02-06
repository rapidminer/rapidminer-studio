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
import com.rapidminer.tools.math.container.Range;

import java.util.HashSet;
import java.util.Set;


/**
 * The label is the first class, if the sum of all arguments modulo 2 is 0, it is the second class
 * if the sum modulo 3 is 0 and the third class if the sum modulo 5 is 0. In all other cases the
 * label is the fourth class.
 * 
 * @author Ingo Mierswa
 */
public class MultiClassificationFunction extends ClassificationFunction {

	Attribute nominalLabel = AttributeFactory.createAttribute("label", Ontology.NOMINAL);

	public MultiClassificationFunction() {
		getLabel().getMapping().mapString("one");
		getLabel().getMapping().mapString("two");
		getLabel().getMapping().mapString("three");
		getLabel().getMapping().mapString("four");
	}

	@Override
	public Attribute getLabel() {
		return nominalLabel;
	}

	@Override
	public double calculate(double[] args) throws FunctionException {
		double sumD = 0.0d;
		for (int i = 0; i < args.length; i++) {
			sumD += args[i];
		}
		int sum = Math.abs((int) Math.round(sumD));
		if ((sum % 2) == 0) {
			return getLabel().getMapping().mapString("one");
		} else if ((sum % 3) == 0) {
			return getLabel().getMapping().mapString("two");
		} else if ((sum % 5) == 0) {
			return getLabel().getMapping().mapString("three");
		} else {
			return getLabel().getMapping().mapString("four");
		}
	}

	@Override
	public ExampleSetMetaData getGeneratedMetaData() {
		ExampleSetMetaData emd = new ExampleSetMetaData();
		// label
		AttributeMetaData amd = new AttributeMetaData("label", Ontology.NOMINAL, Attributes.LABEL_NAME);
		Set<String> valueSet = new HashSet<String>();
		valueSet.add("one");
		valueSet.add("two");
		valueSet.add("three");
		valueSet.add("four");
		amd.setValueSet(valueSet, SetRelation.EQUAL);
		emd.addAttribute(amd);

		// attributes
		for (int i = 0; i < getTotalNumberOfAttributes(); i++) {
			amd = new AttributeMetaData("att" + (i + 1), Ontology.REAL);
			amd.setValueRange(new Range(lower, upper), SetRelation.EQUAL);
			emd.addAttribute(amd);
		}
		emd.setNumberOfExamples(getTotalNumberOfExamples());
		return emd;
	}

}
