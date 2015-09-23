/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.template.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;


/**
 * Component to show a rotating progress indicator whenever visible.
 *
 * @author Simon Fischer
 *
 */
public class ProgressIndicator extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int NUMBER_OF_BALLS = 20;
	private static final int LOOP_TIME = 1200;
	private static final Color BACKGROUND = Color.WHITE;

	private long startTime;

	private boolean showing = true;
	private Object updateLock = new Object();
	private boolean abortUpdateThread = false;
	private Thread updateThread;

	public ProgressIndicator() {

		this.startTime = System.currentTimeMillis();
		setBackground(BACKGROUND);
		addAncestorListener(new AncestorListener() {

			@Override
			public void ancestorRemoved(AncestorEvent event) {
				pauseAnimation();
			}

			@Override
			public void ancestorMoved(AncestorEvent event) {}

			@Override
			public void ancestorAdded(AncestorEvent event) {
				resumeAnimation();
			}
		});

	}

	@Override
	public void removeNotify() {
		super.removeNotify();
		terminateAnimation();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHints(ProcessDrawer.HI_QUALITY_HINTS);
		int offset = (int) ((System.currentTimeMillis() - startTime) * NUMBER_OF_BALLS / LOOP_TIME);
		for (int i = 0; i < NUMBER_OF_BALLS; i++) {
			int val = Math.min(255, 160 + i * 127 / NUMBER_OF_BALLS);
			g2d.setColor(new Color(val, val, val));
			int index = (-offset + i) % NUMBER_OF_BALLS;
			double angle = Math.PI * 2 * index / NUMBER_OF_BALLS;
			int size = Math.min(getWidth(), getHeight());
			double x = Math.sin(angle) * size * 0.4 + getWidth() / 2;
			double y = Math.cos(angle) * size * 0.4 + getHeight() / 2;
			double bs = size / 12;
			g.fillArc((int) (x - bs / 2), (int) (y - bs / 2), (int) bs, (int) bs, 0, 360);
		}
	}

	private void resumeAnimation() {
		showing = true;
		if (updateThread == null) {
			abortUpdateThread = false;
			startAnimation();
		}
		synchronized (updateLock) {
			updateLock.notifyAll();
		}
	}

	private void pauseAnimation() {
		showing = false;
		synchronized (updateLock) {
			updateLock.notifyAll();
		}
	}

	private void terminateAnimation() {
		abortUpdateThread = true;
		updateThread = null;
	}

	private void startAnimation() {
		abortUpdateThread = false;
		updateThread = new Thread("progress-indicator-updater") {

			@Override
			public void run() {
				while (!abortUpdateThread) {
					if (!showing) {
						try {
							synchronized (updateLock) {
								updateLock.wait();
							}
						} catch (InterruptedException e) {
						}
					}
					// safe to call from this thread
					repaint();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
				}
			};
		};
		updateThread.start();
	}
}
