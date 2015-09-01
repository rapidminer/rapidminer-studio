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

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;


/**
 * Can be started to ensure that a Button is not out of the boundaries of the Desktop.
 * 
 * @author Thilo Kamradt
 * 
 */
public class NotViewableStep extends Step {

	private AlignedSide alignment;
	private Window owner;
	private String i18nKey = "hiddenButton";
	private String dockableKey;
	private Component attachTo;
	private String watch;
	private ComponentListener compListener = null;
	private boolean showMe;

	public NotViewableStep(AlignedSide preferredAlignment, Window owner, String shouldBeViewable, String attachToKey) {
		this.owner = owner;
		this.alignment = preferredAlignment;
		this.watch = shouldBeViewable;
		this.dockableKey = attachToKey;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.gui.tour.Step#createBubble()
	 */
	@Override
	boolean createBubble() {
		this.showMe = BubbleWindow.isButtonOnScreen(watch) == BubbleWindow.OBJECT_NOT_SHOWING;
		if (showMe) {
			bubble = new DockableBubble(owner, alignment, i18nKey, dockableKey);
			attachTo = RapidMinerGUI.getMainFrame().getDockingDesktop().getContext().getDockableByKey(dockableKey)
					.getComponent();
			compListener = new ComponentListener() {

				@Override
				public void componentShown(ComponentEvent e) {
					// we do not need this part
				}

				@Override
				public void componentResized(ComponentEvent e) {
					if (BubbleWindow.isButtonOnScreen(watch) == BubbleWindow.OBJECT_SHOWING_ON_SCREEN) {
						bubble.triggerFire();
						attachTo.removeComponentListener(this);
					}
				}

				@Override
				public void componentMoved(ComponentEvent e) {
					// we do not need this part

				}

				@Override
				public void componentHidden(ComponentEvent e) {
					// we do not need this part

				}
			};
			attachTo.addComponentListener(compListener);
		}
		return showMe;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.gui.tour.Step#stepCanceled()
	 */
	@Override
	protected void stepCanceled() {
		if (compListener != null) {
			attachTo.removeComponentListener(compListener);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.gui.tour.Step#checkPreconditions()
	 */
	@Override
	public Step[] getPreconditions() {
		return new Step[] {};
	}

}
