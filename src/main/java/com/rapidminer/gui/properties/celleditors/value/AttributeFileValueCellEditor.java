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

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JButton;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.attributeeditor.AttributeEditorDialog;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeAttributeFile;


/**
 * This is an extension of the FileValueCellEditor which also supports the opening of an
 * AttributeEditor. This editor should be used if an attribute description file is desired instead
 * of a normal file.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class AttributeFileValueCellEditor extends FileValueCellEditor {

	private static final long serialVersionUID = 99319694250830796L;

	private transient Operator exampleSource;

	private JButton button;

	public AttributeFileValueCellEditor(ParameterTypeAttributeFile type) {
		super(type);
		button = new JButton(new ResourceAction(true, "edit_attributefile") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				buttonPressed();
			}
		});
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setToolTipText("Edit or create attribute description files and data (XML).");
		addButton(button, GridBagConstraints.RELATIVE);

		addButton(createFileChooserButton(), GridBagConstraints.REMAINDER);
	}

	@Override
	public void setOperator(Operator exampleSource) {
		this.exampleSource = exampleSource;
	}

	private void buttonPressed() {
		Object value = getCellEditorValue();
		File file = value == null ? null : RapidMinerGUI.getMainFrame().getProcess().resolveFileName(value.toString());
		AttributeEditorDialog dialog = new AttributeEditorDialog(exampleSource, file);
		dialog.setVisible(true);
		setText(dialog.getFile());
		fireEditingStopped();
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
