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
package com.rapidminer.storage.hdf5;

/**
 * Exception thrown by the {@link Hdf5ExampleSetReader} to indicate the reason why the read file does not match the
 * admissible format.
 *
 * @author Gisa Meier
 * @since 9.7.0
 */
public class HdfReaderException extends IllegalArgumentException {

	public enum Reason {
		/**
		 * the dictionary contains a String more than once
		 */
		DICTIONARY_NOT_UNIQUE,

		/**
		 * The dataset is not contiguous
		 */
		NON_CONTIGUOUS,

		/**
		 * A mandatory hdf5 attribute is missing
		 */
		MISSING_ATTRIBUTE,

		/**
		 * The information in the file is inconsistent
		 */
		INCONSISTENT_FILE,

		/**
		 * The object type is not allowed at a certain place
		 */
		ILLEGAL_TYPE,

		/**
		 * Reading the hdf5 data type is not supported
		 */
		UNSUPPORTED_TYPE,

		/**
		 * File is a meta data file but was read as data
		 */
		IS_META_DATA
	}

	private final Reason type;

	HdfReaderException(Reason type, String message) {
		super(message);
		this.type = type;
	}

	/**
	 * Returns the reason for the exceptions.
	 *
	 * @return the reason
	 */
	public Reason getReason() {
		return type;
	}
}