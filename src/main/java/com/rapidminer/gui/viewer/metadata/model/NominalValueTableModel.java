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
package com.rapidminer.gui.viewer.metadata.model;

import com.rapidminer.gui.viewer.metadata.dialogs.NominalValueDialog;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.container.ValueAndCount;

import java.util.List;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;


/**
 * {@link TableModel} backing the {@link NominalValueDialog}.
 * 
 * @author Marco Boeck
 * 
 */
public class NominalValueTableModel extends DefaultTableModel {

	private static final long serialVersionUID = -5156301816523523916L;

	/** index of the index column */
	public static final int INDEX_INDEX = 0;

	/** index of the value column */
	public static final int INDEX_VALUE = 1;

	/** index of the absolute count column */
	public static final int INDEX_ABSOLUTE_COUNT = 2;

	/** index of the relative count column */
	public static final int INDEX_RELATIVE_COUNT = 3;

	/** the title of the index column */
	private static final String NAME_COLUMN_INDEX = I18N.getMessage(I18N.getGUIBundle(),
			"gui.dialog.attribute_statistics.nominal_values_dialog.column_index.title");

	/** the title of the value column */
	private static final String NAME_COLUMN_VALUE = I18N.getMessage(I18N.getGUIBundle(),
			"gui.dialog.attribute_statistics.nominal_values_dialog.column_value.title");

	/** the title of the relative count column */
	private static final String NAME_COLUMN_REL_COUNT = I18N.getMessage(I18N.getGUIBundle(),
			"gui.dialog.attribute_statistics.nominal_values_dialog.column_rel_count.title");

	/** the title of the absolute count column */
	private static final String NAME_COLUMN_ABS_COUNT = I18N.getMessage(I18N.getGUIBundle(),
			"gui.dialog.attribute_statistics.nominal_values_dialog.column_abs_count.title");

	/** list containing the nominal values and their count */
	private List<ValueAndCount> listOfValues;

	/** all value counts added */
	private int totalCount;

	/**
	 * Creates a new {@link NominalValueTableModel} instance with the given list of nominal values
	 * and their absolute count.
	 * 
	 * @param listOfValues
	 */
	public NominalValueTableModel(List<ValueAndCount> listOfValues) {
		super();
		this.listOfValues = listOfValues;

		totalCount = 0;
		for (ValueAndCount value : listOfValues) {
			totalCount += value.getCount();
		}
	}

	@Override
	public int getRowCount() {
		return listOfValues != null ? listOfValues.size() : 0;
	}

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public String getColumnName(int columnIndex) {
		switch (columnIndex) {
			case INDEX_INDEX:
				return NAME_COLUMN_INDEX;
			case INDEX_VALUE:
				return NAME_COLUMN_VALUE;
			case INDEX_ABSOLUTE_COUNT:
				return NAME_COLUMN_ABS_COUNT;
			case INDEX_RELATIVE_COUNT:
				return NAME_COLUMN_REL_COUNT;
			default:
				return null;
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
			case INDEX_INDEX:
				return Integer.class;
			case INDEX_VALUE:
				return String.class;
			case INDEX_ABSOLUTE_COUNT:
				return Integer.class;
			case INDEX_RELATIVE_COUNT:
				return Double.class;
			default:
				return Object.class;
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch (columnIndex) {
			case INDEX_INDEX:
				return rowIndex + 1;	// human index starts at 1
			case INDEX_VALUE:
				return listOfValues.get(rowIndex).getValue();
			case INDEX_ABSOLUTE_COUNT:
				return listOfValues.get(rowIndex).getCount();
			case INDEX_RELATIVE_COUNT:
				return ((double) listOfValues.get(rowIndex).getCount() / totalCount);
			default:
				return null;
		}
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		// we don't set values
	}

}
