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
package com.rapidminer.gui.operatormenu;

import com.rapidminer.gui.tools.ResourceMenu;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;

import javax.swing.Icon;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;


/**
 * A menu for operator groups.
 * 
 * @author Tobias Malbrecht
 */
public class OperatorGroupMenu extends ResourceMenu {

	private static final long serialVersionUID = -2163282611857073088L;

	private final Icon openFolderIcon = SwingTools.createIcon("16/"
			+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.menu.operator_group.open.icon"));

	private final Icon closedFolderIcon = SwingTools.createIcon("16/"
			+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.menu.operator_group.closed.icon"));

	public OperatorGroupMenu(String groupName) {
		super("operator_group");
		setText(groupName);
		setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.menu.operator_group.tip", groupName));
		setIcon(closedFolderIcon);
		this.addMenuListener(new MenuListener() {

			@Override
			public void menuCanceled(MenuEvent e) {
				setIcon(closedFolderIcon);
			}

			@Override
			public void menuDeselected(MenuEvent e) {
				setIcon(closedFolderIcon);
			}

			@Override
			public void menuSelected(MenuEvent e) {
				setIcon(openFolderIcon);
			}

		});
	}
}
