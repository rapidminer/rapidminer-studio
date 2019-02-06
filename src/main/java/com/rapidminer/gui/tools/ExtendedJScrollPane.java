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

import java.awt.Component;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;


/**
 * This extended version of the JScrollPane uses increased numbers of unit increments for both
 * scroll bars making it more useful for mouse wheels.
 * 
 * @author Ingo Mierswa
 */
public class ExtendedJScrollPane extends JScrollPane {

	private static final long serialVersionUID = 218317624316997140L;

	public ExtendedJScrollPane() {
		super();
		getHorizontalScrollBar().setUnitIncrement(10);
		getVerticalScrollBar().setUnitIncrement(10);
	}

	public ExtendedJScrollPane(Component component) {
		super(component);
		getHorizontalScrollBar().setUnitIncrement(10);
		getVerticalScrollBar().setUnitIncrement(10);

		if (component instanceof ExtendedJTable) {
			ExtendedJTable table = (ExtendedJTable) component;
			table.setExtendedScrollPane(this);
		}

		// scrollpane in scrollpane support
		addMouseWheelListener(e -> {

			// horizontal scrolling is done with shift held down
			// this works for both Windows as well as OS X touchpad
			boolean scrollVertical = !e.isShiftDown() || !getHorizontalScrollBar().isShowing();
			JScrollBar scrollBar = scrollVertical ? getVerticalScrollBar() : getHorizontalScrollBar();
			// if no scrollbar is shown, just dispatch scroll event to parent so parent scrollpane can actually scroll
			if (!scrollBar.isShowing()) {
				getParent().dispatchEvent(e);
			}
			// if vertical scrollbar is at top position, allow parent scrollpane to scroll up as well
			// OR
			// if horizontal scrollbar is at leftmost position, allow parent scrollpane to scroll left as well
			if (e.getWheelRotation() < 0 && scrollBar.getValue() == scrollBar.getMinimum()) {
				getParent().dispatchEvent(e);
			}
			// if vertical scrollbar is at bottom position, allow parent scrollpane to scroll down as well
			// OR
			// if horizontal scrollbar is at rightmost position, allow parent scrollpane to scroll right as well
			if (e.getWheelRotation() > 0 && scrollBar.getValue() + scrollBar.getVisibleAmount() == scrollBar.getMaximum()) {
				getParent().dispatchEvent(e);
			}
		});
	}

	@Override
	public void setViewportView(Component component) {
		super.setViewportView(component);

		if (component instanceof ExtendedJTable) {
			ExtendedJTable table = (ExtendedJTable) component;
			table.setExtendedScrollPane(this);
		}
	}
}
