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
package com.rapidminer.gui.dialog;

import com.rapidminer.gui.properties.celleditors.value.PropertyValueCellEditor;
import com.rapidminer.operator.Operator;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;


/**
 * Editor for attribute weights. A text field for numeric values and three buttons. The first button
 * sets the weight to zero, the second button to 1, and the third resets the value to the old
 * weight. This editor is used by an {@link AttributeWeightsTableModel}.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public class AttributeWeightCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = 4648838759294286088L;

	private JPanel panel = new JPanel();

	private JTextField textField = new JTextField(12);

	private GridBagLayout gridBagLayout = new GridBagLayout();

	public AttributeWeightCellEditor(double oldValue) {
		super();
		panel.setLayout(gridBagLayout);
		panel.setToolTipText("The weight for this attribute.");
		textField.setToolTipText("The weight for this attribute.");

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		gridBagLayout.setConstraints(textField, c);
		panel.add(textField);
		c.weightx = 0;
		addButton(createValueButton("Zero", "0.0"), 1);
		addButton(createValueButton("One", "1.0"), GridBagConstraints.RELATIVE);
		addButton(createValueButton("Reset", oldValue + ""), GridBagConstraints.REMAINDER);
	}

	/** Does nothing. */
	@Override
	public void setOperator(Operator operator) {}

	protected JButton createValueButton(String name, final String newValue) {
		JButton button = new JButton(name);
		button.setMargin(new Insets(0, 0, 0, 0));
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				textField.setText(newValue);
				fireEditingStopped();
			}
		});
		button.setToolTipText("Sets the weight of this attribute to " + newValue + ".");
		return button;
	}

	protected void addButton(JButton button, int gridwidth) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = gridwidth;
		c.weightx = 0;
		c.fill = GridBagConstraints.BOTH;
		gridBagLayout.setConstraints(button, c);
		panel.add(button);
	}

	@Override
	public Object getCellEditorValue() {
		return (textField.getText().trim().length() == 0) ? null : textField.getText().trim();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
		textField.setText((value == null) ? "" : value.toString());
		return panel;
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

	@Override
	public boolean rendersLabel() {
		return false;
	}
}
