/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import static com.rapidminer.operator.ports.DeliveringPortManager.LAST_DELIVERING_PORT;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.table.TableViewCreator;
import com.rapidminer.belt.transform.RowFilterer;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.processeditor.results.DisplayContext;
import com.rapidminer.gui.processeditor.results.ResultTabActionVisualizer;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.report.Tableable;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * Can be used to display (parts of) the data by means of a JTable. Used to display {@link IOTable} results, same as
 * {@link DataViewer} does for {@link ExampleSet}. Uses a {@link BeltTableDataViewerTable} to display the data.
 *
 * @author Ingo Mierswa, Gisa Meier
 * @since 9.7.0
 */
public class BeltTableDataViewer extends JPanel implements Tableable {

	private static final long serialVersionUID = -8115228636932871865L;

	private static final int DEFAULT_MAX_SIZE_FOR_FILTERING = 100_000;

	/**
	 * the filter options, the ones with labels and predictions are only there if there are labels/predictions
	 */
	private static final String FILTER_ALL = "all";
	private static final String FILTER_CORRECT_PREDICTIONS = "correct_predictions";
	private static final String FILTER_WRONG_PREDICTIONS = "wrong_predictions";
	private static final String FILTER_NO_MISSING_LABELS = "no_missing_labels";
	private static final String FILTER_MISSING_LABELS = "missing_labels";
	private static final String FILTER_NO_MISSING_ATTRIBUTES = "no_missing_attributes";
	private static final String FILTER_MISSING_ATTRIBUTES = "missing_attributes";


	private BeltTableDataViewerTable dataTable;

	/** filter label display */
	private JLabel filterLabel;

	private transient IOTable originalTable;

