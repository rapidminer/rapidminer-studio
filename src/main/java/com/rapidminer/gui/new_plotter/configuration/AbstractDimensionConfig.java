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
package com.rapidminer.gui.new_plotter.configuration;

import com.rapidminer.gui.new_plotter.listener.DimensionConfigListener;
import com.rapidminer.gui.new_plotter.listener.events.DimensionConfigChangeEvent;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class AbstractDimensionConfig implements DimensionConfig {

	private final PlotDimension dimension;
	private boolean fireEvents = true;

	private transient List<WeakReference<DimensionConfigListener>> listeners = new LinkedList<WeakReference<DimensionConfigListener>>();

	public AbstractDimensionConfig(PlotDimension dimension) {
		this.dimension = dimension;
	}

	@Override
	public PlotDimension getDimension() {
		return dimension;
	}

	@Override
	public void addDimensionConfigListener(DimensionConfigListener l) {
		listeners.add(new WeakReference<DimensionConfigListener>(l));
	}

	@Override
	public void removeDimensionConfigListener(DimensionConfigListener l) {
		Iterator<WeakReference<DimensionConfigListener>> it = listeners.iterator();
		while (it.hasNext()) {
			DimensionConfigListener listener = it.next().get();
			if (listener == null || listener == l) {
				it.remove();
			}
		}
	}

	protected void setFireEvents(boolean fireEvents) {
		this.fireEvents = fireEvents;
	}

	protected boolean isFiringEvents() {
		return this.fireEvents;
	}

	protected void fireDimensionConfigChanged(DimensionConfigChangeEvent e) {
		if (isFiringEvents()) {
			Iterator<WeakReference<DimensionConfigListener>> it = listeners.iterator();
			while (it.hasNext()) {
				DimensionConfigListener listener = it.next().get();
				if (listener == null) {
					it.remove();
				} else {
					listener.dimensionConfigChanged(e);
				}
			}
		}
	}
}
