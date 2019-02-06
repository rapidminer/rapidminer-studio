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
package com.rapidminer.gui.viewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.look.ui.TableHeaderUI;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.learner.associations.FrequentItemSets;


/**
 * The viewer for frequent item sets.
 *
 * @author Ingo Mierswa
 */
public class FrequentItemSetVisualization extends JPanel {

	private static final long serialVersionUID = -4353590225271845908L;

	private FrequentItemSetsTableModel model;

	private JLabel totalSizeLabel = new JLabel();

	private JLabel minItemSetSizeLabel = new JLabel();

	public FrequentItemSetVisualization(final FrequentItemSets frequentSets) {
		setLayout(new BorderLayout());

		// main panel
		{
			JPanel mainPanel = new JPanel();
			GridBagLayout layout = new GridBagLayout();
			mainPanel.setLayout(layout);
			mainPanel.setOpaque(true);
			mainPanel.setBackground(Colors.WHITE);
			mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 5, 10, 10));
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1;
			c.weighty = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;

			if (frequentSets.size() == 0) {
				JLabel emptyLabel = new JLabel("no itemsets found");
				JPanel emptyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				emptyPanel.add(emptyLabel);
				layout.setConstraints(emptyPanel, c);
				mainPanel.add(emptyPanel);
			} else {
				frequentSets.sortSets();
				this.model = new FrequentItemSetsTableModel(frequentSets);
				ExtendedJTable table = new ExtendedJTable(this.model, true);
				table.setRowHeight(PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);
				table.setRowHighlighting(true);

				table.getTableHeader().putClientProperty(RapidLookTools.PROPERTY_TABLE_HEADER_BACKGROUND, Colors.WHITE);
				((TableHeaderUI) table.getTableHeader().getUI()).installDefaults();

				JScrollPane tablePane = new ExtendedJScrollPane(table);
				tablePane.setBorder(null);
				tablePane.setBackground(Colors.WHITE);
				tablePane.getViewport().setBackground(Colors.WHITE);
				layout.setConstraints(tablePane, c);
				mainPanel.add(tablePane);
			}

			add(mainPanel, BorderLayout.CENTER);
		}

		// control panel
		{
			GridBagLayout layout = new GridBagLayout();
			JPanel controlPanel = new JPanel(layout);
			controlPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 5));
			controlPanel.setOpaque(true);
			controlPanel.setBackground(Colors.WHITE);
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 0;
			c.weighty = 0;
			c.insets = new Insets(4, 4, 4, 4);
			c.gridwidth = GridBagConstraints.REMAINDER;

			totalSizeLabel.setText("No. of Sets: " + frequentSets.size());
			layout.setConstraints(totalSizeLabel, c);
			controlPanel.add(totalSizeLabel);

			minItemSetSizeLabel.setText("Total Max. Size: " + frequentSets.getMaximumSetSize());
			layout.setConstraints(minItemSetSizeLabel, c);
			controlPanel.add(minItemSetSizeLabel);

			Component strut = Box.createVerticalStrut(10);
			layout.setConstraints(strut, c);
			controlPanel.add(strut);

			JLabel label = new JLabel("Min. Size: ");
			c.gridwidth = GridBagConstraints.RELATIVE;
			layout.setConstraints(label, c);
			controlPanel.add(label);

			final JTextField minSizeField = new JTextField(4);
			minSizeField.setText("1");
			c.gridwidth = GridBagConstraints.REMAINDER;
			layout.setConstraints(minSizeField, c);
			controlPanel.add(minSizeField);

			label = new JLabel("Max. Size: ");
			c.gridwidth = GridBagConstraints.RELATIVE;
			layout.setConstraints(label, c);
			controlPanel.add(label);

			final JTextField maxSizeField = new JTextField(4);
			maxSizeField.setText(frequentSets.getMaximumSetSize() + "");
			c.gridwidth = GridBagConstraints.REMAINDER;
			layout.setConstraints(maxSizeField, c);
			controlPanel.add(maxSizeField);

			label = new JLabel("Contains Item: ");
			c.gridwidth = GridBagConstraints.REMAINDER;
			layout.setConstraints(label, c);
			controlPanel.add(label);

			final JTextField itemNameField = new JTextField(8);
			c.gridwidth = GridBagConstraints.REMAINDER;
			layout.setConstraints(itemNameField, c);
			controlPanel.add(itemNameField);

			// update button
			final JButton updateButton = new JButton("Update View");
			c.gridwidth = GridBagConstraints.REMAINDER;
			layout.setConstraints(updateButton, c);
			controlPanel.add(updateButton);

			updateButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					int minNumber = 1;
					String minText = minSizeField.getText();
					if (minText != null && minText.trim().length() >= 0) {
						try {
							minNumber = Integer.parseInt(minText);
						} catch (NumberFormatException ex) {
							SwingTools.showVerySimpleErrorMessage("only_num_values_for_min_nr_of_items");
							return;
						}
					}

					int maxNumber = frequentSets.getMaximumSetSize();
					String maxText = maxSizeField.getText();
					if (maxText != null && maxText.trim().length() >= 0) {
						try {
							maxNumber = Integer.parseInt(maxText);
						} catch (NumberFormatException ex) {
							SwingTools.showVerySimpleErrorMessage("only_num_values_for_min_nr_of_items");
							return;
						}
					}

					String searchString = itemNameField.getText();
					if (searchString != null) {
						if (searchString.trim().length() == 0) {
							searchString = null;
						}
					}

					if (model != null) {
						model.updateFilter(minNumber, maxNumber, searchString);
						totalSizeLabel.setText("No. of Sets: " + model.getRowCount());
						minItemSetSizeLabel.setText("Total Max. Size: " + (model.getColumnCount() - 2));
					}
				}
			});

			// fill panel
			JLabel filler = new JLabel();
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.weighty = 1;
			layout.setConstraints(filler, c);
			controlPanel.add(filler);

			add(controlPanel, BorderLayout.WEST);
		}
	}
}
