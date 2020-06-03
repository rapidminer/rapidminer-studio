/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
 * Thrown if a repository contains multiple {@link IOObjectEntry IOObjectEntries} with the same name (prefix).
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class RepositoryIOObjectEntryDuplicateFoundException extends RepositoryException {


	public RepositoryIOObjectEntryDuplicateFoundException() {}

	/**
	 * @param absoluteLocation the absolute repository location path that triggered this exception
	 */
	public RepositoryIOObjectEntryDuplicateFoundException(String absoluteLocation) {
		super(I18N.getErrorMessage("repository.error.duplicate_ioobject_entry", absoluteLocation));
	}

	public RepositoryIOObjectEntryDuplicateFoundException(Throwable cause) {
		super(cause);
	}

	public RepositoryIOObjectEntryDuplicateFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
