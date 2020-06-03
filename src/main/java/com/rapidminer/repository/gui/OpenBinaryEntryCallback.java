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

import com.rapidminer.repository.BinaryEntry;


/**
 * Callback interface when a {@link BinaryEntry} is to be opened by the {@link OpenBinaryEntryActionRegistry}.
 *
 * @author Marco Boeck
 * @since 9.7
 */
@FunctionalInterface
public interface OpenBinaryEntryCallback {

	/**
	 * This method is triggered by the user UI action (e.g. a double-click on the entry). It is started in a {@link
	 * com.rapidminer.gui.tools.ProgressThread}, so the UI does not block during this call. Note that this means that
	 * you must use {@link javax.swing.SwingUtilities#invokeLater(Runnable)} if you want to manipulate UI elements
	 * here.
	 *
	 * @param entry the entry, never {@code null}
	 */
	void openEntry(BinaryEntry entry);
}
