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
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;

import com.rapidminer.gui.properties.AttributeOrderingDialog;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.preprocessing.filter.attributes.AttributeOrderingOperator;
import com.rapidminer.parameter.ParameterTypeAttributeOrderingRules;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Tools;


/**
 * The cell editor for {@link ParameterTypeAttributeOrderingRules}.
 *
 * @author Nils Woehler
 */
public class AttributeOrderingCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -2387465714767785072L;

	public static final char SEPARATION_CHAR = '|';

	private String attributeListString = "";

	private final JButton button;

	private Operator operator;

	public AttributeOrderingCellEditor(final ParameterTypeAttributeOrderingRules type) {
		this.button = new JButton(new ResourceAction(true, "attribute_ordering") {

			private static final long serialVersionUID = -4890375754223285831L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				if (operator == null) {
					return;
				}
				LinkedList<String> preSelectedAttributeNames = new LinkedList<String>();
				String combinedNames = null;
				try {
						combinedNames = operator.getParameter(type.getKey());
				} catch (UndefinedParameterError er) {
				}
				if (combinedNames != null) {
					for (String attributeName : combinedNames.split("\\|")) {
						preSelectedAttributeNames.add(Tools.unmask(SEPARATION_CHAR, attributeName));
					}
				}
				AttributeOrderingDialog dialog = new AttributeOrderingDialog(type, preSelectedAttributeNames,
						operator.getParameterAsBoolean(AttributeOrderingOperator.PARAMETER_USE_REGEXP));
				dialog.setVisible(true);
				if (dialog.isOk()) {
					StringBuilder builder = new StringBuilder();
					boolean first = true;
					Collection<String> attributeNames = dialog.getSelectedAttributeNames();
					for (String attributeName : attributeNames) {
						if (!first) {
							builder.append(SEPARATION_CHAR);
						}
						builder.append(Tools.mask(SEPARATION_CHAR, attributeName));
						first = false;
					}
					attributeListString = builder.toString();
					fireEditingStopped();
				} else {
					fireEditingCanceled();
				}
			}
		});
		button.setMargin(new Insets(0, 0, 0, 0));
	}

	@Override
	public Object getCellEditorValue() {
		return attributeListString;
	}

	@Override
	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public boolean useEditorAsRenderer() {
		return false;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		return button;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return button;
	}

	@Override
	public void activate() {
		button.doClick();
	}
}
