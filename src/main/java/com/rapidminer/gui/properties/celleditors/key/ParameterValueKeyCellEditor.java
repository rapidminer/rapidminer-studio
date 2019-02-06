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

import com.rapidminer.Process;
import com.rapidminer.gui.properties.ParameterChangeListener;
import com.rapidminer.gui.properties.PropertyTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeParameterValue;
import com.rapidminer.parameter.ParameterTypeValue;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;


/**
 * Parameter editor for a {@link ParameterTypeValue}, i.e. a parameter name and value of a single
 * operator. This can for example be used by parameter optimization operators.
 * 
 * @author Ingo Mierswa
 */
public class ParameterValueKeyCellEditor extends AbstractCellEditor implements PropertyKeyCellEditor {

	private static final long serialVersionUID = -2559892872774108384L;

	private JPanel panel = new JPanel();

	private JComboBox<String> operatorCombo = new JComboBox<>();

	private JComboBox<String> parameterCombo = new JComboBox<>();

	private transient OperatorChain parentOperator;

	private transient Process process;

	private transient ParameterChangeListener listener = null;

	private boolean fireEvent = true;

	public ParameterValueKeyCellEditor(ParameterTypeParameterValue type) {}

	protected Object readResolve() {
		this.process = this.parentOperator.getProcess();
		return this;
	}

	@Override
	public void setOperator(Operator operator, PropertyTable propertyTable) {
		this.parentOperator = (OperatorChain) operator;
		this.process = parentOperator.getProcess();
		operatorCombo = createOperatorCombo(propertyTable);
		parameterCombo = createParameterCombo((String) operatorCombo.getSelectedItem(), propertyTable);

		panel.setLayout(new GridLayout(1, 2));

		panel.add(operatorCombo);
		panel.add(parameterCombo);

		fireParameterChangedEvent();
	}

	private JComboBox<String> createOperatorCombo(final PropertyTable propertyTable) {
		List<Operator> allInnerOps = parentOperator.getAllInnerOperators();
		Vector<String> allOpNames = new Vector<String>();
		Iterator<Operator> i = allInnerOps.iterator();
		while (i.hasNext()) {
			allOpNames.add(i.next().getName());
		}
		Collections.sort(allOpNames);
		final JComboBox<String> combo = new JComboBox<>(allOpNames);
		combo.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				String operatorName = (String) combo.getSelectedItem();
				panel.remove(parameterCombo);
				parameterCombo = createParameterCombo(operatorName, propertyTable);
				panel.add(parameterCombo);
				fireParameterChangedEvent();
				fireEditingStopped();
			}
		});
		if (combo.getItemCount() == 0) {
			combo.addItem("add inner operators");
		} else {
			combo.setSelectedIndex(0);
		}
		return combo;
	}

	private JComboBox<String> createParameterCombo(String operatorName, PropertyTable propertyTable) {
		JComboBox<String> combo = new JComboBox<>();

		Operator operator = process.getOperator((String) operatorCombo.getSelectedItem());
		if (operator != null) {
			Iterator<ParameterType> i = operator.getParameters().getParameterTypes().iterator();
			while (i.hasNext()) {
				combo.addItem(i.next().getKey());
			}
		}

		if (combo.getItemCount() == 0) {
			combo.addItem("no parameters");
		}

		combo.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				fireParameterChangedEvent();
				fireEditingStopped();
			}
		});

		combo.setSelectedIndex(0);

		return combo;
	}

	@Override
	public Object getCellEditorValue() {
		String result = operatorCombo.getSelectedItem() + "." + parameterCombo.getSelectedItem();
		return result;
	}

	private void setValue(String valueName) {
		this.fireEvent = false;
		if (valueName != null) {
			String[] components = valueName.split("\\.");
			if (components.length == 2) {
				String operator = components[0];
				String parameterName = components[1];
				operatorCombo.setSelectedItem(operator);
				parameterCombo.setSelectedItem(parameterName);
			} else {
				operatorCombo.setSelectedIndex(0);
				parameterCombo.setSelectedIndex(0);
			}
		} else {
			operatorCombo.setSelectedIndex(0);
			parameterCombo.setSelectedIndex(0);
		}
		this.fireEvent = true;
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

	public void setParameterChangeListener(ParameterChangeListener listener) {
		this.listener = listener;
	}

	public void fireParameterChangedEvent() {
		if (fireEvent) {
			if (listener != null) {
				String operatorName = (String) operatorCombo.getSelectedItem();
				String parameterName = (String) parameterCombo.getSelectedItem();
				listener.parameterSelectionChanged(this.parentOperator, operatorName, parameterName);
			}
		}
	}
}
