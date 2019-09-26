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

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rapidminer.Process;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;


/**
 * Executes an {@link ExecutionUnit} by invoking the operators in their (presorted) ordering.
 * Instances of this class can be shared.
 *
 * @author Simon Fischer, Marco Boeck
 *
 */
public class SimpleUnitExecutor implements UnitExecutor {

	@Override
	public void execute(ExecutionUnit unit) throws OperatorException {
		Logger logger = unit.getEnclosingOperator().getLogger();
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Executing subprocess " + unit.getEnclosingOperator().getName() + "." + unit.getName()
					+ ". Execution order is: " + unit.getOperators());
		}
		Process process = unit.getEnclosingOperator().getProcess();
		Enumeration<Operator> opEnum = unit.getOperatorEnumeration();
		Operator lastOperator = null;
		Operator operator = opEnum.hasMoreElements() ? opEnum.nextElement() : null;
		while (operator != null) {

			// fire event that we are about to start the next operator
			if (process != null) {
				// gather input data for connected ports
				List<FlowData> input = new LinkedList<>();
				if (operator.getInputPorts() != null) {
					for (InputPort inputPort : operator.getInputPorts().getAllPorts()) {
						if (inputPort.isConnected()) {
							IOObject data = inputPort.getRawData();
							if (data != null) {
								data = FlowCleaner.INSTANCE.checkCleanup(data, inputPort);
								input.add(new FlowData(data, inputPort));
							}
						}
					}
				}
				process.fireProcessFlowBeforeOperator(lastOperator, operator, input);
			}

			// execute the operator
			operator.execute();

			lastOperator = operator;
			operator = opEnum.hasMoreElements() ? opEnum.nextElement() : null;

			// fire event that we finished last operator
			if (process != null) {
				// gather output data for connected ports
				List<FlowData> output = new LinkedList<>();
				if (lastOperator.getOutputPorts() != null) {
					for (OutputPort outputPort : lastOperator.getOutputPorts().getAllPorts()) {
						if (outputPort.isConnected()) {
							IOObject data = outputPort.getRawData();
							if (data != null) {
								output.add(new FlowData(data, outputPort));
							}
						}
					}
				}
				process.fireProcessFlowAfterOperator(lastOperator, operator, output);
			}
			lastOperator.freeMemory();
		}

	}

}
