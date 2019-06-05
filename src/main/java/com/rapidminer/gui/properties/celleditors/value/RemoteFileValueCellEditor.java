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
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.properties.DefaultRMCellEditor;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterTypeRemoteFile;


/**
 * Cell editor consisting of a text field which stores a file name and a small button for opening a
 * file chooser for remote files (for example files stored in Dropbox).
 *
 * @author Gisa Schaefer
 * @since 6.1.0
 *
 */
public class RemoteFileValueCellEditor extends DefaultRMCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = 8012729317514228920L;

	private JPanel container;

	private JButton fileOpenButton;

	public RemoteFileValueCellEditor(final ParameterTypeRemoteFile type) {
		super(new JTextField());
		this.container = new JPanel(new GridBagLayout());
		this.container.setToolTipText(type.getDescription());

		GridBagConstraints gbc = new GridBagConstraints();

		editorComponent.setToolTipText(type.getDescription());
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.weighty = 1;
		gbc.weightx = 1;
		container.add(editorComponent, gbc);

		editorComponent.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				// The event is only fired if the focus loss is permanently,
				// i.e. it is not fired if the user e.g. just switched to another window.
				// Otherwise any changes made after switching back to RapidMiner would
				// not be saved.
				if (!e.isTemporary()) {
					fireEditingStopped();
				}
			}
		});

		fileOpenButton = new JButton();
		fileOpenButton.setAction(new ResourceAction(true, "choose_remote_file") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				fileOpenButton.setEnabled(false);
				ProgressThread openFileChooserThread = new ProgressThread("open_remote_file_chooser") {

					@Override
					public void run() {
						getProgressListener().setTotal(100);
						getProgressListener().setCompleted(10);

						try {
							type.getRemoteFileSystemView().checkFileSystemAvailability();
						} catch (UserError e1) {
							SwingTools.showVerySimpleErrorMessage("cannot_open_remote_file_chooser");
							fileOpenButton.setEnabled(true);
							return;
						}
						getProgressListener().setCompleted(30);

						// get given path
						String predefinedText = ((JTextField) editorComponent).getText();
						if (predefinedText == null || predefinedText.isEmpty()) {
							predefinedText = "";
						}

						// check if the element exists
						boolean fileExists = type.getRemoteFileSystemView().parentExists(predefinedText);

						// create the JFileChooser accordingly
						final JFileChooser chooser = fileExists ? new JFileChooser(predefinedText, type
								.getRemoteFileSystemView()) : new JFileChooser(type.getRemoteFileSystemView());

						chooser.setFileSelectionMode(type.getFileSelectionMode());

						getProgressListener().setCompleted(80);
						SwingUtilities.invokeLater(() -> {
							int returnvalue = chooser.showOpenDialog(RapidMinerGUI.getMainFrame());
							if (returnvalue == JFileChooser.APPROVE_OPTION) {
								((JTextField) editorComponent).setText(type.getRemoteFileSystemView()
										.getNormalizedPathName(chooser.getSelectedFile()));
								fireEditingStopped();
							}
							fileOpenButton.setEnabled(true);
						});
					}

				};
				openFileChooserThread.setStartDialogShowTimer(true);
				openFileChooserThread.setShowDialogTimerDelay(1500);
				openFileChooserThread.start();
			}
		});
		gbc.gridx += 1;
		gbc.weightx = 0;
		gbc.insets = new Insets(0, 5, 0, 0);
		container.add(fileOpenButton, gbc);
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public boolean useEditorAsRenderer() {
		return true;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if (value != null) {
			((JTextField) editorComponent).setText(String.valueOf(value));
		}
		return container;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		if (value != null) {
			((JTextField) editorComponent).setText(String.valueOf(value));
		}
		return container;
	}

	@Override
	public Object getCellEditorValue() {
		return ((JTextField) editorComponent).getText();
	}

	@Override
	public void setOperator(Operator operator) {}

	@Override
	public void activate() {
		fileOpenButton.doClick();
	}

}
