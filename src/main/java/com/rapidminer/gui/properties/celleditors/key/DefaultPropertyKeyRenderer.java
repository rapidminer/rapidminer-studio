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
package com.rapidminer.gui.properties.celleditors.key;

import com.rapidminer.gui.properties.DefaultRMCellEditor;
import com.rapidminer.gui.properties.PropertyTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;


/**
 * Renders the key in a {@link PropertyTable} either bold or plain depending on whether or not the
 * parameter is optional.
 * 
 * @author Ingo Mierswa Exp $
 */
public class DefaultPropertyKeyRenderer extends DefaultRMCellEditor implements PropertyKeyCellEditor {

	private static final long serialVersionUID = 5599667832953596663L;

	private transient ParameterType type;

	public DefaultPropertyKeyRenderer(ParameterType type) {
		super(new JTextField());
		this.type = type;
		((JTextField) editorComponent).setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
	}

	/** Does nothing. */
	@Override
	public void setOperator(Operator operator, PropertyTable table) {}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return getTableCellEditorComponent(table, value, isSelected, row, column);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
		JComponent c = (JComponent) super.getTableCellEditorComponent(table, value, selected, row, column);
		if (!type.isOptional() && (type.getDefaultValue() == null)) {
			c.setFont(c.getFont().deriveFont(Font.BOLD, c.getFont().getSize()));
		}
		if (selected) {
			c.setBackground(SwingTools.LIGHTEST_BLUE);
		} else {
			c.setBackground(Color.WHITE);
		}
		return c;
	}
}
