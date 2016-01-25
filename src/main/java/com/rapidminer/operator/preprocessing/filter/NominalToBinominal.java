/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


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

	public NominalToBinominal(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd) {
		boolean transformBinominal = getParameterAsBoolean(PARAMETER_TRANSFORM_BINOIMINAL);
		if (amd.isNominal()) {
			Collection<AttributeMetaData> newAttributeMetaDataCollection = new LinkedList<AttributeMetaData>();
			if (!transformBinominal && (amd.getValueSet().size() == 2)) {
				amd.setType(Ontology.BINOMINAL);
				return Collections.singletonList(amd);
			} else {
				if (amd.getValueSetRelation() != SetRelation.UNKNOWN) {
					for (String value : amd.getValueSet()) {
						String name = amd.getName()
								+ (getParameterAsBoolean(PARAMETER_USE_UNDERSCORE_IN_NAME) ? "_" : " = ") + value;
						AttributeMetaData newAttributeMetaData = new AttributeMetaData(name, Ontology.BINOMINAL);
						Set<String> values = new TreeSet<String>();
						values.add("false");
						values.add("true");
						newAttributeMetaData.setValueSet(values, SetRelation.EQUAL);
						newAttributeMetaDataCollection.add(newAttributeMetaData);
						emd.mergeSetRelation(amd.getValueSetRelation());
					}
				}
				return newAttributeMetaDataCollection;
			}
		} else {
			return null; // Collections.singleton(amd);
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
		types.add(new ParameterTypeBoolean(
				PARAMETER_USE_UNDERSCORE_IN_NAME,
				"Indicates if underscores should be used in the new attribute names instead of empty spaces and '='. Although the resulting names are harder to read for humans it might be more appropriate to use these if the data should be written into a database system.",
				false));
		return types;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NOMINAL };
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), NominalToBinominal.class,
				attributeSelector);
	}

}
