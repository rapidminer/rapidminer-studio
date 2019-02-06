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
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.tools.Ontology;

import java.util.ArrayList;
import java.util.List;


/**
 * Generates a constant attribute. The format is &quot;const[value]()&quot; for the
 * {@link com.rapidminer.operator.features.construction.FeatureGenerationOperator} operator.
 * 
 * @author Ingo Mierswa Exp $
 */
public class ConstantGenerator extends FeatureGenerator {

	public static final String FUNCTION_NAME = "const";

	private double constant = 1.0d;

	private String constantString = "1";

	public ConstantGenerator() {}

	public ConstantGenerator(double constant) {
		this.constant = constant;
		this.constantString = constant + "";
	}

	@Override
	public void setArguments(Attribute[] args) {

	}

	@Override
	public FeatureGenerator newInstance() {
		return new ConstantGenerator();
	}

	@Override
	public String getFunction() {
		return FUNCTION_NAME + "(" + constantString + ")";
	}

	@Override
	public void setFunction(String functionName) {
		int leftIndex = functionName.indexOf("(");
		int rightIndex = functionName.indexOf(")");
		if ((leftIndex != -1) && (rightIndex != -1)) {
			this.constantString = functionName.substring(leftIndex + 1, rightIndex);
			this.constant = Double.parseDouble(constantString);
		}
	}

	@Override
	public Attribute[] getInputAttributes() {
		return new Attribute[0];
	}

	@Override
	public Attribute[] getOutputAttributes(ExampleTable input) {
		Attribute ao = AttributeFactory.createAttribute(Ontology.NUMERICAL, Ontology.SINGLE_VALUE, getFunction()); // +
																													// "()");
		return new Attribute[] { ao };
	}

	/**
	 * Returns all compatible input attribute arrays for this generator from the given example set
	 * as list.
	 */
	@Override
	public List<Attribute[]> getInputCandidates(ExampleSet exampleSet, String[] functions) {
		return new ArrayList<Attribute[]>();
	}

	@Override
	public void generate(DataRow data) throws GenerationException {
		try {
			if (resultAttributes[0] != null) {
				data.set(resultAttributes[0], constant);
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new GenerationException("a:" + getArgument(0), ex);
		}
	}

	@Override
	public String toString() {
		return getFunction();
	}
}
