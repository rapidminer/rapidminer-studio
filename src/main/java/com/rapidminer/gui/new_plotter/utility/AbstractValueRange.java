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
package com.rapidminer.gui.new_plotter.utility;

import com.rapidminer.gui.new_plotter.listener.ValueRangeListener;
import com.rapidminer.gui.new_plotter.listener.events.ValueRangeChangeEvent;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Value range that is a abstract superclass for most value ranges. Implements applyRange and
 * applyRangeOnDataTableRows.
 * 
 * @author Marius Helf, Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class AbstractValueRange implements ValueRange, Cloneable {

	private transient List<WeakReference<ValueRangeListener>> listeners = new LinkedList<WeakReference<ValueRangeListener>>();

	@Override
	public abstract ValueRange clone();

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public void addValueRangeListener(ValueRangeListener l) {
		listeners.add(new WeakReference<ValueRangeListener>(l));
	}

	@Override
	public void removeValueRangeListener(ValueRangeListener l) {
		Iterator<WeakReference<ValueRangeListener>> it = listeners.iterator();
		while (it.hasNext()) {
			ValueRangeListener listener = it.next().get();
			if (listener == null || listener == l) {
				it.remove();
			}
		}
	}

	protected void fireValueRangeChanged(ValueRangeChangeEvent e) {
		Iterator<WeakReference<ValueRangeListener>> it = listeners.iterator();
		while (it.hasNext()) {
			ValueRangeListener listener = it.next().get();
			if (listener == null) {
				it.remove();
			} else {
				listener.valueRangeChanged(e);
			}
		}
	}
}
