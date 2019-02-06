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
package com.rapidminer.tools.math.distribution.kernel;

import com.rapidminer.tools.math.distribution.NormalDistribution;

import java.util.TreeSet;


/**
 * An updatable estimated kernel density distribution. Update strategy greedily creates and merges
 * kernels and assigns value to these kernels.
 * 
 * @author Tobias Malbrecht
 */
public class GreedyKernelDistribution extends KernelDistribution {

	public static final long serialVersionUID = -3298190542815818L;

	private static final double DEFAULT_MINIMUM_BANDWIDTH = 0.1;

	private static final int DEFAULT_NUMBER_OF_KERNELS = 10;

	private int numberOfKernels;

	private double minBandwidth;

	private TreeSet<NormalKernel> kernels;

	public GreedyKernelDistribution() {
		this(DEFAULT_MINIMUM_BANDWIDTH, DEFAULT_NUMBER_OF_KERNELS);
	}

	public GreedyKernelDistribution(double minBandwidth, int numberOfKernels) {
		this.numberOfKernels = numberOfKernels;
		this.minBandwidth = minBandwidth;
		kernels = new TreeSet<NormalKernel>();
	}

	@Override
	public void update(double value, double weight) {
		if (!Double.isNaN(value) && !Double.isNaN(weight)) {
			boolean kernelUpdated = false;
			double bestAssignmentDistance = Double.POSITIVE_INFINITY;
			double bestMergeDistance = Double.POSITIVE_INFINITY;
			NormalKernel bestAssignmentKernel = null;
			NormalKernel lastKernel = null;
			NormalKernel bestMergeKernel1 = null;
			NormalKernel bestMergeKernel2 = null;
			for (NormalKernel kernel : kernels) {
				double assignmentDistance = Math.abs(value - kernel.getMean());
				if (assignmentDistance == 0) {
					kernel.update(value, weight);
					kernelUpdated = true;
					break;
				}
				if (assignmentDistance < bestAssignmentDistance) {
					bestAssignmentDistance = assignmentDistance;
					bestAssignmentKernel = kernel;
				}
				if (lastKernel != null) {
					double mergeDistance = Math.abs(lastKernel.getMean() - kernel.getMean());
					if (mergeDistance < bestMergeDistance) {
						bestMergeDistance = mergeDistance;
						bestMergeKernel1 = lastKernel;
						bestMergeKernel2 = kernel;
					}
				}
				lastKernel = kernel;
			}
			if (!kernelUpdated) {
				if (kernels.size() < numberOfKernels) {
					NormalKernel kernel = new NormalKernel(minBandwidth);
					kernel.update(value, weight);
					kernels.add(kernel);
				} else {
					if (bestAssignmentDistance < bestMergeDistance) {
						bestAssignmentKernel.update(value, weight);
					} else {
						bestMergeKernel1.update(bestMergeKernel2);
						kernels.remove(bestMergeKernel2);
						NormalKernel kernel = new NormalKernel(minBandwidth);
						kernel.update(value, weight);
						kernels.add(kernel);
					}
				}
			}
		}
	}

	@Override
	public void update(double value) {
		update(value, 1.0d);
	}

	@Override
	public String getAttributeName() {
		return null;
	}

	@Override
	public int getNumberOfParameters() {
		return 0;
	}

	@Override
	public String getParameterName(int index) {
		return null;
	}

	@Override
	public double getParameterValue(int index) {
		return Double.NaN;
	}

	@Override
	public double getUpperBound() {
		double maxMean = Double.NEGATIVE_INFINITY;
		double maxStandardDeviation = DEFAULT_BANDWIDTH;
		for (NormalKernel kernel : kernels) {
			double mean = kernel.getMean();
			double standardDeviation = kernel.getStandardDeviation();
			if (mean > maxMean) {
				maxMean = mean;
			}
			if (standardDeviation > maxStandardDeviation) {
				maxStandardDeviation = standardDeviation;
			}
		}
		return NormalDistribution.getUpperBound(maxMean, maxStandardDeviation);
	}

	@Override
	public double getLowerBound() {
		double minMean = Double.POSITIVE_INFINITY;
		double maxStandardDeviation = DEFAULT_BANDWIDTH;
		for (NormalKernel kernel : kernels) {
			double mean = kernel.getMean();
			double standardDeviation = kernel.getStandardDeviation();
			if (mean < minMean) {
				minMean = mean;
			}
			if (standardDeviation > maxStandardDeviation) {
				maxStandardDeviation = standardDeviation;
			}
		}
		return NormalDistribution.getLowerBound(minMean, maxStandardDeviation);
	}

	@Override
	public double getTotalWeight() {
		double totalWeight = 0;
		for (NormalKernel kernel : kernels) {
			totalWeight += kernel.getTotalWeight();
		}
		return totalWeight;
	}

	@Override
	public double getProbability(double value) {
		double probability = 0;
		double totalWeight = 0;
		for (NormalKernel kernel : kernels) {
			probability += kernel.getTotalWeight() * kernel.getProbability(value);
			totalWeight += kernel.getTotalWeight();
		}
		return probability / totalWeight;
	}
}
