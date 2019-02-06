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
package com.rapidminer.studio.concurrency.internal;

import java.util.concurrent.ExecutionException;

import com.rapidminer.Process;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.PortUserError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;


/**
 * Helper class for exception handling when using {@link ConcurrencyExecutionService}.
 * <p>
 * Note that this part of the API is only temporary and might be removed in future versions again.
 * </p>
 *
 * @author Sebastian Land
 * @since 7.4
 * @see ConcurrencyExecutionServiceProvider Provider to get the concurrency execution service
 */
public enum ExecutionExceptionHandling {

	/** the singleton instance */
	INSTANCE;

	/**
	 * Finds the corresponding Port in original Ports by the index of the source Port in source Ports.
	 */
	private Port getMatchingPort(Port sourcePort, Ports<?> sourcePorts, Ports<?> originalPorts) {
		for (int i = 0; i < sourcePorts.getNumberOfPorts(); i++) {
			if (sourcePorts.getPortByIndex(i).equals(sourcePort)) {
				return originalPorts.getPortByIndex(i);
			}
		}

		return null;
	}

	/**
	 * Tries to get the underlying cause of an {@link ExecutionException} which occurred while using
	 * {@link ConcurrencyExecutionServiceProvider}.
	 *
	 * @param e
	 *            the exception which occurred during execution via
	 *            {@link ConcurrencyExecutionService} methods.
	 * @param process
	 *            the process in which the exception occurred
	 * @return the {@link UserError} or {@link OperatorException} that caused the execution
	 *         exception.
	 * @throws OperatorException
	 *             if the execution exception cannot be processed.
	 * @throws RuntimeException
	 *             if the cause of the exception was a runtime exception
	 * @throws Error
	 *             if the cause of the exception was an error
	 */
	public OperatorException processExecutionException(ExecutionException e, Process process)
			throws OperatorException, RuntimeException, Error {
		// unpack stacked execution exceptions
		Throwable cause = e.getCause();
		while (cause != null && cause != cause.getCause() && cause instanceof ExecutionException) {
			cause = cause.getCause();
		}

		if (cause != null) {

			Throwable innerCause = cause;
			// if the inner cause is an instance of an operator exception
			// we'll handle this exception as root cause
			if (innerCause instanceof OperatorException) {
				cause = innerCause;
			}

			// try to re-map the operator and port to the original process
			if (cause instanceof UserError) {
				UserError error = (UserError) cause;
				Operator sourceOperator = error.getOperator();

				if (process != null && sourceOperator != null) {
					error.setOperator(process.getOperator(sourceOperator.getName()));
				}
				if (sourceOperator != null && cause instanceof PortUserError) {
					PortUserError portError = (PortUserError) error;
					Port errorPort = portError.getPort();

					Port originalPort = getMatchingPort(errorPort, sourceOperator.getInputPorts(), error.getOperator().getInputPorts());
					if (originalPort != null) {
						portError.setPort(originalPort);
						return portError;
					}

					originalPort = getMatchingPort(errorPort, sourceOperator.getOutputPorts(), error.getOperator().getOutputPorts());
					if (originalPort != null) {
						portError.setPort(originalPort);
						return portError;
					}

					if (sourceOperator instanceof OperatorChain) {
						OperatorChain sourceOperatorChain = (OperatorChain) sourceOperator;
						OperatorChain originalOperatorChain = (OperatorChain) error.getOperator();

						for (int i = 0; i < sourceOperatorChain.getNumberOfSubprocesses(); i++) {
							ExecutionUnit sourceExecutionUnit = sourceOperatorChain.getSubprocess(i);
							ExecutionUnit originalExecutionUnit = originalOperatorChain.getSubprocess(i);

							originalPort = getMatchingPort(errorPort, sourceExecutionUnit.getInnerSinks(), originalExecutionUnit.getInnerSinks());
							if (originalPort != null) {
								portError.setPort(originalPort);
								return portError;
							}

							originalPort = getMatchingPort(errorPort, sourceExecutionUnit.getInnerSources(), originalExecutionUnit.getInnerSources());
							if (originalPort != null) {
								portError.setPort(originalPort);
								return portError;
							}
						}
					}
				}
				return error;
			} else if (cause instanceof OperatorException) {
				return (OperatorException) cause;
			} else if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			}
		}
		return new OperatorException("There seems to be an unknown problem", cause);
	}

}
