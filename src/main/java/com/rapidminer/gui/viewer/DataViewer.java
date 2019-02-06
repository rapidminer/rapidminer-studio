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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Condition;
import com.rapidminer.example.set.ConditionCreationException;
import com.rapidminer.example.set.ConditionedExampleSet;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.processeditor.results.ResultTabActionVisualizer;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.operator.tools.ExpressionEvaluationException;
import com.rapidminer.report.Tableable;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * Can be used to display (parts of) the data by means of a JTable. Used to display
 * {@link ExampleSet} results. Uses a {@link DataViewerTable} to display the data.
 *
 * @author Ingo Mierswa
 */
public class DataViewer extends JPanel implements Tableable {

	private static final long serialVersionUID = -8114228636932871865L;

	private static final int DEFAULT_MAX_SIZE_FOR_FILTERING = 100_000;


	private DataViewerTable dataTable = new DataViewerTable();

	/**
	 * filter label display
	 */
	private JLabel filterLabel;

	private transient ExampleSet originalExampleSet;


	public DataViewer(ExampleSet exampleSet) {
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
		gbc.anchor = GridBagConstraints.WEST;

		final JComponent resultActionsComponent = ResultTabActionVisualizer.createResultActionsComponent(() -> originalExampleSet);
		if (resultActionsComponent != null) {
			gbc.insets = new Insets(0, 10, 0, 0);
			headerBar.add(new JLabel(I18N.getGUILabel("data_view.open_in.label")), gbc);

			gbc.gridx += 1;
			gbc.insets = new Insets(0, 0, 0, 0);
			headerBar.add(resultActionsComponent, gbc);
		}

		// add filler component in middle
		gbc.gridx += 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		headerBar.add(new JLabel(), gbc);

		// filter
		filterLabel = new JLabel(I18N.getGUILabel("data_view.filter.label"));

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
		filterSelector.setToolTipText(I18N.getGUILabel("data_view.filter.tip"));
		filterSelector.addItemListener(e -> updateFilter((String) filterSelector.getSelectedItem()));

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
		gbc.insets = new Insets(0, 10, 0, 0);
		headerBar.add(filterSelector, gbc);

		add(headerBar, BorderLayout.NORTH);

		JScrollPane tableScrollPane = new ExtendedJScrollPane(dataTable);
		tableScrollPane.setOpaque(true);
		tableScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
		tableScrollPane.setBackground(Colors.WHITE);
		tableScrollPane.getViewport().setBackground(Colors.WHITE);
		add(tableScrollPane, BorderLayout.CENTER);

		int noExamples = originalExampleSet.size();
		int noSpecial = originalExampleSet.getAttributes().specialSize();
		int noRegular = originalExampleSet.getAttributes().size();
		String infoTextBegin = I18N.getGUILabel("data_view.filter_result_begin.label");
		String infoTextExample = I18N.getGUILabel(
				noExamples == 1 ? "data_view.filter_result_example.label" : "data_view.filter_result_examples.label", noExamples);
		String infoTextSpecial = I18N.getGUILabel(
				noSpecial == 1 ? "data_view.filter_result_special.label" : "data_view.filter_result_specials.label", noSpecial);
		String infoTextRegular = I18N.getGUILabel(
				noRegular == 1 ? "data_view.filter_result_regular.label" : "data_view.filter_result_regulars.label", noRegular);
		JLabel generalInfo = new JLabel(infoTextBegin + infoTextExample + infoTextSpecial + infoTextRegular);
		final Border emptyBorder = BorderFactory.createEmptyBorder(6, 10, 6, 0);
		generalInfo.setBorder(emptyBorder);

		setExampleSet(exampleSet);

		if (!dataTable.canShowAllRows()) {
			// if this happens there are too many rows to be displayed by swing because its an int overflow. Adding information for the user
			final JLabel tooManyRowsLabel = new ResourceLabel("datatable.too_many_rows");
			tooManyRowsLabel.setBorder(emptyBorder);
			JPanel panel = new JPanel(new GridBagLayout());
			panel.setOpaque(false);
			GridBagConstraints panelGbc = new GridBagConstraints();
			panelGbc.gridx = 0;
			panelGbc.gridy = 0;
			panelGbc.weightx = 1;
			panelGbc.anchor = GridBagConstraints.WEST;
			panel.add(tooManyRowsLabel, panelGbc);
			panelGbc.gridy++;
			panel.add(generalInfo, panelGbc);
			add(panel, BorderLayout.SOUTH);
		} else {
			add(generalInfo, BorderLayout.SOUTH);
		}

		dataTable.unpack();
	}

	public void setExampleSet(ExampleSet exampleSet) {
		dataTable.setExampleSet(exampleSet);
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

	private void updateFilter(String conditionName) {
		ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_EXAMPLESET_VIEW_FILTER, ActionStatisticsCollector.VALUE_FILTER_SELECTED, conditionName);

		ExampleSet filteredExampleSet;
		try {
			Condition condition = ConditionedExampleSet.createCondition(conditionName, originalExampleSet, null);
			filteredExampleSet = new ConditionedExampleSet(originalExampleSet, condition);
		} catch (ConditionCreationException | ExpressionEvaluationException ex) {
			originalExampleSet.getLog().logError(
					"Cannot create condition '" + conditionName + "' for filtered data view: " + ex.getMessage()
							+ ". Using original data set view...");
			filteredExampleSet = originalExampleSet;
		}
		updateFilterCounter(filteredExampleSet);
		setExampleSet(filteredExampleSet);
	}

	private void updateFilterCounter(ExampleSet filteredExampleSet) {
		filterLabel.setText(I18N.getGUILabel("data_view.filter.label", filteredExampleSet.size(), originalExampleSet.size()));
	}

}
