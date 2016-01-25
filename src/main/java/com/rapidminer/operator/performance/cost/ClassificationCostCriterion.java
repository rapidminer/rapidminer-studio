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
package com.rapidminer.operator.performance.cost;

import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.operator.performance.MeasuredPerformance;
import com.rapidminer.tools.math.Averagable;


/**
 * This performance Criterion works with a given cost matrix. Every classification result creates
 * costs. Costs should be minimized since that the fitness is - cost.
 *
 * @author Sebastian Land
 */
public class ClassificationCostCriterion extends MeasuredPerformance {

	private static final long serialVersionUID = -7466139591781285005L;

	private double[][] costMatrix;
	private double exampleCount;
	private double costs;
	private double totalCosts;
	private double totalExampleCount;
	Attribute label;
	Attribute predictedLabel;
	private Map<String, Integer> classOrderMap = null;

	/**
	 * Clone constructor
	 *
	 * @param other
	 *            the object to be cloned from
	 */
	public ClassificationCostCriterion(ClassificationCostCriterion other) {
		this.exampleCount = other.exampleCount;
		this.costs = other.costs;
		if (other.label != null) {
			this.label = (Attribute) other.label.clone();
		} else {
			this.label = null;
		}

		if (other.predictedLabel != null) {
			this.predictedLabel = (Attribute) other.predictedLabel.clone();
		} else {
			this.predictedLabel = null;
		}

		this.classOrderMap.putAll(other.classOrderMap);

		if (other.costMatrix != null) {
			this.costMatrix = new double[other.costMatrix.length][];
			for (int i = 0; i < other.costMatrix.length; ++i) {
				if (other.costMatrix[i] != null) {
					this.costMatrix[i] = new double[other.costMatrix[i].length];
					for (int j = 0; j < other.costMatrix[i].length; ++j) {
						this.costMatrix[i][j] = other.costMatrix[i][j];
					}
				} else {
					this.costMatrix[i] = null;
				}
			}
		} else {
			this.costMatrix = null;
		}
	}

	/**
	 * This constructor is for counting with the order respective to the internal nominal mapping.
	 * It is recommended to explicitly define the classOrder and use the
	 * {@link #ClassificationCostCriterion(double[][], Map, Attribute, Attribute)} constructor
	 * instead.
	 */
	public ClassificationCostCriterion(double[][] costMatrix, Attribute label, Attribute predictedLabel) {
		this(costMatrix, null, label, predictedLabel);
	}

	/**
	 * Constructor to explicitly define the order of class names. Please take into account that the
	 * cost matrix must have the same dimensions as the classorderMap has size and each interger
	 * between 0 and size-1 must occur once in the classOrderMap.
	 */
	public ClassificationCostCriterion(double[][] costMatrix, Map<String, Integer> classOrderMap, Attribute label,
			Attribute predictedLabel) {
		this.classOrderMap = classOrderMap;
		this.costMatrix = costMatrix;
		this.label = label;
		this.predictedLabel = predictedLabel;
		exampleCount = 0;
		costs = 0;
		totalCosts = 0;
		totalExampleCount = 0;
	}

	@Override
	public String getDescription() {
		return "This Criterion delievers the misclassificationCosts";
	}

	@Override
	public String getName() {
		return "Misclassificationcosts";
	}

	@Override
	public void countExample(Example example) {
		exampleCount++;
		totalExampleCount++;
		if (classOrderMap == null) {
			costs += costMatrix[(int) example.getValue(predictedLabel)][(int) example.getValue(label)];
			totalCosts += costMatrix[(int) example.getValue(predictedLabel)][(int) example.getValue(label)];
		} else {
			int predictedLabelIndex = classOrderMap.get(example.getNominalValue(predictedLabel));
			int labelIndex = classOrderMap.get(example.getNominalValue(label));
			costs += costMatrix[predictedLabelIndex][labelIndex];
			totalCosts += costMatrix[predictedLabelIndex][labelIndex];
		}
	}

	@Override
	public double getExampleCount() {
		return exampleCount;
	}

	@Override
	public double getFitness() {
		return -costs;
	}

	@Override
	protected void buildSingleAverage(Averagable averagable) {
		ClassificationCostCriterion other = (ClassificationCostCriterion) averagable;
		totalCosts += other.getTotalCosts();
		totalExampleCount += other.getTotalExampleCount();
	}

	@Override
	public double getMikroAverage() {
		return totalCosts / totalExampleCount;
	}

	@Override
	public double getMikroVariance() {
		return 0;
	}

	/**
	 * Delivers the total amount of considered examples for the MikroAverange.
	 *
	 * @return total number of examples used
	 */
	protected double getTotalExampleCount() {
		return totalExampleCount;
	}

	/**
	 * Delivers the total amount of costs considered for the MikroAverange.
	 *
	 * @return total costs
	 */
	protected double getTotalCosts() {
		return totalCosts;
	}

}
