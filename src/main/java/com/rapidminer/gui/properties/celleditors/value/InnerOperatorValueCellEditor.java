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

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.parameter.ParameterTypeInnerOperator;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;


/**
 * Parameter editor for {@link com.rapidminer.parameter.ParameterTypeInnerOperator}.
 * 
 * @author Ingo Mierswa
 */
public class InnerOperatorValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {// ,
																											// Observer<Operator>
																											// {

	private static final long serialVersionUID = -2559892872774108384L;

	private JComboBox<String> operatorCombo = new JComboBox<>();

	private transient OperatorChain parentOperator;

	public InnerOperatorValueCellEditor(ParameterTypeInnerOperator type) {}

	@Override
	public void setOperator(Operator parentOperator) {
		this.parentOperator = (OperatorChain) parentOperator;
		// We cannot add observers here. First, it hangs the GUI and second it is not guaranteed
		// that it is removed again later. To be more precise, it is guaranteed that it is not
		// removed.
		// this.parentOperator.addObserver(this, true);
		this.operatorCombo = new JComboBox<>();
		updateOperatorCombo();
	}

	private void updateOperatorCombo() {
		Object selectedItem = this.operatorCombo.getSelectedItem();
		this.operatorCombo.removeAllItems();
		List<Operator> allInnerOps = parentOperator.getAllInnerOperators();
		Vector<String> allOpNames = new Vector<String>();
		Iterator<Operator> i = allInnerOps.iterator();
		while (i.hasNext()) {
			allOpNames.add(i.next().getName());
		}
		Collections.sort(allOpNames);
		for (String opName : allOpNames) {
			this.operatorCombo.addItem(opName);
		}
		this.operatorCombo.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				fireEditingStopped();
			}
		});
		if (this.operatorCombo.getItemCount() == 0) {
			this.operatorCombo.addItem("add inner operators");
		}
		this.operatorCombo.setSelectedItem(selectedItem);
	}

	@Override
	public Object getCellEditorValue() {
		return operatorCombo.getSelectedItem();
	}

	public void setValue(String valueName) {
		if (valueName != null) {
			operatorCombo.setSelectedItem(valueName);
		} else {
			operatorCombo.setSelectedIndex(0);
		}
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
		setValue((String) value);
		return operatorCombo;
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

	// @Override
	// public void update(Observable<Operator> observable, Operator arg) {
	// updateOperatorCombo();
	// }

	@Override
	public boolean rendersLabel() {
		return false;
	}
}
