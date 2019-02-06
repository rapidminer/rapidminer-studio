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
package com.rapidminer.gui.flow.processrendering.view.components;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Timer;

import com.rapidminer.operator.Operator;


/**
 * Timer used to roll out names of operators.
 *
 * @author Simon Fischer
 */
public class InterpolationMap {

	private static final long DELAY = 300;
	private static final long INTERVAL = 500;
	private static final int REPAINT_DELAY = 50;

	/** This component will be repainted regularly if there are pending rollouts. */
	private final Component component;
	private final Map<Operator, InterpolatedValue> map = new HashMap<Operator, InterpolatedValue>();

	private final Timer timer = new Timer(REPAINT_DELAY, new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			fireRepaint();
		}
	});

	private static class InterpolatedValue {

		private final long startTime = System.currentTimeMillis();
		/**
		 * True if interpolating from 0 to {@link #extensionValue} and false if interpolating from
		 * {@link #extensionValue} to 0.
		 */
		private final boolean up;
		private double extensionValue = 1d;

		private InterpolatedValue(boolean up, double extensionValue) {
			this.up = up;
			this.extensionValue = extensionValue;
		}

		/** Returns the interpolated value at this point of time. */
		private double getValue() {
			long now = System.currentTimeMillis();
			if (up) {
				if (now < startTime + DELAY) {
					return 0d;
				} else if (now > startTime + DELAY + INTERVAL) {
					return extensionValue;
				} else {
					return extensionValue * damp(((double) now - (double) startTime - DELAY) / INTERVAL);
				}
			} else {
				if (now < startTime) {
					return extensionValue;
				} else if (now > startTime + INTERVAL) {
					return 0d;
				} else {
					return extensionValue - extensionValue * damp(((double) now - (double) startTime) / INTERVAL);
				}
			}
		}

		/** Returns true iff time is up. */
		private boolean isDone(long now) {
			return !up && now > startTime + INTERVAL;
		}

		/** Rescales the interpolated value to make it smoother. */
		private double damp(double value) {
			return Math.sqrt(value);
		}
	}

	public InterpolationMap(Component component) {
		this.component = component;
		timer.setRepeats(false);
	}

	protected void fireRepaint() {
		component.repaint();
		cleanUp();
		if (!map.isEmpty() && !timer.isRunning()) {
			timer.start();
		}
	}

	/** Starts interpolating the extension value for the given operator. */
	public void rollOut(Operator operator) {
		map.put(operator, new InterpolatedValue(true, 1d));
		if (!timer.isRunning()) {
			timer.start();
		}
	}

	/**
	 * Starts interpolating the extension value for the given operator in the opposite direction,
	 * starting with the current value.
	 */
	public void rollIn(Operator operator) {
		map.put(operator, new InterpolatedValue(false, getValue(operator)));
		if (!timer.isRunning()) {
			timer.start();
		}
	}

	/** Returns the current interpolated value for the given operator. */
	public double getValue(Operator op) {
		InterpolatedValue val = map.get(op);
		if (val == null) {
			return 0d;
		} else {
			double value = val.getValue();
			return value;
		}
	}

	/** Removes unused ({@link InterpolatedValue#isDone(long)}) values. */
	private void cleanUp() {
		long now = System.currentTimeMillis();
		Iterator<Map.Entry<Operator, InterpolatedValue>> i = map.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<Operator, InterpolatedValue> entry = i.next();
			if (entry.getValue().isDone(now)) {
				i.remove();
			}
		}
	}

	public void clear() {
		map.clear();
	}
}
