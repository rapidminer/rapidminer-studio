/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Condition;
import com.rapidminer.example.set.ConditionCreationException;
import com.rapidminer.example.set.ConditionedExampleSet;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.operator.tools.ExpressionEvaluationException;
import com.rapidminer.report.Tableable;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ParameterService;


/**
 * Can be used to display (parts of) the data by means of a JTable. Used to display
 * {@link ExampleSet} results. Uses a {@link DataViewerTable} to display the data.
 *
 * @author Ingo Mierswa
 */
public class DataViewer extends JPanel implements Tableable {

	private static final long serialVersionUID = -8114228636932871865L;

	private static final int DEFAULT_MAX_SIZE_FOR_FILTERING = 100000;

	private JLabel generalInfo = new JLabel();
	{
		generalInfo.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
	}

	private DataViewerTable dataTable = new DataViewerTable();

	/** Filter label display. */
	private JLabel filterLabel;

	private transient ExampleSet originalExampleSet;

	private JScrollPane tableScrollPane;

	public DataViewer(ExampleSet exampleSet, boolean providedFilter) {
		super(new BorderLayout());
		setOpaque(true);
		setBackground(Colors.WHITE);
		this.originalExampleSet = exampleSet;

		JPanel headerBar = new JPanel(new GridBagLayout());
		headerBar.setOpaque(true);
		headerBar.setBackground(Colors.WHITE);
		headerBar.setBorder(BorderFactory.createEmptyBorder(7, 5, 0, 10));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;

		StringBuffer infoText = new StringBuffer("ExampleSet (");
		int noExamples = originalExampleSet.size();
		infoText.append(noExamples);
		infoText.append(noExamples == 1 ? " example, " : " examples, ");
		int noSpecial = originalExampleSet.getAttributes().specialSize();
		infoText.append(noSpecial);
		infoText.append(noSpecial == 1 ? " special attribute, " : " special attributes, ");
		int noRegular = originalExampleSet.getAttributes().size();
		infoText.append(noRegular);
		infoText.append(noRegular == 1 ? " regular attribute)" : " regular attributes)");
		generalInfo.setText(infoText.toString());
		headerBar.add(generalInfo, gbc);

		// add filler component in middle
		gbc.gridx += 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		headerBar.add(new JLabel(), gbc);

		// filter
		if (providedFilter) {
			filterLabel = new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.label.data_view.filter.label"));

			gbc.gridx += 1;
			gbc.fill = GridBagConstraints.NONE;
			gbc.weightx = 0.0;
			headerBar.add(filterLabel, gbc);

			updateFilterCounter(originalExampleSet);
			List<String> applicableFilterNames = new LinkedList<>();
			for (String conditionName : ConditionedExampleSet.KNOWN_CONDITION_NAMES) {
				try {
					ConditionedExampleSet.createCondition(conditionName, exampleSet, null);
					applicableFilterNames.add(conditionName);
				} catch (ConditionCreationException ex) {
				} // Do nothing
			}
			String[] applicableConditions = new String[applicableFilterNames.size()];
			applicableFilterNames.toArray(applicableConditions);
			final JComboBox<String> filterSelector = new JComboBox<>(applicableConditions);
			filterSelector.putClientProperty(RapidLookTools.PROPERTY_INPUT_BACKGROUND_DARK, true);
			filterSelector.setMinimumSize(new Dimension(140, 30));
			filterSelector.setPreferredSize(new Dimension(140, 30));
			filterSelector
			.setToolTipText("These filters can be used to skip examples in the view fulfilling the filter condition.");
			filterSelector.addItemListener(new ItemListener() {

				@Override
				public void itemStateChanged(ItemEvent e) {
					updateFilter((String) filterSelector.getSelectedItem());
				}
			});

			int maxNumberBeforeFiltering = DEFAULT_MAX_SIZE_FOR_FILTERING;
			String maxString = ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_MAX_STATISTICS_ROWS);
			if (maxString != null) {
				try {
					maxNumberBeforeFiltering = Integer.parseInt(maxString);
				} catch (NumberFormatException e) {
					// do nothing
				}
			}
			if (exampleSet.size() > maxNumberBeforeFiltering) {
				filterSelector.setEnabled(false);
			}
			gbc.gridx += 1;
			gbc.fill = GridBagConstraints.VERTICAL;
			gbc.weighty = 1.0;
			gbc.insets = new Insets(0, 10, 0, 0);
			headerBar.add(filterSelector, gbc);
		}

		add(headerBar, BorderLayout.NORTH);

		tableScrollPane = new ExtendedJScrollPane(dataTable);
		tableScrollPane.setOpaque(true);
		tableScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
		tableScrollPane.setBackground(Colors.WHITE);
		tableScrollPane.getViewport().setBackground(Colors.WHITE);
		add(tableScrollPane, BorderLayout.CENTER);

		setExampleSet(exampleSet);

		dataTable.unpack();
	}

	public void setExampleSet(ExampleSet exampleSet) {
		dataTable.setExampleSet(exampleSet);
	}

	private void updateFilter(String conditionName) {
		ExampleSet filteredExampleSet = originalExampleSet;
		try {
			Condition condition = ConditionedExampleSet.createCondition(conditionName, originalExampleSet, null);
			filteredExampleSet = new ConditionedExampleSet(originalExampleSet, condition);
		} catch (ConditionCreationException ex) {
			originalExampleSet.getLog().logError(
					"Cannot create condition '" + conditionName + "' for filtered data view: " + ex.getMessage()
					+ ". Using original data set view...");
			filteredExampleSet = originalExampleSet;
		} catch (ExpressionEvaluationException ex) {
			originalExampleSet.getLog().logError(
					"Cannot create condition '" + conditionName + "' for filtered data view: " + ex.getMessage()
					+ ". Using original data set view...");
			filteredExampleSet = originalExampleSet;
		}
		updateFilterCounter(filteredExampleSet);
		setExampleSet(filteredExampleSet);
	}

	private void updateFilterCounter(ExampleSet filteredExampleSet) {
		filterLabel.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.data_view.filter.label",
				filteredExampleSet.size(), originalExampleSet.size()));
	}

	@Override
	public void prepareReporting() {
		dataTable.prepareReporting();
	}

	@Override
	public void finishReporting() {
		dataTable.finishReporting();
	}

	@Override
	public String getColumnName(int columnIndex) {
		return dataTable.getColumnName(columnIndex);
	}

	@Override
	public String getCell(int row, int column) {
		return dataTable.getCell(row, column);
	}

	@Override
	public int getColumnNumber() {
		return dataTable.getColumnNumber();
	}

	@Override
	public int getRowNumber() {
		return dataTable.getRowNumber();
	}

	@Override
	public boolean isFirstLineHeader() {
		return false;
	}

	@Override
	public boolean isFirstColumnHeader() {
		return false;
	}

}
