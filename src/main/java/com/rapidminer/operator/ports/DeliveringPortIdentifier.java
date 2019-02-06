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

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserData;


/**
 * This class helps to identify an {@link OutputPort}. Resolving can be done using the {@link DeliveringPortManager}.
 *
 * @author Andreas Timm
 * @since 9.1
 */
public class DeliveringPortIdentifier implements UserData<Object> {
	/**
	 * A default ID for an {@link OutputPort} that is an outer {@link OutputPort}.
	 */
	static final int DEFAULT_SUBPROCESS_ID = -1;

	private final String operatorName;
	private final String portName;
	private final int subprocessId;


	/**
	 * Constructor to identify an {@link OutputPort} of a normal {@link Operator}
	 *
	 * @param operatorName
	 * 		that equals {@link Operator#getName()}
	 * @param portName
	 * 		that equals {@link Port#getName()}
	 */
	DeliveringPortIdentifier(String operatorName, String portName) {
		this(operatorName, portName, DEFAULT_SUBPROCESS_ID);
	}

	/**
	 * Constructor to identify an internal {@link Port} of a {@link com.rapidminer.operator.OperatorChain OperatorChain}
	 * and therefore requires the ID of the subprocess.
	 *
	 * @param operatorName
	 * 		that equals {@link Operator#getName()}
	 * @param portName
	 * 		that equals {@link Port#getName()}
	 * @param subprocessId
	 * 		ID of the subprocess that is the {@link Ports} -> {@link PortOwner#getPortHandler()} in the operatorName's {@link Operator}
	 */
	DeliveringPortIdentifier(String operatorName, String portName, int subprocessId) {
		this.operatorName = operatorName;
		this.portName = portName;
		if (subprocessId < 0) {
			this.subprocessId = DEFAULT_SUBPROCESS_ID;
		} else {
			this.subprocessId = subprocessId;
		}
	}

	/**
	 * {@link Operator Operator's} name to be resolvable using
	 * {@link com.rapidminer.Process#getOperator(String) Process.getOperator(String)}
	 */
	public String getOperatorName() {
		return operatorName;
	}

	/**
	 * Name of the {@link OutputPort} to be resolvable using {@link OutputPorts#getPortByName(String)}
	 */
	public String getPortName() {
		return portName;
	}

	/**
	 * ID of the {@link com.rapidminer.operator.ExecutionUnit ExecutionUnit} (subprocess) if the
	 * {@link OutputPort} resides inside an {@link com.rapidminer.operator.OperatorChain OperatorChain}
	 */
	public int getSubprocessId() {
		return subprocessId;
	}

	@Override
	public String toString() {
		String subprocessPart = subprocessId != DEFAULT_SUBPROCESS_ID ? ":" + subprocessId : "";
		return operatorName + subprocessPart + "/" + portName;
	}

	@Override
	public UserData<Object> copyUserData(Object newParent) {
		return new DeliveringPortIdentifier(operatorName, portName, subprocessId);
	}
}
