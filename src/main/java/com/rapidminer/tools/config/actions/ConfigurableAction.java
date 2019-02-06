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
package com.rapidminer.tools.config.actions;

import com.rapidminer.tools.config.AbstractConfigurator;
import com.rapidminer.tools.config.Configurable;


/**
 * <p>
 * Actions which can be performed for a {@link Configurable} must implement this interface. An
 * example would be to clear some kind of cache. These actions can be defined per
 * {@link Configurable} instance, so two {@link Configurable}s of the same
 * {@link AbstractConfigurator Configurator} type can have different actions.
 * </p>
 * 
 * @author Marco Boeck
 * 
 */
public interface ConfigurableAction {

	/**
	 * Returns <code>true</code> if this action makes use of GUI components, e.g. Swing dialogs,
	 * etc. If not, returns <code>false</code>.
	 * 
	 * @return
	 */
	public boolean hasUI();

	/**
	 * Executed when the action is performed. The {@link ActionResult} indicates
	 * success/failure/neither and an optional message which can be diplayed to the user.
	 * 
	 * This method is called in a separate thread.
	 * 
	 * @return
	 */
	public ActionResult doWork();

	/**
	 * Returns the name of the action which is displayed to the user.
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * Returns the tooltip of the action which is displayed to the user.
	 * 
	 * @return
	 */
	public String getTooltip();

	/**
	 * Returns the name of the icon for this action. If this is <code>null</code>, no icon will be
	 * shown.
	 * 
	 * @return
	 */
	public String getIconName();
}
