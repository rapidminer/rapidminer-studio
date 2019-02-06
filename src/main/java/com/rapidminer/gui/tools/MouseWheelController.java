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

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Objects;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;


/**
 * This class allows you to control the units scrolled for each mouse wheel rotation relative to
 * the unit increment value of the {@link JScrollBar}. Specifying a scroll amount of 1, is equivalent to clicking
 * the unit scroll button of the scroll bar once.
 *
 * @author Rob Camick, Jan Czogalla
 * @see <a href="https://tips4java.wordpress.com/2010/01/10/mouse-wheel-controller/">Mouse Wheel Controller</a>
 * @see <a href="https://tips4java.wordpress.com/about/">Licensing</a>
 * @since 8.2
 */
public class MouseWheelController implements MouseWheelListener {
	private JScrollPane scrollPane;
	private int scrollAmount = 0;
	private MouseWheelListener[] realListeners;

	/**
	 * Convenience constructor to create the class with a scroll amount of 1.
	 *
	 * @param scrollPane
	 * 		the scroll pane being used by the mouse wheel
	 */
	public MouseWheelController(JScrollPane scrollPane) {
		this(scrollPane, 1);
	}

	/**
	 * Create the class with the specified scroll amount.
	 *
	 * @param scrollAmount
	 * 		the scroll amount to by used for this scroll pane
	 * @param scrollPane
	 * 		the scroll pane being used by the mouse wheel
	 */
	public MouseWheelController(JScrollPane scrollPane, int scrollAmount) {
		this.scrollPane = Objects.requireNonNull(scrollPane);
		setScrollAmount(scrollAmount);
		install();
	}

	/**
	 * @returns the scroll amount.
	 */
	public int getScrollAmount() {
		return scrollAmount;
	}

	/**
	 * Set the scroll amount. Controls the amount the {@link JScrollPane} will scroll for each mouse wheel rotation.
	 * The amount is relative to the unit increment value of the scrollbar being scrolled.
	 *
	 * @param scrollAmount
	 * 		an integer value. A value of zero will use the
	 * 		default scroll amount for your OS.
	 */
	public void setScrollAmount(int scrollAmount) {
		this.scrollAmount = scrollAmount;
	}

	/**
	 * Install this class as the default {@link MouseWheelListener} for {@link MouseWheelEvent MouseWheelEvents}.
	 *
	 * Original listeners will be moved to the realListeners. Can be undone via {@link MouseWheelController#uninstall()}.
	 */
	public void install() {
		if (realListeners != null) {
			return;
		}

		//  Keep track of original listeners so we can use them to redispatch an altered MouseWheelEvent
		realListeners = scrollPane.getMouseWheelListeners();

		for (MouseWheelListener mwl : realListeners) {
			scrollPane.removeMouseWheelListener(mwl);
		}

		//  Intercept events so they can be redispatched
		scrollPane.addMouseWheelListener(this);
	}

	/**
	 * Remove the class as the default {@link MouseWheelListener} and reinstall the original listeners.
	 */
	public void uninstall() {
		if (realListeners == null) {
			return;
		}

		//  Remove this class as the default listener
		scrollPane.removeMouseWheelListener(this);

		//  Install the default listeners
		for (MouseWheelListener mwl : realListeners) {
			scrollPane.addMouseWheelListener(mwl);
		}

		realListeners = null;
	}

	/**
	 * Redispatch a {@link MouseWheelEvent} to the real {@link MouseWheelListener MouseWheelListeners}
	 */
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		//  Create an altered event to redispatch
		if (scrollAmount != 0) {
			e = createScrollAmountEvent(e);
		}

		//  Redispatch the event to original MouseWheelListener
		for (MouseWheelListener mwl : realListeners) {
			mwl.mouseWheelMoved(e);
		}
	}

	private MouseWheelEvent createScrollAmountEvent(MouseWheelEvent e) {
		//  Reset the scroll amount
		return new MouseWheelEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiers(),
				e.getX(), e.getY(), e.getXOnScreen(), e.getYOnScreen(),
				e.getClickCount(), e.isPopupTrigger(), e.getScrollType(), scrollAmount, e.getWheelRotation());
	}
}
