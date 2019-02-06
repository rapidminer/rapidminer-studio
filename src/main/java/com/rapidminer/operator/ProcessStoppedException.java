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
package com.rapidminer.operator;

/**
 * Exception class whose instances are thrown by instances of the class 'Operator' or of one of its
 * subclasses.
 *
 * @author Ingo Mierswa
 */
public class ProcessStoppedException extends OperatorException {

	private static final long serialVersionUID = 8299515313202467747L;

	private transient final Operator op;
	private String opName;

	public ProcessStoppedException(Operator o) {
		super("Process stopped in " + o.getName());
		this.op = o;
		this.opName = op.getName();
	}

	public ProcessStoppedException() {
		super("Process stopped");
		this.op = null;
		this.opName = "";
	}

	public Operator getOperator() {
		return op;
	}

	public String getOperatorName() {
		return opName;
	}

}
