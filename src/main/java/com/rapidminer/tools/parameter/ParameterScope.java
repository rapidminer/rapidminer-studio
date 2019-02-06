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
package com.rapidminer.tools.parameter;

/**
 * The ParameterScope defines where the associated Parameter is used. PreStartParameters will be
 * exported with the key of their parameter type as an environment variable. This can be used for
 * example for setting up things that need to be influenced before the jvm starts.
 * 
 * ModifyingPreStartParameters will be appended to existing environment variables.
 * 
 * @author Sebastian Land
 */
public class ParameterScope {

	private String preStartName = null;
	private boolean isModifyingPreStartParameter = false;
	private boolean isGuiParameter = false;
	private boolean isFileAccessParameter = false;

	/**
	 * This is this a preStartParameter, this will return the name of the environment variable that
	 * should be set by this parameter. Otherwise null is returned.
	 */
	public String getPreStartName() {
		return preStartName;
	}

	public boolean isPreStartParameter() {
		return preStartName == null;
	}

	public boolean isGuiParameter() {
		return isGuiParameter;
	}

	public boolean isFileAccessParameter() {
		return isFileAccessParameter;
	}

	public boolean isModifyingPreStartParameter() {
		return isModifyingPreStartParameter;
	}
}
