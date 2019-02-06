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
package com.rapidminer.gui.tools.actions;

import com.rapidminer.gui.LoggedAbstractAction;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ExtendedJTableSorterModel;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.SwingTools;

import java.awt.event.ActionEvent;

import javax.swing.Icon;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class AddToSortingColumnsAction extends LoggedAbstractAction {

	private static final long serialVersionUID = -6389072245302152679L;

	private static final String DESCENDING_ICON_NAME = "sort_descending.png";
	private static final String ASCENDING_ICON_NAME = "sort_ascending.png";

	private static final Icon[] DESCENDING_ICONS = new Icon[IconSize.values().length];
	private static final Icon[] ASCENDING_ICONS = new Icon[IconSize.values().length];

	static {
		int counter = 0;
		for (IconSize size : IconSize.values()) {
			DESCENDING_ICONS[counter] = SwingTools.createIcon(size.getSize() + "/" + DESCENDING_ICON_NAME);
			ASCENDING_ICONS[counter] = SwingTools.createIcon(size.getSize() + "/" + ASCENDING_ICON_NAME);
			counter++;
		}
	}

	private ExtendedJTable table;

	private int direction;

	public AddToSortingColumnsAction(ExtendedJTable table, int direction, IconSize size) {
		super("Add to Sorting Columns (" + (direction == ExtendedJTableSorterModel.DESCENDING ? "Descending" : "Ascending")
				+ ")", direction == ExtendedJTableSorterModel.DESCENDING ? DESCENDING_ICONS[size.ordinal()]
				: ASCENDING_ICONS[size.ordinal()]);
		this.table = table;
		this.direction = direction;
		putValue(SHORT_DESCRIPTION,
				"Adds the selected column to the set of columns according to which this table is sorted.");
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		this.table.setSortingStatus(this.direction, false);
	}
}
