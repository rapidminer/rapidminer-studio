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
package com.rapidminer.gui.look.ui;

import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicListUI;


/**
 * The UI used for lists.
 * 
 * @author Ingo Mierswa
 */
public class ListUI extends BasicListUI {

	public static ComponentUI createUI(JComponent c) {
		return new ListUI();
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();
	}

	@Override
	protected void uninstallDefaults() {
		super.uninstallDefaults();
	}

	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
	}

	@Override
	public void uninstallUI(JComponent c) {
		super.uninstallUI(c);
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void paintCell(Graphics g, int row, Rectangle rowBounds, ListCellRenderer cellRenderer, ListModel dataModel,
			ListSelectionModel selModel, int leadIndex) {
		super.paintCell(g, row, rowBounds, cellRenderer, dataModel, selModel, leadIndex);
	}
}
