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
package com.rapidminer.tools.config;

import com.rapidminer.tools.config.actions.ConfigurableAction;


/**
 * An abstract class which already implements methods from {@link ConfigurableAction} which are not
 * needed for testing the configurable. Only {@link #doWork()} has to be implemented.
 * 
 * @author Nils Woehler
 * 
 */
public abstract class TestConfigurableAction implements ConfigurableAction {

	@Override
	public boolean hasUI() {
		return false;  // no need to implement
	}

	@Override
	public String getName() {
		return null; // no need to implement
	}

	@Override
	public String getTooltip() {
		return null;  // no need to implement
	}

	@Override
	public String getIconName() {
		return null;  // no need to implement
	}

}
