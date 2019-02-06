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
package com.rapidminer.gui.tools;

import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.MouseInputAdapter;


/**
 * Utility class to get information about hovering index in a JList ListCellEditor.
 *
 *
 * @author Nils Woehler
 *
 */
public class ListHoverHelper {

	private static class HoverListener extends MouseInputAdapter implements ListDataListener, PropertyChangeListener,
			ComponentListener, HierarchyListener, HierarchyBoundsListener, Runnable {

		private JList<?> list;
		private int hoverIndex;
		private boolean enabled, running;

		private Point lastLocation;

		public HoverListener(JList<?> l) {
			list = l;
			hoverIndex = -1;
		}

		public void setEnabled(boolean value) {
			if (enabled == value) {
				return;
			}

			enabled = value;

			if (enabled) {
				list.addMouseListener(this);
				list.addMouseMotionListener(this);
				list.addPropertyChangeListener(this);
				list.getModel().addListDataListener(this);

				list.addComponentListener(this);
				list.addHierarchyListener(this);
				list.addHierarchyBoundsListener(this);

				list.putClientProperty(HOVER, this);
			} else {
				repaint(hoverIndex());

				list.removeMouseListener(this);
				list.removeMouseMotionListener(this);
				list.removePropertyChangeListener(this);
				list.getModel().removeListDataListener(this);

				list.removeHierarchyBoundsListener(this);
				list.removeHierarchyListener(this);
				list.removeComponentListener(this);

				list.putClientProperty(HOVER, null);
			}
		}

		public int hoverIndex() {
			return hoverIndex;
		}

		private void setHoverIndex(int value) {
			hoverIndex = value;
		}

		private void repaint(int index) {
			if (index != -1) {
				list.repaint(list.getCellBounds(index, index));
			}
		}

		private Point toScreen(Point p) {
			p = new Point(p);
			SwingUtilities.convertPointToScreen(p, list);
			return p;
		}

		private Point toList(Point p) {
			if (!list.isShowing()) {
				return null;
			}

			Point s = list.getLocationOnScreen();

			s.x = p.x - s.x;
			s.y = p.y - s.y;

			return s;
		}

		// p is in screen coordinate system (or zero, denoting not known or outside)
		private void updateHover(Point p) {
			int h = hoverIndex();

			Point q = p == null ? null : toList(p);

			int newHoverIndex = p == null ? -1 : list.locationToIndex(q);

			if (h != newHoverIndex) {
				repaint(h);
				repaint(newHoverIndex);

				setHoverIndex(newHoverIndex);
			}

			lastLocation = p;
		}

		// updateLater is used to make sure that the cell bounds are already updated.
		// this may have problems if called initially not from the event-dispatch thread
		private void updateLater() {
			if (!running) {
				running = true;
				EventQueue.invokeLater(this);
			}
		}

		@Override
		public void run() {
			running = false;

			updateHover(lastLocation);
		}

		@Override
		public void propertyChange(PropertyChangeEvent e) {
			String name = e.getPropertyName();

			if (name.equals("model")) {
				((ListModel<?>) e.getOldValue()).removeListDataListener(this);
				((ListModel<?>) e.getNewValue()).addListDataListener(this);

				setHoverIndex(-1);
				updateLater();
			} else if (name.equals("font") || name.equals("cellRenderer") || name.equals("fixedRowWidth")
					|| name.equals("fixedRowHeight") || name.equals("prototypeCellValue")
					|| name.equals("layoutOrientation")) {

				// very unspecific
				list.repaint();

				updateLater();
			}
		}

		@Override
		public void hierarchyChanged(HierarchyEvent e) {
			if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
				if (!list.isShowing()) {
					lastLocation = null;
					setHoverIndex(-1);
				}
				// else: nothing to be done as mouse location is not known (only 1.5)
			}
		}

		@Override
		public void componentShown(ComponentEvent e) {}

		@Override
		public void componentHidden(ComponentEvent e) {
			// handled by hierarchyChanged
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			updateHover(lastLocation);
		}

		@Override
		public void ancestorMoved(HierarchyEvent e) {
			updateHover(lastLocation);
		}

		@Override
		public void componentResized(ComponentEvent e) {
			updateLater();
		}

		@Override
		public void ancestorResized(HierarchyEvent e) {
			updateLater();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			updateHover(toScreen(e.getPoint()));
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			updateHover(toScreen(e.getPoint()));
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			updateHover(toScreen(e.getPoint()));
		}

		@Override
		public void mouseExited(MouseEvent e) {
			updateHover(null);
		}

		// These implementations are quite unspecific.
		@Override
		public void intervalAdded(ListDataEvent e) {
			if (hoverIndex() >= e.getIndex0()) {
				setHoverIndex(-1);
			}

			updateLater();
		}

		@Override
		public void intervalRemoved(ListDataEvent e) {
			if (hoverIndex() >= e.getIndex0()) {
				setHoverIndex(-1);
			}

			updateLater();
		}

		@Override
		public void contentsChanged(ListDataEvent e) {
			updateLater();
		}
	}

	private static Object HOVER = "xxx.Hover";

	public static int index(JList<?> l) {
		HoverListener h = (HoverListener) l.getClientProperty(HOVER);

		return h == null ? -1 : h.hoverIndex();
	}

	public static void install(JList<?> l) {
		if (l.getClientProperty(HOVER) == null) {
			HoverListener h = new HoverListener(l);

			h.setEnabled(true);
		}
	}

	public static void uninstall(JList<?> l) {
		if (l != null) {
			HoverListener h = (HoverListener) l.getClientProperty(HOVER);

			h.setEnabled(false);
		}
	}

}
