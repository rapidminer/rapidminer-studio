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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.BinominalMapping;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.ProcessTools;


/**
 * Correct internal mapping of binominal attributes according to the specified positive and negative
 * values. If the internal mapping differs from the specifications, the mapping is switched. If the
 * mapping contains other values than the specified ones, the mapping is not corrected and the
 * attribute is simply skipped.
 *
 * @author Tobias Malbrecht
 */
public class InternalBinominalRemapping extends AbstractDataProcessing {

	/**
	 * The parameter name for &quot;The attributes to which the mapping correction should be
	 * applied.&quot;
	 */
	public static final String PARAMETER_ATTRIBUTES = "attributes";

	/** The parameter name for &quot;Consider also special attributes (label, id...)&quot; */
	public static final String PARAMETER_APPLY_TO_SPECIAL_FEATURES = "apply_to_special_features";

	/** The parameter name for &quot;The first/negative/false value.&quot; */
	public static final String PARAMETER_NEGATIVE_VALUE = "negative_value";

	/** The parameter name for &quot;The second/positive/true value.&quot; */
	public static final String PARAMETER_POSITIVE_VALUE = "positive_value";

	/**
	 * Incompatible version, old version writes into the example set.
	 */
	private static final OperatorVersion VERSION_MAY_WRITE_INTO_DATA = new OperatorVersion(7, 3, 0);

	private AttributeSubsetSelector attributeSelector = new AttributeSubsetSelector(this, getInputPort(),
			Ontology.BINOMINAL);

	public InternalBinominalRemapping(OperatorDescription description) {
		super(description);
		getExampleSetInputPort().addPrecondition(attributeSelector.makePrecondition());
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		String negativeValue = getParameterAsString(PARAMETER_NEGATIVE_VALUE);
		String positiveValue = getParameterAsString(PARAMETER_POSITIVE_VALUE);

		Set<Attribute> attributes = attributeSelector.getAttributeSubset(exampleSet, false);
		Set<Attribute> remappedAttributes = new HashSet<>();

		for (Attribute attribute : attributes) {
			if (attribute.isNominal()) {
				if (negativeValue.equals(attribute.getMapping().getPositiveString())
						&& positiveValue.equals(attribute.getMapping().getNegativeString())) {
					// create inverse value mapping
					attribute.getMapping().clear();
					attribute.getMapping().mapString(negativeValue);
					attribute.getMapping().mapString(positiveValue);
					remappedAttributes.add(attribute);
				} else if (!negativeValue.equals(attribute.getMapping().getNegativeString())
						|| !positiveValue.equals(attribute.getMapping().getPositiveString())) {
					logWarning("specified values do not match values of attribute " + attribute.getName()
							+ ", attribute is skipped.");
				}
			}
		}

		for (Attribute attribute : attributes) {
			if (remappedAttributes.contains(attribute)) {
				for (Example example : exampleSet) {
					double value = example.getValue(attribute);
					if (!Double.isNaN(value)) {
						if (value == BinominalMapping.NEGATIVE_INDEX) {
							example.setValue(attribute, BinominalMapping.POSITIVE_INDEX);
						} else if (value == BinominalMapping.POSITIVE_INDEX) {
							example.setValue(attribute, BinominalMapping.NEGATIVE_INDEX);
						}
					}
				}
			}
		}

		return exampleSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(ProcessTools.setSubsetSelectorPrimaryParameter(attributeSelector.getParameterTypes(), true));
		types.add(new ParameterTypeString(PARAMETER_NEGATIVE_VALUE, "The first/negative/false value.", false));
		types.add(new ParameterTypeString(PARAMETER_POSITIVE_VALUE, "The second/positive/true value.", false));
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return getCompatibilityLevel().isAbove(VERSION_MAY_WRITE_INTO_DATA);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.addAll(super.getIncompatibleVersionChanges(),
				new OperatorVersion[] { VERSION_MAY_WRITE_INTO_DATA });
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				InternalBinominalRemapping.class, attributeSelector);
	}
}
