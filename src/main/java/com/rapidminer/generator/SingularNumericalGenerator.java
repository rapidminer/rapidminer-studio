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
package com.rapidminer.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;


/**
 * Generators of this class will have one numerical input attribute and one output attribute.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public abstract class SingularNumericalGenerator extends FeatureGenerator {

	private static final Attribute[] INPUT_ATTR = { AttributeFactory.createAttribute(Ontology.NUMERICAL) };

	private boolean performLogging = true;

	public SingularNumericalGenerator() {}

	/**
	 * Subclasses have to implement this method to calculate the function result.
	 */
	public abstract double calculateValue(double value);

	@Override
	public Attribute[] getInputAttributes() {
		return INPUT_ATTR;
	}

	@Override
	public Attribute[] getOutputAttributes(ExampleTable input) {
		Attribute a1 = getArgument(0);
		Attribute ao = AttributeFactory.createAttribute(Ontology.NUMERICAL, Ontology.SINGLE_VALUE,
				getFunction() + "(" + a1.getConstruction() + ")");
		return new Attribute[] { ao };
	}

	/**
	 * Returns all compatible input attribute arrays for this generator from the given example set
	 * as list.
	 */
	@Override
	public List<Attribute[]> getInputCandidates(ExampleSet exampleSet, String[] functions) {
		List<Attribute[]> result = new ArrayList<>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (checkCompatibility(attribute, INPUT_ATTR[0], functions)) {
				result.add(new Attribute[] { attribute });
			}
		}
		return result;
	}

	@Override
	public void generate(DataRow data) throws GenerationException {
		try {
			Attribute a = getArgument(0);
			double value = data.get(a);
			double r = calculateValue(value);

			if (performLogging) {
				if (Double.isNaN(r)) {
					performLogging = false;
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.generator.SingularNumericalGenerator.nan_generated", getFunction());
				} else if (Double.isInfinite(r)) {
					performLogging = false;
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.generator.SingularNumericalGenerator.infinite_value_generated", getFunction());
					r = Double.NaN;
				}
			} else if (Double.isInfinite(r)) {
				r = Double.NaN;
			}

			if (resultAttributes[0] != null) {
				data.set(resultAttributes[0], r);
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new GenerationException("a:" + getArgument(0), ex);
		}
	}

	@Override
	public String toString() {
		String s = "singular function ";
		if (resultAttributes != null && resultAttributes.length > 0 && resultAttributes[0] != null) {
			s += resultAttributes[0].getName() + ":=";
		}
		s += getFunction() + "(";
		if (argumentsSet()) {
			s += getArgument(0).getName();
		}
		s += ")";
		return s;
	}
}
