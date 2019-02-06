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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.parameter.ParameterTypeDirectory;
import com.rapidminer.parameter.ParameterTypeFile;


/**
 * Cell editor consisting of a text field and a small button for opening a file chooser. Should be
 * used for parameters / properties which are files (but no special files like attribute description
 * files). If the desired property is a directory, the button automatically opens a file chooser for
 * directories.
 *
 * @see com.rapidminer.gui.properties.celleditors.value.AttributeFileValueCellEditor
 * @author Simon Fischer, Ingo Mierswa, Nils Woehler
 */
public abstract class FileValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = -8235047960089702819L;

	private final JPanel panel = new JPanel();

	private final JTextField textField = new JTextField(12);

	private final ParameterTypeFile type;

	private final GridBagLayout gridBagLayout = new GridBagLayout();

	private JButton button;

	public FileValueCellEditor(ParameterTypeFile type) {
		this.type = type;
		panel.setLayout(gridBagLayout);
		panel.setToolTipText(type.getDescription());
		textField.setToolTipText(type.getDescription());
		textField.addActionListener(e -> fireEditingStopped());
		textField.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				// The event is only fired if the focus loss is permanently,
				// i.e. it is not fired if the user e.g. just switched to another window.
				if (!e.isTemporary()) {
					fireEditingStopped();
				}
			}

			@Override
			public void focusGained(FocusEvent e) {}
		});

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		c.weightx = 1;
		gridBagLayout.setConstraints(textField, c);
		panel.add(textField);
	}

	protected JButton createFileChooserButton() {
		JButton button = new JButton(new ResourceAction(true, "choose_file") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				buttonPressed();
			}
		});
		button.setMargin(new Insets(0, 0, 0, 0));
		return button;
	}

	protected void addButton(JButton button, int gridwidth) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = gridwidth;
		c.weightx = 0;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 5, 0, 0);
		gridBagLayout.setConstraints(button, c);
		panel.add(button);

		this.button = button;
	}

	private void buttonPressed() {
		String value = (String) getCellEditorValue();
		value = resolveMacros(value);
		File file = value == null || value.length() == 0 ? null : RapidMinerGUI.getMainFrame().getProcess()
				.resolveFileName(value);
		File selectedFile = SwingTools.chooseFile(RapidMinerGUI.getMainFrame(), file, true,
				type instanceof ParameterTypeDirectory, type.getExtensions(), type.getKeys(),
				type.isAddAllFileExtensionsFilter());
		if (selectedFile != null) {
			setText(selectedFile);
			fireEditingStopped();
		} else {
			fireEditingCanceled();
		}
	}

	private String resolveMacros(String value) {
		try {
			return type.substituteMacros(value, RapidMinerGUI.getMainFrame().getProcess().getMacroHandler());
		} catch (Exception e) {
			return value;
		}
	}

	protected void setText(File file) {
		if (file == null) {
			textField.setText("");
		} else {
			textField.setText(file.getPath());
		}
	}

	@Override
	public Object getCellEditorValue() {
		return textField.getText().trim().length() == 0 ? null : textField.getText().trim();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
		textField.setText(value == null ? "" : value.toString());
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
	public void activate() {
		if (button != null) {
			button.doClick();
		}
	}

}
