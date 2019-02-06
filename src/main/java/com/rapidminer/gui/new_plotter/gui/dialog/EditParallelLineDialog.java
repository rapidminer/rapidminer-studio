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
package com.rapidminer.gui.new_plotter.gui.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.new_plotter.configuration.AxisParallelLineConfiguration;
import com.rapidminer.gui.new_plotter.configuration.LineFormat.LineStyle;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;


/**
 * This dialog allows the user to edit a {@link AxisParallelLineConfiguration} line (crosshair
 * line).
 * 
 * @author Marco Boeck
 * @deprecated since 9.2.0
 */
@Deprecated
public class EditParallelLineDialog extends JDialog {

	/** the ok {@link JButton} */
	private JButton okButton;

	/** the cancel {@link JButton} */
	private JButton cancelButton;

	/** the text field with the line value */
	private JTextField valueField;

	/** the text field with the line width */
	private JTextField widthField;

	/** the line color button */
	private JButton lineColorButton;

	/** the combobox to chose the {@link LineStyle} */
	private JComboBox<LineStyle> lineStyleCombobox;

	/** the line color */
	private Color lineColor;

	/** the {@link AxisParallelLineConfiguration} line to edit */
	private AxisParallelLineConfiguration line;

	private static final long serialVersionUID = 1932257219370926682L;

	/**
	 * Creates a new {@link EditParallelLineDialog}.
	 */
	public EditParallelLineDialog() {
		super(ApplicationFrame.getApplicationFrame());
		setupGUI();
	}

	/**
	 * Setup the GUI.
	 */
	private void setupGUI() {
		JPanel mainPanel = new JPanel();
		this.setContentPane(mainPanel);

		// start layout
		mainPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 2, 5);
		JLabel valueLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.value.label"));
		this.add(valueLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		valueField = new JTextField();
		valueField.setInputVerifier(new InputVerifier() {

			@Override
			public boolean verify(JComponent input) {
				return verifyValueInput(input);
			}
		});
		valueField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.value.tip"));
		this.add(valueField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		JLabel colorLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.color.label"));
		this.add(colorLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		lineColorButton = new JButton(new ResourceAction(true, "edit_parallel_line.select_line_color") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				createLineColorDialog();
			}
		});
		this.add(lineColorButton, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		JLabel widthLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.width.label"));
		this.add(widthLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		widthField = new JTextField();
		widthField.setInputVerifier(new InputVerifier() {

			@Override
			public boolean verify(JComponent input) {
				return verifyWidthInput(input);
			}
		});
		widthField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.width.tip"));
		this.add(widthField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		JLabel styleLabel = new JLabel(
				I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.line_style.label"));
		this.add(styleLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		lineStyleCombobox = new JComboBox<>(LineStyle.values());
		((DefaultComboBoxModel<LineStyle>) lineStyleCombobox.getModel()).removeElement(LineStyle.NONE);
		lineStyleCombobox
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.line_style.tip"));
		lineStyleCombobox.setSelectedItem(LineStyle.SOLID);
		this.add(lineStyleCombobox, gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets = new Insets(15, 5, 5, 5);
		this.add(new JSeparator(), gbc);

		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		okButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.ok.label"));
		okButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.ok.tip"));
		okButton.setIcon(SwingTools.createIcon("24/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.ok.icon")));
		okButton.setMnemonic(I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.ok.mne").toCharArray()[0]);
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean successful = editLine();
				// don't dispose dialog if not successful
				if (!successful) {
					return;
				}

				EditParallelLineDialog.this.dispose();
			}
		});
		okButton.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					okButton.doClick();
				}
			}
		});
		this.add(okButton, gbc);

		gbc.gridx = 1;
		gbc.gridy = 5;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.EAST;
		cancelButton = new JButton(I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.cancel.label"));
		cancelButton.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.cancel.tip"));
		cancelButton.setIcon(SwingTools.createIcon("24/"
				+ I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.cancel.icon")));
		cancelButton.setMnemonic(I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.cancel.mne")
				.toCharArray()[0]);
		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// cancel requested, close dialog
				EditParallelLineDialog.this.dispose();
			}
		});
		this.add(cancelButton, gbc);

