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
package com.rapidminer.gui.tools;

import com.rapidminer.gui.ConditionalAction;

import javax.swing.JMenu;


/**
 * This will create a menu, whose settings are take from a .properties file being part of the GUI
 * Resource bundles of RapidMiner. These might be accessed using the I18N class.
 * 
 * A resource menu needs a key specifier, which will be used to build the complete keys of the form:
 * gui.action.menu.<specifier>.label = Which will be the caption gui.action.menu.<specifier>.tip =
 * Which will be the tool tip gui.action.menu.<specifier>.mne = Which will give you access to the
 * accelerator key. Please make it the same case as in the label
 * 
 * @author Simon Fischer, Sebastian Land
 */
public class ResourceMenu extends JMenu {

	private static final long serialVersionUID = -7711922457461154801L;

	public ResourceMenu(String i18Key) {
		super(new ResourceActionAdapter("menu." + i18Key) {

			private static final long serialVersionUID = 1L;
			{
				setCondition(EDIT_IN_PROGRESS, DONT_CARE);
			}
		});
	}

	/**
	 * Enables or Disables the menu, if an edit is in progress. Default is <code>true</code>.
	 * 
	 * @param enable
	 *            <code>true</code> if the menu should be enabled and false otherwise
	 */
	public void enableOnEditInProgress(boolean enable) {
		ResourceActionAdapter action = (ResourceActionAdapter) getAction();
		if (enable) {
			action.setCondition(ConditionalAction.EDIT_IN_PROGRESS, ConditionalAction.DONT_CARE);
		} else {
			action.setCondition(ConditionalAction.EDIT_IN_PROGRESS, ConditionalAction.DISALLOWED);
		}
	}
}
