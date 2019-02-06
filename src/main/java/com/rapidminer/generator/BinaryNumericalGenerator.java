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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


/**
 * Objects of this generator class have two numerical input attributes and one output attribute.
 * 
 * @author Ingo Mierswa
 */
public abstract class BinaryNumericalGenerator extends FeatureGenerator {

	private static final Attribute[] INPUT_ATTR = { AttributeFactory.createAttribute(Ontology.NUMERICAL),
			AttributeFactory.createAttribute(Ontology.NUMERICAL) };

	public abstract double calculateValue(double value1, double value2);

	/** Must return true if this generator is commutative. */
	public abstract boolean isCommutative();

	/** Must return true if this generator is self applicable. */
	public abstract boolean isSelfApplicable();

	@Override
	public Attribute[] getInputAttributes() {
		return INPUT_ATTR;
	}

	@Override
	public Attribute[] getOutputAttributes(ExampleTable input) {
		Attribute a1 = getArgument(0);
		Attribute a2 = getArgument(1);
		String construction1 = a1.getConstruction();
		if (!construction1.equals(a1.getName())) {
			if (!construction1.startsWith("(") && !construction1.endsWith(")")) {
				construction1 = "(" + construction1 + ")";
			}
		}
		String construction2 = a2.getConstruction();
		if (!construction2.equals(a2.getName())) {
			if (!construction2.startsWith("(") && !construction2.endsWith(")")) {
				construction2 = "(" + construction2 + ")";
			}
		}
		Attribute ao = AttributeFactory.createAttribute(Ontology.REAL, Ontology.SINGLE_VALUE, "(" + construction1 + " "
				+ getFunction() + " " + construction2 + ")");
		return new Attribute[] { ao };
	}

	/**
	 * Returns all compatible input attribute arrays for this generator from the given example set
	 * as list.
	 */
	@Override
	public List<Attribute[]> getInputCandidates(ExampleSet exampleSet, String[] functions) {
		List<Attribute[]> result = new ArrayList<Attribute[]>();
		Attributes attributes = exampleSet.getAttributes();
		if (getSelectionMode() == SELECTION_MODE_ALL) {
			for (Attribute first : attributes) {
				if (!checkCompatibility(first, INPUT_ATTR[0], functions)) {
					continue;
				}
				for (Attribute second : attributes) {
					if (checkCompatibility(second, INPUT_ATTR[1], functions)) {
						result.add(new Attribute[] { first, second });
					}
				}
			}
		} else {
			if (isCommutative() && isSelfApplicable()) {
				int firstCounter = 0;
				for (Attribute first : attributes) {
					if (checkCompatibility(first, INPUT_ATTR[0], functions)) {
						int secondCounter = 0;
						for (Attribute second : attributes) {
							if (secondCounter >= firstCounter) {
								if (checkCompatibility(second, INPUT_ATTR[1], functions)) {
									result.add(new Attribute[] { first, second });
								}
							}
							secondCounter++;
						}
					}
					firstCounter++;
				}
			} else if (isCommutative() && !isSelfApplicable()) {
				int firstCounter = 0;
				for (Attribute first : attributes) {
					if (checkCompatibility(first, INPUT_ATTR[0], functions)) {
						int secondCounter = 0;
						for (Attribute second : attributes) {
							if (secondCounter > firstCounter) {
								if (checkCompatibility(second, INPUT_ATTR[1], functions)) {
									result.add(new Attribute[] { first, second });
								}
							}
							secondCounter++;
						}
					}
					firstCounter++;
				}
			} else if (!isCommutative() && isSelfApplicable()) {
				for (Attribute first : attributes) {
					if (!checkCompatibility(first, INPUT_ATTR[0], functions)) {
						continue;
					}
					for (Attribute second : attributes) {
						if (checkCompatibility(second, INPUT_ATTR[1], functions)) {
							result.add(new Attribute[] { first, second });
						}
					}
				}
			} else if (!isCommutative() && !isSelfApplicable()) {
				int firstCounter = 0;
				for (Attribute first : attributes) {
					if (checkCompatibility(first, INPUT_ATTR[0], functions)) {
						int secondCounter = 0;
						for (Attribute second : attributes) {
							if (firstCounter != secondCounter) {
								if (checkCompatibility(second, INPUT_ATTR[1], functions)) {
									result.add(new Attribute[] { first, second });
								}
							}
							secondCounter++;
						}
					}
					firstCounter++;
				}
			}
		}
		return result;
	}

	@Override
	public void generate(DataRow data) throws GenerationException {
		try {
			Attribute a0 = getArgument(0);
			Attribute a1 = getArgument(1);
			double o1 = data.get(a0);
			double o2 = data.get(a1);
			double r = calculateValue(o1, o2);

			if (Double.isInfinite(r)) {
				// LogService.getGlobal().log(getFunction() + ": Infinite value generated.",
				// LogService.WARNING);
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.generator.BinaryNumericalGenerator.infinite_value_generated", getFunction());
			}
			if (Double.isNaN(r)) {
				// LogService.getGlobal().log(getFunction() + ": NaN generated.",
				// LogService.WARNING);
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.generator.BinaryNumericalGenerator.nan_generated",
						getFunction());
			}

			if (resultAttributes[0] != null) {
				data.set(resultAttributes[0], r);
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new GenerationException("a1:" + getArgument(0) + " a2: " + getArgument(1), ex);
		}
	}

	@Override
	public String toString() {
		String s = "binary function (";
		if ((resultAttributes != null) && (resultAttributes.length > 0) && (resultAttributes[0] != null)) {
			s += resultAttributes[0].getName() + ":=";
		}
		if (argumentsSet()) {
			s += getArgument(0).getName() + " ";
		}
		s += getFunction();
		if (argumentsSet()) {
			s += " " + getArgument(1).getName();
		}
		s += ")";
		return s;
	}
}
