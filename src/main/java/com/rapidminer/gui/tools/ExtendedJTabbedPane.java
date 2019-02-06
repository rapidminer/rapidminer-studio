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

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;


/**
 * Creates a tabbed pane with tabs at top and which scroll the tabs instead of creating multiple
 * lines.
 * 
 * @author Ingo Mierswa
 */
public class ExtendedJTabbedPane extends JTabbedPane {

	private static final long serialVersionUID = 8798498172271429876L;

	public ExtendedJTabbedPane() {
		super(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
	}

	/*
	 * Adds a component and tip represented by a title and/or icon, either of which can be null.
	 * Cover method for insertTab. Additionally this method will register the context of the tab on
	 * the help service to switch context based help if tab is shown.
	 * 
	 * @param title the title to be displayed in this tab
	 * 
	 * @param icon the icon to be displayed in this tab
	 * 
	 * @param component the component to be displayed when this tab is clicked
	 * 
	 * @param tip the tooltip to be displayed for this tab
	 * 
	 * @param helpTopicId the help topic id of the context to be shown if tab is activeted
	 * 
	 * public void addTab(String title, Icon icon, Component component, String tip, String
	 * helpTopicId) { //SingleSelectionModel thisModel = this.getModel(); super.addTab(title, icon,
	 * component, tip); //HelpService.registerModel(thisModel, getTabCount() - 1, helpTopicId); }
	 */
}
