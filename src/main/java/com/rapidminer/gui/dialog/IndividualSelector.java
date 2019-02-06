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
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.viewer.MetaDataViewer;
import com.rapidminer.operator.features.FeatureOperator;
import com.rapidminer.operator.features.Individual;
import com.rapidminer.operator.features.Population;


/**
 * This dialog can be used to select an individual from a population.
 *
 * @author Ingo Mierswa
 */
@SuppressWarnings("deprecation")
public class IndividualSelector extends JDialog implements ListSelectionListener {

	private static final long serialVersionUID = -6512675217777454316L;

	private transient Population population;

	private ExtendedJTable individualTable;

	private MetaDataViewer metaDataViewer = null;

	private boolean selected = false;

	private ExampleSet exampleSet = null;

	public IndividualSelector(ExampleSet exampleSet, Population population) {
		this(exampleSet, population, true);
	}

	public IndividualSelector(ExampleSet exampleSet, Population population, boolean modal) {
		this(RapidMinerGUI.getMainFrame(), exampleSet, population, -1, -1, modal);
	}

	public IndividualSelector(Frame owner, ExampleSet exampleSet, Population population, int width, int height, boolean modal) {
		super(owner, "Result Individual Selection", modal);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.population = population;

		this.exampleSet = exampleSet;

		individualTable = new ExtendedJTable(new IndividualSelectorTableModel(Tools.getRegularAttributeNames(exampleSet),
				population), true);
		individualTable.setBorder(BorderFactory.createLoweredBevelBorder());
		individualTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		individualTable.setCellSelectionEnabled(false);
		individualTable.setColumnSelectionAllowed(false);
		individualTable.setRowSelectionAllowed(true);
		individualTable.getSelectionModel().addListSelectionListener(this);
		JScrollPane tablePane = new ExtendedJScrollPane(individualTable);
		tablePane.setBorder(BorderFactory.createTitledBorder("All Individuals"));

		// meta data viewer
		ExampleSet result = FeatureOperator.createCleanClone(exampleSet, population.get(0).getWeights());
		this.metaDataViewer = new MetaDataViewer(result, false);
		JScrollPane metaDataPane = new ExtendedJScrollPane(metaDataViewer);
		metaDataPane.setBorder(BorderFactory.createTitledBorder("Selected Individual"));

		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainSplitPane.add(tablePane);
		mainSplitPane.add(metaDataPane);

		getContentPane().add(mainSplitPane, BorderLayout.CENTER);

		// button panel
		JPanel buttonPanel = new JPanel();
		GridBagLayout buttonLayout = new GridBagLayout();
		buttonPanel.setLayout(buttonLayout);
		GridBagConstraints buttonC = new GridBagConstraints();
		buttonC.fill = GridBagConstraints.BOTH;
		buttonC.weightx = 0;
		buttonC.insets = new Insets(4, 4, 4, 4);

		JButton saveDataButton = new JButton("Save Data...");
		saveDataButton.setToolTipText("Save the upper data table into a file in CSV format.");
		saveDataButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				saveData();
			}
		});
		buttonLayout.setConstraints(saveDataButton, buttonC);
		buttonPanel.add(saveDataButton);

		JPanel fillPanel = new JPanel();
		buttonC.weightx = 1;
		buttonLayout.setConstraints(fillPanel, buttonC);
		buttonPanel.add(fillPanel);

		buttonC.weightx = 0;
		JButton selectButton = new JButton("Select");
		selectButton
				.setToolTipText("Use the currently selected individual as result instead of the best one according to the main criterion.");
		selectButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				select();
			}
		});
		buttonLayout.setConstraints(selectButton, buttonC);
		buttonPanel.add(selectButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton
				.setToolTipText("Do not use the currently selected individual but the best one according to the main criterion.");
		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
		buttonLayout.setConstraints(cancelButton, buttonC);
		buttonPanel.add(cancelButton);

		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		individualTable.addRowSelectionInterval(0, 0);

		// size and location
		if ((width < 0) || (height < 0)) {
			setSize(800, 600);
			mainSplitPane.setDividerLocation(400);
		} else {
			setSize(width, height);
			mainSplitPane.setDividerLocation(height / 2);
		}
		setLocationRelativeTo(owner);
	}

	private void saveData() {
		File file = SwingTools.chooseFile(this, null, false, "csv", "comma separated values");
		if (file != null) {
			try (FileWriter fw = new FileWriter(file); PrintWriter out = new PrintWriter(fw)) {
				TableModel model = this.individualTable.getModel();
				for (int c = 0; c < model.getColumnCount(); c++) {
					if (c != 0) {
						out.print(";");
					}
					out.print(model.getColumnName(c));
				}
				out.println();

				for (int r = 0; r < model.getRowCount(); r++) {
					for (int c = 0; c < model.getColumnCount(); c++) {
						if (c != 0) {
							out.print(";");
						}
						out.print(model.getValueAt(r, c));
					}
					out.println();
				}
			} catch (IOException e) {
				SwingTools.showSimpleErrorMessage("cannot_write_data_into_file", e);
			}
		}
	}

	private void select() {
		selected = true;
		dispose();
	}

	private void cancel() {
		dispose();
	}

	public Individual getSelectedIndividual() {
		if (!selected) {
			return null;
		} else {
			int index = individualTable.getModelIndex(individualTable.getSelectedRow());
			if ((index >= 0) && (index < population.getNumberOfIndividuals())) {
				return population.get(index);
			} else {
				return null;
			}
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			int index = individualTable.getModelIndex(individualTable.getSelectedRow());
			if ((index >= 0) && (index < population.getNumberOfIndividuals())) {
				Individual selected = population.get(index);
				ExampleSet result = FeatureOperator.createCleanClone(exampleSet, selected.getWeights());
				metaDataViewer.setExampleSet(result);
			} else {
				metaDataViewer.setExampleSet(null);
			}
		}
	}
}
