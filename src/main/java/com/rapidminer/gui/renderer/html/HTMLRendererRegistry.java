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
package com.rapidminer.gui.renderer.html;

import java.util.function.Function;

import com.rapidminer.tools.Tools;


/**
 * Internal Registry for HTML renderer
 * @author Jonas Wilms-Pfau
 * @since 9.4.1
 */
public enum HTMLRendererRegistry {
	/**
	 * HTML Rendering component.
	 */
	HTML(BasicHTMLRenderer::new);

	private final Function<String, HTMLRenderer> defaultRenderer;
	private Function<String, HTMLRenderer> renderer;


	/**
	 * Creates a new registry with the given default renderer
	 *
	 * @param defaultRenderer
	 * 		the default open source renderer
	 */
	HTMLRendererRegistry(Function<String, HTMLRenderer> defaultRenderer) {
		this.defaultRenderer = defaultRenderer;
		this.renderer = defaultRenderer;
	}

	/**
	 * Registers a renderer if none is registered yet. Requires internal permissions.
	 *
	 * @param renderer
	 * 		the renderer
	 */
	public void registerRenderer(Function<String, HTMLRenderer> renderer) {
		if (renderer != null) {
			try {
				Tools.requireInternalPermission();
				this.renderer = renderer;
			} catch (UnsupportedOperationException e) {
				// do nothing
			}
		}
	}

	/**
	 * Returns a component which renders the given html.
	 *
	 * @param html
	 * 		the html to render, must not be {@code null}
	 * @return the component which displays the text
	 */
	public HTMLRenderer getRenderer(String html) {
		if (html == null) {
			throw new IllegalArgumentException("html must not be null!");
		}

		try {
			Tools.requireInternalPermission();
			return renderer.apply(html);
		} catch (UnsupportedOperationException e) {
			return defaultRenderer.apply(html);
		}
	}
}
