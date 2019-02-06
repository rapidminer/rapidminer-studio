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
package com.rapidminer.gui.flow;

import java.awt.AWTEvent;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


/**
 * This class can be used to add panning support to any {@link JComponent}.
 *
 * @author Simon Fischer
 */
public class PanningManager implements AWTEventListener {

	private static final int PAN_DELAY = 50;
	private static final int PAN_STEP_SIZE = 20;

	private JComponent target;

	private Point mouseOnScreenPoint;

	private Timer timer = new Timer(PAN_DELAY, new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			scrollNow();
		}
	});

	public PanningManager(JComponent target) {
		super();
		this.target = target;
		timer.setRepeats(true);
		timer.start();
		Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
	}

	@Override
	public void eventDispatched(AWTEvent e) {
		if (e instanceof MouseEvent) {
			MouseEvent me = (MouseEvent) e;
			if (!SwingUtilities.isDescendingFrom(me.getComponent(), target)) {
				return;
			}
			if (me.getID() == MouseEvent.MOUSE_RELEASED) {
				// stop when mouse released
				mouseOnScreenPoint = null;
				if (timer.isRunning()) {
					timer.stop();
				}
			} else if (me.getID() == MouseEvent.MOUSE_DRAGGED && me.getComponent() == target) {
				mouseOnScreenPoint = me.getLocationOnScreen();
			} else if (me.getID() == MouseEvent.MOUSE_PRESSED && me.getComponent() == target) {
				mouseOnScreenPoint = me.getLocationOnScreen();
				timer.start();
			}
		}
	}

	/**
	 * Removes the panning support for the component this instance was created for.
	 */
	public void remove() {
		Toolkit.getDefaultToolkit().removeAWTEventListener(this);
		timer.stop();
		this.target = null;
	}

	/**
	 * Start scrolling.
	 */
	private void scrollNow() {
		if (mouseOnScreenPoint != null && target.isShowing()) {
			Point origin = target.getLocationOnScreen();
			Point relative = new Point(mouseOnScreenPoint.x - origin.x, mouseOnScreenPoint.y - origin.y);

			Rectangle visibleRect = target.getVisibleRect();

			if (!visibleRect.contains(relative)) {
				int destX = relative.x;
				if (relative.getX() < visibleRect.getMinX()) {
					destX = (int) visibleRect.getMinX() - PAN_STEP_SIZE;
				}
				if (relative.getX() > visibleRect.getMaxX()) {
					destX = (int) visibleRect.getMaxX() + PAN_STEP_SIZE;
				}

				int destY = relative.y;
				if (relative.getY() < visibleRect.getMinY()) {
					destY = (int) visibleRect.getMinY() - PAN_STEP_SIZE;
				}
				if (relative.getY() > visibleRect.getMaxY()) {
					destY = (int) visibleRect.getMaxY() + PAN_STEP_SIZE;
				}

				target.scrollRectToVisible(new Rectangle(new Point(destX, destY)));
			}
		}
	}
}
