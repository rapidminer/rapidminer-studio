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
package com.rapidminer.tools.math.similarity.numerical;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.tools.math.kernels.Kernel;
import com.rapidminer.tools.math.similarity.DistanceMeasure;


/**
 * This class uses the approach of Schoelkopf (2001) The Kernel Trick for Distances. It hence
 * calculates the distances between two examples in the transformed space defined by the chosen
 * kernel.
 * 
 * @author Sebastian Land
 */
public class KernelEuclideanDistance extends DistanceMeasure {

	private static final long serialVersionUID = 6764039884618489619L;
	private Kernel kernel;

	@Override
	public double calculateDistance(double[] value1, double[] value2) {
		return kernel.calculateDistance(value1, value1) + kernel.calculateDistance(value2, value2) - 2
				* kernel.calculateDistance(value1, value2);
	}

	@Override
	public double calculateSimilarity(double[] value1, double[] value2) {
		return -calculateDistance(value1, value2);
	}

	@Override
	public void init(ExampleSet exampleSet) throws OperatorException {
		super.init(exampleSet);
		Tools.onlyNumericalAttributes(exampleSet, "value based similarities");
	}

	@Override
	public void init(ExampleSet exampleSet, ParameterHandler parameterHandler) throws OperatorException {
		super.init(exampleSet, parameterHandler);
		init(parameterHandler);
	}

	public void init(ParameterHandler handler) throws OperatorException {
		kernel = Kernel.createKernel(handler);
	}

	@Override
	public String toString() {
		return "Kernelspace euclidean distance";
	}
}
