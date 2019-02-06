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
package com.rapidminer.operator.learner.bayes;

import java.util.ArrayList;
import java.util.Collection;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.distribution.DiscreteDistribution;
import com.rapidminer.tools.math.distribution.Distribution;
import com.rapidminer.tools.math.distribution.kernel.FullKernelDistribution;
import com.rapidminer.tools.math.distribution.kernel.GreedyKernelDistribution;
import com.rapidminer.tools.math.distribution.kernel.KernelDistribution;


/**
 * KernelDistributionModel is a model for learners which estimate distributions of attribute values
 * from example sets like NaiveBayes.
 *
 * Predictions are calculated as product of the conditional probabilities for all attributes times
 * the class probability.
 *
 * The basic learning concept is to simply count occurrences of classes and attribute values. This
 * means no probabilities are calculated during the learning step. This is only done before output.
 * Optionally, this calculation can apply a Laplace correction which means in particular that zero
 * probabilities are avoided which would hide information in distributions of other attributes.
 *
 * @author Tobias Malbrecht
 */
public class KernelDistributionModel extends DistributionModel {

	private static final long serialVersionUID = -402827845291958569L;

	private static final String UNKNOWN_VALUE_NAME = "unknown";

	private static final int OPERATOR_PROGRESS_STEPS = 200;

	/** The number of classes. */
	private int numberOfClasses;

	/** The number of attributes. */
	private int numberOfAttributes;

	/** Flags indicating which attribute is nominal. */
	private boolean[] nominal;

	/** Class name (used for result displaying). */
	private String className;

	/** Class values (used for result displaying). */
	private String[] classValues;

	/** Attribute names (used for result displaying). */
	private String[] attributeNames;

	/** Nominal attribute values (used for result displaying). */
	private String[][] attributeValues;

	/** Total weight (or number) of examples used to build the model. */
	private double totalWeight;

	/** Total weight of examples belonging to the separate classes. */
	private double[] classWeights;

	/**
	 * Specifies the total weight of examples in which the different combinations of classes and
	 * (nominal) attribute values co-occur. In the case of numeric attributes the (weighted) sum and
	 * the (weighted) sum of the squared attribute values are stored which are needed to calculate
	 * the mean and the standard deviation/variance of the resulting (assumed) normal distribution.
	 *
	 * Array dimensions: 1st: attributes 2nd: classes 3nd: nominal values or value sum (index=0) and
	 * squared value sum (index=1)
	 */
	private double[][][] weightSums;

	/** Class log (!) a-priori probabilities. */
	private double[] priors;

	/**
	 * Specifies the a-postiori distributions. Contains the log (!) a-postiori Probabilities that
	 * certain values occur given the class value for nominal values. Contains the means and
	 * standard deviations for numerical attributes.
	 *
	 * Array dimensions: 1st: attributes 2nd: classes 3nd: nominal values or mean (index=0) and
	 * standard deviation (index=1)
	 */
	private double[][][] distributionProperties;

	/**
	 * The kernel distributions for the nominal attributes.
	 */
	private KernelDistribution[][] kernelDistributions;

	/**
	 * Captures if laplace correction should be applied when calculating probabilities.
	 */
	boolean laplaceCorrectionEnabled;

	/**
	 * Indicates if the model has recently been updated and the actual probabilities have to be
	 * calculated.
	 */
	private boolean modelRecentlyUpdated;

	private boolean useApplianceGrid;

	private double[][][] grid;

	private int gridSize = 200;