		// misc settings
		this.setMinimumSize(new Dimension(275, 225));
		// center dialog
		this.setLocationRelativeTo(getOwner());
		this.setTitle(I18N.getMessage(I18N.getGUIBundle(), "gui.action.edit_parallel_line.title.label"));
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setModal(true);
		this.addWindowListener(new WindowAdapter() {

			@Override
			public void windowActivated(WindowEvent e) {
				cancelButton.requestFocusInWindow();
			}
		});
	}

	/**
	 * Sets the {@link AxisParallelLineConfiguration} to edit.
	 * 
	 * @param line
	 * @param allowValueEdit
	 *            if set to false, the value field will not be enabled
	 */
	public void setLine(AxisParallelLineConfiguration line, boolean allowValueEdit) {
		if (line == null) {
			throw new IllegalArgumentException("line must not be null!");
		}
		if (!allowValueEdit) {
			this.valueField.setEnabled(false);
		}

		this.line = line;
		this.lineColor = line.getFormat().getColor();
		this.valueField.setText(String.valueOf(line.getValue()));
		this.widthField.setText(String.valueOf(line.getFormat().getWidth()));
		this.lineStyleCombobox.setSelectedItem(line.getFormat().getStyle());
	}

	/**
	 * Shows the dialog.
	 */
	public void showDialog() {
		setVisible(true);
	}

	/**
	 * Verify that the value is correct.
	 * 
	 * @param input
	 * @return true if the value is valid; false otherwise
	 */
	private boolean verifyValueInput(JComponent input) {
		JTextField textField = (JTextField) input;
		String inputString = textField.getText();
		try {
			Double.parseDouble(inputString);
		} catch (NumberFormatException e) {
			textField.setForeground(Color.RED);
			return false;
		}

		textField.setForeground(Color.BLACK);
		return true;
	}

	/**
	 * Verify that the value is correct.
	 * 
	 * @param input
	 * @return true if the value is valid; false otherwise
	 */
	private boolean verifyWidthInput(JComponent input) {
		JTextField textField = (JTextField) input;
		String inputString = textField.getText();
		try {
			float width = Float.parseFloat(inputString);
			if (width <= 0.0) {
				textField.setForeground(Color.RED);
				return false;
			}
		} catch (NumberFormatException e) {
			textField.setForeground(Color.RED);
			return false;
		}

		textField.setForeground(Color.BLACK);
		return true;
	}

	/**
	 * Shows a dialog where the user can select the plot background color.
	 */
	private void createLineColorDialog() {
		Color oldColor = line.getFormat().getColor();
		if (oldColor == null) {
			oldColor = Color.BLACK;
		}
		Color newLineColor = JColorChooser.showDialog(null, I18N.getGUILabel("edit_parallel_line.line_color_title.label"),
				oldColor);
		if (newLineColor != null && !newLineColor.equals(oldColor)) {
			lineColor = newLineColor;
		}
	}

	/**
	 * Edits the line.
	 *
	 * @return true if the line has been edited; false otherwise
	 */
	private boolean editLine() {
		if (line != null) {
			// make sure value is valid, otherwise don't do anything!
			if (!valueField.getInputVerifier().verify(valueField)) {
				valueField.requestFocusInWindow();
				return false;
			}
			// make sure width is valid, otherwise don't do anything!
			if (!widthField.getInputVerifier().verify(widthField)) {
				widthField.requestFocusInWindow();
				return false;
			}

			line.setValue(Double.parseDouble(valueField.getText()));
			line.getFormat().setWidth(Float.parseFloat(widthField.getText()));
			if (lineColor != null) {
				line.getFormat().setColor(lineColor);
			}
			LineStyle selectedLineStyle = (LineStyle) lineStyleCombobox.getSelectedItem();
			if (selectedLineStyle != null) {
				line.getFormat().setStyle(selectedLineStyle);
			}
		} else {
			return false;
		}

		return true;
	}
}
