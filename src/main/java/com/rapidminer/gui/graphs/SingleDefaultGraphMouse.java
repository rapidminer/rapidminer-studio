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
package com.rapidminer.gui.graphs;

import java.awt.event.InputEvent;

import com.rapidminer.gui.graphs.plugins.ExtendedPickingGraphMousePlugin;

import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.TranslatingGraphMousePlugin;


/**
 * This graph mouse does offer no mode selection but instead has all necessary features in its
 * default mode.
 *
 * @author Marco Boeck
 * @since 7.5.0
 *
 */
public class SingleDefaultGraphMouse<V, E> extends PluggableGraphMouse {

	/** used by the scaling plugin for zoom in */
	private float in;

	/** used by the scaling plugin for zoom out */
	private float out;

	private ExtendedPickingGraphMousePlugin<V, E> pickingPlugin;
	private TranslatingGraphMousePlugin translatingPlugin;
	private ScalingGraphMousePlugin scalingPlugin;

	public SingleDefaultGraphMouse(float in, float out) {
		this.in = in;
		this.out = out;

		loadPlugins();
	}

	/**
	 * Create and load the plugins to use.
	 *
	 */
	protected void loadPlugins() {
		pickingPlugin = new ExtendedPickingGraphMousePlugin<V, E>();
		pickingPlugin.setRectangleSelectionEnabled(false);
		translatingPlugin = new TranslatingGraphMousePlugin(InputEvent.BUTTON1_MASK);
		scalingPlugin = new ScalingGraphMousePlugin(new CrossoverScalingControl(), 0, in, out);

		add(pickingPlugin);
		add(translatingPlugin);
		add(scalingPlugin);
	}

}
