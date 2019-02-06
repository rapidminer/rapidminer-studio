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
package com.rapidminer.operator.ports.metadata;

import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.quickfix.DisconnectQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;

import java.util.Collections;
import java.util.List;


/**
 * Indicates that the process data flow contains a loop.
 * 
 * @author Simon Fischer
 * 
 */
public class OperatorLoopError implements MetaDataError {

	private OutputPort outputPort;
	private InputPort inputPort;
	private Port port;

	private final QuickFix fix;

	public OperatorLoopError(InputPort port) {
		this.port = port;
		this.inputPort = port;
		this.outputPort = port.getSource();
		fix = new DisconnectQuickFix(outputPort, inputPort);
	}

	public OperatorLoopError(OutputPort port) {
		this.port = port;
		this.outputPort = port;
		this.inputPort = port.getDestination();
		fix = new DisconnectQuickFix(outputPort, inputPort);
	}

	@Override
	public String getMessage() {
		return "This port is part of a loop.";
	}

	@Override
	public PortOwner getOwner() {
		return port.getPorts().getOwner();
	}

	@Override
	public Port getPort() {
		return port;
	}

	@Override
	public List<QuickFix> getQuickFixes() {
		return Collections.singletonList(fix);
	}

	@Override
	public Severity getSeverity() {
		return Severity.ERROR;
	}
}
