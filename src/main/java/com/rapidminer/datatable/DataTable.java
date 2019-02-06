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
package com.rapidminer.datatable;

import com.rapidminer.Process;
import com.rapidminer.gui.plotter.charts.AbstractChartPanel.Selection;
import com.rapidminer.report.Tableable;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;


/**
 * A data table that contains Object arrays that record process results, data, etc. Instances of
 * this class are automatically created by {@link Process#getDataTables()} and are used mainly by
 * the {@link com.rapidminer.operator.visualization.ProcessLogOperator}. On the other hand, also
 * {@link com.rapidminer.example.ExampleSet}s can also be used as an data table object.
 * 
 * @author Ingo Mierswa
 */
public interface DataTable extends Iterable<DataTableRow>, Tableable {

	/**
	 * Indicates if the column with the given index is nominal. For numerical or date columns, the
	 * value false should be returned.
	 */
	public boolean isNominal(int index);

	/**
	 * Indicates if the column with the given index is nominal. For numerical or date columns, the
	 * value false should be returned.
	 */
	public boolean isTime(int index);

	/**
	 * Indicates if the column with the given index is nominal. For numerical or date columns, the
	 * value false should be returned.
	 */
	public boolean isDate(int index);

	/**
	 * Indicates if the column with the given index is nominal. For numerical or date columns, the
	 * value false should be returned.
	 */
	public boolean isDateTime(int index);

	/**
	 * Indicates if the column with the given index is nominal. For numerical or date columns, the
	 * value false should be returned.
	 */
	public boolean isNumerical(int index);

	/**
	 * If a column is nominal, the index value must be mapped to the nominal value by this method.
	 * If the given column is not nominal, this method might throw a {@link NullPointerException}.
	 */
	public String mapIndex(int column, int index);

	/**
	 * If a column is nominal, the nominal value must be mapped to a (new) index by this method. If
	 * the given column is not nominal, this method might throw a {@link NullPointerException}.
	 */
	public int mapString(int column, String value);

	/** Returns the name of the i-th column. */
	@Override
	public String getColumnName(int i);

	/** Returns the column index of the column with the given name. */
	public int getColumnIndex(String name);

	/** Returns the weight of the column or Double.NaN if no weight is available. */
	public double getColumnWeight(int i);

	/** Returns true if this data table is supporting column weights. */
	public boolean isSupportingColumnWeights();

	/** Returns the total number of columns. */
	public int getNumberOfColumns();

	/**
	 * Returns the total number of special columns. Please note that these columns do not need to be
	 * in an ordered sequence. In order to make sure that a column is a special column the method
	 * {@link #isSpecial(int)} should be used.
	 */
	public int getNumberOfSpecialColumns();

	/**
	 * Returns true if this column is a special column which might usually not be used for some
	 * plotters, for example weights or labels.
	 */
	public boolean isSpecial(int column);

	/** Returns an array of all column names. */
	public String[] getColumnNames();

	/** Returns the name of this data table. */
	public String getName();

	/** Sets the name of the data table. */
	public void setName(String name);

	/** Adds the given {@link DataTableRow} to the table. */
	public void add(DataTableRow row);

	/** Returns an iterator over all {@link DataTableRow}s. */
	@Override
	public Iterator<DataTableRow> iterator();

	/**
	 * Returns the data table row with the given index. Please note that this method is not
	 * guaranteed to be efficiently implemented. If you want to scan the complete data table you
	 * should use the iterator() method instead.
	 */
	public DataTableRow getRow(int index);

	/** Returns the total number of rows. */
	public int getNumberOfRows();

	/**
	 * Returns the number of different values for the i-th column. Might return -1 or throw an
	 * exception if the specific column is not nominal.
	 */
	public int getNumberOfValues(int column);

	/** Must deliver the proper value as string, i.e. the mapped value for nominal columns. */
	public String getValueAsString(DataTableRow row, int column);

	/** Adds a table listener listening for data changes. */
	public void addDataTableListener(DataTableListener dataTableListener);

	/**
	 * Adds a table listener listening for data changes.
	 * 
	 * @param weakReference
	 *            if true, the listener is stored in a weak reference, so that the listener
	 *            mechanism does not keep garbage collection from deleting the listener.
	 */
	public void addDataTableListener(DataTableListener dataTableListener, boolean weakReference);

	/** Removes the given listener from the list of data change listeners. */
	public void removeDataTableListener(DataTableListener dataTableListener);

	/** Writes the table into the given writer. */
	public void write(PrintWriter out) throws IOException;

	/** Performs a sampling of this data table. Following operations should only work on the sample. */
	public DataTable sample(int newSize);

	/** Returns true if this data table contains missing values. */
	public boolean containsMissingValues();

	// public boolean isDeselected(int index);
	public boolean isDeselected(String id);

	public void setSelection(Selection selection);

	public int getSelectionCount();
}
