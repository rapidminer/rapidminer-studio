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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.math.container.Range;


/**
 * Converts all numerical attributes to binary ones. If the value of an attribute is between the
 * specified minimal and maximal value, it becomes <em>false</em>, otherwise <em>true</em>. If the
 * value is missing, the new value will be missing. The default boundaries are both set to 0, thus
 * only 0.0 is mapped to false and all other values are mapped to true.
 *
 * @author Sebastian Land, Ingo Mierswa, Shevek, Tobias Malbrecht
 */
public class NumericToBinominal extends NumericToNominal {

	/** The parameter name for &quot;The minimal value which is mapped to false (included).&quot; */
	public static final String PARAMETER_MIN = "min";

	/** The parameter name for &quot;The maximal value which is mapped to false (included).&quot; */
	public static final String PARAMETER_MAX = "max";

	public NumericToBinominal(OperatorDescription description) {
		super(description);
	}

	private double min;

	private double max;

	@Override
	public ExampleSet applyOnFiltered(ExampleSet exampleSet) throws OperatorException {
		min = getParameterAsDouble(PARAMETER_MIN);
		max = getParameterAsDouble(PARAMETER_MAX);
		return super.applyOnFiltered(exampleSet);
	}

	@Override
	public ExampleSetMetaData applyOnFilteredMetaData(ExampleSetMetaData emd) throws UndefinedParameterError {
		double min = getParameterAsDouble(PARAMETER_MIN);
		double max = getParameterAsDouble(PARAMETER_MAX);

		for (AttributeMetaData amd : emd.getAllAttributes()) {
			if (amd.isNumerical()) {
				Range valueRange = amd.getValueRange();
				amd.setType(Ontology.BINOMINAL);
				// all values below min?
				if (amd.getValueSetRelation() != SetRelation.SUPERSET && valueRange.getUpper() < min
						|| valueRange.getLower() > max) {
					amd.setValueSet(Collections.singleton("true"), SetRelation.EQUAL);
					continue;
				}
				// all values above max?
				if (amd.getValueSetRelation() != SetRelation.SUPERSET && valueRange.getLower() > min
						&& valueRange.getUpper() < max) {
					amd.setValueSet(Collections.singleton("false"), SetRelation.EQUAL);
					continue;
				}
				Set<String> values = new TreeSet<String>();
				values.add("false");
				values.add("true");
				amd.setValueSet(values, SetRelation.SUBSET);
			}
		}
		return emd;
	}

	@Override
	protected void setValue(Example example, Attribute newAttribute, double value) throws OperatorException {
		if (Double.isNaN(value)) {
			example.setValue(newAttribute, Double.NaN);
		} else if (value < min || value > max) {
			example.setValue(newAttribute, newAttribute.getMapping().mapString("true"));
		} else {
			example.setValue(newAttribute, newAttribute.getMapping().mapString("false"));
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeDouble(PARAMETER_MIN, "The minimal value which is mapped to false (included).",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0d, false));
		types.add(new ParameterTypeDouble(PARAMETER_MAX, "The maximal value which is mapped to false (included).",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0d, false));
		return types;
	}

	@Override
	protected int getGeneratedAttributevalueType() {
		return Ontology.BINOMINAL;
	}

	@Override
	protected Attribute makeAttribute() {
		Attribute att = AttributeFactory.createAttribute(getGeneratedAttributevalueType());
		att.getMapping().mapString("false");
		att.getMapping().mapString("true");
		return att;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), NumericToBinominal.class,
				null);
	}
}
