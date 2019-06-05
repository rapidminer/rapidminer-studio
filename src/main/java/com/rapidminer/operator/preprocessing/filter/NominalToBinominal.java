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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.operator.preprocessing.PreprocessingOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * This operator maps the values of all nominal values to binary attributes. For example, if a
 * nominal attribute with name &quot;costs&quot; and possible nominal values &quot;low&quot;,
 * &quot;moderate&quot;, and &quot;high&quot; is transformed, the result is a set of three binominal
 * attributes &quot;costs = low&quot;, &quot;costs = moderate&quot;, and &quot;costs = high&quot;.
 * Only one of the values of each attribute is true for a specific example, the other values are
 * false.
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class NominalToBinominal extends PreprocessingOperator {

	public static final String PARAMETER_USE_UNDERSCORE_IN_NAME = "use_underscore_in_name";

	public static final String PARAMETER_TRANSFORM_BINOIMINAL = "transform_binominal";

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	public NominalToBinominal(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd) {
		if (amd.isNominal()) {
			LinkedList<AttributeMetaData> newAttributeMetaDataCollection = new LinkedList<>();
			boolean transformBinomial = getParameterAsBoolean(PARAMETER_TRANSFORM_BINOIMINAL);
			boolean useUnderscoreInName = getParameterAsBoolean(PARAMETER_USE_UNDERSCORE_IN_NAME);
			if (!transformBinomial && amd.getValueSet().size() == 2) {
				amd.setType(Ontology.BINOMINAL);
				amd.setValueSetRelation(SetRelation.EQUAL);
				newAttributeMetaDataCollection.add(amd);
			} else if (amd.getValueSetRelation() == SetRelation.UNKNOWN) {
				String name = amd.getName();
				if (transformBinomial) {
					// In this case we know that the original variable has been replaced. But we do not know the
					// names and number of the new attributes. Therefore, we mark the variable name with ? to show
					// that the meta data is incorrect.
					name += (useUnderscoreInName ? "_" : " = ") + "?";
					AttributeMetaData newAttributeMetaData = newBinomialAttributeMetaData(name);
					newAttributeMetaDataCollection.add(newAttributeMetaData);
				} else {
					// We assume the value set is of size 2 (binomial) because it is the most common case and
					// we do not have any information about the attribute's value set.
					// Whenever this assumption is wrong the meta data will be incorrect.
					amd.setType(Ontology.BINOMINAL);
					amd.setValueSetRelation(SetRelation.UNKNOWN);
					newAttributeMetaDataCollection.add(amd);
				}
			} else {
				for (String value : amd.getValueSet()) {
					String name = amd.getName() + (useUnderscoreInName ? "_" : " = ") + value;
					AttributeMetaData newAttributeMetaData = newBinomialAttributeMetaData(name);
					newAttributeMetaDataCollection.add(newAttributeMetaData);
				}
			}
			return newAttributeMetaDataCollection;
		} else {
			return null;
		}
	}

	@Override
	public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {
		PreprocessingModel model = new NominalToBinominalModel(exampleSet,
				getParameterAsBoolean(PARAMETER_TRANSFORM_BINOIMINAL),
				getParameterAsBoolean(PARAMETER_USE_UNDERSCORE_IN_NAME));
		return model;
	}

	@Override
	public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
		return NominalToBinominalModel.class;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_TRANSFORM_BINOIMINAL,
				"Indicates if attributes which are already binominal should be dichotomized.", false, false));
		types.add(new ParameterTypeBoolean(PARAMETER_USE_UNDERSCORE_IN_NAME,
				"Indicates if underscores should be used in the new attribute names instead of empty spaces and '='. Although the resulting names are harder to read for humans it might be more appropriate to use these if the data should be written into a database system.",
				false));
		return types;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NOMINAL };
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
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), NominalToBinominal.class,
				attributeSelector);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_MAY_WRITE_INTO_DATA });
	}

	/**
	 * Helper method that creates binomial attribute meta data.
	 *
	 * @param name
	 * 		the attributes name
	 * @return new meta data
	 */
	private AttributeMetaData newBinomialAttributeMetaData(String name) {
		AttributeMetaData newAttributeMetaData = new AttributeMetaData(name, Ontology.BINOMINAL);
		Set<String> values = new TreeSet<>();
		values.add("false");
		values.add("true");
		newAttributeMetaData.setValueSet(values, SetRelation.EQUAL);
		return newAttributeMetaData;
	}
}
