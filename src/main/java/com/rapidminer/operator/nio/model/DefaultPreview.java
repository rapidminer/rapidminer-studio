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
package com.rapidminer.operator.nio.model;

import java.util.LinkedList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.nio.ImportWizardUtils;
import com.rapidminer.tools.ProgressListener;


/**
 * Data container and table model for previews. Reads a few lines from a {@link DataResultSet} to
 * display them. This should be only used in case where no more efficient implementation for the
 * TableModel is available.
 *
 * @author Simon Fischer
 *
 */
public class DefaultPreview extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private String[][] data;

	private String[] columnNames;

	private int previewSize = ImportWizardUtils.getPreviewLength();

	/**
	 * Maximum number of characters visible in a single cell. The number of characters is limited in
	 * order to prevent possible GUI freezes.
	 */
	private static final int CELL_MAX_LENGTH = 100;

	public DefaultPreview(DataResultSet resultSet, ProgressListener l) throws OperatorException, ParseException {
		read(resultSet, l);
	}

	public void read(DataResultSet resultSet, ProgressListener listener) throws OperatorException, ParseException {
		if (listener != null) {
			listener.setTotal(previewSize);
		}
		List<String[]> dataList = new LinkedList<>();
		resultSet.reset(listener);
		while (resultSet.hasNext() && dataList.size() < previewSize) {
			resultSet.next(listener);
			String[] row = new String[resultSet.getNumberOfColumns()];
			for (int i = 0; i < row.length; i++) {
				row[i] = shortenDisplayValue(resultSet.getString(i));
			}
			dataList.add(row);
			if (listener != null) {
				listener.setCompleted(dataList.size());
			}
		}
		// copy to array since will be accessed by index
		this.data = dataList.toArray(new String[dataList.size()][]);
		columnNames = resultSet.getColumnNames();
		if (listener != null) {
			listener.complete();
		}
	}

	@Override
	public int getRowCount() {
		return data.length;
	}

	@Override
	public String getColumnName(int column) {
		return columnNames[column];
	}

	@Override
	public int getColumnCount() {
		if (columnNames != null) {
			return columnNames.length;
		} else {
			if (data != null && data.length > 0) {
				return data[0].length;
			} else {
				return 1;
			}
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		final String[] row = data[rowIndex];
		if (row == null) {
			return null;
		} else if (columnIndex >= row.length) {
			return null;
		} else {
			return row[columnIndex];
		}
	}

	/**
	 * Shortens the provided value to a maximum length of {@link #CELL_MAX_LENGTH} so we do not run
	 * into any displaying issues.
	 *
	 * @param displayValue
	 *            the value to be shortened. Can be <code>null</code>.
	 * @return the full value of length below {@link #CELL_MAX_LENGTH} or a value with only
	 *         {@link #CELL_MAX_LENGTH} characters
	 */
	public static final String shortenDisplayValue(String displayValue) {
		if (displayValue != null && displayValue.length() > CELL_MAX_LENGTH) {
			// chop text and end with "..."
			return displayValue.substring(0, CELL_MAX_LENGTH - 1) + "\u2026";
		} else {
			return displayValue;
		}
	}

}