	public KernelDistributionModel(ExampleSet exampleSet, boolean laplaceCorrectionEnabled, int estimationMode,
			int bandwidthSelectionMode, double bandwidth, int numberOfKernels, int gridSize) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.laplaceCorrectionEnabled = laplaceCorrectionEnabled;
		this.useApplianceGrid = gridSize > 10;
		this.gridSize = gridSize;
		Attribute labelAttribute = exampleSet.getAttributes().getLabel();
		numberOfClasses = labelAttribute.getMapping().size();
		numberOfAttributes = exampleSet.getAttributes().size();
		nominal = new boolean[numberOfAttributes];
		attributeNames = new String[numberOfAttributes];
		attributeValues = new String[numberOfAttributes][];
		className = labelAttribute.getName();
		classValues = new String[numberOfClasses];
		for (int i = 0; i < numberOfClasses; i++) {
			classValues[i] = labelAttribute.getMapping().mapIndex(i);
		}
		int attributeIndex = 0;
		weightSums = new double[numberOfAttributes][numberOfClasses][];
		distributionProperties = new double[numberOfAttributes][numberOfClasses][];
		kernelDistributions = new KernelDistribution[numberOfAttributes][numberOfClasses];
		for (Attribute attribute : exampleSet.getAttributes()) {
			attributeNames[attributeIndex] = attribute.getName();
			if (attribute.isNominal()) {
				nominal[attributeIndex] = true;
				int mappingSize = attribute.getMapping().size() + 1;
				attributeValues[attributeIndex] = new String[mappingSize];
				for (int i = 0; i < mappingSize - 1; i++) {
					attributeValues[attributeIndex][i] = attribute.getMapping().mapIndex(i);
				}
				attributeValues[attributeIndex][mappingSize - 1] = UNKNOWN_VALUE_NAME;
				for (int i = 0; i < numberOfClasses; i++) {
					weightSums[attributeIndex][i] = new double[mappingSize];
					distributionProperties[attributeIndex][i] = new double[mappingSize];
				}
			} else {
				nominal[attributeIndex] = false;
				for (int i = 0; i < numberOfClasses; i++) {
					switch (estimationMode) {
						case KernelNaiveBayes.ESTIMATION_MODE_FULL:
							switch (bandwidthSelectionMode) {
								case KernelNaiveBayes.BANDWIDTH_SELECTION_MODE_HEURISTIC:
									kernelDistributions[attributeIndex][i] = new FullKernelDistribution();
									break;
								case KernelNaiveBayes.BANDWIDTH_SELECTION_MODE_FIX:
									kernelDistributions[attributeIndex][i] = new FullKernelDistribution(bandwidth);
									break;
							}
							break;
						case KernelNaiveBayes.ESTIMATION_MODE_GREEDY:
							kernelDistributions[attributeIndex][i] = new GreedyKernelDistribution(bandwidth,
									numberOfKernels);
							break;
						default:
							kernelDistributions[attributeIndex][i] = new FullKernelDistribution();
					}
				}
			}
			attributeIndex++;
		}

		// initialization of total and a priori weight counters
		totalWeight = 0.0d;
		classWeights = new double[numberOfClasses];
		priors = new double[numberOfClasses];

		if (useApplianceGrid) {
			grid = new double[numberOfAttributes][numberOfClasses][];
		}

		// update the model
		update(exampleSet);

