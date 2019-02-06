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
package com.rapidminer.gui.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.rapidminer.example.AttributeWeights;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.Tools;


/**
 * This weights dialog is used to show attribute weights created by the process, by the user, or
 * were loaded from a file. Several view modes and sorting are supported.
 * 
 * @author Ingo Mierswa ingomierswa Exp $
 */
public class AttributeWeightsDialog extends JDialog {

	private static final long serialVersionUID = 5700615743712147883L;

	private boolean ok = false;

	private AttributeWeightsTableModel attributeTableModel;

	private JTextField minWeightField = new JTextField("0.0");

	private JCheckBox minWeightCheckBox = new JCheckBox("Show smaller weights");

	private JLabel selectionCount = new JLabel();

	/** Creates a new dialog for the given feature weights. */
	public AttributeWeightsDialog(AttributeWeights weights) {
		super(ApplicationFrame.getApplicationFrame(), "Attribute Weights", true);
		getContentPane().setLayout(new BorderLayout());

		// buttons
		JPanel buttonPanel = new JPanel();
		JButton okButton = new JButton("Ok");
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ok = true;
				AttributeWeightsDialog.this.dispose();
			}
		});
		buttonPanel.add(okButton);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ok = false;
				AttributeWeightsDialog.this.dispose();
			}
		});
		buttonPanel.add(cancelButton);

		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		// ================================================================================

		// table with feature weights
		attributeTableModel = new AttributeWeightsTableModel(weights);

		JTable weightTable = new ExtendedJTable() {

			private static final long serialVersionUID = -7129594652364114532L;

			@Override
			public TableCellEditor getCellEditor(int row, int column) {
				if (column == 1) {
					return attributeTableModel.getWeightEditor(row);
				} else {
					return super.getCellEditor(row, column);
				}
			}

			@Override
			public TableCellRenderer getCellRenderer(int row, int column) {
				if (column == 1) {
					return attributeTableModel.getWeightEditor(row);
				} else {
					return super.getCellRenderer(row, column);
				}
			}
		};
		weightTable.setModel(attributeTableModel);

		JScrollPane scrollPane = new ExtendedJScrollPane(weightTable);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		// ================================================================================

		// control button panel
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		JPanel controlPanel = new JPanel();
		controlPanel.setLayout(layout);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.insets = new Insets(11, 11, 11, 11);

		final JComboBox<String> viewModes = new JComboBox<>(AttributeWeightsTableModel.VIEW_MODES);
		viewModes.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				attributeTableModel.setViewMode(viewModes.getSelectedIndex());
				update();
			}
		});
		layout.setConstraints(viewModes, c);
		controlPanel.add(viewModes);

		JButton updateButton = new JButton("Update");
		updateButton.setToolTipText("Click to update the view.");
		updateButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				update();
			}
		});
		layout.setConstraints(updateButton, c);
		controlPanel.add(updateButton);

		JLabel minWeightLabel = new JLabel("Min weight:");
		c.weightx = 0;
		c.gridwidth = GridBagConstraints.RELATIVE;
		layout.setConstraints(minWeightLabel, c);
		controlPanel.add(minWeightLabel);

		c.weightx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		minWeightField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				update();
			}
		});
		layout.setConstraints(minWeightField, c);
		controlPanel.add(minWeightField);

		minWeightCheckBox.setToolTipText("If not marked only weights greater than min weight will be shown.");
		minWeightCheckBox.setSelected(true);
		minWeightCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				update();
			}
		});
		layout.setConstraints(minWeightCheckBox, c);
		controlPanel.add(minWeightCheckBox);

		layout.setConstraints(selectionCount, c);
		controlPanel.add(selectionCount);
		updateSelectionCounter();

		JPanel dummy = new JPanel();
		c.weighty = 1;
		layout.setConstraints(dummy, c);
		controlPanel.add(dummy);
		c.weighty = 0;

		final JCheckBox overwriteCheckBox = new JCheckBox("overwrite");
		overwriteCheckBox
				.setToolTipText("If marked loaded weights will overwrite the current ones (weight of 0 does always overwrite).");
		overwriteCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				attributeTableModel.setOverwriteMode(overwriteCheckBox.isSelected());
			}
		});
		layout.setConstraints(overwriteCheckBox, c);
		controlPanel.add(overwriteCheckBox);

		JButton loadButton = new JButton("Load");
		loadButton.setToolTipText("Load weights from file.");
		loadButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				load();
			}
		});
		layout.setConstraints(loadButton, c);
		controlPanel.add(loadButton);

		JButton saveButton = new JButton("Save");
		saveButton.setToolTipText("Save current weights to file.");
		saveButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});
		layout.setConstraints(saveButton, c);
		controlPanel.add(saveButton);

		controlPanel.setBorder(BorderFactory.createEmptyBorder(11, 11, 11, 11));
		getContentPane().add(controlPanel, BorderLayout.WEST);

		pack();
		setLocationRelativeTo(getOwner());
	}

	private void updateSelectionCounter() {
		selectionCount.setText(attributeTableModel.getNumberOfSelected() + " selected / "
				+ attributeTableModel.getTotalNumber() + " total");
	}

	private void update() {
		double minWeight = Double.NEGATIVE_INFINITY;
		try {
			minWeight = Double.parseDouble(minWeightField.getText().trim());
		} catch (NumberFormatException e) {
			minWeightField.setText(attributeTableModel.getMinWeight() + "");
		}

		if (!minWeightCheckBox.isSelected()) {
			attributeTableModel.setMinWeight(minWeight);
		} else {
			attributeTableModel.setMinWeight(Double.NEGATIVE_INFINITY);
		}

		attributeTableModel.updateTable();
		updateSelectionCounter();
	}

	private void load() {
		File file = SwingTools.chooseFile(null, null, true, "wgt", "attribute weight file");
		try {
			AttributeWeights fileWeights = AttributeWeights.load(file);
			attributeTableModel.mergeWeights(fileWeights);
		} catch (IOException e) {
			SwingTools.showSimpleErrorMessage(this, "cannot_load_attr_weights_from_file", e, file.getName());
		}
		update();
	}

	private void save() {
		File file = SwingTools.chooseFile(null, null, true, "wgt", "attribute weight file");
		try {
			attributeTableModel.getAttributeWeights().writeAttributeWeights(file, Tools.getDefaultEncoding());
		} catch (IOException e) {
			SwingTools.showSimpleErrorMessage(this, "cannot_write_attr_weights_to_file", e, file.getName());
		}
	}

	public boolean isOk() {
		return ok;
	}

	public AttributeWeights getAttributeWeights() {
		return attributeTableModel.getAttributeWeights();
	}

}
