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
package com.rapidminer.operator.error;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;


/**
 * This exception will be thrown if something is wrong with a parameter. If possible, use the more
 * specific subclasses for improved error handling in the GUI.
 *
 * @author Marco Boeck
 * @since 6.5.0
 */
public class ParameterError extends UserError {

	private static final long serialVersionUID = -7390311132493751678L;

	/** the parameter key which caused the error */
	private String key;

	public ParameterError(Operator operator, int code, String parameterkey, String additionalText) {
		this(operator, code, parameterkey, new Object[] { parameterkey, additionalText });
	}

	public ParameterError(Operator operator, int code, String parameterkey, Object... arguments) {
		super(operator, code, arguments);
		this.key = parameterkey;
	}

	public ParameterError(Operator operator, String code, String parameterkey, String additionalText) {
		this(operator, code, parameterkey, new Object[] { parameterkey, additionalText });
	}

	public ParameterError(Operator operator, String code, String parameterkey, Object... arguments) {
		super(operator, code, arguments);
		this.key = parameterkey;
	}

	/**
	 * @return the key of the parameter which caused the error. Can be {@code null} in very rare cases or subclasses
	 */
	public String getKey() {
		return key;
	}
}
