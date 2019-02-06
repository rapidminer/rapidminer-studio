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
package com.rapidminer.operator.ports;

import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;


/**
 * The owner of a port. Ports belonging to an operator may have different connection contexts: An
 * outer port's connection context is the {@link ExecutionUnit} that contains the operator, whereas
 * an inner port's connection context is the subprocess it belongs to. The {@link OperatorChain}
 * returned by {@link #getPortHandler()} is the operator chain that should be displayed by the
 * process renderer when editing this port. For outer ports, this is the enclosing operator chain
 * and for inner ports it it the operator itself.
 * 
 * @author Simon Fischer
 */
public interface PortOwner {

	/** Returns the operator to which these ports are attached. */
	public Operator getOperator();

	/**
	 * Returns the operator that should be displayed by the GUI if ports are edited. If this is an
	 * inner port, this is the operator itself. If it is an outer port, it is the operator
	 * containing the enclosing process.
	 */
	public OperatorChain getPortHandler();

	/** Returns a human readable name used for error messages and gui. */
	public String getName();

	/** Two ports can connect iff they belong to the same connection context. */
	public ExecutionUnit getConnectionContext();
}
