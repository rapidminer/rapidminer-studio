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
package com.rapidminer.studio.internal;

/**
 * Provider for a startup dialog. Can show the startup dialog with a given button preselected.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 */
public interface StartupDialogProvider {

	/**
	 * The buttons for the startup dialog.
	 *
	 * @author Gisa Schaefer
	 *
	 */
	public enum ToolbarButton {
		GETTING_STARTED, TUTORIAL, NEW_PROCESS, OPEN_PROCESS;
	}

	/**
	 * Shows the startup dialog with the button preselected.
	 *
	 * @param button
	 *            the button to preselect
	 */
	void show(ToolbarButton button);
}
