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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.look.ui.TableHeaderUI;
import com.rapidminer.gui.processeditor.results.ResultDisplayTools;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.report.Reportable;
import com.rapidminer.report.Tableable;
import com.rapidminer.tools.Tools;


/**
 * This is the abstract renderer superclass for all renderers which should be a table based on a
 * given {@link TableModel}.
 *
 * @author Ingo Mierswa
 */
public abstract class AbstractTableModelTableRenderer extends NonGraphicalRenderer {

	public static final String RENDERER_NAME = "Table View";

	public static final String PARAMETER_MIN_ROW = "min_row";

	public static final String PARAMETER_MAX_ROW = "max_row";

	public static final String PARAMETER_MIN_COLUMN = "min_column";

	public static final String PARAMETER_MAX_COLUMN = "max_column";

	public static final String PARAMETER_SORT_COLUMN = "sort_column";
	public static final String PARAMETER_SORT_DECREASING = "sort_decreasing";

	/**
	 * This is the default base class for all renderers having already a TableModel. This class is
	 * used to be wrapped around them in order to have a unified interface for reporting.
	 */
	public static class DefaultTableable implements Tableable {

		private TableModel model;

		private int minRow = 0;

		private int maxRow = Integer.MAX_VALUE;

		private int minColumn = 0;

		private int maxColumn = Integer.MAX_VALUE;

		private boolean enableSorting = false;

		private int sortColumn = 0;

		private Integer[] sortIndices = null;

		public DefaultTableable(final TableModel model, Renderer renderer) {
			this.model = model;

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

			try {
				Object sortColumnObj = renderer.getParameter(PARAMETER_SORT_COLUMN);
				if (sortColumnObj != null) {
					sortColumn = Integer.valueOf(sortColumnObj.toString()) - 1;
					if (sortColumn < model.getColumnCount()) {
						Object decreasingOrderO = renderer.getParameter(PARAMETER_SORT_DECREASING);
						final boolean sortDecreasing = decreasingOrderO == null ? false : Boolean.valueOf(decreasingOrderO
								.toString());

						enableSorting = true;

						sortIndices = new Integer[getRowNumber()];
						for (int i = 0; i < sortIndices.length; i++) {
							sortIndices[i] = i;
						}

						Arrays.sort(sortIndices, new Comparator<Integer>() {

							@SuppressWarnings({ "unchecked", "rawtypes" })
							@Override
							public int compare(Integer o1, Integer o2) {
								Comparable c2 = (Comparable<?>) model.getValueAt(minRow + o1, sortColumn);
								Comparable c1 = (Comparable<?>) model.getValueAt(minRow + o2, sortColumn);
								if (c1 == null & c2 == null) {
									return 0;
								}
								if (c1 == null && c2 != null) {
									return -1;
								}
								if (c1 != null && c2 == null) {
									return +1;
								}
								if (sortDecreasing) {
									return c1.compareTo(c2);
								} else {
									return c2.compareTo(c1);
								}
							}
						});
					}
				} else {
					enableSorting = false;
				}
			} catch (UndefinedParameterError e) {
				maxColumn = Integer.MAX_VALUE;
			}
		}

		@Override
		public String getColumnName(int columnIndex) {
			return model.getColumnName(columnIndex + minColumn);
		}

		@Override
		public String getCell(int row, int column) {
			final Object objValue;
			if (enableSorting) {
				objValue = model.getValueAt(sortIndices[row], column + minColumn);
			} else {
				objValue = model.getValueAt(row + minRow, column + minColumn);
			}
			String value = objValue == null ? "" : objValue.toString();
			if (Number.class.isAssignableFrom(model.getColumnClass(column))) {
				return Tools.formatIntegerIfPossible(Double.valueOf(value));
			} else {
				return value;
			}
		}

		@Override
		public int getColumnNumber() {
			int maxC = maxColumn;
			if (maxColumn >= model.getColumnCount()) {
				maxC = model.getColumnCount() - 1;
			}
			return maxC - minColumn + 1;
		}

		@Override
		public int getRowNumber() {
			int maxR = maxRow;
			if (maxRow >= model.getRowCount()) {
				maxR = model.getRowCount() - 1;
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

	public abstract TableModel getTableModel(Object renderable, IOContainer ioContainer, boolean isReporting);

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
		TableModel tableModel = getTableModel(renderable, ioContainer, false);
		if (tableModel != null) {
			ExtendedJTable table = new ExtendedJTable(getTableModel(renderable, ioContainer, false), isSortable(),
					isColumnMovable(), isAutoresize());
			table.setRowHighlighting(true);
			table.setRowHeight(PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);
			table.getTableHeader().putClientProperty(RapidLookTools.PROPERTY_TABLE_HEADER_BACKGROUND, Colors.WHITE);
			((TableHeaderUI) table.getTableHeader().getUI()).installDefaults();

			JScrollPane sp = new ExtendedJScrollPane(table);
			sp.setBorder(BorderFactory.createEmptyBorder(42, 10, 10, 10));
			sp.setBackground(Colors.WHITE);
			sp.getViewport().setBackground(Colors.WHITE);

			JPanel panel = new JPanel(new BorderLayout());
			panel.add(sp, BorderLayout.CENTER);
			return panel;
		} else {
			return ResultDisplayTools.createErrorComponent("No visualization possible for table.");
		}
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer) {
		TableModel tableModel = getTableModel(renderable, ioContainer, true);
		if (tableModel != null) {
			return new DefaultTableable(tableModel, this);
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
		types.add(new ParameterTypeInt(PARAMETER_SORT_COLUMN, "Specifies the column to use for sorting.", 1,
				Integer.MAX_VALUE, max_column));
		types.add(new ParameterTypeBoolean(PARAMETER_SORT_DECREASING, "Use decrease sorting instead.", false));
		return types;
	}
}
