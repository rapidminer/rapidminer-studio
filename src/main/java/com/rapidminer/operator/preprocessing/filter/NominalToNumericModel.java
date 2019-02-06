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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.SimpleAttributes;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.ViewAttribute;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;


/**
 * The model class for the {@link NominalToNumericModel} operator. Can either transform nominals to
 * numeric by simply replacing the nominal values by the respective integer mapping, or by using
 * effect coding or dummy coding.
 *
 * @author Marius Helf
 */
public class NominalToNumericModel extends PreprocessingModel {

	private static final long serialVersionUID = -4203775081616082145L;

	private int codingType;

	/**
	 * maps a target attribute to the value for which it becomes one (for dummy coding)
	 */
	private Map<String, Double> attributeTo1ValueMap = null;

	/**
	 * maps a target attribute to the value for which it becomes -1 or 1 respectively (for effect
	 * coding). The first value of the pair is the value for 1, the second for -1.
	 */
	private Map<String, Pair<Double, Double>> attributeToValuesMap = null;

	/**
	 * maps an original attribute name to the list of all its values which occurred in the training
	 * data. Used for dummy and effect coding.
	 */
	private Map<String, List<String>> attributeToAllNominalValues = null;

	/**
	 * maps source attributes to their comparison group.
	 */
	private Map<String, Double> sourceAttributeToComparisonGroupMap = null;

	/**
	 * maps target attributes in the output set to their respective source attributes in the
	 * training set.
	 */
	private Map<String, String> targetAttributeToSourceAttributeMap = null;

	/**
	 * maps source attributes to the comparison group string. Only used with dummy/effect coding.
	 *
	 * This map is *only* used for displaying the model (i.e. in toResultString()).
	 */
	private Map<String, String> sourceAttributeToComparisonGroupStringsMap = null;

	/**
	 * Relevant only when using dummy coding or effect coding.
	 *
	 * If true, the naming scheme for target attributes is "sourceAttribute_value", if false,
	 * "sourceAttribute = value"
	 */
	private boolean useUnderscoreInName = false;

	private boolean useComparisonGroups = false;

	// how unexpected values are handled. One of ALL_ZEROES_AND_WARNING or ERROR_AND_ABORT.
	private int unexpectedValueHandling = NominalToNumeric.ALL_ZEROES_AND_WARNING;

	/**
	 * Constructs a new model. Use this ctor to create a model for value encoding.
	 *
	 * @param exampleSet
	 * @param codingType
	 *            the coding type. Should be NominalToNumeric.INTEGERS when called manually.
	 */
	public NominalToNumericModel(ExampleSet exampleSet, int codingType) {
		super(exampleSet);
		this.codingType = codingType;
	}

