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
package com.rapidminer.example.table;

/**
 * This data row can be wrapped around another data row (delegate) in order to prevent writing
 * access. This might be useful, for example, for database access.
 * 
 * @author Ingo Mierswa
 */
public class NonWritableDataRow extends DataRow {

	private static final long serialVersionUID = 6148646678312194050L;

	private DataRow delegate;

	public NonWritableDataRow(DataRow delegate) {
		this.delegate = delegate;
	}

	@Override
	protected void ensureNumberOfColumns(int numberOfColumns) {
		this.delegate.ensureNumberOfColumns(numberOfColumns);
	}

	@Override
	protected double get(int index, double defaultValue) {
		return delegate.get(index, defaultValue);
	}

	@Override
	protected void set(int index, double value, double defaultValue) {
		throw new UnsupportedOperationException(
				"The 'set' operation (writing data) is not supported for this type of underlying example tables, e.g. it is not supported for a data table backed up by a database. Please transform into a memory based data table first, for example with the batch processing operator, or materialize the data table in memory.");
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	@Override
	public void trim() {
		delegate.trim();
	}

	@Override
	public int getType() {
		return DataRowFactory.TYPE_SPECIAL;
	}
}
