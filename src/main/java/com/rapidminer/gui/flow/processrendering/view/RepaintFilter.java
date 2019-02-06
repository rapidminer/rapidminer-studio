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
package com.rapidminer.gui.flow.processrendering.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Timer;


/**
 * Filter for the repaints of the {@link ProcessRendererView}. Checks every view milliseconds if a
 * repaint request came in and does the repaint if the last repaint was a certain time ago.
 *
 * @author Gisa Schaefer
 * @since 7.1.0
 */
class RepaintFilter {

	/** the minimal interval between repaints (in milliseconds) */
	private static final int REPAINT_INTERVAL = 30;

	/**
	 * the timer interval (in milliseconds) which determines the maximal waiting time for requests
	 * when none have happened for some time
	 */
	private static final int TIMER_INTERVAL = 10;

	/** the number which the counter should reach for a repaint */
	private static final int COUNTER_BARRIER = REPAINT_INTERVAL / TIMER_INTERVAL;

	private long counter = 0;
	private AtomicBoolean repaintRequested = new AtomicBoolean(false);

	RepaintFilter(final ProcessRendererView view) {
		ActionListener repaintListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				counter++;

				if (repaintRequested.get() && counter >= COUNTER_BARRIER) {
					counter = 0;
					repaintRequested.set(false);
					view.doRepaint();
				}
			}

		};

		Timer timer = new Timer(TIMER_INTERVAL, repaintListener);
		timer.start();
	}

	/**
	 * Requests a repaint for the next possible slot. If there is already a repaint waiting, the
	 * request is discarded.
	 */
	void requestRepaint() {
		repaintRequested.set(true);
	}

}
