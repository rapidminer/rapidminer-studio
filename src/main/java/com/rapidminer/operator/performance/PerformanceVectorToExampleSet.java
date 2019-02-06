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
package com.rapidminer.operator.performance;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
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
 * This operator creates a new example set from the given performance vector. The example set will
 * have a column with the criterion name, and columns for the average, variance and standard
 * deviation.
 *
 * @author Marius Helf
 */
public class PerformanceVectorToExampleSet extends Operator {

	private InputPort performanceInput = getInputPorts().createPort("performance vector", PerformanceVector.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private OutputPort performanceOutput = getOutputPorts().createPort("performance vector");

	private final String ATTRIBUTE_CRITERION = "Criterion";
	private final String ATTRIBUTE_VALUE = "Value";
	// private final String ATTRIBUTE_EXAMPLE_COUNT = "Example Count";
	private final String ATTRIBUTE_STANDARD_DEVIATION = "Standard Deviation";
	private final String ATTRIBUTE_VARIANCE = "Variance";

	public PerformanceVectorToExampleSet(OperatorDescription description) {
		super(description);

		getTransformer().addPassThroughRule(performanceInput, performanceOutput);
		getTransformer().addRule(new GenerateNewExampleSetMDRule(exampleSetOutput) {

			@Override
			public MetaData modifyMetaData(ExampleSetMetaData emd) {
				AttributeMetaData attributeAMD = new AttributeMetaData(ATTRIBUTE_CRITERION, Ontology.NOMINAL);
				AttributeMetaData valueAMD = new AttributeMetaData(ATTRIBUTE_VALUE, Ontology.REAL);
				// AttributeMetaData exampleCountAMD = new
				// AttributeMetaData(ATTRIBUTE_EXAMPLE_COUNT, Ontology.REAL);
				AttributeMetaData stdevAMD = new AttributeMetaData(ATTRIBUTE_STANDARD_DEVIATION, Ontology.REAL);
				AttributeMetaData varianceAMD = new AttributeMetaData(ATTRIBUTE_VARIANCE, Ontology.REAL);
				emd.addAttribute(attributeAMD);
				emd.addAttribute(valueAMD);
				// emd.addAttribute(exampleCountAMD);
				emd.addAttribute(stdevAMD);
				emd.addAttribute(varianceAMD);
				emd.attributesAreKnown();
				emd.numberOfExamplesIsUnkown();
				return emd;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		PerformanceVector performanceVector = performanceInput.getData(PerformanceVector.class);

		List<Attribute> attributes = new LinkedList<Attribute>();
		Attribute nameAttribute = AttributeFactory.createAttribute(ATTRIBUTE_CRITERION, Ontology.NOMINAL);
		Attribute valueAttribute = AttributeFactory.createAttribute(ATTRIBUTE_VALUE, Ontology.REAL);
		// Attribute exampleCountAttribute =
		// AttributeFactory.createAttribute(ATTRIBUTE_EXAMPLE_COUNT, Ontology.REAL);
		Attribute stdevAttribute = AttributeFactory.createAttribute(ATTRIBUTE_STANDARD_DEVIATION, Ontology.REAL);
		Attribute varianceAttribute = AttributeFactory.createAttribute(ATTRIBUTE_VARIANCE, Ontology.REAL);
		attributes.add(nameAttribute);
		attributes.add(valueAttribute);
		// attributes.add(exampleCountAttribute);
		attributes.add(stdevAttribute);
		attributes.add(varianceAttribute);

		int nameIdx = attributes.indexOf(nameAttribute);
		int valueIdx = attributes.indexOf(valueAttribute);
		// int exampleCountIdx = attributes.indexOf(exampleCountAttribute);
		int stdevIdx = attributes.indexOf(stdevAttribute);
		int varianceIdx = attributes.indexOf(varianceAttribute);

		String[] criteriaNames = performanceVector.getCriteriaNames();
		ExampleSetBuilder builder = ExampleSets.from(attributes).withExpectedSize(criteriaNames.length);
		for (String name : criteriaNames) {
			PerformanceCriterion criterion = performanceVector.getCriterion(name);
			double[] data = new double[attributes.size()];
			data[nameIdx] = nameAttribute.getMapping().mapString(name);
			data[valueIdx] = criterion.getAverage();
			// data[exampleCountIdx] = criterion.getExampleCount();
			data[stdevIdx] = criterion.getStandardDeviation();
			data[varianceIdx] = criterion.getVariance();

			builder.addRow(data);
		}
		exampleSetOutput.deliver(builder.build());
		performanceOutput.deliver(performanceVector);
	}
}
