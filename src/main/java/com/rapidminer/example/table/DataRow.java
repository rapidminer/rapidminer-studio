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

import com.rapidminer.example.Attribute;

import java.io.Serializable;


/**
 * This interface defines methods for all entries of ExampleTable implementations. It provides a set
 * and get method for the data. Subclasses may use a double array, a sparse representation, a file
 * or a database.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public abstract class DataRow implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3482048832637144523L;

	/** Returns the value for the given index. */
	protected abstract double get(int index, double defaultValue);

	/** Sets the given data for the given index. */
	protected abstract void set(int index, double value, double defaultValue);

	/**
	 * Ensures that neither <code>get(i)</code> nor <code>put(i,v)</code> throw a runtime exception
	 * for all <i>0 <= i <= numberOfColumns</i>.
	 */
	protected abstract void ensureNumberOfColumns(int numberOfColumns);

	/** Trims the number of columns to the actually needed number. Does nothing by default. */
	public void trim() {}

	/**
	 * This returns the type of this particular {@link DataRow} implementation according to the list
	 * in the {@link DataRowFactory}.
	 */
	public abstract int getType();

	/** Returns a string representation for this data row. */
	@Override
	public abstract String toString();

	/**
	 * Returns the value stored at the given {@link Attribute}'s index. Returns Double.NaN if the
	 * given attribute is null.
	 */
	public double get(Attribute attribute) {
		if (attribute == null) {
			return Double.NaN;
		} else {
			try {
				return attribute.getValue(this);
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new ArrayIndexOutOfBoundsException("DataRow: table index " + attribute.getTableIndex()
						+ " of Attribute " + attribute.getName() + " is out of bounds.");
			}
		}
	}

	/** Sets the value of the {@link Attribute} to <code>value</code>. */
	public void set(Attribute attribute, double value) {
		attribute.setValue(this, value);
	}
}
