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
package com.rapidminer.operator.preprocessing.normalization;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.tools.Ontology;


/**
 * @author Sebastian Land
 * 
 */
public abstract class AbstractNormalizationModel extends PreprocessingModel {

	private static final long serialVersionUID = 9003091723155805502L;

	protected AbstractNormalizationModel(ExampleSet exampleSet) {
		super(exampleSet);

	}

	@Override
	public ExampleSet applyOnData(ExampleSet exampleSet) throws OperatorException {
		Attributes attributes = exampleSet.getAttributes();

		// constructing new attributes with generic names, holding old ones, if old type wasn't real
		Attribute[] oldAttributes = new Attribute[attributes.size()];
		int i = 0;
		for (Attribute attribute : attributes) {
			oldAttributes[i] = attribute;
			i++;
		}
		Attribute[] newAttributes = new Attribute[attributes.size()];
		for (i = 0; i < newAttributes.length; i++) {
			newAttributes[i] = oldAttributes[i];
			if (oldAttributes[i].isNumerical()) {
				if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(oldAttributes[i].getValueType(), Ontology.REAL)) {
					newAttributes[i] = AttributeFactory.createAttribute(Ontology.REAL);
					exampleSet.getExampleTable().addAttribute(newAttributes[i]);
					attributes.addRegular(newAttributes[i]);
				}
			}
		}

		// applying on data
		applyOnData(exampleSet, oldAttributes, newAttributes);

		// removing old attributes and change new attributes name to old ones if needed
		for (i = 0; i < oldAttributes.length; i++) {
			attributes.remove(oldAttributes[i]);
			// if attribute is new, then remove for later storing in correct order
			if (oldAttributes[i] != newAttributes[i]) {
				attributes.remove(newAttributes[i]);
			}
			attributes.addRegular(newAttributes[i]);
			newAttributes[i].setName(oldAttributes[i].getName());
		}

		return exampleSet;
	}

	/**
	 * This method must be implemented by the subclasses. Subclasses have to iterate over the
	 * exampleset and on each example iterate over the oldAttribute array and set the new values on
	 * the corresponding new attribute
	 */
	protected void applyOnData(ExampleSet exampleSet, Attribute[] oldAttributes, Attribute[] newAttributes) {
		// copying data
		for (Example example : exampleSet) {
			for (int i = 0; i < oldAttributes.length; i++) {
				if (oldAttributes[i].isNumerical()) {
					example.setValue(newAttributes[i], computeValue(oldAttributes[i], example.getValue(oldAttributes[i])));
				}
			}
		}
	}

	/**
	 * Subclasses might implement this methods in order to return a value if the implementation
	 * differs from the getValue() method for view creation. Otherwise this method just calls
	 * getValue(). If this method does not deliver enough informations, the subclass might override
	 * {@link #applyOnData(ExampleSet, Attribute[], Attribute[])}
	 */
	public double computeValue(Attribute attribute, double oldValue) {
		return getValue(attribute, oldValue);
	}

}
