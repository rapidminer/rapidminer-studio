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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ModelViewExampleSet;
import com.rapidminer.example.set.NonSpecialAttributesExampleSet;
import com.rapidminer.example.set.RemappedExampleSet;
import com.rapidminer.operator.AbstractModel;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ViewModel;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Returns a more appropriate result icon. This model allows preprocessing Operators to be applied
 * through a view without changing the underlying data. Since Apply Model does not know the models,
 * because they are wrapped within a container model, it is necessary to ask for the parameter
 * PARAMETER_CREATE_VIEW. This must be set by Apply Model, and should be the default behavior.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public abstract class PreprocessingModel extends AbstractModel implements ViewModel {

	private static final long serialVersionUID = -2603663450216521777L;

	private HashMap<String, Object> parameterMap = new HashMap<>();

	protected PreprocessingModel(ExampleSet exampleSet) {
		super(exampleSet);
	}

	/**
	 * Applies the model by changing the underlying data
	 */
	public abstract ExampleSet applyOnData(ExampleSet exampleSet) throws OperatorException;

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// adapting example set to contain only attributes, which were present during learning time
		// and
		// remove roles if necessary
		ExampleSet nonSpecialRemapped = new RemappedExampleSet((isSupportingAttributeRoles()) ? exampleSet
				: new NonSpecialAttributesExampleSet(exampleSet), getTrainingHeader(), false);
		LinkedList<AttributeRole> unusedList = new LinkedList<>();
		Iterator<AttributeRole> iterator = exampleSet.getAttributes().allAttributeRoles();
		while (iterator.hasNext()) {
			AttributeRole role = iterator.next();
			if (nonSpecialRemapped.getAttributes().get(role.getAttribute().getName()) == null) {
				unusedList.add(role);
			}
		}

		// applying by creating view or changing data
		boolean createView = false;
		if (parameterMap.containsKey(PreprocessingOperator.PARAMETER_CREATE_VIEW)) {
			Boolean booleanObject = ((Boolean) parameterMap.get(PreprocessingOperator.PARAMETER_CREATE_VIEW));
			createView = false;
			if (booleanObject != null) {
				createView = booleanObject.booleanValue();
			}
		}

		ExampleSet result;
		if (createView) {
			// creating only view
			result = new ModelViewExampleSet(nonSpecialRemapped, this);
		} else {
			result = applyOnData(nonSpecialRemapped);
		}

		// restoring roles if possible
		Iterator<Attribute> attributeIterator = result.getAttributes().allAttributes();
		List<Pair<Attribute, String>> roleList = new LinkedList<>();
		Attributes inputAttributes = exampleSet.getAttributes();
		while (attributeIterator.hasNext()) {
			Attribute resultAttribute = attributeIterator.next();
			AttributeRole role = inputAttributes.getRole(resultAttribute.getName());
			if (role != null && role.isSpecial()) {
				roleList.add(new Pair<>(resultAttribute, role.getSpecialName()));  // since
																					// underlying
																					// connection
																					// is
																					// changed
			}
		}
		for (Pair<Attribute, String> rolePair : roleList) {
			result.getAttributes().setSpecialAttribute(rolePair.getFirst(), rolePair.getSecond());
		}

		// adding unused
		Attributes resultAttributes = result.getAttributes();
		for (AttributeRole role : unusedList) {
			resultAttributes.add(role);
		}
		return result;
	}

	@Override
	public String toResultString() {
		StringBuilder builder = new StringBuilder();
		Attributes trainAttributes = getTrainingHeader().getAttributes();
		builder.append(getName() + Tools.getLineSeparators(2));
		builder.append("Model covering " + trainAttributes.size() + " attributes:" + Tools.getLineSeparator());
		for (Attribute attribute : trainAttributes) {
			builder.append(" - " + attribute.getName() + Tools.getLineSeparator());
		}
		return builder.toString();
	}

	@Override
	public void setParameter(String key, Object value) {
		parameterMap.put(key, value);
	}

	/**
	 * Subclasses which need to have the attribute roles must return true. Otherwise all selected
	 * attributes are converted into regular and afterwards given their old roles.
	 */
	public boolean isSupportingAttributeRoles() {
		return false;
	}
}
