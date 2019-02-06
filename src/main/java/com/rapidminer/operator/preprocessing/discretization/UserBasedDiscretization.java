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
package com.rapidminer.operator.preprocessing.discretization;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.ProcessTools;
import com.rapidminer.tools.container.Tupel;


/**
 * This operator discretizes a numerical attribute to either a nominal or an ordinal attribute. The
 * numerical values are mapped to the classes according to the thresholds specified by the user. The
 * user can define the classes by specifying the upper limits of each class. The lower limit of the
 * next class is automatically specified as the upper limit of the previous one. A parameter defines
 * to which adjacent class values that are equal to the given limits should be mapped. If the upper
 * limit in the last list entry is not equal to Infinity, an additional class which is automatically
 * named is added. If a '?' is given as class value the according numerical values are mapped to
 * unknown values in the resulting attribute.
 *
 * @author Sebastian Land
 */
public class UserBasedDiscretization extends AbstractDiscretizationOperator {

	static {
		registerDiscretizationOperator(UserBasedDiscretization.class);
	}

	/** The parameter name for the upper limit. */
	public static final String PARAMETER_UPPER_LIMIT = "upper_limit";

	/** The parameter name for &quot;Defines the classes and the upper limits of each class.&quot; */
	public static final String PARAMETER_RANGE_NAMES = "classes";

	private static final String PARAMETER_CLASS_NAME = "class_names";

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	public UserBasedDiscretization(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd)
			throws UndefinedParameterError {
		AttributeMetaData newAMD = new AttributeMetaData(amd.getName(), Ontology.NOMINAL, amd.getRole());
		List<String[]> rangeList = getParameterList(PARAMETER_RANGE_NAMES);
		TreeSet<String> values = new TreeSet<String>();
		for (String[] pair : rangeList) {
			values.add(pair[0]);
		}
		newAMD.setValueSet(values, SetRelation.SUBSET);

		return Collections.singletonList(newAMD);
	}

	@Override
	public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {
		HashMap<String, SortedSet<Tupel<Double, String>>> ranges = new HashMap<String, SortedSet<Tupel<Double, String>>>();
		List<String[]> rangeList = getParameterList(PARAMETER_RANGE_NAMES);

		TreeSet<Tupel<Double, String>> thresholdPairs = new TreeSet<Tupel<Double, String>>();
		for (String[] pair : rangeList) {
			thresholdPairs.add(new Tupel<Double, String>(Double.valueOf(pair[1]), pair[0]));
		}
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) {
				ranges.put(attribute.getName(), thresholdPairs);
			}
		}

		DiscretizationModel model = new DiscretizationModel(exampleSet);
		model.setRanges(ranges);
		return model;
	}

	@Override
	public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
		return DiscretizationModel.class;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ProcessTools.setSubsetSelectorPrimaryParameter(types, false);

		ParameterType classType = new ParameterTypeString(PARAMETER_CLASS_NAME, "The name of this range.");
		ParameterType threshold = new ParameterTypeDouble(PARAMETER_UPPER_LIMIT, "The upper limit.",
				Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		List<String[]> defaultList = new LinkedList<String[]>();

		defaultList.add(new String[] { "first", Double.NEGATIVE_INFINITY + "" });
		defaultList.add(new String[] { "last", Double.POSITIVE_INFINITY + "" });

		ParameterType type = new ParameterTypeList(PARAMETER_RANGE_NAMES,
				"Defines the classes and the upper limits of each class.", classType, threshold, defaultList);
		type.setExpert(false);
		type.setPrimary(true);
		types.add(type);

		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		if (getCompatibilityLevel().isAbove(VERSION_MAY_WRITE_INTO_DATA)) {
			return false;
		} else {
			// old version: true only if original output port is connected
			return isOriginalOutputConnected() && super.writesIntoExistingData();
		}
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				UserBasedDiscretization.class, attributeSelector);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_MAY_WRITE_INTO_DATA });
	}
}
