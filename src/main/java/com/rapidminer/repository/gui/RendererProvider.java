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

import com.rapidminer.gui.renderer.Renderer;
import com.rapidminer.operator.nio.file.BinaryEntryFileObject;


/**
 * Interface for a trivial {@link com.rapidminer.gui.renderer.Renderer} provider.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public interface RendererProvider {

	/**
	 * Returns the renderer for the given {@link com.rapidminer.operator.nio.file.BinaryEntryFileObject}.
	 *
	 * @param binaryEntryFileObject the binary entry file object that should be renderer, never {@code null}
	 * @return the renderer instance, never {@code null}
	 */
	Renderer getRenderer(BinaryEntryFileObject binaryEntryFileObject);

	/**
	 * Returns an icon name for the given {@link com.rapidminer.operator.nio.file.BinaryEntryFileObject}.
	 * <p>
	 * Requirements:
	 *     <ul>
	 *         <li>Icon must be available in sizes 16, 24, 32, and 48px</li>
	 *         <li>Each of the icon sizes above must also have the High-DPI size in the {@code @2x} folder next to it</li>
	 *         <li>{@link com.rapidminer.gui.tools.SwingTools#createIcon(String)} must find it</li>
	 *     </ul>
	 * </p>
	 *
	 * @param binaryEntryFileObject the binary entry file object that needs an icon, never {@code null}
	 * @return the icon name or {@code null} if the default icon should be used
	 */
	String getIconName(BinaryEntryFileObject binaryEntryFileObject);
}
