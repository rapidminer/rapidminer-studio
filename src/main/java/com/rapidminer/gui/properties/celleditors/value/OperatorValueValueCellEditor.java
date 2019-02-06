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
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;

import com.rapidminer.Process;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.Value;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeValue;
import com.rapidminer.parameter.ParameterTypeValue.OperatorValueSelection;


/**
 * Parameter editor for {@link ParameterTypeValue}, i.e. the parameter type for values which are
 * provided by operators.
 *
 * @see com.rapidminer.gui.properties.celleditors.value.DefaultPropertyValueCellEditor
 * @author Ingo Mierswa, Simon Fischer
 */
public class OperatorValueValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = 2332956173516489926L;

	private JPanel panel = new JPanel();

	private JComboBox<String> operatorCombo;

	private JComboBox<String> typeCombo = new JComboBox<>(new String[] { "value", "parameter" });

	private JComboBox<String> valueCombo = new JComboBox<>();

	private transient Process process;

	private ParameterTypeValue type;

	public OperatorValueValueCellEditor(ParameterTypeValue type) {
		this.type = type;
	}

	@Override
	public void setOperator(Operator operator) {
		this.process = operator.getProcess();
		operatorCombo = createOperatorCombo();
		typeCombo.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				fillValueCombo();
				fireEditingStopped();
			}
		});
		valueCombo.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				fireEditingStopped();
			}
		});

		fillValueCombo();

		panel.setLayout(new GridLayout(1, 3));

		panel.add(operatorCombo);
		panel.add(typeCombo);
		panel.add(valueCombo);

		type.setDefaultValue(getCellEditorValue());
	}

	private JComboBox<String> createOperatorCombo() {
		Vector<String> allOps = new Vector<String>(process.getAllOperatorNames());
		Collections.sort(allOps);
		JComboBox<String> combo = new JComboBox<>(allOps);
		combo.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				fillValueCombo();
				fireEditingStopped();
			}
		});
		return combo;
	}

	private void fillValueCombo() {
		valueCombo.removeAllItems();
		Operator operator = process.getOperator((String) operatorCombo.getSelectedItem());
		if (operator != null) {
			switch (typeCombo.getSelectedIndex()) {
				case 0:
					for (Value value : operator.getValues()) {
						valueCombo.addItem(value.getKey());
					}
					if (valueCombo.getItemCount() == 0) {
						valueCombo.addItem("no values");
					}
					break;
				case 1:
					for (ParameterType type : operator.getParameters().getParameterTypes()) {
						valueCombo.addItem(type.getKey());
					}
					if (valueCombo.getItemCount() == 0) {
						valueCombo.addItem("no params");
					}
					break;
			}
		}
		valueCombo.setSelectedIndex(-1);
	}

	@Override
	public Object getCellEditorValue() {
		OperatorValueSelection selection = new OperatorValueSelection((String) operatorCombo.getSelectedItem(),
				(typeCombo.getSelectedIndex() == 0), (String) valueCombo.getSelectedItem());
		return ParameterTypeValue.transformOperatorValueSelection2String(selection);
	}

	public void setValue(String valueName) {
		if (valueName != null) {
			OperatorValueSelection selection = ParameterTypeValue.transformString2OperatorValueSelection(valueName);
			if (selection != null) {
				operatorCombo.setSelectedItem(selection.getOperator());
				typeCombo.setSelectedIndex(selection.isValue() ? 0 : 1);
				valueCombo.setSelectedItem(selection.isValue() ? selection.getValueName() : selection.getParameterName());
			} else {
				operatorCombo.setSelectedIndex(0);
				typeCombo.setSelectedIndex(0);
				valueCombo.setSelectedIndex(-1);
			}
		} else {
			operatorCombo.setSelectedIndex(0);
			typeCombo.setSelectedIndex(0);
			valueCombo.setSelectedIndex(-1);
		}
		fireEditingStopped();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
		setValue((String) value);
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
