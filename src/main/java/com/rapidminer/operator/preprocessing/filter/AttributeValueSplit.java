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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
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
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeRegexp;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * <p>
 * This operator creates new attributes from a nominal attribute by dividing the nominal values into
 * parts according to a split criterion (regular expression). This operator provides two different
 * modes, depending on the setting of the parameter &quot;splitting_mode&quot;.
 * </p>
 *
 * <h3>Ordered Splits</h3>
 * <p>
 * In the first split mode, called ordered_split, the resulting attributes get the name of the
 * original attribute together with a number indicating the order. For example, if the original data
 * contained the values<br/>
 * <br/>
 *
 * attribute-name <br/>
 * -------------- <br/>
 * value1 <br/>
 * value2, value3 <br/>
 * value3 <br/>
 * <br/>
 *
 * and should be divided by the separating commas, the resulting attributes would be
 * attribute-name1, attribute-name2, attribute-name3 with the tuples (value1, ?, ?), (value2,
 * value3, ?), and (value3, ?, ?), respectively. This mode is useful if the original values
 * indicated some order like, for example, a preference.
 * </p>
 *
 * <h3>Unordered Splits</h3>
 * <p>
 * In the second split mode, called unordered_split, the resulting attributes get the name of the
 * original attribute together with the value for each of the occurring values. For example, if the
 * original data contained the values<br/>
 * <br/>
 *
 * attribute-name <br/>
 * -------------- <br/>
 * value1 <br/>
 * value2, value3 <br/>
 * value3 <br/>
 * <br/>
 *
 * and again should be divided by the separating commas, the resulting attributes would be
 * attribute-name-value1, attribute-name-value2, and attribute-name-value3 with the tuples (true,
 * false, false), (false, true, true), and (false, false, true), respectively. This mode is useful
 * if the order is not important but the goal is a basket like data set containing all occurring
 * values.
 * </p>
 *
 * @author Ingo Mierswa, Nils Woehler
 */
public class AttributeValueSplit extends AbstractDataProcessing {

	public static final String PARAMETER_SPLIT_PATTERN = "split_pattern";

	public static final String PARAMETER_SPLIT_MODE = "split_mode";

	public final static String[] SPLIT_MODES = new String[] { "ordered_split", "unordered_split" };

	public final static int SPLIT_MODE_ORDERED = 0;

	public final static int SPLIT_MODE_UNORDERED = 1;

	/** last version where selected but missing attributes were silently ignored */
	private static final OperatorVersion OPERATOR_VERSION_6_0_3 = new OperatorVersion(6, 0, 3);

	private AttributeSubsetSelector attributeSubsetSelector = new AttributeSubsetSelector(this, getExampleSetInputPort(),
			Ontology.NOMINAL);

	public AttributeValueSplit(OperatorDescription description) {
		super(description);

	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		String splittingRegex = getParameterAsString(PARAMETER_SPLIT_PATTERN);
		try {
			Pattern splittingPattern = Pattern.compile(splittingRegex);
			ExampleSetMetaData subset = attributeSubsetSelector.getMetaDataSubset(metaData, false, true);
			SetRelation attributeSetRelation = SetRelation.EQUAL;
			int type = getParameterAsInt(PARAMETER_SPLIT_MODE);
			for (AttributeMetaData amd : subset.getAllAttributes()) {
				if (!amd.isSpecial() && amd.isNominal()) {
					attributeSetRelation = attributeSetRelation.merge(amd.getValueSetRelation());

					// removing old attribute
					metaData.removeAttribute(metaData.getAttributeByName(amd.getName()));

					switch (type) {
						case SPLIT_MODE_ORDERED:

							int maxNumber = 0;
							if (amd.getValueSetRelation() == SetRelation.SUBSET
									|| amd.getValueSetRelation() == SetRelation.UNKNOWN) {
								maxNumber = 3;
							}
							String[][] valueParts = new String[amd.getValueSet().size()][];
							int i = 0;
							for (String value : amd.getValueSet()) {
								valueParts[i] = splittingPattern.split(value);
								maxNumber = Math.max(maxNumber, valueParts[i].length);
								i++;
							}

							// creating new attributes
							for (i = 0; i < maxNumber; i++) {
								AttributeMetaData newAmd = new AttributeMetaData(amd.getName() + "_" + (i + 1),
										Ontology.NOMINAL);
								Set<String> valueSet = new HashSet<>();
								for (int value = 0; value < valueParts.length; value++) {
									if (valueParts[value].length > i) {
										valueSet.add(valueParts[value][i]);
									}
								}
								newAmd.setValueSet(valueSet, amd.getValueSetRelation());
								if (i > 0) {
									newAmd.getNumberOfMissingValues().increaseByUnknownAmount();
								}
								metaData.addAttribute(newAmd);
							}
							break;
						case SPLIT_MODE_UNORDERED:
							Set<String> splitValuesSet = new HashSet<>();

							for (String value : amd.getValueSet()) {
								String[] splitValue = splittingPattern.split(value);
								for (int k = 0; k < splitValue.length; k++) {
									splitValuesSet.add(splitValue[k]);
								}
							}

							// creating new attributes
							for (String splitValue : splitValuesSet) {
								AttributeMetaData newAmd = new AttributeMetaData(amd.getName() + "_" + splitValue,
										Ontology.NOMINAL);
								Set<String> valueSet = new HashSet<>();
								valueSet.add("true");
								valueSet.add("false");

								newAmd.setValueSet(valueSet, amd.getValueSetRelation());
								metaData.addAttribute(newAmd);
							}
						default:
							break;
					}

				}
			}

			metaData.mergeSetRelation(attributeSetRelation);
		} catch (PatternSyntaxException e) {
		}
		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		String splittingRegex = getParameterAsString(PARAMETER_SPLIT_PATTERN);
		Pattern splittingPattern = null;
		try {
			splittingPattern = Pattern.compile(splittingRegex);
		} catch (PatternSyntaxException e) {
			throw new UserError(this, 206, splittingRegex, e.getMessage());
		}

		int type = getParameterAsInt(PARAMETER_SPLIT_MODE);
		// Until version 6.0.3 there was thrown no UserError when attributes were missing.
		// Compatibility check to avoid older processes to fail.
		boolean errorOnMissing = getCompatibilityLevel().isAtMost(OPERATOR_VERSION_6_0_3) ? false : true;

		for (Attribute attribute : attributeSubsetSelector.getAttributeSubset(exampleSet, false, errorOnMissing)) {
			if (attribute.isNominal()) {
				switch (type) {
					case SPLIT_MODE_ORDERED:
						orderedSplit(exampleSet, attribute, splittingPattern);
						break;
					case SPLIT_MODE_UNORDERED:
					default:
						unorderedSplit(exampleSet, attribute, splittingPattern);
						break;
				}
			}
		}

		return exampleSet;
	}

