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
package com.rapidminer.operator.learner.associations;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.GenerateNewExampleSetMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.container.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * 
 * @author Tobias Malbrecht
 */
public class FrequentItemSetsToData extends Operator {

	public static final String PARAMETER_GENERATE_ITEM_SET_INDICATORS = "generate_item_set_indicators";
	public static final String PARAMETER_DATAMANAGEMENT = "datamanagement";

	private InputPort frequentItemSetsInput = getInputPorts().createPort("frequent item sets", FrequentItemSets.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort frequentItemSetsOutput = getOutputPorts().createPort("frequent item sets");

	public FrequentItemSetsToData(OperatorDescription description) {
		super(description);
		getTransformer().addRule(new GenerateNewExampleSetMDRule(exampleSetOutput) {

			@Override
			public MetaData modifyMetaData(ExampleSetMetaData emd) {
				emd.addAttribute(new AttributeMetaData("Items", Ontology.NOMINAL));
				emd.addAttribute(new AttributeMetaData("Size", null, Ontology.INTEGER,
						new Range(0, Double.POSITIVE_INFINITY)));
				emd.addAttribute(new AttributeMetaData("Frequency", null, Ontology.INTEGER, new Range(0,
						Double.POSITIVE_INFINITY)));
				emd.addAttribute(new AttributeMetaData("Support", null, Ontology.REAL, new Range(0, 1)));
				emd.addAttribute(new AttributeMetaData("Score", null, Ontology.REAL, new Range(Double.NEGATIVE_INFINITY,
						Double.POSITIVE_INFINITY)));
				if (getParameterAsBoolean(PARAMETER_GENERATE_ITEM_SET_INDICATORS)) {
					emd.attributesAreSuperset();
				} else {
					emd.attributesAreKnown();
				}
				emd.numberOfExamplesIsUnkown();
				return emd;
			}
		});
		getTransformer().addPassThroughRule(frequentItemSetsInput, frequentItemSetsOutput);
	}

	@Override
	public void doWork() throws OperatorException {
		FrequentItemSets sets = frequentItemSetsInput.getData(FrequentItemSets.class);
		boolean generateItemSetIndicators = getParameterAsBoolean(PARAMETER_GENERATE_ITEM_SET_INDICATORS);

		Attribute itemsAttribute = AttributeFactory.createAttribute("Items", Ontology.NOMINAL);
		Attribute sizeAttribute = AttributeFactory.createAttribute("Size", Ontology.INTEGER);
		Attribute frequencyAttribute = AttributeFactory.createAttribute("Frequency", Ontology.INTEGER);
		Attribute supportAttribute = AttributeFactory.createAttribute("Support", Ontology.REAL);
		Attribute scoreAttribute = AttributeFactory.createAttribute("Score", Ontology.REAL);
		List<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(itemsAttribute);
		attributes.add(sizeAttribute);
		attributes.add(frequencyAttribute);
		attributes.add(supportAttribute);
		attributes.add(scoreAttribute);
		Map<Item, Attribute> itemAttributeMap = new TreeMap<Item, Attribute>();
		if (generateItemSetIndicators) {
			for (FrequentItemSet set : sets) {
				for (Item item : set.getItems()) {
					if (!itemAttributeMap.containsKey(item)) {
						Attribute attribute = AttributeFactory.createAttribute(item.toString(), Ontology.BINOMINAL);
						attributes.add(attribute);
						itemAttributeMap.put(item, attribute);
					}
				}
			}
		}

		MemoryExampleTable exampleTable = new MemoryExampleTable(attributes);
		DataRowFactory dataRowFactory = new DataRowFactory(getParameterAsInt(PARAMETER_DATAMANAGEMENT), '.');
		for (FrequentItemSet set : sets) {
			DataRow dataRow = dataRowFactory.create(attributes.size());
			dataRow.set(itemsAttribute, itemsAttribute.getMapping().mapString(set.getItemsAsString()));
			dataRow.set(sizeAttribute, set.getNumberOfItems());
			dataRow.set(frequencyAttribute, set.getFrequency());
			double support = (double) set.getFrequency() / (double) sets.getNumberOfTransactions();
			dataRow.set(supportAttribute, support);
			for (Attribute attribute : itemAttributeMap.values()) {
				dataRow.set(attribute, Double.NaN);
			}
			double independentProbabilityEstimate = 1;
			for (Item item : set.getItems()) {
				independentProbabilityEstimate *= (double) item.getFrequency() / (double) sets.getNumberOfTransactions();
			}
			dataRow.set(scoreAttribute, support / independentProbabilityEstimate);
			if (generateItemSetIndicators) {
				for (Item item : set.getItems()) {
					assert (itemAttributeMap.containsKey(item)) : "item not inserted";
					Attribute attribute = itemAttributeMap.get(item);
					dataRow.set(attribute, attribute.getMapping().mapString("true"));
				}
			}
			exampleTable.addDataRow(dataRow);
		}

		ExampleSet exampleSet = exampleTable.createExampleSet();
		exampleSetOutput.deliver(exampleSet);
		frequentItemSetsOutput.deliver(sets);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_GENERATE_ITEM_SET_INDICATORS,
				"Determines whether item indicator attributes should be generated for the item sets.", false));
		types.add(new ParameterTypeCategory(PARAMETER_DATAMANAGEMENT, "Determines, how the data is represented internally.",
				DataRowFactory.TYPE_NAMES, DataRowFactory.TYPE_DOUBLE_ARRAY));
		return types;
	}
}
