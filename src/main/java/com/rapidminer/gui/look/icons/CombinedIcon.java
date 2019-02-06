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
package com.rapidminer.gui.look.icons;

import java.awt.Component;
import java.awt.Graphics;
import java.util.Objects;
import javax.swing.Icon;


/**
 * Helper Icon class in case you need to display two {@link Icon Icons} but there is only one {@link Icon} supported by
 * the {@link Component}
 *
 * @author Andreas Timm
 * @since 8.2
 */
public class CombinedIcon implements Icon {

	/**
	 * The orientation to lay out the combined icons
	 */
	public enum Orientation {
		HORIZONTAL {
			@Override
			int getWidth(Icon firstIcon, Icon secondIcon, int gap) {
				return firstIcon.getIconWidth() + secondIcon.getIconWidth() + gap;
			}

			@Override
			int getHeight(Icon firstIcon, Icon secondIcon, int gap) {
				return Math.max(firstIcon.getIconHeight(), secondIcon.getIconHeight());
			}

			@Override
			int getSecondIconX(int offset, Icon firstIcon, int gap) {
				return offset + firstIcon.getIconWidth() + gap;
			}

			@Override
			int getSecondIconY(int offset, Icon firstIcon, int gap) {
				return offset;
			}
		}, VERTICAL {
			@Override
			int getWidth(Icon firstIcon, Icon secondIcon, int gap) {
				return Math.max(firstIcon.getIconWidth(), secondIcon.getIconWidth());
			}

			@Override
			int getHeight(Icon firstIcon, Icon secondIcon, int gap) {
				return firstIcon.getIconHeight() + secondIcon.getIconHeight() + gap;
			}

			@Override
			int getSecondIconX(int offset, Icon firstIcon, int gap) {
				return offset;
			}

			@Override
			int getSecondIconY(int offset, Icon firstIcon, int gap) {
				return offset + firstIcon.getIconHeight() + gap;
			}
		};

		/**
		 * Calculates the overall width for the given icons depending on the orientation
		 *
		 * @param firstIcon
		 * 		the icon that will be displayed to the left or on top of the secondIcon
		 * @param secondIcon
		 * 		the other icon to be displayed next to the firstIcon
		 * @param gap
		 * 		the space between the icons
		 * @return amount of pixels required to display this combination of icons
		 */
		abstract int getWidth(Icon firstIcon, Icon secondIcon, int gap);

		/**
		 * Calculates the overall height for the given icons depending on the orientation
		 *
		 * @param firstIcon
		 * 		the icon that will be displayed to the left or on top of the secondIcon
		 * @param secondIcon
		 * 		the other icon to be displayed next to the firstIcon
		 * @param gap
		 * 		the space between the icons
		 * @return amount of pixels required to display this combination of icons
		 */
		abstract int getHeight(Icon firstIcon, Icon secondIcon, int gap);

		/**
		 * Calculates the x position for the second icon depending on the orientation
		 *
		 * @param offset
		 * 		the offset to add when calculating the position
		 * @param firstIcon
		 * 		the icon that will be displayed to the left or on top of the secondIcon
		 * @param gap
		 * 		the space between the icons
		 * @return the x position for the second icon
		 */
		abstract int getSecondIconX(int offset, Icon firstIcon, int gap);

		/**
		 * Calculates the y position for the second icon depending on the orientation
		 *
		 * @param offset
		 * 		the offset to add when calculating the position
		 * @param firstIcon
		 * 		the icon that will be displayed to the left or on top of the secondIcon
		 * @param gap
		 * 		the space between the icons
		 * @return the y position for the second icon
		 */
		abstract int getSecondIconY(int offset, Icon firstIcon, int gap);
	}

	/**
	 * Default orientation is horizontal
	 */
	private Orientation orientation = Orientation.HORIZONTAL;

	/**
	 * Define a gap to separate the {@link Icon Icons}
	 */
	private int gap = 0;

	/**
	 * The {@link Icon Icons} that will be combined in the named order
	 */
	private Icon firstIcon;
	private Icon secondIcon;

	/**
	 * Construct this instance to combine the provided icons. Based on the orientation the first icon will be placed to
	 * the left or above the second icon with a gap. Default orientation is horizontal with a gap of zero.
	 *
	 * @param firstIcon
	 * 		icon to be displayed first
	 * @param secondIcon
	 * 		icon to be displayed second
	 */
	public CombinedIcon(Icon firstIcon, Icon secondIcon) {
		this.firstIcon = Objects.requireNonNull(firstIcon, "First icon may not be null!");
		this.secondIcon = Objects.requireNonNull(secondIcon, "Second icon may not be null!");
	}

	/**
	 * The {@link Orientation}, either horizontal or vertical
	 *
	 * @return current {@link Orientation}
	 */
	public Orientation getOrientation() {
		return orientation;
	}

	/**
	 * Only supporting defined {@link Orientation Orientations}
	 *
	 * @param orientation
	 * 		one of HORIZONTAL or VERTICAL
	 */
	public void setOrientation(Orientation orientation) {
		this.orientation = orientation;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		firstIcon.paintIcon(c, g, x, y);
		secondIcon.paintIcon(c, g, orientation.getSecondIconX(x, firstIcon, gap), orientation.getSecondIconY(y, firstIcon, gap));
	}

	@Override
	public int getIconWidth() {
		return orientation.getWidth(firstIcon, secondIcon, gap);
	}

	@Override
	public int getIconHeight() {
		return orientation.getHeight(firstIcon, secondIcon, gap);
	}

	/**
	 * Get the current setting for the gap between displayed {@link Icon Icons}
	 *
	 * @return amount of pixel to separate the {@link Icon Icons}
	 */
	public int getGap() {
		return gap;
	}

	/**
	 * Change the gap between the {@link Icon Icons}
	 *
	 * @param gap
	 * 		amount of pixel to separate the {@link Icon Icons}
	 */
	public void setGap(int gap) {
		this.gap = gap;
	}
}
