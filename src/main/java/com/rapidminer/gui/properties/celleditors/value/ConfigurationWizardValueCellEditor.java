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

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.wizards.ConfigurationWizardCreator;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeConfiguration;


/**
 * Cell editor consisting of a simple button which opens a configuration wizard for the
 * corresponding operator.
 * 
 * @author Ingo Mierswa
 */
public class ConfigurationWizardValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -7163760967040772736L;

	private transient final ParameterTypeConfiguration type;

	private final JButton button;

	public ConfigurationWizardValueCellEditor(ParameterTypeConfiguration type) {
		this.type = type;
		button = new JButton(new ResourceAction(true, "wizard." + type.getWizardCreator().getI18NKey()) {

			private static final long serialVersionUID = 5340097986173787690L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				buttonPressed();
			}
		});
		button.setToolTipText(type.getDescription());
	}

	/** Does nothing. */
	@Override
	public void setOperator(Operator operator) {}

	private void buttonPressed() {
		ConfigurationWizardCreator creator = type.getWizardCreator();
		if (creator != null) {
			creator.createConfigurationWizard(type, type.getWizardListener());
		}
	}

	@Override
	public Object getCellEditorValue() {
		return null;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
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

	@Override
	public boolean rendersLabel() {
		return true;
	}

	@Override
	public void activate() {
		button.doClick();
	}
}
