/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.repository.versioned;

import org.apache.commons.lang.StringUtils;

import com.rapidminer.repository.RepositoryException;
import com.rapidminer.versioning.repository.DataSummary;
import com.rapidminer.versioning.repository.GeneralFile;


/**
 * A {@link DataSummary} that can act as a {@link RepositoryException} to indicate that something went wrong during
 * meta data creation.
 *
 * @author Jan Czogalla
 * @since 9.7
 */
public class FaultyDataSummary extends RepositoryException implements DataSummary {

	public FaultyDataSummary() {}

	public FaultyDataSummary(String message) {
		super(message);
	}

	public FaultyDataSummary(Throwable cause) {
		super(cause);
	}

	public FaultyDataSummary(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public String getSummary() {
		return getMessage();
	}

	/**
	 * Creates a {@link FaultyDataSummary} where a wrong file type was encountered
	 *
	 * @param file
	 * 		the file that has the wrong type
	 * @return a faulty data summary with the additional message "wrong file type"
	 */
	public static FaultyDataSummary wrongFileType(GeneralFile<?> file) {
		return additionalInfo(file, "wrong file type");
	}

	/**
	 * Creates a {@link FaultyDataSummary} with the base message "No summary available for file" and the given file's path.
	 * An additional message might be appended with a semicolon if not {@code null} or empty.
	 *
	 * @param file
	 * 		the file that caused the problem
	 * @param addition
	 * 		an optional additional message
	 * @return a faulty data summary
	 */
	public static FaultyDataSummary additionalInfo(GeneralFile<?> file, String addition) {
		addition = StringUtils.trimToEmpty(addition);
		if (!addition.isEmpty()) {
			addition = "; " + addition;
		}
		return new FaultyDataSummary("No summary available for file " + file.getPath() + addition);
	}


	/**
	 * Creates a {@link FaultyDataSummary} with the base message "No summary available for file" and the given file's path.
	 * Accepts an additional {@link Throwable} as the cause.
	 *
	 * @param file
	 * 		the file that caused the problem
	 * @param cause
	 * 		the underlying cause; should not be {@code null}
	 * @return a faulty data summary
	 */
	public static FaultyDataSummary withCause(GeneralFile<?> file, Throwable cause) {
		return new FaultyDataSummary("No summary available for file " + file.getPath(), cause);
	}
}
