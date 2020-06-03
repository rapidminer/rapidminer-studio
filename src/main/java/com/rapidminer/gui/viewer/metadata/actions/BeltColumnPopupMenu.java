/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import java.awt.Component;
import javax.swing.JPopupMenu;

import com.rapidminer.gui.viewer.metadata.BeltColumnStatisticsPanel;
import com.rapidminer.gui.viewer.metadata.model.BeltMetaDataStatisticsModel;


/**
 * It is undesirable to instantiate {@link BeltMetaDataStatisticsModel#PAGE_SIZE} popup menus with
 * n*PAGE_SIZE actions each which all do the exact same thing. For that reason the latest
 * {@link BeltColumnStatisticsPanel} is stored when the popup is opened so it can be retrieved by the
 * actions later.
 * 
 * @author Marco Boeck, Gisa Meier
 * @since 9.7.0
 */
public class BeltColumnPopupMenu extends JPopupMenu {

	private static final long serialVersionUID = 1726669511974218061L;

	private BeltColumnStatisticsPanel csp;

	@Override
	public void show(Component invoker, int x, int y) {
		super.show(invoker, x, y);

		// check component hierarchy for BeltAttributeStatisticsPanel
		Component c = invoker;
		while (c != null) {
			if (BeltColumnStatisticsPanel.class.isAssignableFrom(c.getClass())) {
				this.csp = (BeltColumnStatisticsPanel) c;
				break;
			}
			c = c.getParent();
		}
	}

	/**
	 * Returns the {@link BeltColumnStatisticsPanel} on which this popup was last opened.
	 * 
	 * @return the column statistics panel
	 */
	BeltColumnStatisticsPanel getColumnStatisticsPanel() {
		return csp;
	}
}
