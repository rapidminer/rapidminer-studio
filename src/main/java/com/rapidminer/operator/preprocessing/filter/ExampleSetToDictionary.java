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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.operator.preprocessing.PreprocessingOperator;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * This operator takes two example sets and transforms the second into a dictionary. The second
 * example set must contain two nominal attributes. For every example in this set a dictionary entry
 * is created matching the first attribute value to the second. Finally, this dictionary is used to
 * replace substrings in the first example set to replacements.
 *
 * @author Simon Fischer
 */
public class ExampleSetToDictionary extends PreprocessingOperator {

	private static final String PARAMETER_REGEXP = "use_regular_expressions";
	private static final String PARAMETER_TO_LOWERCASE = "convert_to_lowercase";
	private static final String PARAMETER_FROM_ATTRIBUTE = "from_attribute";
	private static final String PARAMETER_TO_ATTRIBUTE = "to_attribute";
	private static final String PARAMETER_FIRST_MATCH_ONLY = "first_match_only";

	private final InputPort dictionaryInput = getInputPorts().createPort("dictionary");

	/**
	 * Incompatible version, old version writes into the exampleset, if original output port is not
	 * connected.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 1, 1);

	public ExampleSetToDictionary(OperatorDescription description) {
		super(description);
		dictionaryInput.addPrecondition(new ExampleSetPrecondition(dictionaryInput) {

			@Override
			public void makeAdditionalChecks(ExampleSetMetaData dictionaryMD) throws UndefinedParameterError {
				if (dictionaryMD.getAttributeSetRelation() == SetRelation.EQUAL
						|| dictionaryMD.getAttributeSetRelation() == SetRelation.SUPERSET) {
					String from = getParameterAsString(PARAMETER_FROM_ATTRIBUTE);
					String to = getParameterAsString(PARAMETER_TO_ATTRIBUTE);
					if (!(dictionaryMD.containsAttributeName(from) == MetaDataInfo.YES) && !from.isEmpty()) {
						dictionaryInput.addError(new SimpleMetaDataError(Severity.ERROR, dictionaryInput,
								"missing_attribute", from));
					}
					if (!(dictionaryMD.containsAttributeName(to) == MetaDataInfo.YES) && !to.isEmpty()) {
						dictionaryInput.addError(new SimpleMetaDataError(Severity.ERROR, dictionaryInput,
								"missing_attribute", to));
					}
				}
			}
		});
	}

	@Override
	protected ExampleSetMetaData modifyMetaData(ExampleSetMetaData exampleSetMetaData) throws UndefinedParameterError {
		// then delete known value sets on attributes
		AttributeSubsetSelector selector = new AttributeSubsetSelector(this, getExampleSetInputPort());
		ExampleSetMetaData subset = selector.getMetaDataSubset(exampleSetMetaData, false);
		for (AttributeMetaData amd : subset.getAllAttributes()) {
			if (amd.isNominal()) {
				AttributeMetaData originalAttribute = exampleSetMetaData.getAttributeByName(amd.getName());
				originalAttribute.setValueSet(new HashSet<String>(0), SetRelation.UNKNOWN);
			}
		}
		return super.modifyMetaData(exampleSetMetaData);
	}

	@Override
	/* This method isn't called anymore */
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd) {
		return null;
	}

	@Override
	public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {
		ExampleSet dictionarySet = dictionaryInput.getData(ExampleSet.class);

		AttributeSubsetSelector subsetSelector = new AttributeSubsetSelector(this, getExampleSetInputPort());
		boolean toLowerCase = getParameterAsBoolean(PARAMETER_TO_LOWERCASE);

		List<String[]> replacements = new LinkedList<>();

		Attribute from = dictionarySet.getAttributes().get(getParameterAsString(PARAMETER_FROM_ATTRIBUTE));
		Attribute to = dictionarySet.getAttributes().get(getParameterAsString(PARAMETER_TO_ATTRIBUTE));
		if (from == null) {
			throw new UndefinedParameterError(PARAMETER_FROM_ATTRIBUTE, this);
		}
		if (to == null) {
			throw new UndefinedParameterError(PARAMETER_TO_ATTRIBUTE, this);
		}
		if (!from.isNominal()) {
			throw new UserError(this, 119, from.getName(), this);
		}
		if (!to.isNominal()) {
			throw new UserError(this, 119, to.getName(), this);
		}
		for (Example example : dictionarySet) {
			if (toLowerCase) {
				replacements.add(new String[] { example.getValueAsString(from).toLowerCase(),
						example.getValueAsString(to).toLowerCase() });
			} else {
				replacements.add(new String[] { example.getValueAsString(from), example.getValueAsString(to) });
			}
		}
		return new Dictionary(exampleSet, subsetSelector.getAttributeSubset(exampleSet, false), replacements,
				getParameterAsBoolean(PARAMETER_REGEXP), toLowerCase, getParameterAsBoolean(PARAMETER_FIRST_MATCH_ONLY));
	}

	@Override
	public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
		return Dictionary.class;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeAttribute(PARAMETER_FROM_ATTRIBUTE,
				"Name of the attribute that specifies what is replaced.", dictionaryInput, false, Ontology.NOMINAL));
		types.add(new ParameterTypeAttribute(PARAMETER_TO_ATTRIBUTE, "Name of the attribute that specifies replacements.",
				dictionaryInput, false, Ontology.NOMINAL));
		types.add(new ParameterTypeBoolean(PARAMETER_REGEXP,
				"Choose whether the replacements are treated as regular expressions.", false));
		types.add(new ParameterTypeBoolean(PARAMETER_TO_LOWERCASE,
				"Choose whether the strings are converted to lower case.", false));
		types.add(new ParameterTypeBoolean(
				PARAMETER_FIRST_MATCH_ONLY,
				"If checked, only the first match in the dictionary will be considered. Otherwise, subsequent matches will be applied iteratively.",
				false));
		return types;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.ATTRIBUTE_VALUE };
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
				ExampleSetToDictionary.class, attributeSelector);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_MAY_WRITE_INTO_DATA });
	}
}
