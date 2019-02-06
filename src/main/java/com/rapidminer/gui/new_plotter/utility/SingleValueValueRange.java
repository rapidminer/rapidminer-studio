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
package com.rapidminer.gui.new_plotter.utility;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Tools;

import java.sql.Date;
import java.text.DateFormat;
import java.util.Formatter;
import java.util.Locale;


/**
 * Value range that contains only one single value.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class SingleValueValueRange extends AbstractValueRange {

	private double value;
	private String valueString;
	private int columnIdx;

	/**
	 * Instantiates a new {@link SingleValueValueRange}.
	 * 
	 * @param value
	 *            the value of the value range.
	 */
	public SingleValueValueRange(double value) {
		this(value, (String) null, -1);
	}

	/**
	 * Instantiates a new {@link SingleValueValueRange}.
	 * 
	 * @param value
	 *            the value of the value range
	 * @param valueString
	 *            a string representation of the value, which will be used in the
	 *            {@link #toString()} method. If null, a value string is created based on the
	 * @param columnIdx
	 *            the column idx which is used in the {@link #keepRow(DataTableRow)} method.
	 */
	public SingleValueValueRange(double value, String valueString, int columnIdx) {
		this.value = value;
		this.valueString = valueString;
		this.columnIdx = columnIdx;
	}

	/**
	 * Instantiates a new {@link SingleValueValueRange}.
	 * 
	 * The string representation is created based on the data type of a column in a data table.
	 * 
	 * @param value
	 *            the value of the value range
	 * @param dataTable
	 *            a data table, from which the data type of the value is extracted
	 * @param columnIdx
	 *            the column index in data table which is used to extract the data type of the value
	 * @throws IllegalArgumentException
	 *             if the specified column has an unknown data type (i.e. neither numerical, nominal
	 *             nor date).
	 */
	public SingleValueValueRange(double value, DataTable dataTable, int columnIdx) {
		this.value = value;
		this.columnIdx = columnIdx;
		if (columnIdx >= 0) {
			if (Double.isNaN(value)) {
				valueString = I18N.getGUILabel("plotter.unknown_value_label");
			} else if (dataTable.isNumerical(columnIdx)) {
				valueString = Double.toString(value);
			} else if (dataTable.isNominal(columnIdx)) {
				valueString = dataTable.mapIndex(columnIdx, (int) value);
			} else if (dataTable.isDateTime(columnIdx)) {
				valueString = Tools.formatDateTime(new Date((long) value));
			} else {
				throw new IllegalArgumentException("unknown data type in data table - this should not happen");
			}
		}
	}

	/**
	 * Instantiates a new {@link SingleValueValueRange}. Value is considered to represent a date and
	 * is formatted with the provided {@link DateFormat} .
	 * 
	 * @param value
	 *            the value of the value range
	 * @param dateFormat
	 *            the format which is used to create a string representation of <i>value</i>.
	 * @throws NullPointerException
	 *             iff dateFormat is null.
	 */
	public SingleValueValueRange(double value, DateFormat dateFormat) {
		if (dateFormat == null) {
			throw new NullPointerException("null format not allowed");
		}

		this.value = value;
		this.valueString = dateFormat.format(new Date((long) value));
	}

	@Override
	public boolean keepRow(DataTableRow row) {
		return row.getValue(columnIdx) == this.value;
	}

	@Override
	public String toString() {
		// if (valueString != null) {
		return valueString;
		// } else {
		// StringBuilder builder = new StringBuilder();
		// double roundedValue = DataStructureUtils.roundToPowerOf10(getValue(), visualPrecision);
		// Formatter formatter = new Formatter(builder, Locale.getDefault());
		// String format;
		// if (visualPrecision < 0) {
		// format = "%."+(-visualPrecision)+"f";
		// } else {
		// format = "%.0f";
		// }
		//
		// formatter.format(format, roundedValue);
		// return builder.toString();
		// }
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof SingleValueValueRange)) {
			return false;
		}

		SingleValueValueRange other = (SingleValueValueRange) obj;
		return (other.value == this.value);
	}

	@Override
	public int hashCode() {
		Double dValue = new Double(value);
		return dValue.hashCode();
	}

	@Override
	public double getValue() {
		return value;
	}

	@Override
	public ValueRange clone() {
		SingleValueValueRange clone = new SingleValueValueRange(value, valueString, columnIdx);
		return clone;
	}

	@Override
	public boolean definesUpperLowerBound() {
		return false;
	}

	@Override
	public double getUpperBound() {
		return Double.NaN;
	}

	@Override
	public double getLowerBound() {
		return Double.NaN;
	}

	public void setVisualPrecision(int precision) {
		StringBuilder builder = new StringBuilder();
		double roundedValue = DataStructureUtils.roundToPowerOf10(getValue(), precision);
		Formatter formatter = new Formatter(builder, Locale.getDefault());
		String format;
		if (precision < 0) {
			format = "%." + -precision + "f";
		} else {
			format = "%.0f";
		}

		formatter.format(format, roundedValue);
		formatter.close();
		valueString = builder.toString();
	}

	public void setDateFormat(DateFormat dateFormat) {
		valueString = dateFormat.format(new Date((long) value));
	}
}
