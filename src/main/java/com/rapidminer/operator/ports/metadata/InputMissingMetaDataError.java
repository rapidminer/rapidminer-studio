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

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.quickfix.ConnectToQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;


/**
 * Indicates that input for a port was missing or had the wrong type.
 * 
 * @author Simon Fischer
 */
public class InputMissingMetaDataError extends SimpleMetaDataError {

	private InputPort inputPort;
	private Class<? extends IOObject> desiredClass;

	public InputMissingMetaDataError(InputPort inputPort, Class<? extends IOObject> desiredClazz) {
		this(inputPort, desiredClazz, null);
	}

	public InputMissingMetaDataError(InputPort inputPort, Class<? extends IOObject> desiredClass,
			Class<? extends IOObject> receivedClass) {
		super(Severity.ERROR, inputPort, receivedClass == null ? "input_missing" : "expected",
				receivedClass == null ? new Object[] { inputPort.getSpec() }
						: new Object[] { desiredClass.getSimpleName(), receivedClass.getSimpleName() });
		this.inputPort = inputPort;
		this.desiredClass = desiredClass;
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<QuickFix> getQuickFixes() {
		List<QuickFix> fixes = new LinkedList<QuickFix>();
		if (desiredClass != null) {
			for (OutputPort outputPort : inputPort.getPorts().getOwner().getConnectionContext().getAllOutputPorts()) {
				if (!outputPort.isConnected() && outputPort.getMetaData() != null
						&& desiredClass.isAssignableFrom(outputPort.getMetaData().getObjectClass())) {
					fixes.add(new ConnectToQuickFix(inputPort, outputPort));
				}
			}

		}
		return fixes;
	}
}