	private void orderedSplit(ExampleSet exampleSet, Attribute attribute, Pattern splittingPattern) {
		// check for maximum number
		int maxNumber = 0;
		for (Example example : exampleSet) {
			String value = example.getNominalValue(attribute);
			String[] parts = splittingPattern.split(value);
			maxNumber = Math.max(maxNumber, parts.length);
		}

		if (maxNumber >= 2) {
			// create new attributes
			Attribute[] newAttributes = new Attribute[maxNumber];
			for (int a = 0; a < maxNumber; a++) {
				newAttributes[a] = AttributeFactory.createAttribute(attribute.getName() + "_" + (a + 1), Ontology.NOMINAL);
				exampleSet.getExampleTable().addAttribute(newAttributes[a]);
				exampleSet.getAttributes().addRegular(newAttributes[a]);
			}

			// fill new attributes with values
			for (Example example : exampleSet) {
				int p = 0;
				// check if value is missing, otherwise a "?" string could be filled in.
				if (!Double.isNaN(example.getValue(attribute))) {
					String value = example.getNominalValue(attribute);
					String[] parts = splittingPattern.split(value);

					for (String part : parts) {
						example.setValue(newAttributes[p], newAttributes[p].getMapping().mapString(part));
						p++;
					}
				}

				while (p < maxNumber) {
					example.setValue(newAttributes[p], Double.NaN);
					p++;
				}
			}
			exampleSet.getAttributes().remove(attribute);
		}
	}

	private void unorderedSplit(ExampleSet exampleSet, Attribute attribute, Pattern splittingPattern) {
		// check for maximum number
		SortedSet<String> allValues = new TreeSet<>();
		boolean splitFound = false;
		for (Example example : exampleSet) {
			String value = example.getNominalValue(attribute);
			String[] parts = splittingPattern.split(value);
			for (String part : parts) {
				allValues.add(part);
			}
			if (parts.length > 1) {
				splitFound = true;
			}
		}

		if (splitFound) {
			// create new attributes
			Attribute[] newAttributes = new Attribute[allValues.size()];
			Map<String, Integer> indexMap = new HashMap<>();
			int a = 0;
			Iterator<String> v = allValues.iterator();
			while (v.hasNext()) {
				String value = v.next();
				newAttributes[a] = AttributeFactory.createAttribute(attribute.getName() + "_" + value, Ontology.BINOMINAL);
				newAttributes[a].getMapping().mapString("false");
				newAttributes[a].getMapping().mapString("true");
				exampleSet.getExampleTable().addAttribute(newAttributes[a]);
				exampleSet.getAttributes().addRegular(newAttributes[a]);
				indexMap.put(value, a);
				a++;
			}

			// fill new attributes with values
			for (Example example : exampleSet) {
				// set all new attributes to false
				for (Attribute newAttribute : newAttributes) {
					example.setValue(newAttribute, newAttribute.getMapping().mapString("false"));
				}

				String value = example.getNominalValue(attribute);
				String[] parts = splittingPattern.split(value);
				// int p = 0;
				for (String part : parts) {
					Attribute newAttribute = newAttributes[indexMap.get(part)];
					example.setValue(newAttribute, newAttribute.getMapping().mapString("true"));
					// p++;
				}
			}
			exampleSet.getAttributes().remove(attribute);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(attributeSubsetSelector.getParameterTypes());

		ParameterType type = new ParameterTypeRegexp(PARAMETER_SPLIT_PATTERN,
				"The pattern which is used for dividing the nominal values into different parts.", ",");
		type.setExpert(false);
		type.setPrimary(true);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_SPLIT_MODE,
				"The split mode of this operator, either ordered splits (keeping the original order) or unordered (keeping basket-like information).",
				SPLIT_MODES, SPLIT_MODE_ORDERED);
		type.setExpert(false);
		types.add(type);

		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), AttributeValueSplit.class,
				attributeSubsetSelector);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] changes = super.getIncompatibleVersionChanges();
		changes = Arrays.copyOf(changes, changes.length + 1);
		changes[changes.length - 1] = OPERATOR_VERSION_6_0_3;
		return changes;
	}
}
