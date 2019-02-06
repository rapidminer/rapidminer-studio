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
package com.rapidminer.gui.actions;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * This class is an extension of the regular Swing {@link MouseAdapter}. The advantage of this class
 * is that it fires its {@link #click(MouseEvent)} method both for a regular click (where the mouse
 * does not move between mousePressed and mouseReleased) and clicks where the mouse is pressed,
 * moved and then released. This solves the irritating issue that only perfectly still clicks count
 * as clicks in Swing.
 * <p>
 * To add a popup menu, simply overwrite {@link #showContextMenu(Point)} which triggers on popup
 * clicks.
 * </p>
 *
 * @author Marco Boeck
 * @since 7.0.0
 *
 */
public abstract class ExtendedMouseClickedAdapter extends MouseAdapter {

	/** detect mouse click + drag + release on component */
	Point armed;

	boolean onlyLeftClick;

	/**
	 * Creates a new adapter which only fires the {@link #click(MouseEvent)} method for left clicks.
	 */
	public ExtendedMouseClickedAdapter() {
		this(true);
	}

	/**
	 * Creates a new adapter which fires the action either for all clicks or only for left clicks.
	 *
	 * @param onlyLeftClick
	 *            if {@code true}, only left clicks will trigger the {@link #click(MouseEvent)}
	 *            method
	 */
	public ExtendedMouseClickedAdapter(boolean onlyLeftClick) {
		this.onlyLeftClick = onlyLeftClick;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.isPopupTrigger()) {
			showContextMenu(e.getPoint());
			armed = null;
			return;
		}

		armed = e.getPoint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger()) {
			showContextMenu(e.getPoint());
			armed = null;
			return;
		}

		if (armed != null && !armed.equals(e.getPoint())) {
			click(e);
			armed = null;
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		armed = null;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.isPopupTrigger()) {
			showContextMenu(e.getPoint());
		} else {
			click(e);
		}
		armed = null;
	}

	/**
	 * This method is called when a mouse click occured on the listened ocmponent.
	 *
	 * @param e
	 *            the mouse event which triggered the click
	 */
	public abstract void click(MouseEvent e);

	/**
	 * This method is called when a popup trigger click occured on the listened ocmponent.
	 *
	 * @param point
	 *            the relative coordinates within the target component of the click
	 */
	public void showContextMenu(Point point) {};

}
