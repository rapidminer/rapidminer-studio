/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;

import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Executes an {@link ExecutionUnit} by invoking the operators in their (presorted) ordering.
 * Instances of this class can be shared.
 * 
 * @author Simon Fischer
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
		Enumeration<Operator> opEnum = unit.getOperatorEnumeration();
		while (opEnum.hasMoreElements()) {
			// for (Operator operator : unit.getOperators()) {
			Operator operator = opEnum.nextElement();
			operator.execute();
			operator.freeMemory();
		}

	}

}
