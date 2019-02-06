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

import java.util.Collections;
import java.util.List;


/**
 * Class representing an error that occurred while configuring the columns for reading a
 * {@link DataSet}.
 *
 * @author Gisa Schaefer
 * @since 7.0.0
 */
final class ColumnError {

	private final List<Integer> affectedColumns;
	private final String problematicValue;
	private final String message;

	/**
	 * Creates a {@link ColumnError} with the given data.
	 *
	 * @param affectedColumns
	 *            a list of affected columns
	 * @param problematicValue
	 *            the problematic value
	 * @param message
	 *            the error message
	 */
	ColumnError(List<Integer> affectedColumns, String problematicValue, String message) {
		this.affectedColumns = affectedColumns;
		this.problematicValue = problematicValue;
		this.message = message;
	}

	/**
	 * @return the list of affected columns
	 */
	List<Integer> getAffectedColumns() {
		return Collections.unmodifiableList(affectedColumns);
	}

	/**
	 * @return the problematic value
	 */
	String getProblematicValue() {
		return problematicValue;
	}

	/**
	 * @return the error message
	 */
	String getMessage() {
		return message;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (affectedColumns == null ? 0 : affectedColumns.hashCode());
		result = prime * result + (message == null ? 0 : message.hashCode());
		result = prime * result + (problematicValue == null ? 0 : problematicValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ColumnError other = (ColumnError) obj;
		if (affectedColumns == null) {
			if (other.affectedColumns != null) {
				return false;
			}
		} else if (!affectedColumns.equals(other.affectedColumns)) {
			return false;
		}
		if (message == null) {
			if (other.message != null) {
				return false;
			}
		} else if (!message.equals(other.message)) {
			return false;
		}
		if (problematicValue == null) {
			if (other.problematicValue != null) {
				return false;
			}
		} else if (!problematicValue.equals(other.problematicValue)) {
			return false;
		}
		return true;
	}

}
