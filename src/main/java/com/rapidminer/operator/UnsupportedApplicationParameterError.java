/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
 **/
package com.rapidminer.operator;

/**
 * UserError that indicates an unsupported model application parameter (fixed message 204).
 *
 * @author Thilo Kamradt
*/
public class UnsupportedApplicationParameterError extends UserError {

	private static final long serialVersionUID = 1L;

	public UnsupportedApplicationParameterError(Operator operator, String modelName, String parameterName) {
		super(operator, 204, modelName, parameterName);
	}
}
