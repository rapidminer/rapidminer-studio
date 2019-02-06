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
package com.rapidminer.studio.io.gui.internal.steps.configuration;

/**
 * Class representing an error that occurred while reading a {@link DataSet}.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 */
final class ParsingError {

	private int column;
	private int row;
	private String originalValue;
	private String message;

	/**
	 * Creates a new parsing error with the given data.
	 *
	 * @param column
	 *            the column index of the erroneous cell
	 * @param row
	 *            the row index of the erroneous cell
	 * @param originalValue
	 *            the original value that could not be parsed, can be {@code null}
	 * @param message
	 *            the error message
	 */
	ParsingError(int column, int row, String originalValue, String message) {
		this.column = column;
		this.row = row;
		this.originalValue = originalValue;
		this.message = message;
	}

	/**
	 * @return the column where the error happened
	 */
	int getColumn() {
		return column;
	}

	/**
	 * @return the row where the error happened
	 */
	int getRow() {
		return row;
	}

	/**
	 * @return the original value that could not be parsed, can be {@code null} if the original
	 *         value cannot be accessed
	 */
	String getOriginalValue() {
		return originalValue;
	}

	/**
	 * @return the error message
	 */
	String getMessage() {
		return message;
	}

}
