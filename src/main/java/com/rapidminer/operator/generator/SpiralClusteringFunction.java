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
import java.util.Set;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;


/**
 * Generates a data set for two attributes. The data build two spiral functions around the origin up
 * to a maximal radius.
 * 
 * @author Ingo Mierswa
 */
public class SpiralClusteringFunction extends ClusterFunction {

	/** The number of turns. */
	private static final int NUMBER_OF_TURNS = 3;

	/** The offset for the spiral angle. */
	private static final double ANGLE_OFFSET = 3.0d * Math.PI / 2.0d;

	/** The angle factor. */
	private static final double ANGLE_FACTOR = 2 * Math.PI;

	/** The radius factor for the second spiral. */
	private static final double RADIUS_OFFSET = 2.0d;

	/** The maximal radius of the spiral. */
	private double bound = 10.0d;

	/** The current number. */
	private int currentNumber = 0;

	/** The currently used angle phi. */
	private double currentPhi = 0.0d;

	/** The angle delta which is added for each iteration. */
	private double deltaPhi;

	/** The current radius factor. */
	private double currentRadiusOffset = 0.0d;

	/** The currently used label. */
	private double currentLabel;

	/** The label attribute. */
	Attribute label = AttributeFactory.createAttribute("label", Ontology.NOMINAL);

	public SpiralClusteringFunction() {
		label.getMapping().mapString("spiral1");
		label.getMapping().mapString("spiral2");
	}

	/** Does nothing. */
	@Override
	public void init(RandomGenerator random) {
		this.currentPhi = ANGLE_OFFSET;
		this.deltaPhi = (NUMBER_OF_TURNS * ANGLE_FACTOR - ANGLE_OFFSET) / (this.numberOfExamples / 2.0d);
		this.currentNumber = 0;
		this.currentRadiusOffset = 0.0d;
		this.currentLabel = getLabel().getMapping().mapString("spiral1");
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
		if (number != 2) {
			throw new FunctionException("Spiral clustering function", "must have 2 attributes!");
		}
		double[] args = new double[number];

		if (currentNumber == numberOfExamples / 2) {
			currentPhi = ANGLE_OFFSET;
			currentRadiusOffset = RADIUS_OFFSET;
			currentLabel = getLabel().getMapping().mapString("spiral2");
		}
		currentPhi += deltaPhi;
		double r = bound * currentPhi / (ANGLE_FACTOR * NUMBER_OF_TURNS - ANGLE_OFFSET) - currentRadiusOffset;
		args[0] = Math.cos(currentPhi) * r + random.nextGaussian() * 0.1d;
		args[1] = Math.sin(currentPhi) * r + random.nextGaussian() * 0.1d;

		currentNumber++;
		return args;
	}

	@Override
	protected Set<String> getClusterSet() {
		HashSet<String> clusters = new HashSet<String>();
		clusters.add("spiral1");
		clusters.add("spiral2");
		return clusters;
	}

	@Override
	public int getMinNumberOfAttributes() {
		return 2;
	}

	@Override
	public int getMaxNumberOfAttributes() {
		return 2;
	}
}
