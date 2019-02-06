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
package com.rapidminer.tools.math.smoothing;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;

import java.util.LinkedList;
import java.util.List;


/**
 * This class provides functionality in order to create SmoothingKernels in operators in a parameter
 * depended way.
 * 
 * @author Sebastian Land
 */
public class SmoothingKernels {

	public static final String PARAMETER_SMOOTHING_KERNEL = "smoothing_kernel";

	public static final String[] KERNEL_NAMES = new String[] { "Rectangular", "Triangular", "Epanechnikov", "Bisquare",
			"Tricube", "Triweight", "Gaussian", "Exponential", "McLain" };

	public static final Class<?>[] KERNEL_CLASSES = new Class[] { RectangularSmoothingKernel.class,
			TriangularSmoothingKernel.class, EpanechnikovSmoothingKernel.class, BisquareSmoothingKernel.class,
			TricubeSmoothingKernel.class, TriweightSmoothingKernel.class, GaussianSmoothingKernel.class,
			ExponentialSmoothingKernel.class, McLainSmoothingKernel.class };

	public static final List<ParameterType> getParameterTypes(ParameterHandler handler) {
		List<ParameterType> types = new LinkedList<ParameterType>();
		ParameterType type = new ParameterTypeCategory(PARAMETER_SMOOTHING_KERNEL,
				"Determines which kernel type is used to calculate the weights of distant examples.", KERNEL_NAMES, 5);
		type.setExpert(false);
		types.add(type);
		return types;
	}

	public static final SmoothingKernel createKernel(ParameterHandler handler) throws OperatorException {
		int chosenKernel = handler.getParameterAsInt(PARAMETER_SMOOTHING_KERNEL);
		try {
			return (SmoothingKernel) KERNEL_CLASSES[chosenKernel].newInstance();
		} catch (InstantiationException e) {
			throw new OperatorException("Could not instanciate distance measure " + KERNEL_NAMES[chosenKernel]);
		} catch (IllegalAccessException e) {
			throw new OperatorException("Could not instanciate distance measure " + KERNEL_NAMES[chosenKernel]);
		}
	}
}
