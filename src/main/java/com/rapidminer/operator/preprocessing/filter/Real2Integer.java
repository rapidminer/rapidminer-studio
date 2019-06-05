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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.math.container.Range;


/**
 * Converts all real valued attributes to integer valued attributes. Each real value is simply
 * cutted (default) or rounded. If the value is missing, the new value will be missing.
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class Real2Integer extends AbstractFilteredDataProcessing {

	public static final String PARAMETER_ROUND = "round_values";

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	/**
	 * Old version converts infinite real values to Long.MAX_VALUE or Long.MIN_VALUE. The new version preserves infinite
	 * values.
	 */
	private static final OperatorVersion VERSION_CAN_NOT_HANDLE_INFINITY = new OperatorVersion(9, 2, 1);

	public Real2Integer(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSetMetaData applyOnFilteredMetaData(ExampleSetMetaData emd) {
		boolean round = getParameterAsBoolean(PARAMETER_ROUND);

		for (AttributeMetaData amd : emd.getAllAttributes()) {
			if ((Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), Ontology.NUMERICAL))
					&& (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), Ontology.INTEGER))) {
				amd.setType(Ontology.INTEGER);
			}
			double lower = realToInt(round, amd.getValueRange().getLower());
			double upper = realToInt(round, amd.getValueRange().getUpper());
			amd.setValueRange(new Range(lower, upper), SetRelation.EQUAL);
		}
		return emd;
	}

	@Override
	public ExampleSet applyOnFiltered(ExampleSet exampleSet) throws OperatorException {
		boolean round = getParameterAsBoolean(PARAMETER_ROUND);

		List<Attribute> newAttributes = new LinkedList<Attribute>();
		Iterator<Attribute> a = exampleSet.getAttributes().iterator();
		while (a.hasNext()) {
			Attribute attribute = a.next();
			if ((Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.NUMERICAL))
					&& (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.INTEGER))) {
				Attribute newAttribute = AttributeFactory.createAttribute(attribute.getName(), Ontology.INTEGER);
				newAttributes.add(newAttribute);
				exampleSet.getExampleTable().addAttribute(newAttribute);
				for (Example example : exampleSet) {
					double originalValue = example.getValue(attribute);
					if (Double.isNaN(originalValue)) {
						example.setValue(newAttribute, Double.NaN);
					} else if (Double.isInfinite(originalValue) &&
							getCompatibilityLevel().isAbove(VERSION_CAN_NOT_HANDLE_INFINITY)) {
						example.setValue(newAttribute, originalValue);
					} else {
						long newValue = round ? Math.round(originalValue) : (long) originalValue;
						example.setValue(newAttribute, newValue);
					}
				}
				a.remove();
			}
		}

		for (Attribute attribute : newAttributes) {
			exampleSet.getAttributes().addRegular(attribute);
		}

		return exampleSet;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.REAL };
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_ROUND, "Indicates if the values should be rounded instead of cutted.",
				false));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		if (getCompatibilityLevel().isAbove(VERSION_MAY_WRITE_INTO_DATA)) {
			return true;
		} else {
			// old version: true only if original output port is connected
			return isOriginalOutputConnected();
		}
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), Real2Integer.class, null);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[]{VERSION_MAY_WRITE_INTO_DATA, VERSION_CAN_NOT_HANDLE_INFINITY });
	}

	/**
	 * Helper method that either transforms the real to an int by rounding or by ignoring the fractional part.
	 */
	private double realToInt(boolean round, double real) {
		double result;
		if (!Double.isFinite(real) && getCompatibilityLevel().isAbove(VERSION_CAN_NOT_HANDLE_INFINITY)) {
			// preserves pos/neg infinity and NaN
			result = real;
		} else {
			if (round) {
				result = Math.round(real);
			} else {
				result = (long) real;
			}
		}
		return result;
	}
}
