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


/**
 * This class holds information about a processing step for an IOObject. Currently the name of the
 * operator and port is remembered.
 * 
 * @author Sebastian Land
 */
public class ProcessingStep {

	private String operatorName;

	private String portName;

	/**
	 * This constructor builds a ProcessingStep from the current operator and the used outputport of
	 * the respective IOObject.
	 */
	public ProcessingStep(Operator operator, OutputPort port) {
		this.operatorName = operator.getName();
		this.portName = port.getName();
	}

	/**
	 * This returns the name of the operator, which processed the respective IOObject.
	 */
	public String getOperatorName() {
		return operatorName;
	}

	/**
	 * This return the name of the output port, which was used to return the respective IOObject.
	 */
	public String getPortName() {
		return portName;
	}

	@Override
	public int hashCode() {
		return operatorName.hashCode() ^ portName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ProcessingStep)) {
			return false;
		}
		ProcessingStep other = (ProcessingStep) obj;
		return operatorName.equals(other.operatorName) && portName.equals(other.portName);
	}

	@Override
	public String toString() {
		return operatorName + "." + portName;
	}
}
