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
package com.rapidminer.operator.preprocessing.transformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeRegexp;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * This operator converts an example set by dividing examples which consist of multiple observations
 * (at different times) into multiple examples, where each example covers on point in time. An index
 * attribute is added, which contains denotes the actual point in time the example belongs to after
 * the transformation. The parameter <code>keep_missings</code> specifies whether examples should be
 * kept, even if if exhibits missing values for all series at a certain point in time. The parameter
 * create_nominal_index is only applicable if only one time series per example exists. Instead of
 * using a numeric index, then the names of the attributes representing the single time points are
 * used as index attribute.
 *
 * @author Tobias Malbrecht
 */
public class Attribute2ExamplePivoting extends ExampleSetTransformationOperator {

	private static final OperatorVersion CHANGE_INCLUDED_SPECIAL = new OperatorVersion(5, 1, 1);

	public static final String PARAMETER_ATTRIBUTE_NAME_REGEX = "attributes";

	public static final String PARAMETER_SERIES = "attribute_name";

	public static final String PARAMETER_INDEX_ATTRIBUTE = "index_attribute";

	public static final String PARAMETER_KEEP_MISSINGS = "keep_missings";

	public static final String PARAMETER_CREATE_NOMINAL_INDEX = "create_nominal_index";

	public Attribute2ExamplePivoting(OperatorDescription description) {
		super(description);

		getExampleSetInputPort().addPrecondition(new ExampleSetPrecondition(getExampleSetInputPort()));
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) throws UndefinedParameterError {
		List<String[]> seriesList = getParameterList(PARAMETER_SERIES);

		ExampleSetMetaData emd = new ExampleSetMetaData();
		emd.getNumberOfExamples().increaseByUnknownAmount();
		emd.mergeSetRelation(SetRelation.SUBSET);

		int numberOfSeries = seriesList.size();
		boolean createNominalIndex = getParameterAsBoolean(PARAMETER_CREATE_NOMINAL_INDEX);
		// checking if nominal index should be created and only one series must be treated
		if (numberOfSeries > 1 && createNominalIndex) {
			addError(new SimpleProcessSetupError(Severity.ERROR, getPortOwner(),
					Collections.singletonList(new ParameterSettingQuickFix(this, PARAMETER_CREATE_NOMINAL_INDEX, "false")),
					"parameter_combination_number_forbidden", PARAMETER_CREATE_NOMINAL_INDEX, PARAMETER_SERIES, "1"));
		}

		String[] seriesNames = new String[numberOfSeries];
		Pattern[] seriesPatterns = new Pattern[numberOfSeries];
		ArrayList<Vector<AttributeMetaData>> seriesAttributes = new ArrayList<>(numberOfSeries);
		int[] attributeTypes = new int[numberOfSeries];
		Iterator<String[]> iterator = seriesList.iterator();
		int j = 0;
		while (iterator.hasNext()) {
			String[] pair = iterator.next();
			seriesNames[j] = pair[0];
			seriesPatterns[j] = Pattern.compile(pair[1]);
			seriesAttributes.add(j, new Vector<>());
			j++;
		}
		String indexParamName = getParameterAsString(PARAMETER_INDEX_ATTRIBUTE);
		// identify series attributes and check attribute types
		for (AttributeMetaData attribute : metaData.getAllAttributes()) {
			if (attribute.getName().equals(indexParamName)) {
				addError(new SimpleMetaDataError(Severity.ERROR,
						getInputPort(), Collections.singletonList(new ParameterSettingQuickFix(this,
								PARAMETER_INDEX_ATTRIBUTE, "newly_quick_fix_created_index_attr")),
						"already_contains_attribute", indexParamName));
			}
			if (!attribute.isSpecial()) {
				boolean matched = false;
				for (int i = 0; i < numberOfSeries; i++) {
					Matcher matcher = seriesPatterns[i].matcher(attribute.getName());
					if (matcher.matches()) {
						matched = true;
						seriesAttributes.get(i).add(attribute);
						attributeTypes[i] = attribute.getValueType();
						break;
					}
				}
				if (!matched) {
					emd.addAttribute(attribute);
				}
			}
		}

		// index attribute
		if (!createNominalIndex) {
			emd.addAttribute(new AttributeMetaData(getParameterAsString(PARAMETER_INDEX_ATTRIBUTE), Ontology.INTEGER));
		} else {
			emd.addAttribute(new AttributeMetaData(getParameterAsString(PARAMETER_INDEX_ATTRIBUTE), Ontology.POLYNOMINAL));
		}

		// series attributes
		for (int i = 0; i < numberOfSeries; i++) {
			emd.addAttribute(new AttributeMetaData(seriesNames[i], attributeTypes[i]));
		}

		for (AttributeMetaData amd : emd.getAllAttributes()) {
			amd.getNumberOfMissingValues().increaseByUnknownAmount();
		}

		return emd;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// init operator progress
		getProgress().setTotal(exampleSet.size());

		List<String[]> seriesList = getParameterList(PARAMETER_SERIES);

		int numberOfSeries = seriesList.size();
		boolean createNominalIndex = getParameterAsBoolean(PARAMETER_CREATE_NOMINAL_INDEX);
		// checking if nominal index should be created and only one series must be treated
		if (numberOfSeries > 1 && createNominalIndex) {
			throw new UserError(this, 207, new Object[] { "true", PARAMETER_CREATE_NOMINAL_INDEX,
					"More than one series listed in attribute names" });
		}

		String[] seriesNames = new String[numberOfSeries];
		Pattern[] seriesPatterns = new Pattern[numberOfSeries];
		ArrayList<Vector<Attribute>> seriesAttributes = new ArrayList<>(numberOfSeries);
		int[] attributeTypes = new int[numberOfSeries];
		Iterator<String[]> iterator = seriesList.iterator();
		int j = 0;
		while (iterator.hasNext()) {
			String[] pair = iterator.next();
			seriesNames[j] = pair[0];
			seriesPatterns[j] = Pattern.compile(pair[1]);
			seriesAttributes.add(j, new Vector<>());
			attributeTypes[j] = Ontology.ATTRIBUTE_VALUE;
			j++;
		}

		Vector<Attribute> newAttributes = new Vector<>();
		Vector<Attribute> constantAttributes = new Vector<>();

		// identify series attributes and check attribute types
		Iterator<Attribute> attributes;

		/*
		 * COMPATIBILITY: depending on the operator version we change the behavior
		 */
		if (getCompatibilityLevel().isAtMost(CHANGE_INCLUDED_SPECIAL)) {
			attributes = exampleSet.getAttributes().iterator();
		} else {
			attributes = exampleSet.getAttributes().allAttributes();
		}

		Attribute attribute;
		while (attributes.hasNext()) {
			attribute = attributes.next();

			boolean matched = false;
			for (int i = 0; i < numberOfSeries; i++) {
				Matcher matcher = seriesPatterns[i].matcher(attribute.getName());
				if (matcher.matches()) {
					matched = true;
					seriesAttributes.get(i).add(attribute);
					if (attributeTypes[i] != Ontology.ATTRIBUTE_VALUE) {
						if (attribute.getValueType() != attributeTypes[i]) {
							throw new UserError(this, "de_pivot.type_mismatch", attribute.getName(), Ontology.VALUE_TYPE_NAMES[attribute.getValueType()], Ontology.VALUE_TYPE_NAMES[attributeTypes[i]], seriesNames[i]);
						}
					} else {
						attributeTypes[i] = attribute.getValueType();
					}
				}
			}
			if (!matched) {
				Attribute attributeCopy = AttributeFactory.createAttribute(attribute.getName(), attribute.getValueType());
				if (attribute.isNominal()) {
					attributeCopy.setMapping((NominalMapping) attribute.getMapping().clone());
				}
				newAttributes.add(attributeCopy);
				constantAttributes.add(attribute);
			}
		}

		// check series length
		int seriesLength = 0;
		if (numberOfSeries >= 1) {
			seriesLength = seriesAttributes.get(0).size();
			for (int i = 0; i < numberOfSeries - 1; i++) {
				seriesLength = seriesAttributes.get(i).size();
				if (seriesLength != seriesAttributes.get(i + 1).size()) {
					throw new UserError(this, "de_pivot.length_mismatch", seriesNames[i], seriesAttributes.get(i).size(), seriesNames[i + 1], seriesAttributes.get(i + 1).size());
				}
			}
		}

		// index attributes
		Attribute indexAttribute;
		if (!createNominalIndex) {
			indexAttribute = AttributeFactory.createAttribute(getParameterAsString(PARAMETER_INDEX_ATTRIBUTE),
					Ontology.INTEGER);
		} else {
			indexAttribute = AttributeFactory.createAttribute(getParameterAsString(PARAMETER_INDEX_ATTRIBUTE),
					Ontology.POLYNOMINAL);
		}
		if (newAttributes.contains(indexAttribute)) {
			throw new UserError(this, 956, indexAttribute.getName());
		} else {
			newAttributes.add(indexAttribute);
		}

		// series attributes
		for (int i = 0; i < numberOfSeries; i++) {
			if (attributeTypes[i] == Ontology.ATTRIBUTE_VALUE) {
				logError("Cannot create pivot attribute " + seriesNames[i] + ": No matching attributes found.");
			} else {
				Attribute seriesAttribute = AttributeFactory.createAttribute(seriesNames[i], attributeTypes[i]);
				newAttributes.add(seriesAttribute);
			}
		}

		ExampleSetBuilder builder = ExampleSets.from(newAttributes);
		boolean keepMissings = getParameterAsBoolean(PARAMETER_KEEP_MISSINGS);
		int counter = 0;
		for (Example example : exampleSet) {

			// report progress every 100 examples
			++counter;
			if (counter % 100 == 0) {
				getProgress().step(100);
				counter = 0;
			}

			int l = 0;
			for (int k = 0; k < seriesLength; k++) {
				l++;
				double[] data = new double[newAttributes.size()];
				for (int i = 0; i < data.length; i++) {
					data[i] = Double.NaN;
				}

				// set constant attribute values
				for (int i = 0; i < constantAttributes.size(); i++) {
					data[i] = example.getValue(constantAttributes.get(i));
				}

				// set index attribute value
				if (!createNominalIndex) {
					data[data.length - numberOfSeries - 1] = l;
				} else {
					data[data.length - numberOfSeries - 1] = indexAttribute.getMapping()
							.mapString(seriesAttributes.get(0).get(k).getName());
				}

				// set series attribute values
				boolean onlyMissings = true;
				for (int i = 0; i < numberOfSeries; i++) {
					Attribute seriesAttribute = seriesAttributes.get(i).get(k);
					double seriesValue = example.getValue(seriesAttribute);
					double newValue = Double.NaN;
					if (!Double.isNaN(seriesValue)) {
						if (seriesAttribute.isNominal()) {
							newValue = newAttributes.get(newAttributes.size() - numberOfSeries + i).getMapping()
									.mapString(seriesAttribute.getMapping().mapIndex((int) seriesValue));
						} else {
							newValue = seriesValue;
						}
						onlyMissings = false;
					}
					data[data.length - numberOfSeries + i] = newValue;
				}
				checkForStop();
				if (!keepMissings && onlyMissings) {
					continue;
				} else {
					builder.addRow(data);
				}
			}
		}

		// create and deliver example set
		ExampleSet result = builder.build();
		result.recalculateAllAttributeStatistics();
		result.getAnnotations().addAll(exampleSet.getAnnotations());

		getProgress().complete();
		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeList(PARAMETER_SERIES,
				"Maps a number of source attributes onto result attributes.",
				new ParameterTypeString("attribute_name", "Specifies the name of the resulting attribute"),
				new ParameterTypeRegexp(PARAMETER_ATTRIBUTE_NAME_REGEX, "Attributes that forms series.", false) {

					private static final long serialVersionUID = 8133149560984042645L;

					@Override
					public Collection<String> getPreviewList() {
						InputPort inPort = getInputPort();
						Collection<String> regExpPreviewList = new LinkedList<>();
						if (inPort == null) {
							return super.getPreviewList();
						}
						MetaData metaData = inPort.getMetaData();
						if (metaData instanceof ExampleSetMetaData) {
							ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
							for (AttributeMetaData amd : emd.getAllAttributes()) {
									regExpPreviewList.add(amd.getName());
							}
						}
						return regExpPreviewList;
					}
				});
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeString(PARAMETER_INDEX_ATTRIBUTE, "Name of newly created index attribute.", false, false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_CREATE_NOMINAL_INDEX,
				"Indicates if the index attribute should contain the full attribute name. (Only works with one group)",
				false);
		types.add(type);
		type = new ParameterTypeBoolean(PARAMETER_KEEP_MISSINGS, "Keep missing values.", false);
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
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				Attribute2ExamplePivoting.class, null);
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return new OperatorVersion[] { CHANGE_INCLUDED_SPECIAL };
	}
}
