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
package com.rapidminer.operator.preprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.MemoryCleanUp;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.io.ExampleSource;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * Creates a fresh and clean copy of the data in memory. Might be very useful in combination with
 * the {@link MemoryCleanUp} operator after large preprocessing trees using lot of views or data
 * copies.
 *
 * @author Ingo Mierswa
 */
public class MaterializeDataInMemory extends AbstractDataProcessing {

	public MaterializeDataInMemory(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		ExampleSet createdSet = materializeExampleSet(exampleSet, getParameterAsInt(ExampleSource.PARAMETER_DATAMANAGEMENT));
		return createdSet;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeCategory(ExampleSource.PARAMETER_DATAMANAGEMENT,
				"Determines, how the data is represented internally.", DataRowFactory.TYPE_NAMES,
				DataRowFactory.TYPE_DOUBLE_ARRAY, false));
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				MaterializeDataInMemory.class, null);
	}

	/**
	 * Creates a materialized copy of the given example set, i.e., a hard copy with all unnecessary
	 * abstraction layers being removed.
	 *
	 * @param exampleSet
	 *            the example set to materialize
	 * @param dataManagement
	 *            the data management strategy
	 * @return the materialized example set
	 * @throws UndefinedParameterError
	 */
	public static ExampleSet materializeExampleSet(ExampleSet exampleSet, int dataManagement) throws UndefinedParameterError {
		// create new attributes and table
		List<Attribute> attributes = new ArrayList<Attribute>(exampleSet.getAttributes().allSize());
		Map<Attribute, String> specialAttributes = new HashMap<Attribute, String>();
		Iterator<AttributeRole> a = exampleSet.getAttributes().allAttributeRoles();
		while (a.hasNext()) {
			AttributeRole role = a.next();
			Attribute attribute = role.getAttribute();

			Attribute newAttribute = AttributeFactory.createAttribute(attribute.getName(), attribute.getValueType());

			if (attribute.isNominal()) {
				// If we do not create a deep copy of the nominal mapping, subsequent modifications
				// will affect the original example set as well.
				newAttribute.setMapping((NominalMapping) attribute.getMapping().clone());
			}

			if (role.isSpecial()) {
				specialAttributes.put(newAttribute, role.getSpecialName());
			}

			attributes.add(newAttribute);
		}

		// fill table with data
		MemoryExampleTable table = new MemoryExampleTable(attributes);
		DataRowFactory factory = new DataRowFactory(dataManagement, '.');
		for (Example example : exampleSet) {
			Iterator<Attribute> i = exampleSet.getAttributes().allAttributes();
			int attributeCounter = 0;
			DataRow row = factory.create(attributes.size());
			while (i.hasNext()) {
				Attribute attribute = i.next();
				double value = example.getValue(attribute);
				Attribute newAttribute = attributes.get(attributeCounter);
				if (attribute.isNominal()) {
					if (!Double.isNaN(value)) {
						String nominalValue = attribute.getMapping().mapIndex((int) value);
						value = newAttribute.getMapping().mapString(nominalValue);
					}
				}
				row.set(newAttribute, value);
				attributeCounter++;
			}
			table.addDataRow(row);
		}

		// create and return result
		ExampleSet createdSet = table.createExampleSet(specialAttributes);
		createdSet.getAnnotations().addAll(exampleSet.getAnnotations());
		return createdSet;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}
}
