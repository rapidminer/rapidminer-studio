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
import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.AbstractExampleSetProcessing;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.preprocessing.filter.ChangeAttributeRole;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * <p>
 * This operator transposes an example set, i.e. the columns with become the new rows and the old
 * rows will become the columns. Hence, this operator works very similar to the well know transpose
 * operation for matrices.
 * </p>
 *
 * <p>
 * If an Id attribute is part of the given example set, the ids will become the names of the new
 * attributes. The names of the old attributes will be transformed into the id values of a new
 * special Id attribute. Since no other &quot;special&quot; examples or data rows exist, all other
 * new attributes will be regular after the transformation. You can use the
 * {@link ChangeAttributeRole} operator in order to change one of these into a special type
 * afterwards.
 * </p>
 *
 * <p>
 * If all old attribute have the same value type, all new attributes will have this value type.
 * Otherwise, the new value types will all be &quot;nominal&quot; if at least one nominal attribute
 * was part of the given example set and &quot;real&quot; if the types contained mixed numbers.
 * </p>
 *
 * <p>
 * This operator produces a copy of the data in the main memory and it therefore not suggested to
 * use it on very large data sets.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class ExampleSetTranspose extends AbstractExampleSetProcessing {

	public ExampleSetTranspose(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		return metaData.transpose();
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// init operator progress
		int numberOfAllAttributeRoles = 0;
		Iterator<AttributeRole> aIter = exampleSet.getAttributes().allAttributeRoles();
		while (aIter.hasNext()) {
			aIter.next();
			numberOfAllAttributeRoles++;
		}
		getProgress().setTotal(numberOfAllAttributeRoles);
		// determine new value types
		int valueType = Ontology.REAL;
		Iterator<AttributeRole> a = exampleSet.getAttributes().allAttributeRoles();
		while (a.hasNext()) {
			AttributeRole attributeRole = a.next();
			if (!attributeRole.isSpecial() || !attributeRole.getSpecialName().equals(Attributes.ID_NAME)) {
				if (attributeRole.getAttribute().isNominal()) {
					valueType = Ontology.NOMINAL;
					break;
				}
			}
		}

		// create new attributes
		List<Attribute> newAttributes = new ArrayList<Attribute>(exampleSet.size());
		Attribute newIdAttribute = AttributeFactory.createAttribute(Attributes.ID_NAME, Ontology.NOMINAL);
		newAttributes.add(newIdAttribute);
		Attribute oldIdAttribute = exampleSet.getAttributes().getId();
		if (oldIdAttribute != null) {
			for (Example e : exampleSet) {
				double idValue = e.getValue(oldIdAttribute);
				String attributeName = "att_" + idValue;
				if (oldIdAttribute.isNominal()) {
					if (Double.isNaN(idValue)) {
						newAttributes.add(AttributeFactory.createAttribute(valueType));
					} else {
						attributeName = oldIdAttribute.getMapping().mapIndex((int) idValue);
						newAttributes.add(AttributeFactory.createAttribute(attributeName, valueType));
					}
				} else {
					newAttributes.add(AttributeFactory.createAttribute(attributeName, valueType));
				}
			}
		} else {
			for (int i = 0; i < exampleSet.size(); i++) {
				newAttributes.add(AttributeFactory.createAttribute("att_" + (i + 1), valueType));
			}
		}

		// create and fill table
		MemoryExampleTable table = new MemoryExampleTable(newAttributes);
		a = exampleSet.getAttributes().allAttributeRoles();
		while (a.hasNext()) {
			AttributeRole attributeRole = a.next();
			if (!attributeRole.isSpecial() || !attributeRole.getSpecialName().equals(Attributes.ID_NAME)) {
				Attribute attribute = attributeRole.getAttribute();
				double[] data = new double[exampleSet.size() + 1];
				data[0] = newIdAttribute.getMapping().mapString(attribute.getName());
				int counter = 1;
				for (Example e : exampleSet) {
					double currentValue = e.getValue(attribute);
					data[counter] = currentValue;
					Attribute newAttribute = newAttributes.get(counter);
					if (newAttribute.isNominal()) {
						if (!Double.isNaN(currentValue)) {
							String currentValueString = currentValue + "";
							if (attribute.isNominal()) {
								currentValueString = attribute.getMapping().mapIndex((int) currentValue);
							}
							data[counter] = newAttribute.getMapping().mapString(currentValueString);
						}
					}
					counter++;
				}
				table.addDataRow(new DoubleArrayDataRow(data));
			}
			getProgress().step();
		}

		// create and deliver example set
		getProgress().complete();
		ExampleSet result = table.createExampleSet(null, null, newIdAttribute);
		result.getAnnotations().addAll(exampleSet.getAnnotations());
		return result;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), ExampleSetTranspose.class,
				null);
	}
}
