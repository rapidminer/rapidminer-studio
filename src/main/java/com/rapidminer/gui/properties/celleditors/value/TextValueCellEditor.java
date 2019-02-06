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
import java.awt.event.ActionEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;

import com.rapidminer.gui.properties.TextPropertyDialog;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.syntax.JEditTextArea;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeText;


/**
 * A cell editor with a button that opens a {@link JEditTextArea}.
 *
 * @author Ingo Mierswa
 */
public class TextValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -4429790999365057931L;

	private ParameterTypeText type;

	private Operator operator;

	private JButton button = new JButton(new ResourceAction(true, "edit_text") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			TextPropertyDialog dialog = new TextPropertyDialog(type, text, operator);
			dialog.setVisible(true);
			if (dialog.isOk()) {
				text = dialog.getText();
				setButtonText();
				fireEditingStopped();
			} else {
				fireEditingCanceled();
			}
		}
	});

	private String text = null;

	public TextValueCellEditor(ParameterTypeText type) {
		this.type = type;
		button.setMargin(new java.awt.Insets(0, 0, 0, 0));
		button.setToolTipText(type.getDescription());
		setButtonText();
	}

	@Override
	public void setOperator(final Operator operator) {
		this.operator = operator;
	}

	@Override
	public Object getCellEditorValue() {
		return text;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
		this.text = (String) value;
		setButtonText();
		return button;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return getTableCellEditorComponent(table, value, isSelected, row, column);
	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	private void setButtonText() {
		if (text != null && text.length() > 0) {
			button.setText("Edit Text (" + text.length() + " characters)...");
		} else {
			button.setText("Edit Text...");
		}
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
