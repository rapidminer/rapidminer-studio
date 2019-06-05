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
package com.rapidminer.repository;

import com.rapidminer.tools.I18N;

/**
 * Thrown if a repository entry was of the wrong type.
 *
 * @author Marco Boeck
 * @since 8.2
 */
public class RepositoryEntryWrongTypeException extends RepositoryException {


	public RepositoryEntryWrongTypeException() {}

	public RepositoryEntryWrongTypeException(String message) {
		super(message);
	}

	/** @since 9.3 */
	public RepositoryEntryWrongTypeException(RepositoryLocation location, String expectedType, String actualType) {
		this(I18N.getErrorMessage("repository.error.mismatched_entry_type", location, expectedType, actualType));
	}

	public RepositoryEntryWrongTypeException(Throwable cause) {
		super(cause);
	}

	public RepositoryEntryWrongTypeException(String message, Throwable cause) {
		super(message, cause);
	}

}
