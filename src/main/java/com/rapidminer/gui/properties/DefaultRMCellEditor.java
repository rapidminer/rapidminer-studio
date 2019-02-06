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
package com.rapidminer.gui.properties;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;


/**
 * This cell editor adds a nice looking border to table cell editor components if they are selected.
 * 
 * @author Nils Woehler
 * 
 */
public class DefaultRMCellEditor extends DefaultCellEditor {

	private static final long serialVersionUID = 1L;

	public DefaultRMCellEditor(JCheckBox checkBox) {
		super(checkBox);
	}

	public DefaultRMCellEditor(JComboBox<?> jComboBox) {
		super(jComboBox);
	}

	public DefaultRMCellEditor(JTextField textfield) {
		super(textfield);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
		if (c instanceof JComponent && !(c instanceof JCheckBox)) {
			JComponent component = (JComponent) c;
			if (isSelected) {
				component.setBorder(FocusedComponentBorder.getInstance());
			} else {
				component.setBorder(null);
			}
		}
		return c;
	}

}
