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
package com.rapidminer.operator.execution;

import java.util.List;

import com.rapidminer.Process;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;


/**
 * <p>
 * A filter which can be added to a {@link Process} via
 * {@link Process#addProcessFlowFilter(ProcessFlowFilter)} to intercept the data flow between
 * operators. Each registered filter is called directly before and after each operator runs. Filters
 * are blocking, i.e. the next operator cannot start until all filters are done, so avoid doing
 * anything expensive here!
 * </p>
 *
 * @author Marco Boeck
 * @since 7.2
 */
public interface ProcessFlowFilter {

	/**
	 * Called before an operator is executed and delays execution of the operator until this method
	 * has returned for all registered filters.
	 *
	 * @param previousOperator
	 *            the previous operator; may be {@code null} for the first operator in a subprocess
	 * @param nextOperator
	 *            the next operator to be called, never {@code null}
	 * @param input
	 *            a list of all input data for the next operator. The order of the data is in the
	 *            same order as the ports but ports which have no input data or an not connected
	 *            will be skipped in this list. Can be empty but never {@code null}
	 * @throws OperatorException
	 *             if something goes wrong during filter application. Will stop the process with the
	 *             thrown exception.
	 */
	public void preOperator(Operator previousOperator, Operator nextOperator, List<FlowData> input) throws OperatorException;

	/**
	 * Called after an operator has been executed and delays execution of the following operators
	 * until this method has returned for all registered filters.
	 *
	 * @param previousOperator
	 *            the operator which just finished, never {@code null}
	 * @param nextOperator
	 *            the next operator to be called; may be {@code null} if this was the last operator
	 *            in a subprocess
	 * @param output
	 *            a list of all output data from the previous operator. The order of the data is in
	 *            the same order as the ports but ports which have no output data or an not
	 *            connected will be skipped in this list. Can be empty but never {@code null}
	 * @throws OperatorException
	 *             if something goes wrong during filter application. Will stop the process with the
	 *             thrown exception.
	 */
	public void postOperator(Operator previousOperator, Operator nextOperator, List<FlowData> output)
			throws OperatorException;
}
