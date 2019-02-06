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
package com.rapidminer.operator.performance.cost;

import java.util.HashMap;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.performance.MeasuredPerformance;
import com.rapidminer.tools.math.Averagable;


/**
 * This performance Criterion works with a given cost matrix. Every classification result creates
 * costs. Costs should be minimized since that the fitness is - cost (-getMikroAverage()).
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
	private final Map<String, Integer> classOrderMap;

	/**
	 * Behaves like the pre 9.0.3
	 * @since 9.0.3
	 */
	private static final int BROKEN_FITNESS = 0;
	/**
	 * Reserved for future use
	 * @since 9.0.3
	 */
	private static final int AFTER_BROKEN_FITNESS = 1;
	/**
	 * If the version field is missing from a serialized object, the default value is used (0 = BROKEN_FITNESS)
	 * @since 9.0.3
	 */
	private int version = AFTER_BROKEN_FITNESS;

	/**
	 * Clone constructor
	 *
	 * @param other
	 *            the object to be cloned from
	 */
	public ClassificationCostCriterion(ClassificationCostCriterion other) {
		super(other);
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

		if (other.classOrderMap == null) {
			this.classOrderMap = null;
		} else {
			this.classOrderMap = new HashMap<String, Integer>(other.classOrderMap);
		}

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
		this.totalCosts = other.totalCosts;
		this.totalExampleCount = other.totalExampleCount;
		this.version = other.version;
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
		if (version == BROKEN_FITNESS) {
			return exampleCount;
		} else {
			return totalExampleCount;
		}
	}

	@Override
	public double getFitness() {
		if (version == BROKEN_FITNESS) {
			return -costs;
		} else {
			return -getMikroAverage();
		}
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
		if (version == BROKEN_FITNESS) {
			return 0;
		} else {
			return Double.NaN;
		}
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
	 * Delivers the total amount of costs considered for the MicroAverange.
	 *
	 * @return total costs
	 */
	protected double getTotalCosts() {
		return totalCosts;
	}

	/**
	 * Makes this criterion behave like the given version
	 *
	 * @param compatibilityLevel
	 * 		the compatibility level
	 * @since 9.0.3
	 */
	public void setVersion(OperatorVersion compatibilityLevel) {
		if (compatibilityLevel.isAtMost(CostEvaluator.WRONG_FITNESS)) {
			this.version = BROKEN_FITNESS;
		} else {
			this.version = AFTER_BROKEN_FITNESS;
		}
	}
}
