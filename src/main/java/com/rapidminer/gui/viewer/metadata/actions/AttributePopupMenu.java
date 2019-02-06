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
package com.rapidminer.gui.viewer.metadata.actions;

import com.rapidminer.gui.viewer.metadata.AttributeStatisticsPanel;
import com.rapidminer.gui.viewer.metadata.model.MetaDataStatisticsModel;

import java.awt.Component;

import javax.swing.JPopupMenu;


/**
 * It is undesirable to instantiate {@link MetaDataStatisticsModel#PAGE_SIZE} popup menus with
 * n*PAGE_SIZE actions each which all do the exact same thing. For that reason the latest
 * {@link AttributeStatisticsPanel} is stored when the popup is opened so it can be retrieved by the
 * actions later.
 * 
 * @author Marco Boeck
 * 
 */
public class AttributePopupMenu extends JPopupMenu {

	private static final long serialVersionUID = 1726669511974218061L;

	private AttributeStatisticsPanel asp;

	@Override
	public void show(Component invoker, int x, int y) {
		super.show(invoker, x, y);

		// check component hierarchy for AttributeStatisticsPanel
		Component c = invoker;
		while (c != null) {
			if (AttributeStatisticsPanel.class.isAssignableFrom(c.getClass())) {
				this.asp = (AttributeStatisticsPanel) c;
				break;
			}
			c = c.getParent();
		}
	}

	/**
	 * Returns the {@link AttributeStatisticsPanel} on which this popup was last opened.
	 * 
	 * @return
	 */
	AttributeStatisticsPanel getAttributeStatisticsPanel() {
		return asp;
	}
}
