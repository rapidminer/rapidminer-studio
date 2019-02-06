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

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * This operator generates TF-IDF values from the input data. The input example set must contain
 * either simple counts, which will be normalized during calculation of the term frequency TF, or it
 * already contains the calculated term frequency values (in this case no normalization will be
 * done).
 *
 * @author Ingo Mierswa
 */
public class TFIDFFilter extends AbstractDataProcessing {

	/**
	 * The parameter name for &quot;Indicates if term frequency values should be generated (must be
	 * done if input data is given as simple occurence counts).&quot;
	 */
	public static final String PARAMETER_CALCULATE_TERM_FREQUENCIES = "calculate_term_frequencies";

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	public TFIDFFilter(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		for (AttributeMetaData amd : metaData.getAllAttributes()) {
			if (!amd.isSpecial() && amd.isNumerical()) {
				amd.getMean().setUnkown();
				amd.setValueSetRelation(SetRelation.UNKNOWN);
			}
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		if (exampleSet.size() < 1) {
			throw new UserError(this, 110, new Object[] { "1" });
		}
		if (exampleSet.getAttributes().size() == 0) {
			throw new UserError(this, 106, new Object[0]);
		}

		// init
		double[] termFrequencySum = new double[exampleSet.size()];
		List<Attribute> attributes = new LinkedList<Attribute>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) {
				attributes.add(attribute);
			}
		}
		int[] documentFrequencies = new int[attributes.size()];

		// calculate frequencies
		int index = 0;
		for (Attribute attribute : attributes) {
			int exampleCounter = 0;
			for (Example example : exampleSet) {
				double value = example.getValue(attribute);
				termFrequencySum[exampleCounter] += value;
				if (value > 0) {
					documentFrequencies[index]++;
				}
				exampleCounter++;
			}
			index++;
			checkForStop();
		}

		// calculate IDF values
		double[] inverseDocumentFrequencies = new double[documentFrequencies.length];
		for (int i = 0; i < attributes.size(); i++) {
			inverseDocumentFrequencies[i] = Math.log((double) exampleSet.size() / (double) documentFrequencies[i]);
		}

		// set values
		boolean calculateTermFrequencies = getParameterAsBoolean(PARAMETER_CALCULATE_TERM_FREQUENCIES);
		index = 0;
		for (Attribute attribute : attributes) {
			int exampleCounter = 0;
			for (Example example : exampleSet) {
				double value = example.getValue(attribute);
				if (termFrequencySum[exampleCounter] == 0.0d || Double.isNaN(inverseDocumentFrequencies[index])) {
					example.setValue(attribute, 0.0d);
				} else {
					double tf = value;
					if (calculateTermFrequencies) {
						tf /= termFrequencySum[exampleCounter];
					}
					double idf = inverseDocumentFrequencies[index];
					example.setValue(attribute, (tf * idf));
				}
				exampleCounter++;
			}
			index++;
			checkForStop();
		}
		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(
				PARAMETER_CALCULATE_TERM_FREQUENCIES,
				"Indicates if term frequency values should be generated (must be done if input data is given as simple occurence counts).",
				true);
		type.setExpert(false);
		types.add(type);
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
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), TFIDFFilter.class, null);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_MAY_WRITE_INTO_DATA });
	}
}
