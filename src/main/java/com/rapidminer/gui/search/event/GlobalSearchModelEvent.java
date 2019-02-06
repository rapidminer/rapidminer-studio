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

/**
 * An event for the {@link com.rapidminer.gui.search.model.GlobalSearchModel} when the whole model (or multiple categories) changes.
 *
 * @author Marco Boeck
 * @since 8.1
 *
 */
public final class GlobalSearchModelEvent {

	/**
	 * Defines different kind of {@link GlobalSearchModelEvent}s.
	 *
	 */
	public enum ModelEvent {

		/** fired when the error status has changed, i.e. from no error -> error or error -> no error */
		ERROR_STATUS_CHANGED,

		/** fired when the correction suggestion has changed */
		CORRECTION_SUGGESTION_CHANGED,

		/** fired when all categories have been removed, i.e. on the beginning of a category search (only the searched category becomes pending afterwards) */
		ALL_CATEGORIES_REMOVED
	}

	private final ModelEvent type;

	/**
	 * Creates a new {@link GlobalSearchModelEvent} instance for the specified {@link ModelEvent}
	 * .
	 *
	 * @param type
	 *            the event type
	 */
	public GlobalSearchModelEvent(final ModelEvent type) {
		this.type = type;
	}

	/**
	 * Returns the {@link ModelEvent}.
	 *
	 * @return the type of the event
	 */
	public ModelEvent getEventType() {
		return type;
	}
}
