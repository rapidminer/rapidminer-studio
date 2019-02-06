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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;

import java.util.List;


/**
 * Kernel Naive Bayes learner.
 * 
 * @author Tobias Malbrecht
 * @version $Id: KernelNaiveBayes.java,v 1.1.2.1 2009-04-08 14:40:21 tobiasmalbrecht Exp $
 */
public class KernelNaiveBayes extends AbstractLearner {

	public static final String PARAMETER_LAPLACE_CORRECTION = "laplace_correction";

	public static final String PARAMETER_ESTIMATION_MODE = "estimation_mode";

	public static final String[] ESTIMATION_MODES = { "full", "greedy" };

	public static final int ESTIMATION_MODE_FULL = 0;

	public static final int ESTIMATION_MODE_GREEDY = 1;

	public static final String PARAMETER_BANDWIDTH_SELECTION = "bandwidth_selection";

	public static final String[] BANDWIDTH_SELECTION_MODES = { "heuristic", "fix" };

	public static final int BANDWIDTH_SELECTION_MODE_HEURISTIC = 0;

	public static final int BANDWIDTH_SELECTION_MODE_FIX = 1;

	public static final String PARAMETER_BANDWIDTH = "bandwidth";

	public static final String PARAMETER_MINIMUM_BANDWIDTH = "minimum_bandwidth";

	public static final String PARAMETER_NUMBER_OF_KERNELS = "number_of_kernels";

	public static final String PARAMETER_USE_APPLICATION_GRID = "use_application_grid";

	public static final String PARAMETER_APPLICATION_GRID_SIZE = "application_grid_size";

	public KernelNaiveBayes(OperatorDescription description) {
		super(description);
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
		int estimationMode = getParameterAsInt(PARAMETER_ESTIMATION_MODE);
		double bandwidth = estimationMode == ESTIMATION_MODE_FULL ? getParameterAsDouble(PARAMETER_BANDWIDTH)
				: getParameterAsDouble(PARAMETER_MINIMUM_BANDWIDTH);
		int gridSize = getParameterAsBoolean(PARAMETER_USE_APPLICATION_GRID) ? getParameterAsInt(PARAMETER_APPLICATION_GRID_SIZE)
				: 0;
		return new KernelDistributionModel(exampleSet, getParameterAsBoolean(PARAMETER_LAPLACE_CORRECTION), estimationMode,
				getParameterAsInt(PARAMETER_BANDWIDTH_SELECTION), bandwidth, getParameterAsInt(PARAMETER_NUMBER_OF_KERNELS),
				gridSize);
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return KernelDistributionModel.class;
	}

	@Override
	public boolean supportsCapability(OperatorCapability lc) {
		if (lc == OperatorCapability.POLYNOMINAL_ATTRIBUTES) {
			return true;
		}
		if (lc == OperatorCapability.BINOMINAL_ATTRIBUTES) {
			return true;
		}
		if (lc == OperatorCapability.NUMERICAL_ATTRIBUTES) {
			return true;
		}
		if (lc == OperatorCapability.POLYNOMINAL_LABEL) {
			return true;
		}
		if (lc == OperatorCapability.BINOMINAL_LABEL) {
			return true;
		}
		if (lc == OperatorCapability.WEIGHTED_EXAMPLES) {
			return true;
		}
		if (lc == OperatorCapability.UPDATABLE) {
			return true;
		}
		return false;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_LAPLACE_CORRECTION,
				"Use Laplace correction to prevent high influence of zero probabilities.", true);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeCategory(PARAMETER_ESTIMATION_MODE, "The kernel density estimation mode.", ESTIMATION_MODES,
				ESTIMATION_MODE_GREEDY);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeCategory(PARAMETER_BANDWIDTH_SELECTION, "The method to set the kernel bandwidth.",
				BANDWIDTH_SELECTION_MODES, BANDWIDTH_SELECTION_MODE_HEURISTIC);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_ESTIMATION_MODE, ESTIMATION_MODES, false,
				ESTIMATION_MODE_FULL));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_BANDWIDTH, "Kernel bandwidth.", Double.MIN_VALUE, Double.POSITIVE_INFINITY,
				0.1d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_BANDWIDTH_SELECTION,
				BANDWIDTH_SELECTION_MODES, false, BANDWIDTH_SELECTION_MODE_FIX));
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_ESTIMATION_MODE, ESTIMATION_MODES, false,
				ESTIMATION_MODE_FULL));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_MINIMUM_BANDWIDTH, "Minimum kernel bandwidth.", Double.MIN_VALUE,
				Double.POSITIVE_INFINITY, 0.1d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_ESTIMATION_MODE, ESTIMATION_MODES, false,
				ESTIMATION_MODE_GREEDY));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_NUMBER_OF_KERNELS, "Number of kernels.", 1, Integer.MAX_VALUE, 10);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_ESTIMATION_MODE, ESTIMATION_MODES, false,
				ESTIMATION_MODE_GREEDY));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeBoolean(
				PARAMETER_USE_APPLICATION_GRID,
				"Use a kernel density function grid in model application. (Speeds up model application at the expense of the density function precision.)",
				false);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_APPLICATION_GRID_SIZE, "Size of the application grid.", 10, Integer.MAX_VALUE,
				200);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_USE_APPLICATION_GRID, false, true));
		type.setExpert(false);
		types.add(type);
		return types;
	}
}
