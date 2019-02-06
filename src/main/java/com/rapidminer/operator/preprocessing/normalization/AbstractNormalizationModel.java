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
package com.rapidminer.operator.preprocessing.normalization;

import com.rapidminer.RapidMiner;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ParameterService;


/**
 * @author Sebastian Land
 *
 */
public abstract class AbstractNormalizationModel extends PreprocessingModel {

	private static final long serialVersionUID = 9003091723155805502L;

	private static final int OPERATOR_PROGRESS_STEPS = 10_000;

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
				newAttributes[i] = AttributeFactory.createAttribute(Ontology.REAL);
				exampleSet.getExampleTable().addAttribute(newAttributes[i]);
				attributes.addRegular(newAttributes[i]);
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
	 * @throws ProcessStoppedException
	 */
	protected void applyOnData(ExampleSet exampleSet, Attribute[] oldAttributes, Attribute[] newAttributes) throws ProcessStoppedException {
		// initialize progress
		long progressCounter = 0;
		long progressTotal = 0;
		for (Attribute attribute : oldAttributes) {
			if (attribute.isNumerical()) {
				progressTotal++;
			}
		}
		progressTotal *= exampleSet.size();

		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(1000);
		}

		// copying data
		for (int i = 0; i < oldAttributes.length; i++) {
			Attribute oldAttribute = oldAttributes[i];
			if (oldAttribute.isNumerical()) {
				Attribute newAttribute = newAttributes[i];
				for (Example example : exampleSet) {
					example.setValue(newAttribute, computeValue(oldAttribute, example.getValue(oldAttribute)));
					if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
						progress.setCompleted((int) (1000.0d * progressCounter / progressTotal));
					}
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

	@Override
	protected boolean needsRemapping() {
		return false;
	}

	@Override
	protected boolean writesIntoExistingData() {
		return false;
	}
}
