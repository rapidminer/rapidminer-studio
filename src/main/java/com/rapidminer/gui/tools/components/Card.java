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
package com.rapidminer.gui.tools.components;

import javax.swing.Icon;


/**
 * Interface for container holding display information like title, tooltip and icon for cards shown
 * in a {@link ButtonBarCardPanel}s.
 * 
 * @author Nils Woehler
 * 
 */
public interface Card {

	String getKey();

	/**
	 * @return the title of a card. The title shouldn't be too long and must not be
	 *         <code>null</code>. The title will be display below the card icon.
	 */
	String getTitle();

	/**
	 * @return the tooltip that will be shown when hovering over the card.
	 */
	String getTip();

	/**
	 * @return the icon that will be shown above the card title
	 */
	Icon getIcon();

	/**
	 * @return the footer of the card which can be <code>null</code> if the card does not have a
	 *         footer. The footer will be display as small italic text below the title. Only the
	 *         last {@link CardCellRenderer#MAX_CAPTION_LENGTH} characters of a footer will be
	 *         shown.
	 */
	String getFooter();

	/**
	 * @return {@code true} if the card should display a BETA tag, {@code false} otherwise
	 * @since 9.2.0
	 */
	default boolean isBeta() {
		return false;
	}
}
