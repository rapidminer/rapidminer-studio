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
package com.rapidminer.operator.concurrency.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.studio.internal.Resources;


/**
 * This is an abstract class for all Operators that have subprocesses that should be executed in
 * parallel.
 *
 * @author Sebastian Land
 * @since 7.4
 *
 */
public abstract class ParallelOperatorChain extends OperatorChain {

	/** Last version which synchronized remembered data only in special iterations (in most cases the last iteration). */
	public static final OperatorVersion DOES_NOT_ALWAYS_SYNCHRONIZE_REMEMBERED_DATA = new OperatorVersion(8, 2, 0);

	private static String PARAMETER_ENABLE_PARALLEL_EXECUTION = "enable_parallel_execution";

	public ParallelOperatorChain(OperatorDescription description, String... subprocessNames) {
		super(description, subprocessNames);
	}

	/**
	 * This method checks whether the user has disabled the parallel execution or whether there are
	 * breakpoints inside the subprocess. In boths situations the process needs to be executed
	 * synchronously.
	 *
	 * @return
	 */
	protected boolean checkParallelizability() {
		if(Resources.getConcurrencyContext(this).getParallelism() == 1) {
			return false;
		}

		boolean executeParallely = getParameterAsBoolean(PARAMETER_ENABLE_PARALLEL_EXECUTION);
		if (executeParallely) {
			// now check if there's a break point. Then we switch back to serial as well.
			for (ExecutionUnit unit : getSubprocesses()) {
				for (Operator operator : unit.getAllInnerOperators()) {
					if (operator.isEnabled() && operator.hasBreakpoint()) {
						return false;
					}
				}
			}
		}
		return executeParallely;
	}

	/**
	 * This method returns a List of the copies or clones of each IOObject. Copies are simply
	 * references on the same objects if the object is immutable. ExampleSets are provided by cloned
	 * reference or materialized, depending on parameter.
	 *
	 * @param inputData
	 * @param materializeIfPossible
	 *            if {@code true}, {@link ExampleSet}s will be materialized instead of cloned
	 * @return
	 * @throws UndefinedParameterError
	 */
	@SuppressWarnings("unchecked")
	protected <T extends IOObject> List<T> getDataCopy(List<IOObject> inputData, boolean materializeIfPossible)
			throws UndefinedParameterError {
		List<IOObject> clonedInputData = new ArrayList<>(inputData.size());
		for (IOObject object : inputData) {
			clonedInputData.add(getDataCopy(object, materializeIfPossible));
		}
		return (List<T>) clonedInputData;
	}

	/**
	 * This method returns a copy or clone of the given {@link IOObject}. Copies are simply
	 * references on the same objects if the object is immutable. For ExampleSets the behavior
	 * depends on the second parameter. If requested, ExampleSets are checked for their
	 * thread-safety and materialized if necessary.
	 *
	 * @param input
	 * @param materializeUnsafeExampleSets
	 *            if {@code true}, ExampleSets that are not thread-safe are materialized
	 * @return
	 * @throws UndefinedParameterError
	 */
	@SuppressWarnings("unchecked")
	protected <T extends IOObject> T getDataCopy(IOObject input, boolean materializeUnsafeExampleSets)
			throws UndefinedParameterError {
		if (materializeUnsafeExampleSets && input instanceof ExampleSet) {
			ExampleSet set = (ExampleSet) input;
			return (T) ExampleSets.createThreadSafeCopy(set);
		} else {
			if (input != null) {
				return (T) input.copy();
			}
			return null;
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeBoolean(PARAMETER_ENABLE_PARALLEL_EXECUTION,
				"This parameter enables the parallel execution of this operator. Please disable the parallel execution if you run into memory problems.",
				true, true));

		return types;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] incompatibleVersions = super.getIncompatibleVersionChanges();
		OperatorVersion[] extendedIncompatibleVersions = Arrays.copyOf(incompatibleVersions,
				incompatibleVersions.length + 1);
		extendedIncompatibleVersions[incompatibleVersions.length] = DOES_NOT_ALWAYS_SYNCHRONIZE_REMEMBERED_DATA;
		return extendedIncompatibleVersions;
	}

}
