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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;


/**
 * Converts all numerical attributes to nominal ones.
 *
 * @author Ingo Mierswa
 */
public abstract class NumericToNominal extends AbstractFilteredDataProcessing {

	public NumericToNominal(OperatorDescription description) {
		super(description);
	}

	protected abstract void setValue(Example example, Attribute newAttribute, double value) throws OperatorException;

	/** Returns {@link Ontology#NOMINAL} or one of its subtypes. */
	protected abstract int getGeneratedAttributevalueType();

	protected Attribute makeAttribute() {
		return AttributeFactory.createAttribute(getGeneratedAttributevalueType());
	}

	/**
	 * Will be invoked before the setValue method is invoked for each example. This default
	 * implementation does nothing.
	 */
	public void init() throws OperatorException {}

	/**
	 * Will be invoked after the setValue method was invoked for each example. This default
	 * implementation does nothing.
	 */
	public void cleanUp() throws OperatorException {}

	@Override
	public ExampleSetMetaData applyOnFilteredMetaData(ExampleSetMetaData emd) throws UndefinedParameterError {
		for (AttributeMetaData amd : emd.getAllAttributes()) {
			if (!amd.isSpecial() && amd.isNumerical()) {
				amd.setType(Ontology.NOMINAL);
				amd.setValueSetRelation(SetRelation.SUPERSET);
			}
		}
		return emd;
	}

	@Override
	public ExampleSet applyOnFiltered(ExampleSet exampleSet) throws OperatorException {
		Map<Attribute, Attribute> translationMap = new LinkedHashMap<Attribute, Attribute>();
		// creating new nominal attributes
		for (Attribute originalAttribute : exampleSet.getAttributes()) {

			if (originalAttribute.isNumerical()) {
				Attribute newAttribute = makeAttribute();
				translationMap.put(originalAttribute, newAttribute);
			}
		}
		// adding to table and exampleSet
		for (Entry<Attribute, Attribute> replacement : translationMap.entrySet()) {
			Attribute newAttribute = replacement.getValue();
			exampleSet.getExampleTable().addAttribute(newAttribute);
			exampleSet.getAttributes().addRegular(newAttribute);
		}

		// invoke init
		init();

		// initialize progress
		long progress = 0;
		long totalProgress = (long) translationMap.size() * exampleSet.size();
		getProgress().setTotal(1000);

		// over all examples change attribute values
		for (Entry<Attribute, Attribute> replacement : translationMap.entrySet()) {
			Attribute oldAttribute = replacement.getKey();
			Attribute newAttribute = replacement.getValue();
			for (Example example : exampleSet) {
				double oldValue = example.getValue(oldAttribute);
				setValue(example, newAttribute, oldValue);
				if (++progress % 100_000 == 0) {
					getProgress().setCompleted((int) (1000.0d * progress / totalProgress));
				}
			}
		}
		
		// clean up
		cleanUp();

		// removing old attributes
		for (Map.Entry<Attribute, Attribute> entry : translationMap.entrySet()) {
			Attribute originalAttribute = entry.getKey();
			exampleSet.getAttributes().remove(originalAttribute);
			entry.getValue().setName(originalAttribute.getName());
		}

		return exampleSet;
	}

	/**
	 * This adds another column, does not modify existing.
	 */
	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NUMERICAL };
	}
}