	/**
	 * Constructs a new model. Use this ctor to create a model for dummy encoding or effect
	 * encoding.
	 *
	 * @param exampleSet
	 * @param codingType
	 *            the coding type. Should be NominalToNumeric.EFFECT_CODING or DUMMY_CODING.
	 * @param useUnderscoreInName
	 * @see NominalToNumericModel#useUnderscoreInName
	 * @param sourceAttributeToComparisonGroupMap
	 * @see NominalToNumericModel#sourceAttributeToComparisonGroupMap @see
	 *      NominalToNumeric#getSourceAttributeToComparisonGroupMap
	 * @param attributeTo1ValueMap
	 * @see NominalToNumericModel#attributeTo1ValueMap be non-null for dummy coding, should be null
	 *      for effect coding. @see NominalToNumeric#getAttributeTo1ValueMap
	 * @param attributeToValuesMap
	 * @see NominalToNumericModel#attributeToValuesMap be non-null for effect coding, should be null
	 *      for dummy coding. @see NominalToNumeric#getAttributeToValuesMap
	 * @param useComparisonGroup
	 *            Indicates if comparison groups for dummy coding should be used. Is ignored if
	 *            codingType == EFFECT_CODING.
	 * @param unexpectedValueHandling
	 *            Defines how unexpected values are handled. @see
	 *            NominalToNumericModel#unexpectedValueHandling.
	 */
	public NominalToNumericModel(ExampleSet exampleSet, int codingType, boolean useUnderscoreInName,
			Map<String, Double> sourceAttributeToComparisonGroupMap, Map<String, Double> attributeTo1ValueMap,
			Map<String, Pair<Double, Double>> attributeToValuesMap, boolean useComparisonGroups,
			int unexpectedValueHandling) {
		this(exampleSet, codingType);
		this.useUnderscoreInName = useUnderscoreInName;
		this.sourceAttributeToComparisonGroupMap = sourceAttributeToComparisonGroupMap;
		this.attributeTo1ValueMap = attributeTo1ValueMap;
		this.attributeToValuesMap = attributeToValuesMap;
		this.useComparisonGroups = useComparisonGroups || codingType == NominalToNumeric.EFFECT_CODING;
		this.unexpectedValueHandling = unexpectedValueHandling;

		if (useComparisonGroups) {
			// store comparison group strings for display
			assert sourceAttributeToComparisonGroupMap != null; // must not be null for
																 // dummy/effect coding

			sourceAttributeToComparisonGroupStringsMap = new LinkedHashMap<>();
			for (Map.Entry<String, Double> entry : sourceAttributeToComparisonGroupMap.entrySet()) {
				String attributeName = entry.getKey();
				double comparisonGroup = entry.getValue();
				Attribute attribute = exampleSet.getAttributes().get(attributeName);
				String comparisonGroupString = attribute.getMapping().mapIndex((int) comparisonGroup);
				sourceAttributeToComparisonGroupStringsMap.put(attributeName, comparisonGroupString);
			}
		}

		if (codingType == NominalToNumeric.DUMMY_CODING || codingType == NominalToNumeric.EFFECT_CODING) {
			// remember all nominal values from training data
			attributeToAllNominalValues = new HashMap<>();
			for (Attribute attribute : exampleSet.getAttributes()) {
				if (!attribute.isNumerical()) {
					String attributeName = attribute.getName();
					List<String> values = new LinkedList<>();
					for (String value : attribute.getMapping().getValues()) {
						values.add(value);
					}
					attributeToAllNominalValues.put(attributeName, values);
				}
			}

			// remember source attributes for each target attribute
			targetAttributeToSourceAttributeMap = new HashMap<>();
			for (Attribute sourceAttribute : exampleSet.getAttributes()) {
				if (!sourceAttribute.isNumerical()) {
					String sourceAttributeName = sourceAttribute.getName();
					for (String targetAttribute : getTargetAttributesFromSourceAttribute(sourceAttribute)) {
						targetAttributeToSourceAttributeMap.put(targetAttribute, sourceAttributeName);
					}
				}
			}
		}
	}

	@Override
	public ExampleSet applyOnData(ExampleSet exampleSet) throws OperatorException {
		switch (codingType) {
			case NominalToNumeric.INTEGERS_CODING:
				return applyOnDataIntegers(exampleSet);
			case NominalToNumeric.DUMMY_CODING:
				return applyOnDataDummyCoding(exampleSet, false);
			case NominalToNumeric.EFFECT_CODING:
				return applyOnDataDummyCoding(exampleSet, true);
			default:
				assert false;	// codingType must be one of the above
				return null;
		}
	}

	/**
	 * Returns a list containing the names of those attributes which will represent the coding of
	 * the given source attribute.
	 */
	private List<String> getTargetAttributesFromSourceAttribute(Attribute sourceAttribute) {
		List<String> targetNames = new ArrayList<>();
		double comparisonGroup = -1;
		if (useComparisonGroups) {
			comparisonGroup = sourceAttributeToComparisonGroupMap.get(sourceAttribute.getName());
		}

		List<String> originalAttributeValues = attributeToAllNominalValues.get(sourceAttribute.getName());
		String comparisonGroupValue = null;
		if (comparisonGroup != -1) {
			comparisonGroupValue = originalAttributeValues.get((int) comparisonGroup);
		}
		for (String currentValue : originalAttributeValues) {
			if (!useComparisonGroups || !currentValue.equals(comparisonGroupValue)) {
				targetNames.add(NominalToNumeric.getTargetAttributeName(sourceAttribute.getName(), currentValue,
						useUnderscoreInName));
			}
		}
		return targetNames;
	}

	/**
	 * Creates a dummy coding or effect coding from the given example set.
	 *
	 * @param effectCoding
	 *            If true, the function does effect coding. If false, dummy coding.
	 * @throws ProcessStoppedException
	 */
	private ExampleSet applyOnDataDummyCoding(ExampleSet exampleSet, boolean effectCoding) throws ProcessStoppedException {
		// selecting transformation attributes and creating new numeric attributes
		List<Attribute> nominalAttributes = new ArrayList<>();
		List<Attribute> transformedAttributes = new ArrayList<>();
		Map<Attribute, List<Attribute>> targetAttributesFromSources = new HashMap<>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (!attribute.isNumerical()) {
				nominalAttributes.add(attribute);
				List<String> targetNames = getTargetAttributesFromSourceAttribute(attribute);
				List<Attribute> targets = new ArrayList<>();
				for (String targetName : targetNames) {
					Attribute createAttribute = AttributeFactory.createAttribute(targetName, Ontology.INTEGER);
					transformedAttributes.add(createAttribute);
					targets.add(createAttribute);
				}
				targetAttributesFromSources.put(attribute, targets);
			}
		}

