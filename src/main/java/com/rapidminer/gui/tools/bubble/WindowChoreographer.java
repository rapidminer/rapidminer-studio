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
package com.rapidminer.gui.tools.bubble;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.tools.SystemInfoUtilities;


/**
 * The BrowserBubble Choreographer
 * <ul>
 * <li>Displays windows under each other</li>
 * <li>Reuses free spaces after Windows are closed</li>
 * </ul>
 *
 * @author Jonas Wilms-Pfau
 * @since 7.5.0
 *
 */
public class WindowChoreographer {

	/**
	 * ArrayList that does not explode, if you try to set the next value
	 *
	 */
	private static class SetOrAddArrayList<E> extends ArrayList<E> {

		private static final long serialVersionUID = 1L;

		/**
		 * <p>
		 * Also allow to set the next free value.
		 * </p>
		 *
		 * {@inheritDoc}
		 */
		@Override
		public E set(int index, E element) {
			E result = null;
			if (this.size() == index) {
				add(element);
			} else {
				result = super.set(index, element);
			}
			return result;
		}
	}

	private class CloseListener extends WindowAdapter {

		@Override
		public void windowClosed(WindowEvent e) {
			Window closedWindow = e.getWindow();
			Integer freePosition = windowPosition.remove(closedWindow);
			closedWindow.removeWindowListener(closeListener);
			// Mark as free
			if (freePosition != null) {
				freeSpaces.add(freePosition);
				cleanUp();
			}
		}

	}

	/** Simple dialogs if transparency is not supported */
	private static final boolean MODERN_UI = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
			.getDefaultConfiguration().isTranslucencyCapable();

	/** The default margin to the top */
	private static final int DEFAULT_TOP_MARGIN = 50;

	/**
	 * In transparent mode the window cares about the border, since it has to paint the shadow and
	 * the animation, in Window mode it's defined here
	 */
	private static final int DEFAULT_RIGHT_MARGIN = MODERN_UI ? 0 : 100;

	/** In dialog mode we have to add the margin */
	private static final int DEFAULT_BOTTOM_MARGIN = MODERN_UI ? 0 : 50;

	/** Position of each window */
	private Map<Window, Integer> windowPosition = new ConcurrentHashMap<>();

	private NavigableSet<Integer> freeSpaces = new TreeSet<>();

	/** MaxY Coordinate of each Window */
	private List<Integer> windowYOffset = new SetOrAddArrayList<>();

	/** Relative to parent */
	private Component parent;

	/** Reserve bubbles, if the Screen is already full of bubbles */
	private LinkedList<Window> bubbleStack = new LinkedList<>();

	/** The close listener */
	private final WindowListener closeListener = new CloseListener();

	/**
	 * Creates a new WindowChoreographer relative to the MainFrame, with a margin of
	 * {@value #DEFAULT_TOP_MARGIN} to the top
	 */
	public WindowChoreographer() {
		this(RapidMinerGUI.getMainFrame().getContentPane(), DEFAULT_TOP_MARGIN);
	}

	/**
	 * Creates a new WindowChoreographer
	 *
	 * @param parent
	 *            Relative to this Component
	 * @param yOffset
	 *            The initial yOffset
	 */
	public WindowChoreographer(Component parent, int yOffset) {
		this.parent = parent;
		// Initial position
		windowYOffset.set(0, yOffset);

		// Follow the parent window, but only if not in dialog mode
		if (MODERN_UI) {
			// The content pane has no move event
			SwingUtilities.getRoot(parent).addComponentListener(new ComponentAdapter() {

				@Override
				public void componentResized(ComponentEvent e) {
					recalculateWindowPositions();
					cleanUp();
				}

				@Override
				public void componentMoved(ComponentEvent e) {
					recalculateWindowPositions();
					// Double click the taskbar only causes a componentMoved event
					cleanUp();
				}

			});

			// macOS requires a window state changed listener for minimize / maximize
			if (SystemInfoUtilities.getOperatingSystem() == SystemInfoUtilities.OperatingSystem.OSX) {
				SwingUtilities.getWindowAncestor(parent).addWindowStateListener(e -> {
					recalculateWindowPositions();
					cleanUp();
				});
			}
		}
	}

