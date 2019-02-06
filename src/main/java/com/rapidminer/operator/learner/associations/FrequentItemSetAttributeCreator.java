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
package com.rapidminer.operator.learner.associations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.Ontology;


/**
 * This operator takes a FrequentItemSet set within IOObjects and creates attributes for every
 * frequent item set. This attributes indicate if the examples contains all items of this set. The
 * attributes will contain values 0 or 1 and are numerical.
 *
 * @author Sebastian Land
 */
public class FrequentItemSetAttributeCreator extends Operator {

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private InputPort frequentItemSetsInput = getInputPorts().createPort("frequent item sets", FrequentItemSets.class);

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	public FrequentItemSetAttributeCreator(OperatorDescription description) {
		super(description);

		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.SUPERSET));
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		FrequentItemSets sets = frequentItemSetsInput.getData(FrequentItemSets.class);

		// mapping from name to attribute
		HashMap<String, Attribute> attributeMap = new HashMap<String, Attribute>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			attributeMap.put(attribute.getName(), attribute);
		}

		// mapping from frequent set to new attribute
		HashMap<FrequentItemSet, Attribute> setAttributeMap = new HashMap<FrequentItemSet, Attribute>();
		Collection<Attribute> newAttributes = new ArrayList<Attribute>(sets.size());
		for (FrequentItemSet set : sets) {
			// adding attribute
			Attribute newAttribute = AttributeFactory.createAttribute(set.toString(), Ontology.NUMERICAL);
			newAttributes.add(newAttribute);
			setAttributeMap.put(set, newAttribute);
			exampleSet.getAttributes().addRegular(newAttribute);
		}
		ExampleTable table = exampleSet.getExampleTable();
		table.addAttributes(newAttributes);

		// running over examples
		for (Example example : exampleSet) {
			// running over every frequent Set
			for (FrequentItemSet set : sets) {
				example.setValue(setAttributeMap.get(set), checkConditions(example, set, attributeMap));
			}
		}
		exampleSetOutput.deliver(exampleSet);
	}

	private double checkConditions(Example example, FrequentItemSet set, HashMap<String, Attribute> attributeMap) {
		boolean matches = true;
		for (Item item : set.getItems()) {
			Attribute conditionAttribute = attributeMap.get(item.toString());
			if (conditionAttribute != null) {
				double conditionValue = conditionAttribute.getMapping().mapString("true");
				matches = matches && example.getValue(conditionAttribute) == conditionValue;
			} else {
				matches = false;
			}
		}
		if (matches) {
			return 1d;
		}
		return 0d;
	}
}
