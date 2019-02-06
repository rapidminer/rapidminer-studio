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
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableExampleSetAdapter;
import com.rapidminer.datatable.DataTableListener;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.gui.AbstractConfigurationPanel.DatasetTransformationType;
import com.rapidminer.gui.new_plotter.gui.ChartConfigurationPanel;
import com.rapidminer.gui.new_plotter.integration.PlotConfigurationHistory;
import com.rapidminer.gui.plotter.Plotter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.PlotterPanel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.components.ButtonBarCardPanel;
import com.rapidminer.gui.tools.components.ResourceCard;
import com.rapidminer.report.Tableable;


/**
 * Can be used to display (parts of) the data by means of a JTable.
 *
 * @author Ingo Mierswa
 */
public class DataTableViewer extends JPanel implements Tableable, DataTableListener {

	private static final long serialVersionUID = 6878549119308753961L;

	public static final String TABLE_MODE = "TABLE";

	public static final String PLOT_MODE = "PLOT";

	public static final String ADVANCED_MODE = "ADVANCED";

	private JLabel generalInfo = new JLabel();

	private DataTableViewerTable dataTableViewerTable;

	private PlotterPanel plotterPanel;

	private JPanel tablePanel;

	private ChartConfigurationPanel advancedPanel;

	private PlotterConfigurationModel plotterSettings;

	public DataTableViewer(DataTable dataTable) {
		this(dataTable, PlotterConfigurationModel.DATA_SET_PLOTTER_SELECTION, true, TABLE_MODE, false);
	}

	public DataTableViewer(DataTable dataTable, boolean showPlotter) {
		this(dataTable, PlotterConfigurationModel.DATA_SET_PLOTTER_SELECTION, showPlotter, TABLE_MODE, false);
	}

	public DataTableViewer(DataTable dataTable, boolean showPlotter, String startMode) {
		this(dataTable, PlotterConfigurationModel.DATA_SET_PLOTTER_SELECTION, showPlotter, startMode, false);
	}

	public DataTableViewer(DataTable dataTable, LinkedHashMap<String, Class<? extends Plotter>> availablePlotters) {
		this(dataTable, availablePlotters, true, TABLE_MODE, false);
	}

	public DataTableViewer(DataTable dataTable, LinkedHashMap<String, Class<? extends Plotter>> availablePlotters,
			boolean showPlotter, String tableMode, boolean autoResize) {
		super(new BorderLayout());

		// create empty buttonCard
		ButtonBarCardPanel bCard = new ButtonBarCardPanel();

		// Build table view
		this.dataTableViewerTable = new DataTableViewerTable(autoResize);
		this.tablePanel = new JPanel(new BorderLayout());
		JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
		infoPanel.add(generalInfo);
		infoPanel.setOpaque(true);
		infoPanel.setBackground(Colors.WHITE);
		tablePanel.add(infoPanel, BorderLayout.NORTH);
		JScrollPane tableScrollPane = new ExtendedJScrollPane(dataTableViewerTable);
		tableScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 10, 10, 5));
		tableScrollPane.setBackground(Colors.WHITE);
		tableScrollPane.getViewport().setBackground(Colors.WHITE);
		tablePanel.add(tableScrollPane, BorderLayout.CENTER);

		// add data table to the result view
		bCard.addCard(new ResourceCard("data_view", "result_view.data_view"), tablePanel);

		// Add plotters if desired
		if (showPlotter) {
			this.plotterSettings = new PlotterConfigurationModel(availablePlotters, dataTable);
			this.plotterPanel = new PlotterPanel(plotterSettings);
			DataTable plotData = plotterSettings.getDataTable();

			// preface to create ChartConfigationPanel:
			ExampleSet exampleSet = DataTableExampleSetAdapter.createExampleSetFromDataTable(plotData);
			Map<DatasetTransformationType, PlotConfiguration> plotConfigurationMap = PlotConfigurationHistory
					.getPlotConfigurationMap(exampleSet, plotData);
			PlotInstance plotInstance = new PlotInstance(plotConfigurationMap.get(DatasetTransformationType.ORIGINAL),
					plotData);
			this.advancedPanel = new ChartConfigurationPanel(true, plotInstance, plotData,
					plotConfigurationMap.get(DatasetTransformationType.DE_PIVOTED));

			// add Plotter to the result view
			bCard.addCard(new ResourceCard("plot_view", "result_view.plot_view"), plotterPanel);

			// add advanced Charts to the result view
			bCard.addCard(new ResourceCard("advanced_charts", "result_view.advanced_charts"), advancedPanel);
		} // end if (showPlotter)

		// check select desired view
		if (PLOT_MODE.equals(tableMode) && showPlotter) {
			bCard.selectCard("plot_view");
		} else if (ADVANCED_MODE.equals(tableMode) && showPlotter) {
			bCard.selectCard("advanced_charts");
		}

		add(bCard, BorderLayout.CENTER);
		setDataTable(dataTable);
	}

	public DataTable getDataTable() {
		return plotterSettings.getDataTable();
	}

	public PlotterPanel getPlotterPanel() {
		return plotterPanel;
	}

	public DataTableViewerTable getTable() {
		return dataTableViewerTable;
	}

	public void setDataTable(DataTable dataTable) {
		dataTableViewerTable.setDataTable(dataTable);
		if (plotterSettings != null) {
			plotterSettings.setDataTable(dataTable);
		}

		// add listener for correct row count
		dataTable.addDataTableListener(this);
		dataTableUpdated(dataTable);
	}

	@Override
	public void dataTableUpdated(DataTable dataTable) {
		generalInfo.setText(dataTable.getName() + " (" + dataTable.getNumberOfRows() + " rows, "
				+ dataTable.getNumberOfColumns() + " columns)");
	}

	@Override
	public void prepareReporting() {
		dataTableViewerTable.prepareReporting();
	}

	@Override
	public void finishReporting() {
		dataTableViewerTable.finishReporting();
	}

	@Override
	public String getColumnName(int columnIndex) {
		return dataTableViewerTable.getColumnName(columnIndex);
	}

	@Override
	public String getCell(int row, int column) {
		return dataTableViewerTable.getCell(row, column);
	}

	@Override
	public int getColumnNumber() {
		return dataTableViewerTable.getColumnNumber();
	}

	@Override
	public int getRowNumber() {
		return dataTableViewerTable.getRowNumber();
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
