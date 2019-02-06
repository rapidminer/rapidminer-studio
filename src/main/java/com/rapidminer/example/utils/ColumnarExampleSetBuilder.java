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
package com.rapidminer.example.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.IntToDoubleFunction;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowReader;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.internal.ColumnarExampleTable;


/**
 * An {@link ExampleSetBuilder} based on a {@link ColumnarExampleTable}.
 *
 * @author Gisa Schaefer
 * @since 7.3
 */
class ColumnarExampleSetBuilder extends ExampleSetBuilder {

	/** the table that will be created and filled */
	private ColumnarExampleTable table;

	/** number of empty rows to be added */
	private int blankSize;

	/** the reader to use for filling the table */
	private DataRowReader reader;

	/** the functions to use for filling the columns */
	private Map<Attribute, IntToDoubleFunction> columnFillers = new HashMap<>();

	/** stores whether rows were added */
	private boolean rowsAdded = false;

	/** the data management to use */
	private DataManagement management = DataManagement.AUTO;

	/** the expected number of rows */
	private int numberOfRows;

	/**
	 * Creates a builder that stores values in a {@link ColumnarExampleTable} based on the given
	 * attributes.
	 *
	 * @param attributes
	 *            the {@link Attribute}s that the {@link ExampleSet} should contain
	 */
	ColumnarExampleSetBuilder(List<Attribute> attributes) {
		super(attributes);
		setTableIndices();
	}

	/**
	 * Creates a builder that stores values in a {@link ColumnarExampleTable} based on the given
	 * attributes.
	 *
	 * @param attributes
	 *            the {@link Attribute}s that the {@link ExampleSet} should contain
	 */
	ColumnarExampleSetBuilder(Attribute... attributes) {
		super(attributes);
		setTableIndices();
	}

	@Override
	public ExampleSetBuilder withBlankSize(int numberOfRows) {
		this.blankSize = numberOfRows;
		return this;
	}

	@Override
	public ExampleSetBuilder withExpectedSize(int numberOfRows) {
		this.numberOfRows = numberOfRows;
		return this;
	}

	@Override
	public ExampleSetBuilder withDataRowReader(DataRowReader reader) {
		this.reader = reader;
		return this;
	}

	@Override
	public ExampleSetBuilder addDataRow(DataRow dataRow) {
		if (table == null) {
			table = createTable();
		}
		table.addDataRow(dataRow);
		rowsAdded = true;
		return this;
	}

	@Override
	public ExampleSetBuilder addRow(double[] row) {
		if (table == null) {
			table = createTable();
		}
		table.addRow(row);
		rowsAdded = true;
		return this;
	}

	@Override
	public ExampleSetBuilder withColumnFiller(Attribute attribute, IntToDoubleFunction columnFiller) {
		columnFillers.put(attribute, columnFiller);
		return this;
	}

	@Override
	public ExampleSetBuilder withOptimizationHint(DataManagement management) {
		this.management = management;
		return this;
	}

	@Override
	protected ExampleTable getExampleTable() {
		if (table == null) {
			table = createTable();
		}
		if (reader != null) {
			rowsAdded = true;
			while (reader.hasNext()) {
				table.addDataRow(reader.next());
			}
		}
		if (blankSize > 0) {
			table.addBlankRows(blankSize);
		}
		if (columnFillers.size() > 0) {
			writeColumnValues();
		}

		table.complete();

		return table;
	}

	private ColumnarExampleTable createTable() {
		ColumnarExampleTable newTable = new ColumnarExampleTable(getAttributes(), management, true);
		newTable.setExpectedSize(numberOfRows);
		return newTable;
	}

	/**
	 * Writes the values provided by the {@link #columnFillers} into the table.
	 */
	private void writeColumnValues() {
		for (Entry<Attribute, IntToDoubleFunction> entry : columnFillers.entrySet()) {
			if (rowsAdded) {
				// must reset the column when rows were added so that the auto column mechanism can
				// work
				table.resetColumn(entry.getKey());
			}
			table.fillColumn(entry.getKey(), entry.getValue());
		}
	}

}
