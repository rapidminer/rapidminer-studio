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
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.SwingTools;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Icon;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public class SortColumnsAccordingToNameAction extends LoggedAbstractAction {

	private static final long serialVersionUID = 6104623498326293185L;

	private static final String ICON_NAME = "table_refresh.png";

	private static final Icon[] ICONS = new Icon[IconSize.values().length];

	static {
		int counter = 0;
		for (IconSize size : IconSize.values()) {
			ICONS[counter++] = SwingTools.createIcon(size.getSize() + "/" + ICON_NAME);
		}
	}

	private ExtendedJTable table;

	public SortColumnsAccordingToNameAction(ExtendedJTable table, IconSize size) {
		super("Sort Columns by Names", ICONS[size.ordinal()]);
		this.table = table;
		putValue(SHORT_DESCRIPTION, "Rearrange all columns according to the natural order of the column names.");
		putValue(MNEMONIC_KEY, Integer.valueOf(KeyEvent.VK_C));
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		this.table.sortColumnsAccordingToNames();
	}
}
