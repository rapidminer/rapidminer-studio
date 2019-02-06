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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.container.Range;


/**
 * Generates two Gaussian clusters.
 * 
 * @author Ingo Mierswa
 */
public class TwoGaussiansClassificationFunction extends ClusterFunction {

	/** The list of clusters. */
	private List<Cluster> clusters = new LinkedList<Cluster>();

	/** The label attribute. */
	Attribute label = AttributeFactory.createAttribute("label", Ontology.NOMINAL);

	/** The label for the last generated point. */
	private double currentLabel;

	/** Initializes some gaussian clusters. */
	@Override
	public void init(RandomGenerator random) {
		this.clusters.clear();
		double sizeSum = 0.0d;
		int numberOfClusters = 2;
		for (int i = 0; i < numberOfClusters; i++) {
			double[] coordinates = new double[numberOfAttributes];
			double[] sigmas = new double[numberOfAttributes];
			for (int j = 0; j < coordinates.length; j++) {
				coordinates[j] = random.nextDoubleInRange(lowerBound, upperBound);
				sigmas[j] = random.nextDouble();
			}
			int labelIndex = label.getMapping().mapString("cluster" + i);
			double size = random.nextDouble();
			sizeSum += size;
			this.clusters.add(new Cluster(coordinates, sigmas, size, labelIndex));
		}
		for (Cluster cluster : clusters) {
			cluster.size /= sizeSum;
		}
	}

	@Override
	public Attribute getLabel() {
		return label;
	}

	@Override
	public double calculate(double[] att) throws FunctionException {
		return currentLabel;
	}

	@Override
	public double[] createArguments(int number, RandomGenerator random) throws FunctionException {
		int c = 0;
		double prob = random.nextDouble();
		double sizeSum = 0.0d;
		Cluster cluster = null;
		do {
			cluster = clusters.get(c);
			sizeSum += cluster.size;
			if (prob < sizeSum) {
				break;
			}
			c++;
		} while (sizeSum < 1);
		this.currentLabel = cluster.label;
		return cluster.createArguments(random);
	}

	@Override
	protected Set<String> getClusterSet() {
		HashSet<String> set = new HashSet<String>();
		set.add("cluster0");
		set.add("cluster1");
		return set;
	}

	@Override
	public ExampleSetMetaData getGeneratedMetaData() {
		ExampleSetMetaData emd = new ExampleSetMetaData();
		// label
		AttributeMetaData amd = new AttributeMetaData("label", Ontology.NOMINAL, Attributes.LABEL_NAME);
		amd.setValueSet(getClusterSet(), SetRelation.EQUAL);
		emd.addAttribute(amd);

		// attributes
		for (int i = 0; i < numberOfAttributes; i++) {
			amd = new AttributeMetaData("att" + (i + 1), Ontology.REAL);
			amd.setValueRange(new Range(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), SetRelation.SUBSET);
			emd.addAttribute(amd);
		}
		emd.setNumberOfExamples(numberOfExamples);
		return emd;
	}
}
