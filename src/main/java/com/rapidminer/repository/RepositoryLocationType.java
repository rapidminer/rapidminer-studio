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

/**
 * A repository location always has a location type. This helps differentiate whether a folder or a file is meant by it.
 * See {@link RepositoryLocation#locateData()} and {@link RepositoryLocation#locateFolder()}.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public enum RepositoryLocationType {

	/**
	 * represents a {@link DataEntry}. Any access to it should resolve a data entry.
	 */
	DATA_ENTRY,

	/**
	 * represents a {@link Folder}. Any access to it should resolve a folder.
	 */
	FOLDER,

	/**
	 * represents either a {@link Folder} OR a {@link DataEntry}. It is unclear what is meant for either technical
	 * or legacy reasons. Any access will try resolving a data entry first, a folder second only if no data entry
	 * with that name has been found.
	 */
	UNKNOWN;
}
