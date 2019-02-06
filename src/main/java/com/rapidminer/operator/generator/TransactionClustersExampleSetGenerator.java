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
package com.rapidminer.operator.generator;

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.AbstractExampleSource;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.container.Range;


/**
 * Generates a random example set for testing purposes. The data represents a team profit example
 * set.
 *
 * @author Ingo Mierswa
 */
public class TransactionClustersExampleSetGenerator extends AbstractExampleSource {

	/** The parameter name for &quot;The number of generated examples.&quot; */
	public static final String PARAMETER_NUMBER_TRANSACTIONS = "number_transactions";

	/** The parameter name for &quot;The number of generated examples.&quot; */
	public static final String PARAMETER_NUMBER_CUSTOMERS = "number_customers";

	/** The parameter name for &quot;The number of generated examples.&quot; */
	public static final String PARAMETER_NUMBER_ITEMS = "number_items";

	/** The parameter name for &quot;The number of generated examples.&quot; */
	public static final String PARAMETER_NUMBER_CLUSTERS = "number_clusters";

	public TransactionClustersExampleSetGenerator(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet createExampleSet() throws OperatorException {
		// init
		int numberOfTransactions = getParameterAsInt(PARAMETER_NUMBER_TRANSACTIONS);
		int numberOfCustomers = getParameterAsInt(PARAMETER_NUMBER_CUSTOMERS);
		int numberOfClusters = getParameterAsInt(PARAMETER_NUMBER_CLUSTERS);
		int numberOfItems = getParameterAsInt(PARAMETER_NUMBER_ITEMS);
		getProgress().setTotal(numberOfClusters + numberOfTransactions);

		// create table
		List<Attribute> attributes = new ArrayList<>();

		Attribute id = AttributeFactory.createAttribute("Id", Ontology.NOMINAL);
		for (int i = 1; i <= numberOfCustomers; i++) {
			id.getMapping().mapString("Id " + i);
		}
		attributes.add(id);

		Attribute item = AttributeFactory.createAttribute("Item", Ontology.NOMINAL);
		for (int i = 1; i <= numberOfItems; i++) {
			item.getMapping().mapString("Item " + i);
		}
		attributes.add(item);

		Attribute amount = AttributeFactory.createAttribute("Amount", Ontology.INTEGER);
		attributes.add(amount);

		ExampleSetBuilder builder = ExampleSets.from(attributes)
				.withExpectedSize(Math.max(numberOfTransactions, numberOfCustomers));

		// create data
		RandomGenerator random = RandomGenerator.getRandomGenerator(this);
		double[][] probs = new double[numberOfClusters][numberOfItems];
		int[] maxItems = new int[numberOfClusters];
		for (int c = 0; c < numberOfClusters; c++) {
			double sum = 0.0d;
			for (int i = 0; i < numberOfItems; i++) {
				probs[c][i] = random.nextDouble();
				sum += probs[c][i];
			}
			for (int i = 0; i < numberOfItems; i++) {
				probs[c][i] /= sum;
			}
			maxItems[c] = random.nextIntInRange(5, 20);

			getProgress().step();
		}

		double clusterSize = Math.ceil(numberOfCustomers / (double) numberOfClusters);
		for (int n = 0; n < numberOfCustomers; n++) {
			double[] values = new double[3];	// values for the data row in the table: [Id, Item,
			// Amount]
			values[0] = id.getMapping().mapString("Id " + (n + 1));
			int clusterIndex = Math.max(0, Math.min(numberOfClusters - 1, (int) Math.floor((n + 1) / clusterSize)));
			double p = random.nextDouble(); // random number in [0.0, 1.0[
			double sum = 0.0d;
			int itemIndex = 0;
			double itemProb = 0.0d;
			for (int i = 0; i < probs[clusterIndex].length; i++) {
				if (p <= sum) {
					itemIndex = i;
					itemProb = probs[clusterIndex][i];
					break;
				}
				sum += probs[clusterIndex][i];
			}

			values[1] = item.getMapping().mapString("Item " + (itemIndex + 1));

			values[2] = Math.round(Math.max(1, random.nextGaussian() * itemProb * maxItems[clusterIndex]));

			builder.addRow(values);

			getProgress().step();
		}

		for (int n = numberOfCustomers; n < numberOfTransactions; n++) {
			double[] values = new double[3];
			int idNumber = random.nextIntInRange(1, numberOfCustomers + 1);
			values[0] = values[0] = id.getMapping().mapString("Id " + idNumber);
			int clusterIndex = Math.max(0, Math.min(numberOfClusters - 1, (int) Math.floor(idNumber / clusterSize)));
			double p = random.nextDouble();
			double sum = 0.0d;
			int itemIndex = 0;
			double itemProb = 0.0d;
			if (random.nextDouble() < 0.05) {
				itemIndex = random.nextIntInRange(0, numberOfItems);
			} else {
				for (int i = 0; i < probs[clusterIndex].length; i++) {
					if (p <= sum) {
						itemIndex = i;
						itemProb = probs[clusterIndex][i];
						break;
					}
					sum += probs[clusterIndex][i];
				}
			}

			values[1] = item.getMapping().mapString("Item " + (itemIndex + 1));

			values[2] = Math.round(Math.max(1, random.nextGaussian() * itemProb * maxItems[clusterIndex]));

			builder.addRow(values);

			getProgress().step();
		}
		getProgress().complete();

		return builder.withRole(id, Attributes.ID_NAME).build();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeInt(PARAMETER_NUMBER_TRANSACTIONS, "The number of generated transactions.", 1,
				Integer.MAX_VALUE, 10000);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_NUMBER_CUSTOMERS, "The number of generated customers.", 1, Integer.MAX_VALUE,
				1000);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_NUMBER_ITEMS, "The number of generated items.", 1, Integer.MAX_VALUE, 80);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_NUMBER_CLUSTERS, "The number of generated clusters.", 1, Integer.MAX_VALUE, 10);
		type.setExpert(false);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));

		return types;
	}

	@Override
	public MetaData getGeneratedMetaData() throws OperatorException {
		ExampleSetMetaData emd = new ExampleSetMetaData();

		String[] possibleValues = new String[getParameterAsInt(PARAMETER_NUMBER_CUSTOMERS)];
		for (int i = 0; i < possibleValues.length; i++) {
			possibleValues[i] = "Id " + (i + 1);
		}
		emd.addAttribute(new AttributeMetaData("Id", Attributes.ID_NAME, possibleValues));

		possibleValues = new String[getParameterAsInt(PARAMETER_NUMBER_ITEMS)];
		for (int i = 0; i < possibleValues.length; i++) {
			possibleValues[i] = "Item " + (i + 1);
		}
		emd.addAttribute(new AttributeMetaData("Item", null, possibleValues));
		emd.addAttribute(new AttributeMetaData("Amount", null, Ontology.INTEGER, new Range(0, Double.POSITIVE_INFINITY)));

		emd.setNumberOfExamples(getParameterAsInt(PARAMETER_NUMBER_TRANSACTIONS));
		return emd;
	}
}
