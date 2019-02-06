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
package com.rapidminer.gui.tools.dialogs;

import javax.swing.JButton;
import javax.swing.JComponent;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.properties.celleditors.value.PropertyValueCellEditor;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;


/**
 * A dialog to set a single parameter.
 *
 * @author Tobias Malbrecht
 */
public class SetParameterDialog extends ButtonDialog {

	private static final long serialVersionUID = 1484984144870499737L;

	private final Operator operator;

	private final ParameterType type;

	private final PropertyValueCellEditor editor;


	public SetParameterDialog(final Operator operator, final ParameterType type) {
		super(ApplicationFrame.getApplicationFrame(), "set_parameter", ModalityType.MODELESS, new Object[] { type.getKey()
			.replace('_', ' ') });
		this.operator = operator;
		this.type = type;
		editor = PropertyPanel.instantiateValueCellEditor(type, operator);
		JComponent editorComponent = (JComponent) editor.getTableCellEditorComponent(null, type.getDefaultValue(), false, 0,
				1);

		JButton okButton = makeOkButton("set_parameter_dialog_apply");
		layoutDefault(editorComponent, okButton, makeCancelButton());
		getRootPane().setDefaultButton(okButton);
	}

	@Override
	protected String getInfoText() {
		return type.getDescription();
	}

	@Override
	protected void ok() {
		Object value = editor.getCellEditorValue();
		if (value != null && ((String) value).length() != 0) {
			operator.setParameter(type.getKey(), (String) value);
		}
		super.ok();
	}
}