	/**
	 * Adds a Window into the next free position
	 *
	 * @param window
	 *            The window
	 * @return true if the Window could be displayed immediately
	 */
	public synchronized boolean addWindow(Window window) {
		if (window == null) {
			throw new IllegalArgumentException("window must not be null!");
		}
		// Don't trigger in iconified mode
		if (RapidMinerGUI.getMainFrame().getExtendedState() == Frame.ICONIFIED) {
			bubbleStack.add(window);
			return false;
		}

		int pos = getNextPosition(window);
		int yOffset = windowYOffset.get(pos - 1);
		if (!fitsScreen(window, yOffset)) {
			// Lets store the bubble for later
			bubbleStack.add(window);
			return false;
		}
		// Allow a window only one time
		if (windowPosition.containsKey(window)) {
			return false;
		}
		// Remember the position of the window
		windowPosition.put(window, pos);
		// Great job Java there are two remove methods
		freeSpaces.remove(pos);

		// Remember size if it's a new window
		if (pos >= windowYOffset.size()) {
			this.windowYOffset.set(pos, yOffset + window.getHeight() + DEFAULT_BOTTOM_MARGIN);
		}
		window.addWindowListener(closeListener);
		window.setVisible(true);
		recalculateWindowPosition(window, pos);
		return true;
	}

	/**
	 * Follow the parent Component position
	 */
	private void recalculateWindowPositions() {
		windowPosition.forEach(this::recalculateWindowPosition);
	}

	/**
	 * Recalculate the Window position
	 *
	 * @param window the window
	 * @param position the position of the window
	 */
	private void recalculateWindowPosition(Window window, int position) {
		Rectangle parentBounds = parent.getBounds();
		parentBounds.setLocation(parent.getLocationOnScreen());
		int rightX = (int) (parentBounds.getX() + parent.getWidth() - DEFAULT_RIGHT_MARGIN);
		// this was going crazy sometimes
		int topY = (int) parentBounds.getY();
		int yOffset = windowYOffset.get(position);
		// Recalculate the window positions
		window.setLocation(rightX - window.getWidth(), topY + yOffset - window.getHeight());
		// Check if the Window fits into the parents bounds
		if (!parentBounds.contains(window.getBounds())) {
			// back into the bubbleStack
			window.setVisible(false);
			bubbleStack.addFirst(window);
			freeSpaces.add(windowPosition.remove(window));
			window.removeWindowListener(closeListener);
		}
	}

	/**
	 * Returns the next position that fits the window
	 *
	 * @param w the window
	 * @return the next free position
	 */
	private int getNextPosition(Window w) {
		if (!freeSpaces.isEmpty()) {
			Integer pos = freeSpaces.first();
			while (pos != null) {
				int start = windowYOffset.get(pos);
				int end = pos + 2 < windowYOffset.size() ? windowYOffset.get(pos + 1) : parent.getHeight();
				if (w.getHeight() <= end - start) {
					return pos;
				}
				pos = freeSpaces.higher(pos);
			}
		}
		return windowYOffset.size();
	}

	/**
	 * Check if there is enough space for the Window
	 *
	 * @param w
	 *            The window to insert
	 * @param yOffset
	 *            The start offset of the Window
	 * @return true if it fits on the screen
	 */
	private boolean fitsScreen(Window w, int yOffset) {
		return yOffset + w.getHeight() <= parent.getHeight() && parent.getWidth() >= w.getWidth() + DEFAULT_RIGHT_MARGIN;
	}

	/**
	 * Cleans up empty spaces at the end and show new bubbles for the user
	 *
	 */
	private synchronized void cleanUp() {
		// Removes free spaces from the end
		if (!freeSpaces.isEmpty()) {
			Integer free = freeSpaces.last();
			while (free != null) {
				if (free + 1 >= windowYOffset.size()) {
					// End of the list, remove by index NOT value
					int index = free;
					windowYOffset.remove(index);
					freeSpaces.remove(free);
					free = freeSpaces.lower(free);
				} else {
					break;
				}
			}
		}
		// display waiting bubbles now that we have free space again
		boolean enoughSpace = true;
		int initialStackSize = bubbleStack.size();
		for (int count = 0; count < initialStackSize && enoughSpace; count++) {
			enoughSpace = addWindow(bubbleStack.pop());
		}
	}

}
