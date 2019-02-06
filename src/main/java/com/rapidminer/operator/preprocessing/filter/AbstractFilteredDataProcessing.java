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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.NonSpecialAttributesExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.ProcessTools;


/**
 * This class is for preprocessing operators, which can be restricted to use only a subset of the
 * attributes. The MetaData is changed accordingly in a way equivalent to surrounding the operator
 * with an AttributeSubsetPreprocessing operator. Subclasses must overwrite the methods
 * {@link #applyOnFiltered(ExampleSet)} and {@link #applyOnFilteredMetaData(ExampleSetMetaData)} in
 * order to provide their functionality and the correct meta data handling.
 * 
 * @author Sebastian Land
 * 
 */
public abstract class AbstractFilteredDataProcessing extends AbstractDataProcessing {

	private final AttributeSubsetSelector attributeSelector = new AttributeSubsetSelector(this, getExampleSetInputPort(),
			getFilterValueTypes());;

	public AbstractFilteredDataProcessing(OperatorDescription description) {
		super(description);
	}

	private static final OperatorVersion FAIL_ON_MISSING_ATTRIBUTES = new OperatorVersion(6, 0, 3);

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] odlOne = super.getIncompatibleVersionChanges();
		OperatorVersion[] newOne = Arrays.copyOf(odlOne, odlOne.length + 1);
		newOne[odlOne.length] = FAIL_ON_MISSING_ATTRIBUTES;
		return newOne;
	}

	@Override
	protected final MetaData modifyMetaData(ExampleSetMetaData inputMetaData) {
		ExampleSetMetaData workingMetaData = inputMetaData.clone();

		ExampleSetMetaData subsetAmd = attributeSelector.getMetaDataSubset(workingMetaData, false);

		// storing unused attributes and saving roles
		List<AttributeMetaData> unusedAttributes = new LinkedList<>();
		Iterator<AttributeMetaData> iterator = workingMetaData.getAllAttributes().iterator();
		while (iterator.hasNext()) {
			AttributeMetaData amd = iterator.next();
			String name = amd.getName();
			MetaDataInfo containsAttributeName = subsetAmd.containsAttributeName(name);

			if (subsetAmd.getAttributeSetRelation() == SetRelation.SUBSET && containsAttributeName == MetaDataInfo.NO
					|| subsetAmd.getAttributeSetRelation() != SetRelation.SUBSET
					&& containsAttributeName != MetaDataInfo.YES) {
				unusedAttributes.add(amd);
				iterator.remove();
			} else if (amd.isSpecial()) {
				amd.setRegular();
			}
		}

		// retrieving result
		ExampleSetMetaData resultMetaData = workingMetaData;
		try {
			resultMetaData = applyOnFilteredMetaData(workingMetaData);
		} catch (UndefinedParameterError e) {
		}

		// merge result with unusedAttributes: restore special types from original input
		Iterator<AttributeMetaData> r = resultMetaData.getAllAttributes().iterator();
		while (r.hasNext()) {
			AttributeMetaData newMetaData = r.next();
			AttributeMetaData oldMetaData = inputMetaData.getAttributeByName(newMetaData.getName());
			if (oldMetaData != null) {
				if (oldMetaData.isSpecial()) {
					String specialName = oldMetaData.getRole();
					newMetaData.setRole(specialName);
				}
			}
		}

		// add unused attributes again
		resultMetaData.addAllAttributes(unusedAttributes);
		return resultMetaData;
	}

	@Override
	/**
	 * This method filters the attributes according to the AttributeSubsetSelector and
	 * then applies the operation of the subclass on this data. Finally the changed data is merged
	 * back into the exampleSet. This is done in the AttributeSubsetPreprocessing way and somehow doubles the
	 * code.
	 */
	public final ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		ExampleSet workingExampleSet = (ExampleSet) exampleSet.clone();
		Set<Attribute> selectedAttributes = attributeSelector.getAttributeSubset(workingExampleSet, false, this
				.getCompatibilityLevel().isAtMost(FAIL_ON_MISSING_ATTRIBUTES) ? false : true);

		List<Attribute> unusedAttributes = new LinkedList<>();
		Iterator<Attribute> iterator = workingExampleSet.getAttributes().allAttributes();
		while (iterator.hasNext()) {
			Attribute attribute = iterator.next();
			if (!selectedAttributes.contains(attribute)) {
				unusedAttributes.add(attribute);
				iterator.remove();
			}
		}

		// converting special to normal
		workingExampleSet = NonSpecialAttributesExampleSet.create(workingExampleSet);

		// applying filtering
		ExampleSet resultSet = applyOnFiltered(workingExampleSet);

		// transform special attributes back
		Iterator<AttributeRole> r = resultSet.getAttributes().allAttributeRoles();
		while (r.hasNext()) {
			AttributeRole newRole = r.next();
			AttributeRole oldRole = exampleSet.getAttributes().getRole(newRole.getAttribute().getName());
			if (oldRole != null) {
				if (oldRole.isSpecial()) {
					String specialName = oldRole.getSpecialName();
					newRole.setSpecial(specialName);
				}
			}
		}

		// add old attributes if desired
		if (resultSet.size() != exampleSet.size()) {
			throw new UserError(this, 127,
					"changing the size of the example set is not allowed if the non-processed attributes should be kept.");
		}

		if (resultSet.getExampleTable().equals(exampleSet.getExampleTable())) {
			for (Attribute attribute : unusedAttributes) {
				AttributeRole role = exampleSet.getAttributes().getRole(attribute);
				resultSet.getAttributes().add(role);
			}
		} else {
			getLogger()
					.warning(
							"Underlying example table has changed: data copy into new table is necessary in order to keep non-processed attributes.");
			for (Attribute oldAttribute : unusedAttributes) {
				AttributeRole oldRole = exampleSet.getAttributes().getRole(oldAttribute);

				// create and add copy of attribute
				Attribute newAttribute = (Attribute) oldAttribute.clone();
				resultSet.getExampleTable().addAttribute(newAttribute);
				AttributeRole newRole = new AttributeRole(newAttribute);
				if (oldRole.isSpecial()) {
					newRole.setSpecial(oldRole.getSpecialName());
				}
				resultSet.getAttributes().add(newRole);

				// copy data for the new attribute
				Iterator<Example> oldIterator = exampleSet.iterator();
				Iterator<Example> newIterator = resultSet.iterator();
				while (oldIterator.hasNext()) {
					Example oldExample = oldIterator.next();
					Example newExample = newIterator.next();
					newExample.setValue(newAttribute, oldExample.getValue(oldAttribute));
				}
			}
		}
		return resultSet;
	}

	/**
	 * Subclasses have to implement this method in order to operate only on the selected attributes.
	 * The results are merged back into the original example set.
	 */
	public abstract ExampleSet applyOnFiltered(ExampleSet exampleSet) throws OperatorException;

	/**
	 * This method has to be implemented in order to specify the changes of the meta data caused by
	 * the application of this operator.
	 */
	public abstract ExampleSetMetaData applyOnFilteredMetaData(ExampleSetMetaData emd) throws UndefinedParameterError;

	/**
	 * Defines the value types of the attributes which are processed or affected by this operator.
	 * Has to be overridden to restrict the attributes which can be chosen by an
	 * {@link AttributeSubsetSelector}.
	 * 
	 * @return array of value types
	 */
	protected abstract int[] getFilterValueTypes();

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(ProcessTools.setSubsetSelectorPrimaryParameter(attributeSelector.getParameterTypes(), true));
		return types;
	}
}
