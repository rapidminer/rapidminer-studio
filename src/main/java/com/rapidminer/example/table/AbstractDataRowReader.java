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
 * Reads a sequence of DataRows, e.g. from memory, a file or a database.
 * 
 * @author Simon Fischer, Ingo Mierswa ingomierswa Exp $
 */
public abstract class AbstractDataRowReader implements DataRowReader {

	private DataRowFactory factory;

	public AbstractDataRowReader(DataRowFactory factory) {
		this.factory = factory;
	}

	public DataRowFactory getFactory() {
		return factory;
	}

	/**
	 * Will throw a new {@link UnsupportedOperationException} since {@link DataRowReader} does not
	 * have to implement remove.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException("The method 'remove' is not supported by DataRowReaders!");
	}
}
