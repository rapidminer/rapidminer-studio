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
package com.rapidminer.operator.preprocessing;

import java.util.Iterator;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.SimpleAttributes;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.BinominalMapping;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.example.table.PolynominalMapping;
import com.rapidminer.example.table.ViewAttribute;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;


/**
 * This model removes all nominal values. During application it might happen that missing values are
 * introduced. If applied on data, new columns are created in a cloned example set.
 *
 * @author Sebastian Land
 */
public class RemoveUnusedNominalValuesModel extends PreprocessingModel {

	public static class MappingTranslation {

		NominalMapping originalMapping;
		NominalMapping newMapping;

		public MappingTranslation(NominalMapping originalMapping) {
			this.originalMapping = originalMapping;
			if (originalMapping instanceof PolynominalMapping) {
				this.newMapping = new PolynominalMapping();
			} else {
				this.newMapping = new BinominalMapping();
			}
		}
	}

	private static final long serialVersionUID = 1L;

	private static final int OPERATOR_PROGRESS_STEPS = 5000;

	private Map<String, MappingTranslation> translations;

	protected RemoveUnusedNominalValuesModel(ExampleSet exampleSet, Map<String, MappingTranslation> translations) {
		super(exampleSet);

		this.translations = translations;
	}

	@Override
	public Attributes getTargetAttributes(ExampleSet viewParent) {
		SimpleAttributes attributes = new SimpleAttributes();
		// add special attributes to new attributes
		Iterator<AttributeRole> roleIterator = viewParent.getAttributes().allAttributeRoles();
		while (roleIterator.hasNext()) {
			AttributeRole role = roleIterator.next();
			if (role.isSpecial()) {
				attributes.add(role);
			}
		}
		// add regular attributes
		for (Attribute attribute : viewParent.getAttributes()) {
			MappingTranslation mappingTranslation = translations.get(attribute.getName());
			if (!attribute.isNominal() || mappingTranslation == null) {
				attributes.addRegular(attribute);
			} else {
				// giving new attributes old name: connection to rangesMap
				attributes.addRegular(new ViewAttribute(this, attribute, attribute.getName(), attribute.getValueType(),
						mappingTranslation.newMapping));
			}
		}
		return attributes;
	}

	@Override
	public double getValue(Attribute targetAttribute, double value) {
		MappingTranslation mappingTranslation = translations.get(targetAttribute.getName());
		if (mappingTranslation != null && !Double.isNaN(value)) {
			String nominalValue = mappingTranslation.originalMapping.mapIndex((int) value);
			int result = mappingTranslation.newMapping.getIndex(nominalValue);
			if (result != -1) {
				return result;
			}
			return Double.NaN;
		}
		return Double.NaN;
	}

	@Override
	public ExampleSet applyOnData(ExampleSet exampleSet) throws OperatorException {
		ExampleSet resultSet = (ExampleSet) exampleSet.clone();

		Attributes attributes = exampleSet.getAttributes();
		Iterator<Attribute> iterator = attributes.iterator();
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(attributes.size());
		}
		int progressCounter = 0;
		while (iterator.hasNext()) {
			Attribute currentAttribute = iterator.next();
			MappingTranslation mappingTranslation = translations.get(currentAttribute.getName());
			if (mappingTranslation != null) {
				// removing attribute from resultSet
				Attributes resultAttributes = resultSet.getAttributes();
				resultAttributes.remove(resultAttributes.get(currentAttribute.getName()));

				// construct new attribute and add to memory table and exampleset
				Attribute newAttribute = AttributeFactory.createAttribute(currentAttribute.getName(),
						currentAttribute.getValueType(), currentAttribute.getBlockType());
				resultSet.getExampleTable().addAttribute(newAttribute);
				resultAttributes.addRegular(newAttribute);
				AttributeRole role = attributes.getRole(currentAttribute);
				if (role.isSpecial()) {
					resultAttributes.setSpecialAttribute(newAttribute, role.getSpecialName());
				}

				// now copy value for each example
				Iterator<Example> exampleIterator = exampleSet.iterator();
				for (Example resultExample : resultSet) {
					Example example = exampleIterator.next();
					String nominalValue = example.getValueAsString(currentAttribute);
					int result = mappingTranslation.newMapping.getIndex(nominalValue);
					if (result != -1) {
						resultExample.setValue(newAttribute, result);
					} else {
						resultExample.setValue(newAttribute, Double.NaN);
					}
				}
				newAttribute.setMapping(mappingTranslation.newMapping);
			}
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}

		return resultSet;
	}

	@Override
	protected boolean needsRemapping() {
		return isCreateView();
	}
}
