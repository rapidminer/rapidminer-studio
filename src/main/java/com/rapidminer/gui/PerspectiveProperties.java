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
package com.rapidminer.gui;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.processeditor.results.ResultTab;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockableState;
import com.vlsolutions.swing.docking.DockingUtilities;
import com.vlsolutions.swing.docking.TabbedDockableContainer;


/**
 *
 * This class saves and restores certain properties associated with a specific {@link Perspective}.
 * Currently, this class is used to restore the selected tabs and the scroll positions of
 * {@link JScrollPane}s for a specific {@link Perspective} after a perspective switch occurred.
 *
 * @author Dominik Halfkann
 */
public class PerspectiveProperties {

	/**
	 * Try to reposition scroll bars at most the specified number of times (repositioning might fail
	 * due to asynchronous code in the docking framework).
	 */
	private static final int POSITION_SCROLL_BARS_MAX_RETRIES = 5;

	/** Time to wait in between scroll bar repositioning attempts. */
	private static final int POSITION_SCROLL_BARS_WAIT_PERIOD = 50;

	private List<Dockable> focusedDockables = new ArrayList<>();

	private Map<String, ScrollBarsPosition> scrollBarsPositions = new HashMap<>();

	private class ScrollBarsPosition {

		private final int vertical;
		private final int horizontal;

		public ScrollBarsPosition(int vertical, int horizontal) {
			this.vertical = vertical;
			this.horizontal = horizontal;
		}

		public int getVertical() {
			return vertical;
		}

		public int getHorizontal() {
			return horizontal;
		}
	}

	/**
	 * This method stores certain properties about the current {@link Perspective}.
	 */
	public void store() {
		storeFocusedDockables();
		storeScrollBarPositions();
	}

	/**
	 * This method applies properties to the current {@link Perspective} which were previously
	 * stored by {@link #store()}.
	 */
	public void apply() {
		applyFocusedDockables();
		applyScrollBarPositions();
	}

	/**
	 * Sets in the focused {@link ResultTab} to the given dockable. If no previously focused result tab existed, does
	 * nothing.
	 *
	 * @param newResultTabDockable
	 * 		the {@link ResultTab} dockable
	 * @since 8.2.2
	 */
	void setNewFocusedResultTab(Dockable newResultTabDockable) {
		if (!(newResultTabDockable instanceof ResultTab)) {
			throw new IllegalArgumentException("newResultTabDockable must be a ResultTab!");
		}

		// find previous result tab that was focused
		Dockable previouslySelectedResultDockable = null;
		for (Dockable existingDockable : focusedDockables) {
			if (existingDockable instanceof ResultTab) {
				previouslySelectedResultDockable = existingDockable;
				break;
			}
		}

		if (previouslySelectedResultDockable != null) {
			focusedDockables.remove(previouslySelectedResultDockable);
		}
		focusedDockables.add(newResultTabDockable);
	}

	/**
	 * This method saves all tabs which are currently selected/visible.
	 */
	private void storeFocusedDockables() {
		MainFrame mainFrame = RapidMinerGUI.getMainFrame();
		if (mainFrame != null) {
			focusedDockables = new ArrayList<>();
			DockableState[] states = mainFrame.getDockingDesktop().getContext().getDockables();
			List<TabbedDockableContainer> memorizedContainer = new ArrayList<>();
			for (DockableState state : states) {
				TabbedDockableContainer container = DockingUtilities.findTabbedDockableContainer(state.getDockable());
				if (container != null) {
					if (!memorizedContainer.contains(container)) {
						focusedDockables.add(container.getSelectedDockable());
						memorizedContainer.add(container);
					}
				}
			}
		}
	}

	/**
	 * This method will make all tabs visible which were previously saved by
	 * {@link #storeFocusedDockables()}.
	 */
	private void applyFocusedDockables() {
		for (Dockable dockable : focusedDockables) {
			TabbedDockableContainer tabbedContainer = DockingUtilities.findTabbedDockableContainer(dockable);
			if (tabbedContainer != null) {
				tabbedContainer.setSelectedDockable(dockable);
			}
		}
	}

	/**
	 * This method saves scroll positions of all {@link JScrollPane}s in the current Perspective.
	 */
	private void storeScrollBarPositions() {
		MainFrame mainFrame = RapidMinerGUI.getMainFrame();
		if (mainFrame != null) {
			DockableState[] states = mainFrame.getDockingDesktop().getContext().getDockables();
			for (DockableState state : states) {
				Dockable dockable = state.getDockable();
				if (dockable.getComponent() instanceof Container) {
					JScrollPane scrollPane = findScrollPane((Container) dockable.getComponent());
					if (scrollPane != null) {
						ScrollBarsPosition scrollBarsPosition = new ScrollBarsPosition(
								scrollPane.getVerticalScrollBar().getValue(),
								scrollPane.getHorizontalScrollBar().getValue());
						scrollBarsPositions.put(dockable.getDockKey().getKey(), scrollBarsPosition);
					}
				}
			}
		}
	}

	/**
	 * This method will apply scroll positions to all {@link JScrollPane}s which were previously
	 * saved by {@link #storeScrollBarPositions()}
	 */
	private void applyScrollBarPositions() {
		for (final Entry<String, ScrollBarsPosition> scrollBarsPosition : scrollBarsPositions.entrySet()) {
			final Dockable dockable = RapidMinerGUI.getMainFrame().getDockingDesktop().getContext()
					.getDockableByKey(scrollBarsPosition.getKey());
			if (dockable.getComponent() instanceof Container) {
				final JScrollPane scrollPane = findScrollPane((Container) dockable.getComponent());
				if (scrollPane != null) {
					// I'm testing after 50ms if the scrollbars have been set correctly.
					// This is due to the vldocking which seems to resets all windows asynchronously
					// and can reset the scrollbars after they have been set by this method.
					// A simple SwingUtilities.invokeLater() won't do in this case.
					new Thread(new Runnable() {

						@Override
						public void run() {
							final JScrollBar vScrollBar = scrollPane.getVerticalScrollBar();
							final JScrollBar hScrollBar = scrollPane.getHorizontalScrollBar();

							/** Tries reposition scroll bars to stored positions. */
							Runnable scrollBarUpdater = new Runnable() {

								@Override
								public void run() {
									vScrollBar.setValue(scrollBarsPosition.getValue().getVertical());
									hScrollBar.setValue(scrollBarsPosition.getValue().getHorizontal());
								}
							};

							int maxIterations = POSITION_SCROLL_BARS_MAX_RETRIES;
							int i = 0;
							do {
								try {
									SwingUtilities.invokeAndWait(scrollBarUpdater);
									Thread.sleep(POSITION_SCROLL_BARS_WAIT_PERIOD);
								} catch (InterruptedException | InvocationTargetException e) {
									// ignore
								}
								i++;
							} while (i < maxIterations
									&& (vScrollBar.getValue() != scrollBarsPosition.getValue().getVertical()
											|| hScrollBar.getValue() != scrollBarsPosition.getValue().getHorizontal()));
						}
					}).start();
				}
			}
		}
	}

	/**
	 * Utility method that searches for a {@link JScrollPane} starting from the provided Container
	 * and returning the first occurrence.
	 */
	private JScrollPane findScrollPane(Container parent) {
		if (parent != null) {
			for (Component child : parent.getComponents()) {
				if (child instanceof JScrollPane) {
					return (JScrollPane) child;
				} else if (child instanceof Container) {
					JScrollPane scrollPane = findScrollPane((Container) child);
					if (scrollPane != null) {
						return scrollPane;
					}
				}
			}
		}
		return null;
	}

}
