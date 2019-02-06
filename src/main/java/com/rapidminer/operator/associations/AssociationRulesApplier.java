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
package com.rapidminer.operator.associations;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.associations.AssociationRule;
import com.rapidminer.operator.learner.associations.AssociationRules;
import com.rapidminer.operator.learner.associations.Item;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.container.Pair;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * This operator first creates for every given association rule an attribute. Then it checks for
 * every examples if the rule is applicable for this examples. If it is, the attribute of this rule
 * is set to true, otherwise false.
 * 
 * @author Sebastian Land
 * 
 */
public class AssociationRulesApplier extends Operator {

	public static final String PARAMETER_POSITIVE_VALUE = "positive_value";
	public static final String PARAMETER_CONFIDENCE_AGGREGATION = "confidence_aggregation_method";

	public static final String[] AGGREGATION_METHOD = new String[] { "binary", "aggregated confidence",
			"aggregated conviction", "aggregated LaPlace", "aggregated gain", "aggregated lift" };
	public static final int BINARY = 0;
	public static final int MAX_CONFIDENCE = 1;
	public static final int MAX_CONVICTION = 2;
	public static final int MAX_LA_PLACE = 3;
	public static final int MAX_GAIN = 4;
	public static final int MAX_LIFT = 5;

	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private InputPort associationRulesInput = getInputPorts().createPort("association rules", AssociationRules.class);

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	/**
	 * @param description
	 */
	public AssociationRulesApplier(OperatorDescription description) {
		super(description);

		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.SUPERSET));
	}

	@Override
	public void doWork() throws OperatorException {
		AssociationRules rules = associationRulesInput.getData(AssociationRules.class);
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		exampleSet = (ExampleSet) exampleSet.clone();
		// check
		Tools.maximumTwoNominalAttributes(exampleSet, "Apply Association Rules");

		// retrieving parameter
		int selectedAggregation = getParameterAsInt(PARAMETER_CONFIDENCE_AGGREGATION);
		String positiveValueString = null;
		try {
			positiveValueString = getParameterAsString(PARAMETER_POSITIVE_VALUE);
		} catch (UndefinedParameterError err) {
		}

		// determining attributes and their positive indices
		Map<String, Pair<Attribute, Double>> nameAttributePositiveValueMap = new HashMap<String, Pair<Attribute, Double>>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			double positiveIndice = attribute.getMapping().getPositiveIndex();
			if (positiveValueString != null && !positiveValueString.equals("")) {
				positiveIndice = attribute.getMapping().mapString(positiveValueString);
			}
			nameAttributePositiveValueMap.put(attribute.getName(), new Pair<Attribute, Double>(attribute, positiveIndice));
		}

		// now extend example set with one attribute per rule
		Item[] conclusionItems = rules.getAllConclusionItems();
		Arrays.sort(conclusionItems, new Comparator<Item>() {

			@Override
			public int compare(Item o1, Item o2) {
				return o1.toString().compareTo(o2.toString());
			}
		});

		Map<Item, Attribute> itemConfidenceAttributeMap = new HashMap<Item, Attribute>();
		Attribute[] attributes = new Attribute[conclusionItems.length];
		int i = 0;
		for (Item item : conclusionItems) {
			attributes[i] = AttributeFactory.createAttribute(Attributes.CONFIDENCE_NAME + "(" + item.toString() + ")",
					Ontology.REAL);
			itemConfidenceAttributeMap.put(item, attributes[i]);
			i++;
		}

		ExampleTable exampleTable = exampleSet.getExampleTable();
		exampleTable.addAttributes(Arrays.asList(attributes));
		for (i = 0; i < attributes.length; i++) {
			AttributeRole currentRole = new AttributeRole(attributes[i]);
			currentRole.setSpecial(attributes[i].getName());
			exampleSet.getAttributes().add(currentRole);
		}

		// now run through all examples, set each attribute according to applicability of rule
		for (Example example : exampleSet) {
			// setting all values to zero
			for (Attribute confAttribute : attributes) {
				example.setValue(confAttribute, 0d);
			}

			for (i = 0; i < rules.getNumberOfRules(); i++) {
				AssociationRule currentRule = rules.getRule(i);
				boolean premiseFullfilled = true;
				Iterator<Item> premiseIterator = currentRule.getPremiseItems();
				while (premiseIterator.hasNext() && premiseFullfilled) {
					Item premiseItem = premiseIterator.next();
					// now test if each item of the premise has the positive value in the current
					// example
					String attributeName = premiseItem.toString();
					Pair<Attribute, Double> attributePositiveValuePair = nameAttributePositiveValueMap.get(attributeName);
					if (attributePositiveValuePair == null) {
						premiseFullfilled &= false; // if attribute isn't present assume it's false.
					} else {
						premiseFullfilled &= attributePositiveValuePair.getSecond().equals(
								example.getValue(attributePositiveValuePair.getFirst()));
					}
				}

				// if premise is fulfilled: Aggregate rules confidence etc to all conclusion items
				// confidence attributes
				if (premiseFullfilled) {
					Iterator<Item> conclusionIterator = currentRule.getConclusionItems();
					while (conclusionIterator.hasNext()) {
						Item conclusionItem = conclusionIterator.next();
						Attribute attribute = itemConfidenceAttributeMap.get(conclusionItem);

						double ruleConfidence = getConfidence(currentRule, example.getValue(attribute), selectedAggregation);
						example.setValue(attribute, ruleConfidence);
					}
				}
			}
		}

		exampleSetOutput.deliver(exampleSet);
	}

	private double getConfidence(AssociationRule currentRule, double currentSummedConfidence, int selectedAggregation) {
		switch (selectedAggregation) {
			case BINARY:
				return 1;
			case MAX_CONFIDENCE:
				return Math.max(currentSummedConfidence, currentRule.getConfidence());
			case MAX_CONVICTION:
				return Math.max(currentSummedConfidence, currentRule.getConviction());
			case MAX_GAIN:
				return Math.max(currentSummedConfidence, currentRule.getGain());
			case MAX_LA_PLACE:
				return Math.max(currentSummedConfidence, currentRule.getLaplace());
			case MAX_LIFT:
				return Math.max(currentSummedConfidence, currentRule.getLift());
		}
		return 0;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(PARAMETER_CONFIDENCE_AGGREGATION,
				"This selects the method to aggregat the confidence on the items in each fulfilled conclusion.",
				AGGREGATION_METHOD, 0, false);
		types.add(type);

		type = new ParameterTypeString(
				PARAMETER_POSITIVE_VALUE,
				"This parameter determines, which value of the binominal attributes is treated as positive. Attributes with that value are considered as part of a transaction. If left blank, the example set determines, which is value is used.",
				true);
		type.setExpert(true);

		types.add(type);
		return types;
	}
}
