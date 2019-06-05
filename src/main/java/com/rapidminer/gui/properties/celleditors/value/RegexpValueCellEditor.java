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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.rapidminer.gui.properties.RegexpPropertyDialog;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeRegexp;
import com.rapidminer.parameter.ParameterTypeString;


/**
 * A cell editor for regular expression parameters. Supports the direct specification of regular
 * expressions. Also provides a button which starts a dialog to edit regular expressions.
 *
 * @author Tobias Malbrecht, Nils Woehler
 */
public class RegexpValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -2387465714767785072L;

	private final JPanel panel = new JPanel();

	private final JTextField textField = new JTextField(12);

	private JButton button;
	private Operator operator;

	public RegexpValueCellEditor(final ParameterTypeRegexp regexp) {
		panel.setLayout(new GridBagLayout());
		panel.setToolTipText(regexp.getDescription());
		textField.setToolTipText(regexp.getDescription());
		textField.addActionListener(e -> fireEditingStopped());
		textField.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				// fire only if the focus didn't move to the button. If this check
				// would not be included, fireEditingStopped() would prevent the button's
				// ActionEvent from being fired. The user would have to click a second time to
				// trigger the button action.
				// Additionally, the event is only fired if the focus loss is permanently,
				// i.e. it is not fired if the user e.g. just switched to another window.
				// Otherwise any changes made after switching back to RapidMiner would
				// not be saved for the same reasons as stated above.
				if (e.getOppositeComponent() != button && !e.isTemporary()) {
					fireEditingStopped();
				}
			}
		});

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		c.weightx = 1;
		panel.add(textField, c);

		ParameterTypeString replacement = regexp.getReplacementParameter();

		button = new JButton(new ResourceAction(true, "regexp") {

			private static final long serialVersionUID = 3989811306286704326L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				String replacementKey = null;
				if (replacement != null) {
					replacementKey = replacement.getKey();
				}
				RegexpPropertyDialog dialog = new RegexpPropertyDialog(regexp.getPreviewList(), textField.getText(),
						regexp.getDescription(), replacementKey);
				if (replacement != null && operator != null) {
					String replacementValue = operator.getParameters().getParameterOrNull(replacementKey);
					if (replacementValue != null) {
						dialog.setReplacement(replacementValue);
					}
				}
				dialog.setVisible(true);
				if (dialog.wasConfirmed()) {
					textField.setText(dialog.getRegexp());
					if (replacement != null && operator != null) {
						operator.setParameter(replacementKey, dialog.getReplacement());
					}
				}
				fireEditingStopped();
			}
		});
		button.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(FocusEvent e) {
				if (e.getOppositeComponent() != textField && !e.isTemporary()) {
					fireEditingStopped();
				}
			}
		});
		button.setMargin(new Insets(0, 0, 0, 0));
		c.weightx = 0;
		c.insets = new Insets(0, 5, 0, 0);
		panel.add(button, c);
	}

	@Override
	public Object getCellEditorValue() {
		return textField.getText();
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	@Override
	public boolean useEditorAsRenderer() {
		return false;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		textField.setText(value == null ? "" : value.toString());
		return panel;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		textField.setText(value == null ? "" : value.toString());
		return panel;
	}

	@Override
	public void activate() {
		button.doClick();
	}
}
