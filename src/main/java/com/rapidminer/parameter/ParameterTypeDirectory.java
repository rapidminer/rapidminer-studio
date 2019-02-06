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
package com.rapidminer.parameter;

/**
 * A parameter type for directories. Operators ask for the selected directory with
 * {@link com.rapidminer.operator.Operator#getParameterAsFile(String)} .
 * 
 * @author Ingo Mierswa
 */
public class ParameterTypeDirectory extends ParameterTypeFile {

	private static final long serialVersionUID = 8908250135075572154L;

	public ParameterTypeDirectory(String key, String description, boolean optional) {
		super(key, description, null, null);
		setOptional(optional);
	}

	public ParameterTypeDirectory(String key, String description, String defaultFileName) {
		super(key, description, null, defaultFileName);
		setOptional(true);
	}
}
