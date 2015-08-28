/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.viewer.renderer;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;


/**
 * The default number CellRenderer for the Example Set table Aligns numerical values right.
 * 
 * @author Marco Boeck
 * 
 */
public class DataTableNumericalRenderer extends DefaultTableCellRenderer {

	private static final long serialVersionUID = -3376218040898856384L;

	/**
	 * Creates a new {@link DataTableNumericalRenderer}.
	 */
	public DataTableNumericalRenderer() {}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		JLabel label = (JLabel) value;
		// align numbers right
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		return label;
	}
}
