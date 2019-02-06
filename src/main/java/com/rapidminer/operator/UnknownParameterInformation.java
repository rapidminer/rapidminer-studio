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
 * This is a helper class storing information about unknown parameters.
 * 
 * @author Ingo Mierswa
 */
public class UnknownParameterInformation implements Comparable<UnknownParameterInformation> {

	private String operatorName;

	private String operatorClass;

	private String parameterName;

	private String parameterValue;

	public UnknownParameterInformation(String operatorName, String operatorClass, String parameterName, String parameterValue) {
		this.operatorName = operatorName;
		this.operatorClass = operatorClass;
		this.parameterName = parameterName;
		this.parameterValue = parameterValue;
	}

	public String getOperatorName() {
		return operatorName;
	}

	public String getOperatorClass() {
		return operatorClass;
	}

	public String getParameterName() {
		return parameterName;
	}

	public String getParameterValue() {
		return parameterValue;
	}

	@Override
	public int compareTo(UnknownParameterInformation o) {
		return this.operatorClass.compareTo(o.operatorClass);
	}
}
