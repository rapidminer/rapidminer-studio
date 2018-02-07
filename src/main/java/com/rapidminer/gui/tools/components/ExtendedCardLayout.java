/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
package com.rapidminer.gui.tools.components;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;


/**
 * Same as {@link CardLayout}, with the only difference that it does compute its preferred size for the visible components only and ignores the preferred size of components on not displayed cards.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public class ExtendedCardLayout extends CardLayout {

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		Component comp = findDisplayedComponent(parent);

		if (comp != null) {
			Dimension preferredSize = comp.getPreferredSize();
			Insets parentInsets = parent.getInsets();
			return new Dimension(preferredSize.width + parentInsets.left + parentInsets.right, preferredSize.height + parentInsets.top + parentInsets.bottom);
		} else {
			return super.preferredLayoutSize(parent);
		}
	}

	/**
	 * Returns the visible component, aka the component that is currently displayed.
	 *
	 * @param parent
	 * 		the container all cards are in
	 * @return the component or {@code null} if no component is part of this card layout
	 */
	private Component findDisplayedComponent(Container parent) {
		for (Component comp : parent.getComponents()) {
			if (comp.isVisible()) {
				return comp;
			}
		}

		return null;
	}
}