		// ensuring capacity in ExampleTable
		exampleSet.getExampleTable().addAttributes(transformedAttributes);
		for (Attribute attribute : transformedAttributes) {
			exampleSet.getAttributes().addRegular(attribute);
		}

		// initialize progress
		long progressCompletedCounter = 0;
		long progressTotal = (long) nominalAttributes.size() * exampleSet.size();
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(1000);
		}

		// copying values
		for (Attribute nominalAttribute : nominalAttributes) {
			for (Example example : exampleSet) {
				double sourceValue = example.getValue(nominalAttribute);
				for (Attribute targetAttribute : targetAttributesFromSources.get(nominalAttribute)) {
					example.setValue(targetAttribute, getValue(targetAttribute, sourceValue));
				}
				if (progress != null && ++progressCompletedCounter % 10_000 == 0) {
					progress.setCompleted((int) (1000.0d * progressCompletedCounter / progressTotal));
				}
			}
		}

		// remove nominal attributes
		for (Attribute nominalAttribute : nominalAttributes) {
			exampleSet.getAttributes().remove(nominalAttribute);
		}
		return exampleSet;
	}

	/**
	 * Transforms the numerical attributes to integer values (corresponding to the internal
	 * mapping).
	 *
	 * @throws ProcessStoppedException
	 */
	private ExampleSet applyOnDataIntegers(ExampleSet exampleSet) throws ProcessStoppedException {
		// selecting transformation attributes and creating new numeric attributes
		List<Attribute> nominalAttributes = new ArrayList<>();
		LinkedList<Attribute> transformedAttributes = new LinkedList<>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (!attribute.isNumerical()) {
				nominalAttributes.add(attribute);
				// creating new attributes for nominal attributes
				transformedAttributes.add(AttributeFactory.createAttribute(attribute.getName(), Ontology.NUMERICAL));
			}
		}

		// ensuring capacity in ExampleTable
		exampleSet.getExampleTable().addAttributes(transformedAttributes);

		// initialize progress
		long progressCompletedCounter = 0;
		long progressTotal = (long) nominalAttributes.size() * exampleSet.size();
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(1000);
		}

		// copying values
		Iterator<Attribute> target = transformedAttributes.iterator();
		for (Attribute attribute : nominalAttributes) {
			Attribute targetAttribute = target.next();
			for (Example example : exampleSet) {
				example.setValue(targetAttribute, example.getValue(attribute));
				if (progress != null && ++progressCompletedCounter % 100_000 == 0) {
					progress.setCompleted((int) (1000.0d * progressCompletedCounter / progressTotal));
				}
			}
		}

		// removing nominal attributes from example Set
		Attributes attributes = exampleSet.getAttributes();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (!attribute.isNumerical()) {
				attributes.replace(attribute, transformedAttributes.poll());
			}
		}
		return exampleSet;
	}

	@Override
	public Attributes getTargetAttributes(ExampleSet parentSet) {
		SimpleAttributes attributes = new SimpleAttributes();
		// add special attributes to new attributes
		Iterator<AttributeRole> specialRoles = parentSet.getAttributes().specialAttributes();
		while (specialRoles.hasNext()) {
			attributes.add(specialRoles.next());
		}

		// add regular attributes
		for (Attribute attribute : parentSet.getAttributes()) {
			if (!attribute.isNumerical()) {
				if (codingType == NominalToNumeric.EFFECT_CODING || codingType == NominalToNumeric.DUMMY_CODING) {
					double comparisonGroup = -1;
					if (useComparisonGroups) {
						comparisonGroup = sourceAttributeToComparisonGroupMap.get(attribute.getName());
					}
					List<String> valueList = attributeToAllNominalValues.get(attribute.getName());
					if (valueList != null) {
						int currentValue = 0;
						for (String attributeValue : valueList) {
							if (currentValue != comparisonGroup) {
								ViewAttribute viewAttribute = new ViewAttribute(this, attribute, NominalToNumeric
										.getTargetAttributeName(attribute.getName(), attributeValue, useUnderscoreInName),
										Ontology.INTEGER, null);
								attributes.addRegular(viewAttribute);
							}
							++currentValue;
						}
					}
				} else if (codingType == NominalToNumeric.INTEGERS_CODING) {
					attributes.addRegular(new ViewAttribute(this, attribute, attribute.getName(), Ontology.INTEGER, null));
				} else {
					assert false; // unsupported coding
				}
			} else {
				attributes.addRegular(attribute);
			}
		}
		return attributes;
	}

	@Override
	public double getValue(Attribute targetAttribute, double value) {
		if (codingType == NominalToNumeric.DUMMY_CODING) {
			String targetName = targetAttribute.getName();
			Double oneValue = attributeTo1ValueMap.get(targetName);
			if (oneValue != null && oneValue == value) {
				return 1;
			} else {
				// check if the value has been present in the training set
				if (unexpectedValueHandling != NominalToNumeric.ALL_ZEROES_AND_NO_WARNING
						&& !isValueInTrainingSet(targetAttribute, value)) {
					handleUnexpectedValue(targetName);
				}

				return 0;
			}
		} else if (codingType == NominalToNumeric.EFFECT_CODING) {
			String targetName = targetAttribute.getName();
			Pair<Double, Double> storedValue = attributeToValuesMap.get(targetName);
			if (storedValue.getFirst() == value) {
				return 1;
			} else if (storedValue.getSecond() == value) {
				return -1;
			} else {
				// check if the value has been present in the training set
				if (unexpectedValueHandling != NominalToNumeric.ALL_ZEROES_AND_NO_WARNING
						&& !isValueInTrainingSet(targetAttribute, value)) {
					handleUnexpectedValue(targetName);
				}
				return 0;
			}
		} else if (codingType == NominalToNumeric.INTEGERS_CODING) {
			return value;
		} else {
			assert false; // unsupported coding
			return Double.NaN;
		}
	}

	private int handleUnexpectedValue(String targetName) {
		switch (unexpectedValueHandling) {
			case NominalToNumeric.ALL_ZEROES_AND_WARNING:
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.operator.preprocessing.filter.NominalToNumericModel.unexpected_value", targetName);
				return 0;
			case NominalToNumeric.ALL_ZEROES_AND_NO_WARNING:
				return 0;
			default:
				assert false;	// should be one of the above values
				return 0;
		}
	}

	private boolean isValueInTrainingSet(Attribute targetAttribute, double value) {
		String sourceAttribute = targetAttributeToSourceAttributeMap.get(targetAttribute.getName());
		if (sourceAttribute != null) {
			List<String> trainingValues = attributeToAllNominalValues.get(sourceAttribute);
			if (trainingValues != null) {
				int valueCount = trainingValues.size();
				if (value >= valueCount) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	@Override
	public String getName() {
		return "Nominal2Numerical Model";
	}

	@Override
	public String toResultString() {
		StringBuilder builder = new StringBuilder();
		Attributes trainAttributes = getTrainingHeader().getAttributes();
		builder.append(getName() + Tools.getLineSeparators(2));
		String codingTypeString = "";
		switch (codingType) {
			case NominalToNumeric.INTEGERS_CODING:
				codingTypeString = "unique integers";
				break;
			case NominalToNumeric.DUMMY_CODING:
				codingTypeString = "dummy coding";
				break;
			case NominalToNumeric.EFFECT_CODING:
				codingTypeString = "effect coding";
				break;
		}
		builder.append("Coding Type: " + codingTypeString + Tools.getLineSeparator());
		if (!useComparisonGroups) {
			builder.append("Model covering " + trainAttributes.size() + " attributes:" + Tools.getLineSeparator());
			for (Attribute attribute : trainAttributes) {
				builder.append(" - " + attribute.getName() + Tools.getLineSeparator());
			}
		} else {
			builder.append("Model covering " + trainAttributes.size() + " attributes (with comparison group):"
					+ Tools.getLineSeparator());
			for (Attribute attribute : trainAttributes) {
				builder.append(" - " + attribute.getName() + " ('"
						+ sourceAttributeToComparisonGroupStringsMap.get(attribute.getName()) + "')"
						+ Tools.getLineSeparator());
			}
		}
		return builder.toString();
	}

	public int getCodingType() {
		return codingType;
	}

	public Map<String, Double> getAttributeTo1ValueMap() {
		return attributeTo1ValueMap;
	}

	public Map<String, Pair<Double, Double>> getAttributeToValuesMap() {
		return attributeToValuesMap;
	}

	public Map<String, List<String>> getAttributeToAllNominalValues() {
		return attributeToAllNominalValues;
	}

	public Map<String, Double> getSourceAttributeToComparisonGroupMap() {
		return sourceAttributeToComparisonGroupMap;
	}

	public Map<String, String> getTargetAttributeToSourceAttributeMap() {
		return targetAttributeToSourceAttributeMap;
	}

	public boolean shouldUseUnderscoreInName() {
		return useUnderscoreInName;
	}

	public boolean shouldUseComparisonGroups() {
		return useComparisonGroups;
	}

	public int getUnexpectedValueHandling() {
		return unexpectedValueHandling;
	}

}
