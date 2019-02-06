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
package com.rapidminer.operator.ports.quickfix;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;


/**
 * Quick fix that inserts a operator into an existing connection.
 * 
 * @author Simon Fischer
 * 
 */
public abstract class OperatorInsertionQuickFix extends AbstractQuickFix {

	private final InputPort inputPort;
	private final int connectToPort;
	private final int connectFromPort;

	public OperatorInsertionQuickFix(String key, Object[] args, int rating, InputPort inputPort) {
		this(key, args, rating, inputPort, 0, 0);
	}

	public OperatorInsertionQuickFix(String key, Object[] args, int rating, InputPort inputPort, int connectToPort,
			int connectFromPort) {
		super(rating, false, key, args);
		this.inputPort = inputPort;
		this.connectToPort = connectToPort;
		this.connectFromPort = connectFromPort;
	}

	@Override
	public void apply() {
		try {
			Operator operator = createOperator();
			if (operator == null) {
				return;
			}
			ExecutionUnit process = inputPort.getPorts().getOwner().getOperator().getExecutionUnit();
			process.addOperator(operator);
			OutputPort source = inputPort.getSource();
			source.disconnect();
			source.connectTo(operator.getInputPorts().getPortByIndex(connectToPort));
			operator.getOutputPorts().getPortByIndex(connectFromPort).connectTo(inputPort);
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("cannot_insert_operator", e);
		}
	}

	public abstract Operator createOperator() throws OperatorCreationException;

}