	/**
	 * Creates a data table visualization for a {@link IOTable}.
	 *
	 * @param table
	 * 		the belt table to view
	 */
	public BeltTableDataViewer(IOTable table) {
		super(new BorderLayout());
		setOpaque(true);
		setBackground(Colors.WHITE);
		this.originalTable = table;

		JPanel headerBar = new JPanel(new GridBagLayout());
		headerBar.setOpaque(true);
		headerBar.setBackground(Colors.WHITE);
		headerBar.setBorder(BorderFactory.createEmptyBorder(7, 5, 0, 10));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;

		final JComponent resultActionsComponent =
				ResultTabActionVisualizer.createResultActionsComponent(this::convertToExampleSet);
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

		updateFilterCounter(null);
		List<String> applicableFilterNames = new ArrayList<>();
		applicableFilterNames.add(FILTER_ALL);
		boolean hasLabels = !originalTable.getTable().select().withMetaData(ColumnRole.LABEL).labels().isEmpty();
		if (!originalTable.getTable().select().withMetaData(ColumnRole.PREDICTION).labels().isEmpty() && hasLabels) {
			applicableFilterNames.add(FILTER_CORRECT_PREDICTIONS);
			applicableFilterNames.add(FILTER_WRONG_PREDICTIONS);
		}
		applicableFilterNames.add(FILTER_NO_MISSING_ATTRIBUTES);
		applicableFilterNames.add(FILTER_MISSING_ATTRIBUTES);
		if (hasLabels) {
			applicableFilterNames.add(FILTER_NO_MISSING_LABELS);
			applicableFilterNames.add(FILTER_MISSING_LABELS);
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
		if (table.getTable().height() > maxNumberBeforeFiltering) {
			filterSelector.setEnabled(false);
		}
		gbc.gridx += 1;
		gbc.insets = new Insets(0, 10, 0, 0);
		headerBar.add(filterSelector, gbc);

		add(headerBar, BorderLayout.NORTH);

		dataTable = new BeltTableDataViewerTable(table.getTable());

		JScrollPane tableScrollPane = new ExtendedJScrollPane(dataTable);
		tableScrollPane.setOpaque(true);
		tableScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
		tableScrollPane.setBackground(Colors.WHITE);
		tableScrollPane.getViewport().setBackground(Colors.WHITE);
		add(tableScrollPane, BorderLayout.CENTER);

		int noExamples = originalTable.getTable().height();
		int noSpecial = originalTable.getTable().select().withMetaData(ColumnRole.class).labels().size();
		int noRegular = originalTable.getTable().width() - noSpecial;
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

	/**
	 * Convert the table to an example set for the use in Turbo Prep and Auto Model. They require the port user
	 * data, so copy that, too.
	 */
	private ExampleSet convertToExampleSet() {
		ExampleSet exampleSet = TableViewCreator.INSTANCE.convertOnWriteView(originalTable, true);
		exampleSet.setUserData(LAST_DELIVERING_PORT, originalTable.getUserData(LAST_DELIVERING_PORT));
		return exampleSet;
	}

	/**
	 * Updates the filter defined by the dropdown.
	 */
	private void updateFilter(String conditionName) {
		ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_EXAMPLESET_VIEW_FILTER,
				ActionStatisticsCollector.VALUE_FILTER_SELECTED, conditionName);

		boolean invert = false;
		switch (conditionName) {
			case FILTER_CORRECT_PREDICTIONS:
				invert = true;
			case FILTER_WRONG_PREDICTIONS:
				//for now we only consider the first prediction and first label
				List<Column> labels = originalTable.getTable().select().withMetaData(ColumnRole.LABEL).columns();
				List<Column> predictions =
						originalTable.getTable().select().withMetaData(ColumnRole.PREDICTION).columns();
				//already checked that both are not empty
				filterEquals(labels.get(0), predictions.get(0), invert);
				return;
			case FILTER_NO_MISSING_ATTRIBUTES:
				invert = true;
			case FILTER_MISSING_ATTRIBUTES:
				List<Column> regularColumns =
						originalTable.getTable().select().withoutMetaData(ColumnRole.class).columns();
				filterMissings(regularColumns, invert);
				return;
			case FILTER_NO_MISSING_LABELS:
				invert = true;
			case FILTER_MISSING_LABELS:
				List<Column> labelColumns = originalTable.getTable().select().withMetaData(ColumnRole.LABEL).columns();
				filterMissings(labelColumns, invert);
				return;
			case FILTER_ALL:
			default:
				updateRowSelection(null);
		}
	}

	/**
	 * Filters the missing values.
	 *
	 * @param filterColumns
	 * 		the columns to check
	 * @param noMissing
	 * 		if it should filter out the non-missings instead of the missings
	 */
	private void filterMissings(List<Column> filterColumns, boolean noMissing) {
		if (filterColumns.isEmpty()) {
			updateRowSelection(noMissing ? null : new int[0]);
			return;
		}
		boolean[] numericReadable = new boolean[filterColumns.size()];
		int index = 0;
		for (Column col : filterColumns) {
			if (col.type().hasCapability(Column.Capability.NUMERIC_READABLE)) {
				numericReadable[index] = true;
			}
			index++;
		}
		final boolean includeMissing = !noMissing;
		int[] ints = new RowFilterer(filterColumns).filterMixed(row -> {
			for (int i = 0; i < numericReadable.length; i++) {
				if (numericReadable[i]) {
					if (Double.isNaN(row.getNumeric(i))) {
						return includeMissing;
					}
				} else if (row.getObject(i) == null) {
					return includeMissing;

				}
			}
			return !includeMissing;
		}, new DisplayContext());
		updateRowSelection(ints);
	}

	/**
	 * Filters in the rows that are equal or not.
	 *
	 * @param notEqual
	 * 		if {@code true} the not equal rows are filtered in
	 */
	private void filterEquals(Column column1, Column column2, boolean notEqual) {
		int[] filter;
		if (column1.type().category() == Column.Category.NUMERIC && column2.type().category() == Column.Category.NUMERIC) {
			filter = new RowFilterer(Arrays.asList(column1, column2))
					.filterNumeric(row -> (Tools.isEqual(row.get(0), row.get(1))) == notEqual, new DisplayContext());
		} else if (column1.type().hasCapability(Column.Capability.OBJECT_READABLE)
				&& column2.type().hasCapability(Column.Capability.OBJECT_READABLE)) {
			filter = new RowFilterer(Arrays.asList(column1, column2))
					.filterObjects(Object.class, row -> Objects.equals(row.get(0), row.get(1)) == notEqual,
							new DisplayContext());
		} else {
			//one is numeric, one is not -> not equal
			filter = notEqual ? new int[0] : null;
		}
		updateRowSelection(filter);
	}

	/**
	 * Sets the row selection in the view and updates the filter counter.
	 *
	 * @param selection
	 * 		the row selection, can be {@code null} for selecting everything
	 */
	private void updateRowSelection(int[] selection){
		dataTable.setRows(selection);
		updateFilterCounter(selection);
	}

	/**
	 * Sets the text for the filter counter next to the dropdown.
	 */
	private void updateFilterCounter(int[] selection) {
		filterLabel.setText(I18N.getGUILabel("data_view.filter.label", selection == null ?
				originalTable.getTable().height() : selection.length, originalTable.getTable().height()));
	}

}
