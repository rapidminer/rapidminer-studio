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
package com.rapidminer.parameter;

/**
 *
 *
 * @author Marcel Seifert
 *
 */
public class ParameterTypeProcessLocation extends ParameterTypeRepositoryLocation {

	private static final long serialVersionUID = -2679649330364282920L;

	/**
	 * Creates a new parameter type for files with the given extension. If the extension is null no
	 * file filters will be used.
	 *
	 * @param key
	 * @param description
	 * @param optional
	 */
	public ParameterTypeProcessLocation(String key, String description, boolean optional) {
		super(key, description, optional);
	}

	/**
	 * Creates a new parameter type for files with the given extension. If the extension is null no
	 * file filters will be used.
	 *
	 * @param key
	 * @param description
	 * @param allowEntries
	 * @param allowDirectories
	 * @param optional
	 */
	public ParameterTypeProcessLocation(String key, String description, boolean allowEntries, boolean allowDirectories,
			boolean optional) {
		super(key, description, allowEntries, allowDirectories, optional);
	}

	/**
	 * Creates a new parameter type for files with the given extension. If the extension is null no
	 * file filters will be used. If {@link #enforceValidRepositoryEntryName} is set to
	 * <code>true</code>, will enforce valid repository entry names.
	 *
	 * @param key
	 * @param description
	 * @param allowEntries
	 * @param allowDirectories
	 * @param allowAbsoluteEntries
	 * @param optional
	 * @param enforceValidRepositoryEntryName
	 */
	public ParameterTypeProcessLocation(String key, String description, boolean allowEntries, boolean allowDirectories,
			boolean allowAbsoluteEntries, boolean optional, boolean enforceValidRepositoryEntryName) {
		super(key, description, allowEntries, allowDirectories, allowAbsoluteEntries, optional,
				enforceValidRepositoryEntryName);
	}

	/**
	 * Creates a new parameter type for files with the given extension. If the extension is null no
	 * file filters will be used. If {@link #enforceValidRepositoryEntryName} is set to
	 * <code>true</code>, will enforce valid repository entry names.
	 *
	 * @param key
	 * @param description
	 * @param allowEntries
	 * @param allowDirectories
	 * @param allowAbsoluteEntries
	 * @param optional
	 * @param enforceValidRepositoryEntryName
	 * @param onlyWriteableLocations
	 */
	public ParameterTypeProcessLocation(String key, String description, boolean allowEntries, boolean allowDirectories,
			boolean allowAbsoluteEntries, boolean optional, boolean enforceValidRepositoryEntryName,
			boolean onlyWriteableLocations) {
		super(key, description, allowEntries, allowDirectories, allowAbsoluteEntries, optional,
				enforceValidRepositoryEntryName, onlyWriteableLocations);
	}

	/**
	 * Creates a new parameter type for files with the given extension. If the extension is null no
	 * file filters will be used.
	 *
	 * @param key
	 * @param description
	 * @param allowEntries
	 * @param allowDirectories
	 * @param allowAbsoluteEntries
	 * @param optional
	 */
	public ParameterTypeProcessLocation(String key, String description, boolean allowEntries, boolean allowDirectories,
			boolean allowAbsoluteEntries, boolean optional) {
		super(key, description, allowEntries, allowDirectories, allowAbsoluteEntries, optional);
	}

}
