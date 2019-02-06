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
package com.rapidminer.operator.preprocessing.filter;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.operator.preprocessing.PreprocessingOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeStringCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * Abstract superclass for all operators that replenish values, e.g. nan or infinite values.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public abstract class ValueReplenishment extends PreprocessingOperator {

	/**
	 * The parameter name for &quot;Function to apply to all columns that are not explicitly
	 * specified by parameter 'columns'.&quot;
	 */
	public static final String PARAMETER_DEFAULT = "default";

	/** The parameter name for &quot;List of replacement functions for each column.&quot; */
	public static final String PARAMETER_COLUMNS = "columns";

	public ValueReplenishment(OperatorDescription description) {
		super(description);
	}

	/**
	 * Returns the value which should be replaced.
	 */
	public abstract double getReplacedValue();

	/**
	 * Returns the value of the replenishment function with the given index.
	 * 
	 * @throws UndefinedParameterError
	 * @throws UserError
	 */
	public abstract double getReplenishmentValue(int functionIndex, ExampleSet baseExampleSet, Attribute attribute)
			throws UndefinedParameterError, UserError;

	/** Returns an array of all replenishment functions. */
	public abstract String[] getFunctionNames();

	/**
	 * Returns the index of the replenishment function which will be used for attributes not listed
	 * in the parameter list &quot;columns&quot;.
	 */
	public abstract int getDefaultFunction();

	/**
	 * Returns the index of the replenishment function which will be used for attributes listed in
	 * the parameter list &quot;columns&quot;.
	 */
	public abstract int getDefaultColumnFunction();

	@Override
	public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {
		exampleSet.recalculateAllAttributeStatistics();

		int defaultFunction = getParameterAsInt(PARAMETER_DEFAULT);
		List<String[]> functionList = getParameterList(PARAMETER_COLUMNS);

		double replacedValue = getReplacedValue();
		HashMap<String, Double> numericalAndDateReplacementMap = new HashMap<String, Double>();
		HashMap<String, String> nominalReplacementMap = new HashMap<String, String>();
		List<String> functionNames = Arrays.asList(getFunctionNames());
		for (Attribute attribute : exampleSet.getAttributes()) {
			String attributeName = attribute.getName();
			int function = defaultFunction;
			for (String[] pair : functionList) {
				if (pair[0].equals(attributeName)) {
					function = functionNames.indexOf(pair[1]);
					if (function == -1) {
						throw new RuntimeException("Illegal replacement function: " + pair[1]);
					}
				}
			}

			final double replenishmentValue = getReplenishmentValue(function, exampleSet, attribute);
			if (attribute.isNominal()) {
				if ((replenishmentValue == -1) || Double.isNaN(replenishmentValue)) {
					nominalReplacementMap.put(attributeName, null);
				} else {
					nominalReplacementMap.put(attributeName, attribute.getMapping().mapIndex((int) replenishmentValue));
				}
			}
			if (attribute.isNumerical() || Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
				numericalAndDateReplacementMap.put(attributeName, replenishmentValue);
			}
		}

		return new ValueReplenishmentModel(exampleSet, replacedValue, numericalAndDateReplacementMap, nominalReplacementMap);
	}

	@Override
	public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
		return ValueReplenishmentModel.class;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		String[] functionNames = getFunctionNames();
		ParameterType type = new ParameterTypeCategory(PARAMETER_DEFAULT,
				"Function to apply to all columns that are not explicitly specified by parameter 'columns'.", functionNames,
				getDefaultFunction());
		type.setExpert(false);
		types.add(type);
		ParameterTypeStringCategory categories = new ParameterTypeStringCategory(
				"replace_with",
				"Selects the function, which is used to determine the replacement for the missing values of this attribute.",
				functionNames, getFunctionNames()[getDefaultColumnFunction()], false);
		categories.setEditable(false);
		types.add(new ParameterTypeList(PARAMETER_COLUMNS, "List of replacement functions for each column.",
				new ParameterTypeAttribute("attribute", "Specifies the attribute, which missing values are replaced.",
						getExampleSetInputPort()), categories));
		return types;
	}

}
