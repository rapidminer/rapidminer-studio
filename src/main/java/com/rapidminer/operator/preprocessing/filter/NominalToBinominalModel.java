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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.SimpleAttributes;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.BinominalMapping;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.example.table.ViewAttribute;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;


/**
 * This model maps the values of all nominal values to binary attributes. For example, if a nominal
 * attribute with name &quot;costs&quot; and possible nominal values &quot;low&quot;,
 * &quot;moderate&quot;, and &quot;high&quot; is transformed, the result is a set of three binominal
 * attributes &quot;costs = low&quot;, &quot;costs = moderate&quot;, and &quot;costs = high&quot;.
 * Only one of the values of each attribute is true for a specific example, the other values are
 * false.
 *
 * @author Sebastian Land
 */
public class NominalToBinominalModel extends PreprocessingModel {

	private static final long serialVersionUID = 2882937201039541604L;

	private static final int OPERATOR_PROGRESS_STEPS = 1_000_000;

	private final Set<String> dichotomizationAttributeNames;
	private final Set<String> changeTypeAttributeNames;

	private Map<Attribute, Double> binominalAttributeValueMap;

	private boolean useOnlyUnderscoreInNames = false;

	public NominalToBinominalModel(ExampleSet exampleSet, boolean translateBinominals, boolean useOnlyUnderscoreInNames) {
		super(exampleSet);
		this.binominalAttributeValueMap = new LinkedHashMap<Attribute, Double>();
		this.useOnlyUnderscoreInNames = useOnlyUnderscoreInNames;
		this.dichotomizationAttributeNames = new HashSet<String>();
		this.changeTypeAttributeNames = new HashSet<String>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNominal()) {
				if (attribute.getMapping().size() > 2 || translateBinominals) {
					dichotomizationAttributeNames.add(attribute.getName());
				} else {
					changeTypeAttributeNames.add(attribute.getName());
				}
			}
		}
	}

	@Override
	public ExampleSet applyOnData(ExampleSet exampleSet) throws OperatorException {
		// create attributes
		Map<Attribute, Attribute> dichotomizationMap = new LinkedHashMap<Attribute, Attribute>();
		Map<Attribute, Attribute> changeTypeMap = new LinkedHashMap<Attribute, Attribute>();
		Attributes attributes = exampleSet.getAttributes();
		Iterator<Attribute> iterator = attributes.allAttributes();
		while (iterator.hasNext()) {
			Attribute sourceAttribute = iterator.next();
			String sourceAttributeName = sourceAttribute.getName();
			if (dichotomizationAttributeNames.contains(sourceAttributeName)) {
				for (String value : getTrainingHeader().getAttributes().get(sourceAttributeName).getMapping().getValues()) {
					// create nominal mapping
					Attribute newAttribute = AttributeFactory
							.createAttribute(createAttributeName(sourceAttributeName, value), Ontology.BINOMINAL);
					NominalMapping mapping = new BinominalMapping();
					mapping.mapString("false");
					mapping.mapString("true");
					newAttribute.setMapping(mapping);
					binominalAttributeValueMap.put(newAttribute, (double) sourceAttribute.getMapping().mapString(value));
					dichotomizationMap.put(newAttribute, sourceAttribute);
				}
			} else if (changeTypeAttributeNames.contains(sourceAttributeName)) {
				// create new attribute and copy mapping
				Attribute newAttribute = AttributeFactory.createAttribute(sourceAttributeName + "_binominal",
						Ontology.BINOMINAL);
				NominalMapping mapping = new BinominalMapping();
				if (sourceAttribute.getMapping().size() == 0) {
					// handle border case 1: empty mapping
					mapping.mapString("false");
					mapping.mapString("true");
				} else if (sourceAttribute.getMapping().size() == 1) {
					// handle border case 2: mapping contains only one value
					String value = sourceAttribute.getMapping().mapIndex(0);
					mapping.mapString(value);
					if ("true".equals(value)) {
						mapping.mapString("true1");
					} else {
						mapping.mapString("true");
					}
				} else {
					mapping.mapString(sourceAttribute.getMapping().getNegativeString());
					mapping.mapString(sourceAttribute.getMapping().getPositiveString());
				}
				newAttribute.setMapping(mapping);
				changeTypeMap.put(newAttribute, sourceAttribute);
			}
		}

		// add attributes to exampleSet
		exampleSet.getExampleTable().addAttributes(dichotomizationMap.keySet());
		for (Attribute attribute : dichotomizationMap.keySet()) {
			attributes.addRegular(attribute);
		}
		exampleSet.getExampleTable().addAttributes(changeTypeMap.keySet());
		for (Attribute attribute : changeTypeMap.keySet()) {
			attributes.addRegular(attribute);
		}

		// rebuild attribute map because of changed hashCode of exampleTableColumn
		binominalAttributeValueMap = new LinkedHashMap<Attribute, Double>(binominalAttributeValueMap);

		// initialize progress
		long progressCompletedCounter = 0;
		long progressTotal = ((long) dichotomizationMap.size() + changeTypeMap.size()) * exampleSet.size();
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(1000);
		}

		
		// fill new attributes with values
		for (Map.Entry<Attribute, Attribute> entry : dichotomizationMap.entrySet()) {
			for (Example example : exampleSet) {
				double sourceValue = example.getValue(entry.getValue());
				example.setValue(entry.getKey(), getValue(entry.getKey(), sourceValue));
				if (progress != null && ++progressCompletedCounter % OPERATOR_PROGRESS_STEPS == 0) {
					progress.setCompleted((int) (1000.0d * progressCompletedCounter / progressTotal));
				}
			}
		}

		// perform simple copy for binominal attributes
		for (Map.Entry<Attribute, Attribute> entry : changeTypeMap.entrySet()) {
			for (Example example : exampleSet) {
				double sourceValue = example.getValue(entry.getValue());
				example.setValue(entry.getKey(), sourceValue);
				if (progress != null && ++progressCompletedCounter % OPERATOR_PROGRESS_STEPS == 0) {
					progress.setCompleted((int) (1000.0d * progressCompletedCounter /progressTotal));
				}
			}
		}

		// remove old attributes
		Iterator<Attribute> attributeIterator = attributes.allAttributes();
		while (attributeIterator.hasNext()) {
			Attribute attribute = attributeIterator.next();
			if (dichotomizationAttributeNames.contains(attribute.getName())
					|| changeTypeAttributeNames.contains(attribute.getName())) {
				attributeIterator.remove();
			}
		}

		// rename copy attributes to old names
		for (Map.Entry<Attribute, Attribute> entry : changeTypeMap.entrySet()) {
			String oldName = entry.getValue().getName();
			entry.getKey().setName(oldName);
		}

		return exampleSet;
	}

	@Override
	public Attributes getTargetAttributes(ExampleSet applySet) {
		Attributes attributes = getSpecialAttributes(applySet);
		// add regular attributes
		for (Attribute attribute : applySet.getAttributes()) {
			if (dichotomizationAttributeNames.contains(attribute.getName())) {
				// add binominal attributes for every value
				for (String value : getTrainingHeader().getAttributes().get(attribute.getName()).getMapping().getValues()) {
					attributes.addRegular(createBinominalValueAttribute(attribute, value));
				}
			} else {
				// add original if not a sourceAttribute
				attributes.addRegular(attribute);
			}
		}
		return attributes;
	}

	@Override
	public double getValue(Attribute targetAttribute, double value) {
		if (Double.compare(value, binominalAttributeValueMap.get(targetAttribute).doubleValue()) == 0) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Mapping Model for attributes:");
		for (String attributeName : dichotomizationAttributeNames) {
			buffer.append(attributeName + Tools.getLineSeparator());
		}
		return buffer.toString();
	}

	private Attribute createBinominalValueAttribute(Attribute attribute, String value) {
		NominalMapping mapping = new BinominalMapping();
		mapping.mapString("false");
		mapping.mapString("true");
		// giving new attributes old name_value
		String newName = createAttributeName(attribute.getName(), value);
		Attribute newAttribute = new ViewAttribute(this, attribute, newName, Ontology.BINOMINAL, mapping);
		binominalAttributeValueMap.put(newAttribute, (double) attribute.getMapping().mapString(value));
		return newAttribute;
	}

	private Attributes getSpecialAttributes(ExampleSet applySet) {
		Attributes attributes = new SimpleAttributes();
		// add special attributes to new attributes
		Iterator<AttributeRole> roleIterator = applySet.getAttributes().allAttributeRoles();
		while (roleIterator.hasNext()) {
			AttributeRole role = roleIterator.next();
			if (role.isSpecial()) {
				attributes.add(role);
			}
		}
		return attributes;
	}

	private String createAttributeName(String base, String value) {
		if (useOnlyUnderscoreInNames) {
			return base + "_" + value;
		} else {
			return base + " = " + value;
		}
	}

	public Set<String> getDichotomizationAttributeNames() {
		return dichotomizationAttributeNames;
	}

	public Set<String> getChangeTypeAttributeNames() {
		return changeTypeAttributeNames;
	}

	public Map<Attribute, Double> getBinominalAttributeValueMap() {
		return binominalAttributeValueMap;
	}

	public boolean shouldUseOnlyUnderscoreInNames() {
		return useOnlyUnderscoreInNames;
	}

	@Override
	protected boolean needsRemapping() {
		return false;
	}

}
