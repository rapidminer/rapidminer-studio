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
package com.rapidminer.gui.renderer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.processeditor.results.ResultDisplayTools;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.CellColorProvider;
import com.rapidminer.gui.tools.CellColorProviderAlternating;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.viewer.DataTableViewerTable;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.report.Reportable;
import com.rapidminer.report.Tableable;


/**
 * This is the abstract renderer superclass for all renderers which should be a table based on a
 * given {@link DataTable}.
 *
 * @author Ingo Mierswa
 */
public abstract class AbstractDataTableTableRenderer extends NonGraphicalRenderer {

	public static final String RENDERER_NAME = "Table View";

	public static final String PARAMETER_MIN_ROW = "min_row";

	public static final String PARAMETER_MAX_ROW = "max_row";

	public static final String PARAMETER_MIN_COLUMN = "min_column";

	public static final String PARAMETER_MAX_COLUMN = "max_column";

	public static class DefaultTableable implements Tableable {

		private DataTable dataTable;

		private int minRow = 0;

		private int maxRow = Integer.MAX_VALUE;

		private int minColumn = 0;

		private int maxColumn = Integer.MAX_VALUE;

		public DefaultTableable(DataTable dataTable, Renderer renderer) {
			this.dataTable = dataTable;

			try {
				Object minRowO = renderer.getParameter(PARAMETER_MIN_ROW);
				if (minRowO != null) {
					minRow = Integer.valueOf(minRowO.toString()) - 1;
				} else {
					minRow = 0;
				}
			} catch (UndefinedParameterError e) {
				minRow = 0;
			}

			try {
				Object maxRowO = renderer.getParameter(PARAMETER_MAX_ROW);
				if (maxRowO != null) {
					maxRow = Integer.valueOf(maxRowO.toString()) - 1;
				} else {
					maxRow = Integer.MAX_VALUE;
				}
			} catch (UndefinedParameterError e) {
				maxRow = Integer.MAX_VALUE;
			}

			try {
				Object minColO = renderer.getParameter(PARAMETER_MIN_COLUMN);
				if (minColO != null) {
					minColumn = Integer.valueOf(minColO.toString()) - 1;
				} else {
					minColumn = 0;
				}
			} catch (UndefinedParameterError e) {
				minColumn = 0;
			}

			try {
				Object maxColO = renderer.getParameter(PARAMETER_MAX_COLUMN);
				if (maxColO != null) {
					maxColumn = Integer.valueOf(maxColO.toString()) - 1;
				} else {
					maxColumn = Integer.MAX_VALUE;
				}
			} catch (UndefinedParameterError e) {
				maxColumn = Integer.MAX_VALUE;
			}
		}

		@Override
		public String getColumnName(int columnIndex) {
			return dataTable.getColumnName(columnIndex + minColumn);
		}

		@Override
		public String getCell(int row, int column) {
			return dataTable.getCell(row + minRow, column + minColumn);
		}

		@Override
		public int getColumnNumber() {
			int maxC = maxColumn;
			if (maxColumn >= dataTable.getNumberOfColumns()) {
				maxC = dataTable.getNumberOfColumns() - 1;
			}
			return maxC - minColumn + 1;
		}

		@Override
		public int getRowNumber() {
			int maxR = maxRow;
			if (maxRow >= dataTable.getNumberOfRows()) {
				maxR = dataTable.getNumberOfRows() - 1;
			}
			return maxR - minRow + 1;
		}

		@Override
		public void prepareReporting() {}

		@Override
		public void finishReporting() {}

		@Override
		public boolean isFirstLineHeader() {
			return false;
		}

		@Override
		public boolean isFirstColumnHeader() {
			return false;
		}

	}

	@Override
	public String getName() {
		return RENDERER_NAME;
	}

	public abstract DataTable getDataTable(Object renderable, IOContainer ioContainer, boolean isRendering);

	public boolean isSortable() {
		return true;
	}

	public boolean isColumnMovable() {
		return true;
	}

	public boolean isAutoresize() {
		return true;
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		DataTable dataTable = getDataTable(renderable, ioContainer, true);
		if (dataTable != null) {
			DataTableViewerTable resultTable = new DataTableViewerTable(dataTable, isSortable(), isColumnMovable(),
					isAutoresize());
			resultTable.setRowHeight(PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);
			resultTable.setRowHighlighting(true);
			resultTable.setCellColorProvider(getCellColorProvider(resultTable, renderable));

			ExtendedJScrollPane scrollPane = new ExtendedJScrollPane(resultTable);
			scrollPane.setBorder(BorderFactory.createEmptyBorder(42, 10, 15, 10));
			scrollPane.setBackground(Colors.WHITE);
			scrollPane.getViewport().setBackground(Colors.WHITE);

			JPanel component = new JPanel(new BorderLayout());
			component.add(scrollPane, BorderLayout.CENTER);
			return component;
		} else {
			return ResultDisplayTools.createErrorComponent("No visualization possible for table.");
		}
	}

	/**
	 * Subclasses might override this method in order to change the color provider like for
	 * correlation matrices.
	 *
	 * @param renderable
	 */
	protected CellColorProvider getCellColorProvider(ExtendedJTable table, Object renderable) {
		return new CellColorProviderAlternating();
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer) {
		DataTable dataTable = getDataTable(renderable, ioContainer, false);
		if (dataTable != null) {
			return new DefaultTableable(dataTable, this);
		}
		return null;
	}

	@Override
	public List<ParameterType> getParameterTypes(InputPort inputPort) {
		List<ParameterType> types = super.getParameterTypes(inputPort);
		int max_row = Integer.MAX_VALUE;
		int max_column = Integer.MAX_VALUE;
		if (inputPort != null) {
			MetaData metaData = inputPort.getMetaData();
			if (metaData != null) {
				if (metaData instanceof ExampleSetMetaData) {
					ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
					if (emd.getNumberOfExamples().isKnown()) {
						max_row = emd.getNumberOfExamples().getNumber();
					}
					max_column = emd.getAllAttributes().size();
				}
			}
		}
		types.add(new ParameterTypeInt(PARAMETER_MIN_ROW, "Indicates the first row number which should be rendered.", 1,
				Integer.MAX_VALUE, 1));
		types.add(new ParameterTypeInt(PARAMETER_MAX_ROW, "Indicates the last row number which should be rendered.", 1,
				Integer.MAX_VALUE, max_row));
		types.add(new ParameterTypeInt(PARAMETER_MIN_COLUMN, "Indicates the first column number which should be rendered.",
				1, Integer.MAX_VALUE, 1));
		types.add(new ParameterTypeInt(PARAMETER_MAX_COLUMN, "Indicates the last column number which should be rendered.",
				1, Integer.MAX_VALUE, max_column));
		return types;
	}
}
