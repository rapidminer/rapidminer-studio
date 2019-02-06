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
package com.rapidminer.gui.new_plotter.configuration;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.tools.Ontology;


/**
 * This class describes a column of a {@link DataTable} characterized by a name, a column index and
 * a {@link ValueType}.
 *
 * @author Nils Woehler, Marius Helf
 * @deprecated since 9.2.0
 */
@Deprecated
public class DataTableColumn implements Cloneable {

	public enum ValueType {
		INVALID, UNKNOWN, NOMINAL, NUMERICAL, DATE_TIME;

		public static int convertToRapidMinerOntology(ValueType valueType) {
			switch (valueType) {
				case DATE_TIME:
					return Ontology.DATE_TIME;
				case INVALID:
					return Ontology.ATTRIBUTE_VALUE;
				case NUMERICAL:
					return Ontology.NUMERICAL;
				case UNKNOWN:
				case NOMINAL:
				default:
					return Ontology.NOMINAL;
			}
		}

		public static ValueType convertFromRapidMinerOntology(int rmValueType) {
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(rmValueType, Ontology.NUMERICAL)) {
				return NUMERICAL;
			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(rmValueType, Ontology.NOMINAL)) {
				return NOMINAL;
			} else if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(rmValueType, Ontology.DATE_TIME)) {
				return DATE_TIME;
			} else {
				return INVALID;
			}
		}
	}

	private final String columnName;
	private final ValueType valueType;

	public DataTableColumn(String name, ValueType valueType) {
		super();
		this.columnName = name;
		this.valueType = valueType;
	}

	/**
	 * @brief Creates a new {@link DataTableColumn}.
	 * 
	 *        name and value type are automatically initialized from the specified column in
	 *        dataTable. The DataTableColumn does not keep a reference to dataTable.
	 */
	public DataTableColumn(DataTable dataTable, int columnIdx) {
		if (columnIdx >= 0 && columnIdx < dataTable.getColumnNumber()) {
			this.columnName = dataTable.getColumnName(columnIdx);
			if (dataTable.isDateTime(columnIdx)) {
				this.valueType = ValueType.DATE_TIME;
			} else if (dataTable.isNominal(columnIdx)) {
				this.valueType = ValueType.NOMINAL;
			} else if (dataTable.isNumerical(columnIdx)) {
				this.valueType = ValueType.NUMERICAL;
			} else {
				this.valueType = ValueType.INVALID;
			}
		} else {
			this.columnName = null;
			this.valueType = ValueType.INVALID;
		}
	}

	/**
	 * @return the {@link ValueType}
	 */
	public ValueType getValueType() {
		return valueType;
	}

	public boolean isNominal() {
		if (valueType == ValueType.NOMINAL) {
			return true;
		}
		return false;
	}

	public boolean isNumerical() {
		if (valueType == ValueType.NUMERICAL) {
			return true;
		}
		return false;
	}

	public boolean isDate() {
		if (valueType == ValueType.DATE_TIME) {
			return true;
		}
		return false;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return columnName;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (!(obj instanceof DataTableColumn)) {
			return false;
		}

		DataTableColumn other = (DataTableColumn) obj;

		if (valueType != other.valueType) {
			return false;
		}

		if (!(columnName == null ? other.columnName == null : columnName.equals(other.columnName))) {
			return false;
		}

		return true;
	}

	@Override
	public DataTableColumn clone() {
		return new DataTableColumn(getName(), getValueType());
	}

	/**
	 * Finds the column index for the column named columnName in dataTable, checks if that column in
	 * the data table is compatible with to settings valueType, and updates this.columnIdx.
	 * 
	 * If the column does not exist or is not compatible, the columnIdx is set to -1.
	 */
	public static int getColumnIndex(DataTable dataTable, String columnName, ValueType valueType) {
		int columnIdx = dataTable.getColumnIndex(columnName);
		if (columnIdx >= 0) {
			// check value type
			switch (valueType) {
				case NOMINAL:
					if (!dataTable.isNominal(columnIdx)) {
						columnIdx = -1;
					}
					break;
				case NUMERICAL:
					if (!dataTable.isNumerical(columnIdx)) {
						columnIdx = -1;
					}
					break;
				case DATE_TIME:
					if (!dataTable.isDateTime(columnIdx)) {
						columnIdx = -1;
					}
					break;
				case INVALID:
					// do nothing
					break;
				default:
			}
		}
		return columnIdx;
	}

	public boolean isValidForDataTable(DataTable dataTable) {
		int columnIdx = getColumnIndex(dataTable, columnName, valueType);
		return columnIdx >= 0 && valueType != ValueType.INVALID && columnName != null;
	}

	/**
	 * Finds the column index for the column named like dataTableColumn.getName in dataTable, checks
	 * if that column in the data table is compatible with to settings of dataTableColumn (value
	 * type etc.), and updates this.columnIdx.
	 * 
	 * If the column does not exist or is not compatible, the columnIdx is set to -1.
	 */
	public static int getColumnIndex(DataTable dataTable, DataTableColumn dataTableColumn) {
		return getColumnIndex(dataTable, dataTableColumn.getName(), dataTableColumn.getValueType());
	}
}
