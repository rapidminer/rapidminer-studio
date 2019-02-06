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
package com.rapidminer.operator.validation;

import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.visualization.ProcessLogOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * <p>
 * <code>BatchXValidation</code> encapsulates a cross-validation process. The example set
 * {@rapidminer.math S} is split up into <var> number_of_validations</var> subsets
 * {@rapidminer.math S_i}. The inner operators are applied <var>number_of_validations</var> times
 * using {@rapidminer.math S_i} as the test set (input of the second inner operator) and
 * {@rapidminer.math S\backslash S_i} training set (input of the first inner operator).
 * </p>
 *
 * <p>
 * In contrast to the usual cross validation operator (see {@link XValidation}) this operator does
 * not (randomly) split the data itself but uses the partition defined by the special attribute
 * &quot;batch&quot;. This can be an arbitrary nominal or integer attribute where each possible
 * value occurs at least once (since many learning schemes depend on this minimum number of
 * examples).
 * </p>
 *
 * <p>
 * The first inner operator must accept an {@link com.rapidminer.example.ExampleSet} while the
 * second must accept an {@link com.rapidminer.example.ExampleSet} and the output of the first
 * (which is in most cases a {@link com.rapidminer.operator.Model}) and must produce a
 * {@link com.rapidminer.operator.performance.PerformanceVector}.
 * </p>
 *
 * <p>
 * The cross validation operator provides several values which can be logged by means of a
 * {@link ProcessLogOperator}. Of course the number of the current iteration can be logged which
 * might be useful for ProcessLog operators wrapped inside a cross validation. Beside that, all
 * performance estimation operators of RapidMiner provide access to the average values calculated
 * during the estimation. Since the operator cannot ensure the names of the delivered criteria, the
 * ProcessLog operator can access the values via the generic value names:
 * </p>
 * <ul>
 * <li>performance: the value for the main criterion calculated by this validation operator</li>
 * <li>performance1: the value of the first criterion of the performance vector calculated</li>
 * <li>performance2: the value of the second criterion of the performance vector calculated</li>
 * <li>performance3: the value of the third criterion of the performance vector calculated</li>
 * <li>for the main criterion, also the variance and the standard deviation can be accessed where
 * applicable.</li>
 * </ul>
 *
 * @author Ingo Mierswa
 * @deprecated use the {@link #CrossValidationOperator} from the concurrency extension instead.
 */
@Deprecated
public class BatchXValidation extends ValidationChain {

	/**
	 * The parameter name for &quot;Indicates if only performance vectors should be averaged or all
	 * types of averagable result vectors&quot;
	 */
	public static final String PARAMETER_AVERAGE_PERFORMANCES_ONLY = "average_performances_only";
	private int iteration;

	public BatchXValidation(OperatorDescription description) {
		super(description);
		addValue(new ValueDouble("iteration", "The number of the current iteration.") {

			@Override
			public double getDoubleValue() {
				return iteration;
			}
		});
	}

	@Override
	public void estimatePerformance(ExampleSet inputSet) throws OperatorException {
		// split by attribute
		Attribute batchAttribute = inputSet.getAttributes().getSpecial(Attributes.BATCH_NAME);
		if (batchAttribute == null) {
			throw new UserError(this, 113, Attributes.BATCH_NAME);
		}
		SplittedExampleSet splittedES = SplittedExampleSet.splitByAttribute(inputSet, batchAttribute);

		// start crossvalidation
		if (modelOutput.isConnected()) {
			getProgress().setTotal(splittedES.getNumberOfSubsets() + 1);
		} else {
			getProgress().setTotal(splittedES.getNumberOfSubsets());
		}
		getProgress().setCheckForStop(false);

		for (iteration = 0; iteration < splittedES.getNumberOfSubsets(); iteration++) {
			splittedES.selectAllSubsetsBut(iteration);
			learn(splittedES);

			splittedES.selectSingleSubset(iteration);
			evaluate(splittedES);
			inApplyLoop();
			getProgress().step();
		}
	}

	@Override
	protected MDInteger getTestSetSize(MDInteger originalSize) throws UndefinedParameterError {
		return new MDInteger();
	}

	@Override
	protected MDInteger getTrainingSetSize(MDInteger originalSize) throws UndefinedParameterError {
		return new MDInteger();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_AVERAGE_PERFORMANCES_ONLY,
				"Indicates if only performance vectors should be averaged or all types of averagable result vectors", true));
		return types;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		return true;
	}
}
