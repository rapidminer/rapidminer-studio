/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.operator.ports.impl;

import com.rapidminer.Process;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.operator.DebugMode;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.WrapperOperatorRuntimeException;
import com.rapidminer.operator.ports.DeliveringPortManager;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.tools.belt.BeltTools;


/**
 * @author Simon Fischer
 */

public class OutputPortImpl extends AbstractOutputPort {

	/** Use the factory method {@link OutputPorts#createPort(String)} to create OutputPorts. */
	protected OutputPortImpl(Ports<OutputPort> owner, String name, boolean simulatesStack) {
		super(owner, name, simulatesStack);
	}

	@Override
	public void deliver(IOObject object) {
		// disallow advanced columns outside operators until the capability provider can handle them
		if (object instanceof IOTable && BeltTools.hasAdvanced(((IOTable) object).getTable())) {
			throw new WrapperOperatorRuntimeException(new OperatorException("No advanced columns allowed outside Operators"));
		}

		// registering history of object
		if (object != null) {
			object.appendOperatorToHistory(getPorts().getOwner().getOperator(), this);
			DeliveringPortManager.setLastDeliveringPort(object, this);
			// set source if not yet set
			if (object.getSource() == null && getPorts().getOwner().getOperator() != null) {
				object.setSource(getPorts().getOwner().getOperator().getName());
			}
		}

		// delivering data
		setData(object);
		if (isConnected()) {
			getDestination().receive(object);
		}

		Process process = getPorts().getOwner().getOperator().getProcess();
		if (process != null && process.getDebugMode() == DebugMode.COLLECT_METADATA_AFTER_EXECUTION) {
			if (object == null) {
				setRealMetaData(null);
			} else {
				setRealMetaData(MetaData.forIOObject(object));
			}
		} else {
			setRealMetaData(null);
		}

	}

}
