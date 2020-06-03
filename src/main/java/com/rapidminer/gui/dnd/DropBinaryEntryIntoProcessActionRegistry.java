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
package com.rapidminer.gui.dnd;

import com.rapidminer.repository.AbstractFileSuffixRegistry;


/**
 * Registry for custom behavior when the user drops a {@link com.rapidminer.repository.BinaryEntry} from the repository
 * into the currently open process.
 * <p>
 * Note that only one action can be registered per file suffix (see {@link com.rapidminer.repository.BinaryEntry#getSuffix()}.
 * If there is already an action registered, a new registration will silently fail (register method will return {@code
 * false}).
 * </p>
 * <p> Suffix is defined as the content after the last . in a file name. See {@link
 * com.rapidminer.repository.RepositoryTools#getSuffixFromFilename(String)}
 * </p>
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class DropBinaryEntryIntoProcessActionRegistry extends AbstractFileSuffixRegistry<DropBinaryEntryIntoProcessCallback> {

	private static DropBinaryEntryIntoProcessActionRegistry instance;


	/**
	 * Get the registry instance.
	 *
	 * @return the instance, never {@code null}
	 */
	public static synchronized DropBinaryEntryIntoProcessActionRegistry getInstance() {
		if (instance == null) {
			instance = new DropBinaryEntryIntoProcessActionRegistry();
		}

		return instance;
	}
}
