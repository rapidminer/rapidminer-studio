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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.AbstractCellEditor;
import javax.swing.CellEditor;
import javax.swing.JButton;
import javax.swing.JTable;

import com.rapidminer.gui.properties.FilterPropertyDialog;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeFilter;


/**
 * The {@link CellEditor} for the {@link ParameterTypeFilter}. Does nothing except providing a
 * button to open the Filter configuration dialog.
 * 
 * @author Marco Boeck
 * 
 */
public class FilterValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -3025695623413817868L;

	private Operator operator;

	private JButton button;

	private FilterPropertyDialog dialog;

	/**
	 * Creates a new {@link FilterValueCellEditor} instance.
	 */
	public FilterValueCellEditor(final ParameterTypeFilter type) {
		button = new JButton(new ResourceAction(true, "set_filters") {

			private static final long serialVersionUID = 8274776396885048377L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				dialog = new FilterPropertyDialog(operator, type, "filter");
				dialog.setVisible(true);
				// no dialog handling necessary, does everything itself
				fireEditingStopped();
			}
		});
		button.setMargin(new Insets(0, 0, 0, 0));
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		return button;
	}

	@Override
	public Object getCellEditorValue() {
		return null;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return button;
	}

	@Override
	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public void activate() {
		button.doClick();
	}

}
