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
package com.rapidminer.tools.usagestats;

import java.awt.Window;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;


/**
 * Registry for CTA visualizer
 *
 * @author Jonas Wilms-Pfau
 * @since 9.4.1
 */
enum CTAVisualizationRegistry {
	;

	/**
	 * Internally used result container
	 *
	 * @author Jonas Wilms-Pfau
	 * @since 9.4.1
	 */
	static final class CTA {
		private final Window window;
		private final Supplier<String> supplier;

		private CTA(Window window, Supplier<String> supplier) {
			this.window = window;
			this.supplier = supplier;
		}

		public Window getWindow() {
			return window;
		}

		public String getResult() {
			return supplier.get();
		}
	}

	private static Function<String, CTA> visualizer;

	/**
	 * Registers a CTA visualizer
	 *
	 * @param ctaVisualizer
	 * 		the cta visualizer
	 */
	static <T extends Window & Supplier<String>> void registerVisualization(Function<String, T> ctaVisualizer) {
		Tools.requireInternalPermission();
		if (ctaVisualizer != null && visualizer == null) {
			visualizer = s -> {
				T res = ctaVisualizer.apply(s);
				return new CTA(res, res);
			};
		}
	}

	/**
	 * Creates a cta for the string
	 *
	 * @param string
	 * 		the html to display
	 * @return the cta
	 */
	static CTA getVisualization(String string) {
		if (visualizer != null) {
			try {
				return visualizer.apply(string);
			} catch (Exception ex) {
				LogService.getRoot().log(Level.WARNING, "failed to visualize a CTA", ex);
			}
		}
		return null;
	}
}