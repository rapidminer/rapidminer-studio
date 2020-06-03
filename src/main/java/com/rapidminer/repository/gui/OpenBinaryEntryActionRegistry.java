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
package com.rapidminer.repository.gui;

import com.rapidminer.repository.AbstractFileSuffixRegistry;
import com.rapidminer.repository.BinaryEntry;


/**
 * Registry for custom behavior when the user tries to open a {@link com.rapidminer.repository.BinaryEntry} in the
 * repository. This registry will only be used if the user has not defined a custom open action command for a specific
 * file suffix (see {@link com.rapidminer.repository.RepositoryTools#getOpenCommandForSuffix(String)}).
 * <p>
 * Note that only one action can be registered per file suffix. If there is already an action registered, a new
 * registration will silently fail (register method will return {@code false}).
 * </p>
 * <p> Suffix is defined as the content after the last . in a file name, see {@link
 * BinaryEntry#getSuffix()}.</p>
 *
 * @author Marco Boeck
 * @since 9.7
 */
public class OpenBinaryEntryActionRegistry extends AbstractFileSuffixRegistry<OpenBinaryEntryCallback> {

	private static OpenBinaryEntryActionRegistry instance;


	/**
	 * Get the registry instance.
	 *
	 * @return the instance, never {@code null}
	 */
	public static synchronized OpenBinaryEntryActionRegistry getInstance() {
		if (instance == null) {
			instance = new OpenBinaryEntryActionRegistry();
		}

		return instance;
	}
}
