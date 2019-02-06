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
package com.rapidminer.operator.features.weighting;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.GenerateNewExampleSetMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.tools.Ontology;


/**
 * This operator creates a new example set from the given attribute weights. The example set will
 * have two columns, the name of the attribute and the weight for this attribute. It will have as
 * many rows as are described by the give attribute weights.
 *
 * @author Ingo Mierswa
 */
public class AttributeWeights2ExampleSet extends Operator {

	private InputPort weightInput = getInputPorts().createPort("attribute weights", AttributeWeights.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

	public AttributeWeights2ExampleSet(OperatorDescription description) {
		super(description);

		getTransformer().addRule(new GenerateNewExampleSetMDRule(exampleSetOutput) {

			@Override
			public MetaData modifyMetaData(ExampleSetMetaData emd) {
				AttributeMetaData attributeAMD = new AttributeMetaData("Attribute", Ontology.NOMINAL);
				AttributeMetaData weightAMD = new AttributeMetaData("Weight", Ontology.REAL);
				emd.addAttribute(attributeAMD);
				emd.addAttribute(weightAMD);
				emd.attributesAreKnown();
				emd.numberOfExamplesIsUnkown();
				return emd;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		AttributeWeights weights = weightInput.getData(AttributeWeights.class);

		List<Attribute> attributes = new LinkedList<Attribute>();
		Attribute nameAttribute = AttributeFactory.createAttribute("Attribute", Ontology.NOMINAL);
		Attribute weightAttribute = AttributeFactory.createAttribute("Weight", Ontology.REAL);
		attributes.add(nameAttribute);
		attributes.add(weightAttribute);

		ExampleSetBuilder builder = ExampleSets.from(attributes).withExpectedSize(weights.size());
		for (String name : weights.getAttributeNames()) {
			double[] data = new double[2];
			data[0] = nameAttribute.getMapping().mapString(name);
			data[1] = weights.getWeight(name);
			builder.addRow(data);
		}
		exampleSetOutput.deliver(builder.build());
	}
}
