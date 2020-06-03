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

/**
 * Interface for a trivial icon provider.
 * <p>
 * Requirements:
 *     <ul>
 *         <li>Icon must be available in sizes 16, 24, and 48px</li>
 *         <li>Each of the icon sizes above must also have the High-DPI size in the {@code @2x} folder next to it</li>
 *         <li>{@link com.rapidminer.gui.tools.SwingTools#createIcon(String)} must find it</li>
 *     </ul>
 * </p>
 *
 * @author Marco Boeck
 * @since 9.7
 */
@FunctionalInterface
public interface IconProvider {

	/**
	 * Returns the name of the icon.
	 *
	 * @return the icon name (e.g. 'my_icon.png'), never {@code null}
	 */
	String getIconName();
}
