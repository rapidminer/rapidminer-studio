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

import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;


/**
 * Bubble that attaches to and moves with a simple {@link JComponent}.
 *
 * @author Simon Fischer
 *
 */
public class ComponentBubbleWindow extends BubbleWindow {

	private static final long serialVersionUID = 1L;

	private JComponent component;

	private ComponentListener componentListener;

	private AncestorListener ancestorListener;

	public ComponentBubbleWindow(final JComponent component, Window owner, AlignedSide preferredAlignment, String i18nKey,
			JButton[] buttonsToAdd, Object... arguments) {
		this(component, BubbleStyle.COMIC, owner, preferredAlignment, i18nKey, null, null, false, false, buttonsToAdd,
				arguments);
	}

	public ComponentBubbleWindow(final JComponent component, BubbleStyle style, Window owner, AlignedSide preferredAlignment,
			String i18nKey, Font titleFont, Font bodyFont, boolean moveable, boolean showCloseButton, JButton[] buttonsToAdd,
			Object... arguments) {
		super(owner, style, preferredAlignment, i18nKey, null, titleFont, bodyFont, moveable, showCloseButton, buttonsToAdd,
				arguments);
		this.component = component;
		addComponentListenerTo(component);

		componentListener = new ComponentListener() {

			@Override
			public void componentShown(ComponentEvent e) {}

			@Override
			public void componentResized(ComponentEvent e) {}

			@Override
			public void componentMoved(ComponentEvent e) {
				reposition();
			}

			@Override
			public void componentHidden(ComponentEvent e) {}
		};
		component.addComponentListener(componentListener);

		ancestorListener = new AncestorListener() {

			@Override
			public void ancestorRemoved(AncestorEvent event) {
				killBubble(true);
			}

			@Override
			public void ancestorMoved(AncestorEvent event) {
				reposition();
			}

			@Override
			public void ancestorAdded(AncestorEvent event) {
				setVisible(true);
			}
		};
		component.addAncestorListener(ancestorListener);
		super.paint(false);
	}

	@Override
	protected Point getObjectLocation() {
		return component.getLocationOnScreen();
	}

	@Override
	protected int getObjectWidth() {
		return component.getWidth();
	}

	@Override
	protected int getObjectHeight() {
		return component.getHeight();
	}

	private void reposition() {
		// Hide, e.g. if we scroll away
		if (component.getVisibleRect().getHeight() < 20) {
			setVisible(false);
		} else {
			setVisible(true);
			paintAgain(false);
		}
	}

	@Override
	protected void unregisterSpecificListeners() {
		// nothing to do
	}

	@Override
	protected void registerSpecificListener() {
		// nothing to do
	}

	@Override
	public void killBubble(boolean notifyListeners) {
		component.removeAncestorListener(ancestorListener);
		component.removeComponentListener(componentListener);
		super.killBubble(notifyListeners);
	}

	@Override
	public void dispose() {
		component.removeAncestorListener(ancestorListener);
		component.removeComponentListener(componentListener);
		super.dispose();
	}
}
