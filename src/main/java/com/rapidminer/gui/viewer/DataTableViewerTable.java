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

import java.awt.Color;
import java.awt.event.MouseEvent;

import javax.swing.table.JTableHeader;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableListener;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.tools.CellColorProvider;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.SwingTools;


/**
 * Can be used to display (parts of) a DataTable by means of a JTable.
 *
 * @author Ingo Mierswa
 */
public class DataTableViewerTable extends ExtendedJTable implements DataTableListener {

	private static final long serialVersionUID = 3206734427933036268L;

	public static final int ALTERNATING = 0;

	public static final int SCALED = 1;

	public static final int ABS_SCALED = 2;

	private int rendererType = ALTERNATING;

	private double min = 0.0d;

	private double max = 0.0d;

	private DataTableViewerTableModel model;

	public DataTableViewerTable(boolean autoResize) {
		this(null, true, false, autoResize);
	}

	public DataTableViewerTable(DataTable dataTable, boolean sortable, boolean columnMovable, boolean autoResize) {
		super(sortable, columnMovable, autoResize);
		if (dataTable != null) {
			setDataTable(dataTable);
		}
		setCellColorProvider(new CellColorProvider() {

			@Override
			public Color getCellColor(int row, int col) {
				switch (rendererType) {
					case ALTERNATING:
						if (row % 2 == 0) {
							return Color.WHITE;
						} else {
							return SwingTools.LIGHTEST_BLUE;
						}
					case SCALED:
					case ABS_SCALED:
					default:
						Object valueObject = getValueAt(row, col);
						try {
							double value = Double.parseDouble(valueObject.toString());
							if (rendererType == ABS_SCALED) {
								value = Math.abs(value);
							}
							float scaled = (float) ((value - min) / (max - min));
							Color color = new Color(1.0f - scaled * 0.2f, 1.0f - scaled * 0.2f, 1.0f);
							return color;
						} catch (NumberFormatException e) {
							return Color.WHITE;
						}
				}
			}
		});
	}

	@Override
	public void dataTableUpdated(DataTable source) {
		if (this.model != null) {
			this.model.fireTableDataChanged();
		}
	}

	public void setDataTable(DataTable dataTable) {
		this.model = new DataTableViewerTableModel(dataTable);
		setModel(model);

		// if not alternating color scheme the cells are colored scaled with the value
		if (rendererType != ALTERNATING) {
			recalculateStatistics();
		}

		dataTable.addDataTableListener(this);
	}

	public void setRendererType(int rendererType) {
		this.rendererType = rendererType;
		if (rendererType != ALTERNATING) {
			recalculateStatistics();
		}
	}

	private void recalculateStatistics() {
		this.min = Double.POSITIVE_INFINITY;
		this.max = Double.NEGATIVE_INFINITY;
		for (int x = 0; x < getRowCount(); x++) {
			for (int y = 0; y < getColumnCount(); y++) {
				Object valueObject = getValueAt(x, y);
				double value = Double.NaN;
				try {
					value = Double.parseDouble(valueObject.toString());
					if (rendererType == ABS_SCALED) {
						value = Math.abs(value);
					}
				} catch (NumberFormatException e) {
				}
				if (!Double.isNaN(value)) {
					this.min = Math.min(this.min, value);
					this.max = Math.max(this.max, value);
				}
			}
		}
	}

	/** This method ensures that the correct tool tip for the current column is delivered. */
	@Override
	protected JTableHeader createDefaultTableHeader() {
		JTableHeader header = new JTableHeader(columnModel) {

			private static final long serialVersionUID = 1L;

			@Override
			public String getToolTipText(MouseEvent e) {
				java.awt.Point p = e.getPoint();
				int index = columnModel.getColumnIndexAtX(p.x);
				int realColumnIndex = convertColumnIndexToModel(index);
				if (realColumnIndex >= 0 && realColumnIndex < getModel().getColumnCount()) {
					return "The column " + getModel().getColumnName(realColumnIndex);
				} else {
					return "";
				}
			}
		};
		header.putClientProperty(RapidLookTools.PROPERTY_TABLE_HEADER_BACKGROUND, Colors.WHITE);
		return header;
	}
}
