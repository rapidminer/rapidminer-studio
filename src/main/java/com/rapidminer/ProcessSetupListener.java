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
package com.rapidminer;

import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;

import java.util.EventListener;


/**
 * Listener for process setup, i.e. insertion, deletion and property changes of operators.
 * 
 * @author Simon Fischer
 * 
 */
public interface ProcessSetupListener extends EventListener {

	/** Called if a new operator was added to the process. */
	public void operatorAdded(Operator operator);

	/**
	 * Called if an operator was removed from the process.
	 * 
	 * @param oldIndex
	 *            The former index of the operator within its {@link ExecutionUnit}.
	 * @param oldIndexAmongEnabled
	 *            The former index of the operator within its {@link ExecutionUnit} enabled
	 *            operators
	 */
	public void operatorRemoved(Operator operator, int oldIndex, int oldIndexAmongEnabled);

	/** Called if an operator changed in any way, e.g. if it was renamed. */
	public void operatorChanged(Operator operator);

	/** Called if the execution order within an ExecutionUnit changes. */
	public void executionOrderChanged(ExecutionUnit unit);

}
