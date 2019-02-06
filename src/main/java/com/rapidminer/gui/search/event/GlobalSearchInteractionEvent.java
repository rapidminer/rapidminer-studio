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
package com.rapidminer.gui.search.event;


import com.rapidminer.gui.search.model.GlobalSearchRow;


/**
 * An event for the {@link com.rapidminer.gui.search.GlobalSearchCategoryPanel}.
 *
 * @author Marco Boeck
 * @since 8.1
 *
 */
public class GlobalSearchInteractionEvent {

	/**
	 * Defines different kind of {@link GlobalSearchInteractionEvent}s.
	 *
	 */
	public enum InteractionEvent {
		/** fired when the user browses (e.g. mouse-over) the result */
		RESULT_BROWSED,

		/** fired when the user activates (e.g. clicks) the result */
		RESULT_ACTIVATED
	}

	private final InteractionEvent type;
	private final GlobalSearchRow row;


	/**
	 * Creates a new {@link GlobalSearchInteractionEvent} instance for the specified {@link InteractionEvent}
	 * .
	 *
	 * @param type
	 * 		the event type
	 * @param row
	 * 		the row which the user interacted with
	 */
	public GlobalSearchInteractionEvent(final InteractionEvent type, GlobalSearchRow row) {
		this.type = type;
		this.row = row;
	}

	/**
	 * Returns the {@link InteractionEvent}.
	 *
	 * @return the type of the event
	 */
	public InteractionEvent getEventType() {
		return type;
	}

	/**
	 * Returns the row on which the interaction happened.
	 *
	 * @return the row which the user interacted with
	 */
	public GlobalSearchRow getRow() {
		return row;
	}
}