		// calculate the probabilities
		updateDistributionProperties();
	}

	@Override
	public String[] getAttributeNames() {
		return this.attributeNames;
	}

	@Override
	public int getNumberOfAttributes() {
		return this.attributeNames.length;
	}

	/**
	 * Updates the model by counting the occurrences of classes and attribute values in combination
	 * with the class values.
	 *
	 * ATTENTION: only updates the weight counters, distribution properties are not updated, call
	 * updateDistributionProperties() to accomplish this task
	 */
	@Override
	public void update(ExampleSet exampleSet) {
		Attribute weightAttribute = exampleSet.getAttributes().getWeight();
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		for (Example example : exampleSet) {
			double weight = weightAttribute == null ? 1.0d : example.getWeight();
			totalWeight += weight;
			double labelValue = example.getLabel();
			if (!Double.isNaN(labelValue)) {
				int classIndex = (int) example.getLabel();
				classWeights[classIndex] += weight;
				int attributeIndex = 0;
				for (Attribute attribute : regularAttributes) {
					double attributeValue = example.getValue(attribute);
					if (nominal[attributeIndex]) {
						if (!Double.isNaN(attributeValue)) {
							if ((int) attributeValue < weightSums[attributeIndex][classIndex].length - 1) {
								weightSums[attributeIndex][classIndex][(int) attributeValue] += weight;
							} else {
								// extend weight array if attribute value is not in mapping
								for (int i = 0; i < numberOfClasses; i++) {
									double[] newWeightSums = new double[(int) attributeValue + 2];
									newWeightSums[newWeightSums.length
											- 1] = weightSums[attributeIndex][i][weightSums[attributeIndex][i].length - 1];
									for (int j = 0; j < weightSums[attributeIndex][i].length - 1; j++) {
										newWeightSums[j] = weightSums[attributeIndex][i][j];
									}
									weightSums[attributeIndex][i] = newWeightSums;
									distributionProperties[attributeIndex][i] = new double[(int) attributeValue + 2];
								}
								weightSums[attributeIndex][classIndex][(int) attributeValue] += weight;
								// recreate internal attribute value mapping
								attributeValues[attributeIndex] = new String[(int) attributeValue + 2];
								for (int i = 0; i < attributeValues[attributeIndex].length - 1; i++) {
									attributeValues[attributeIndex][i] = attribute.getMapping().mapIndex(i);
								}
								attributeValues[attributeIndex][attributeValues[attributeIndex].length
										- 1] = UNKNOWN_VALUE_NAME;
							}
						} else {
							weightSums[attributeIndex][classIndex][weightSums[attributeIndex][classIndex].length
									- 1] += weight;
						}
					} else {
						if (!Double.isNaN(attributeValue)) {
							kernelDistributions[attributeIndex][classIndex].update(attributeValue, weight);
						}
					}
					attributeIndex++;
				}
			}
		}
		modelRecentlyUpdated = true;
	}

	/**
	 * Updates the distribution properties by calculating the logged probabilities and distribution
	 * parameters on the basis of the weight counters.
	 */
	private void updateDistributionProperties() {
		double f = laplaceCorrectionEnabled ? 1 / totalWeight : Double.MIN_VALUE;
		for (int i = 0; i < numberOfClasses; i++) {
			priors[i] = Math.log(classWeights[i] / totalWeight);
		}
		for (int i = 0; i < numberOfAttributes; i++) {
			if (nominal[i]) {
				for (int j = 0; j < numberOfClasses; j++) {
					for (int k = 0; k < weightSums[i][j].length; k++) {
						distributionProperties[i][j][k] = Math
								.log((weightSums[i][j][k] + f) / (classWeights[j] + f * weightSums[i][j].length));
					}
				}
			}
		}

		if (useApplianceGrid) {
			for (int i = 0; i < numberOfClasses; i++) {
				for (int j = 0; j < numberOfAttributes; j++) {
					if (!nominal[j]) {
						double lowerBound = kernelDistributions[j][i].getLowerBound();
						double upperBound = kernelDistributions[j][i].getUpperBound();
						double precision = (upperBound - lowerBound) / gridSize;
						grid[j][i] = new double[gridSize + 1];
						for (int k = 0; k < gridSize + 1; k++) {
							grid[j][i][k] = kernelDistributions[j][i].getProbability(lowerBound + k * precision);
						}
					}
				}
			}
		}

		modelRecentlyUpdated = false;
	}

	/**
	 * Perform predictions based on the distribution properties.
	 *
	 * @throws ProcessStoppedException
	 */
	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws ProcessStoppedException {
		if (modelRecentlyUpdated) {
			updateDistributionProperties();
		}

		// initialize progress
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
		int progressCounter = 0;
		Attribute[] regularAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		for (Example example : exampleSet) {
			double[] probabilities = new double[numberOfClasses];
			double maxLogProbability = Double.NEGATIVE_INFINITY;
			int mostProbableClass = 0;
			double probabilitySum = 0;
			for (int i = 0; i < numberOfClasses; i++) {
				double logProbability = priors[i];
				int j = 0;
				for (Attribute attribute : regularAttributes) {
					double value = example.getValue(attribute);
					if (nominal[j]) {
						if (!Double.isNaN(value) && (int) value < distributionProperties[j][i].length) {
							logProbability += distributionProperties[j][i][(int) value];
						} else {
							logProbability += distributionProperties[j][i][distributionProperties[j][i].length - 1];
						}
					} else {
						if (!Double.isNaN(value)) {
							if (useApplianceGrid) {
								double upperBound = kernelDistributions[j][i].getUpperBound();
								double lowerBound = kernelDistributions[j][i].getLowerBound();
								double precision = (upperBound - lowerBound) / gridSize;
								if (value >= lowerBound && value <= kernelDistributions[j][i].getUpperBound()) {
									logProbability += Math.log(grid[j][i][(int) ((value - lowerBound) / precision)]);
								} else {
									logProbability += Math.log(kernelDistributions[j][i].getProbability(value));
								}
							} else {
								logProbability += Math.log(kernelDistributions[j][i].getProbability(value));
							}
						}
					}
					j++;
				}
				if (!Double.isNaN(logProbability) && logProbability > maxLogProbability) {
					maxLogProbability = logProbability;
					mostProbableClass = i;
				}
				probabilities[i] = logProbability;
			}
			for (int i = 0; i < numberOfClasses; i++) {
				if (!Double.isNaN(probabilities[i])) {
					probabilities[i] = Math.exp(probabilities[i] - maxLogProbability);
					probabilitySum += probabilities[i];
				} else {
					probabilities[i] = 0;
				}
			}
			if (maxLogProbability == Double.NEGATIVE_INFINITY) {
				example.setPredictedLabel(Double.NaN);
				for (int i = 0; i < numberOfClasses; i++) {
					example.setConfidence(classValues[i], Double.NaN);
				}
			} else {
				example.setPredictedLabel(mostProbableClass);
				for (int i = 0; i < numberOfClasses; i++) {
					example.setConfidence(classValues[i], probabilities[i] / probabilitySum);
				}
			}

			// trigger progress
			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}
		return exampleSet;
	}

	public void setLaplaceCorrectionEnabled(boolean laplaceCorrectionEnabled) {
		this.laplaceCorrectionEnabled = laplaceCorrectionEnabled;
	}

	public boolean getLaplaceCorrectionEnabled() {
		return laplaceCorrectionEnabled;
	}

	@Override
	public double getLowerBound(int attributeIndex) {
		if (!nominal[attributeIndex]) {
			double lowerBound = Double.POSITIVE_INFINITY;
			for (int i = 0; i < numberOfClasses; i++) {
				double currentLowerBound = kernelDistributions[attributeIndex][i].getLowerBound();
				if (!Double.isNaN(currentLowerBound)) {
					lowerBound = Math.min(lowerBound, currentLowerBound);
				}
			}
			return lowerBound;
		} else {
			return Double.NaN;
		}
	}

	@Override
	public double getUpperBound(int attributeIndex) {
		if (!nominal[attributeIndex]) {
			double upperBound = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < numberOfClasses; i++) {
				double currentUpperBound = kernelDistributions[attributeIndex][i].getUpperBound();
				if (!Double.isNaN(currentUpperBound)) {
					upperBound = Math.max(upperBound, currentUpperBound);
				}
			}
			return upperBound;
		} else {
			return Double.NaN;
		}
	}

	@Override
	public boolean isDiscrete(int attributeIndex) {
		if (attributeIndex >= 0 && attributeIndex < nominal.length) {
			return nominal[attributeIndex];
		}
		return false;
	}

	@Override
	public Collection<Integer> getClassIndices() {
		Collection<Integer> classValueIndices = new ArrayList<Integer>(numberOfClasses);
		for (int i = 0; i < numberOfClasses; i++) {
			classValueIndices.add(i);
		}
		return classValueIndices;
	}

	@Override
	public int getNumberOfClasses() {
		return numberOfClasses;
	}

	@Override
	public String getClassName(int index) {
		return classValues[index];
	}

	@Override
	public Distribution getDistribution(int classIndex, int attributeIndex) {
		if (nominal[attributeIndex]) {
			double[] probabilities = new double[distributionProperties[attributeIndex][classIndex].length];
			for (int i = 0; i < probabilities.length; i++) {
				probabilities[i] = Math.exp(distributionProperties[attributeIndex][classIndex][i]);
			}
			return new DiscreteDistribution(attributeNames[attributeIndex], probabilities, attributeValues[attributeIndex]);
		} else {
			return kernelDistributions[attributeIndex][classIndex];
		}
	}

	@Override
	public String toString() {
		if (modelRecentlyUpdated) {
			updateDistributionProperties();
		}

		StringBuffer buffer = new StringBuffer();
		buffer.append("Distribution model for label attribute " + className);
		buffer.append(Tools.getLineSeparators(2));
		for (int i = 0; i < numberOfClasses; i++) {
			String classTitle = "Class " + classValues[i] + " (" + Tools.formatNumber(Math.exp(priors[i])) + ")";
			buffer.append(Tools.getLineSeparator());
			buffer.append(classTitle);
			buffer.append(Tools.getLineSeparator());
			buffer.append(attributeNames.length + " distributions");
			buffer.append(Tools.getLineSeparator());
		}
		return buffer.toString();
	}
}
