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

import com.rapidminer.tools.ParameterService;


/**
 * This interface can be implemented in order to be informed whenever a parameter in the
 * {@link ParameterService} changes. It replaces the ancient SettingsChangeListener that only was
 * informed, when the user changed a parameter.
 * 
 * @author Sebastian Land
 */
public interface ParameterChangeListener {

	/**
	 * This method will be invoked whenever a parameter of the ParameterService has been changed.
	 */
	public void informParameterChanged(String key, String value);

	/**
	 * This method will be called whenever the settings will be saved.
	 */
	public void informParameterSaved();
}
