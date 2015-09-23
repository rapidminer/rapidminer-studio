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
package com.rapidminer.gui.tour;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tour.BubbleWindow.AlignedSide;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.DockableState;
import com.vlsolutions.swing.docking.DockingContext;
import com.vlsolutions.swing.docking.DockingDesktop;
import com.vlsolutions.swing.docking.event.DockableSelectionEvent;
import com.vlsolutions.swing.docking.event.DockableSelectionListener;
import com.vlsolutions.swing.docking.event.DockableStateChangeEvent;
import com.vlsolutions.swing.docking.event.DockableStateChangeListener;
import com.vlsolutions.swing.docking.event.DockingActionEvent;
import com.vlsolutions.swing.docking.event.DockingActionListener;

import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.SwingUtilities;


/**
 * This Step only appears in two cases. If the {@link Dockable} which must be given by ID is not
 * showing or not on the screen.
 * 
 * @author Thilo Kamradt
 * 
 */
public class NotShowingStep extends Step {

	private boolean showMe = false;
	private boolean hidden = false;
	int mode;
	private Window owner = RapidMinerGUI.getMainFrame();
	private String dockableKey;
	private String notOnScreen = "lostDockable";
	private String notShowing = "not_showing";
	private Dockable dockable;
	private HierarchyListener hierachyListener;
	private DockableStateChangeListener dockListener;
	private DockingDesktop desktop = RapidMinerGUI.getMainFrame().getDockingDesktop();
	private DockingContext context = RapidMinerGUI.getMainFrame().getDockableMenu().getDockingContext();
	private DockableSelectionListener dockSelectListener;
	private DockingActionListener dockingActionListener;

	public NotShowingStep(String dockableKey) {
		this.dockableKey = dockableKey;
		this.dockable = context.getDockableByKey(dockableKey);
	}

	@Override
	boolean createBubble() {
		this.mode = BubbleWindow.isDockableOnScreen(dockableKey);
		this.hidden = desktop.getDockableState(context.getDockableByKey(dockableKey)).isHidden();
		switch (mode) {
			case BubbleWindow.OBJECT_NOT_ON_SCREEN:
				// the dockable is not on screen and must be added before the user can continue the
				// Tour
				bubble = new DockableBubble(owner, AlignedSide.MIDDLE, notOnScreen, null, dockable.getDockKey().getName());
				dockListener = new DockableStateChangeListener() {

					@Override
					public void dockableStateChanged(DockableStateChangeEvent changed) {
						if (changed.getNewState().getDockable().getDockKey().getKey().equals(dockableKey)
								&& !changed.getNewState().isClosed()) {
							NotShowingStep.this.conditionComplied();
						}
					}
				};
				desktop.addDockableStateChangeListener(dockListener);
				dockSelectListener = new DockableSelectionListener() {

					@Override
					public void selectionChanged(DockableSelectionEvent arg0) {
						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {
								if (dockable.getComponent().isShowing()) {
									NotShowingStep.this.conditionComplied();
								}
							}
						});
					}
				};
				desktop.addDockableSelectionListener(dockSelectListener);
				showMe = true;
				break;
			case BubbleWindow.OBJECT_NOT_SHOWING:
				// the Dockable is on screen but not viewable, the user needs to put it in the
				// foreground
				if (hidden) {
					// Dockable is pinned to the side. the user needs to unpin the Dockable to
					// continue
					bubble = new DockableBubble(owner, AlignedSide.RIGHT, "hiddenDockable", dockableKey, dockable
							.getDockKey().getName());
					dockingActionListener = new DockingActionListener() {

						@Override
						public void dockingActionPerformed(DockingActionEvent arg0) {
							if (!desktop.getDockableState(dockable).isHidden()) {
								NotShowingStep.this.conditionComplied();
							}
						}

						@Override
						public boolean acceptDockingAction(DockingActionEvent arg0) {
							// no need to deny anything
							return true;
						}
					};
					desktop.addDockingActionListener(dockingActionListener);
					hierachyListener = new HierarchyListener() {

						@Override
						public void hierarchyChanged(HierarchyEvent e) {
							SwingUtilities.invokeLater(new Runnable() {

								@Override
								public void run() {
									DockableState state = desktop.getDockableState(dockable);
									if (state != null && state.isHidden()) {
										bubble.paint(false);
									} else if (state != null) {
										// this case means that the user has restored the
										// perspective
										NotShowingStep.this.conditionComplied();
									}
								}
							});
						}
					};
					dockable.getComponent().addHierarchyListener(hierachyListener);
					showMe = true;
					break;
				} else {
					bubble = new DockableBubble(owner, AlignedSide.RIGHT, notShowing, dockableKey, dockable.getDockKey()
							.getName());
					hierachyListener = new HierarchyListener() {

						@Override
						public void hierarchyChanged(HierarchyEvent e) {
							if (dockable.getComponent().isShowing()) {
								NotShowingStep.this.bubble.triggerFire();
								dockable.getComponent().removeHierarchyListener(hierachyListener);
							}
						}
					};
					dockable.getComponent().addHierarchyListener(hierachyListener);
				}
				showMe = true;
				break;
			default:
		}
		return showMe;
	}

	@Override
	protected void stepCanceled() {
		if (mode == BubbleWindow.OBJECT_NOT_ON_SCREEN) {
			desktop.removeDockableStateChangeListener(dockListener);
			desktop.removeDockableSelectionListener(dockSelectListener);
		} else if (mode == BubbleWindow.OBJECT_NOT_SHOWING) {
			if (hidden) {
				desktop.removeDockingActionListener(dockingActionListener);
			}
			dockable.getComponent().removeHierarchyListener(hierachyListener);
		}

	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] {};
	}

}
