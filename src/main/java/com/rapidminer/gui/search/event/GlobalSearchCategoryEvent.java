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
 * An event for the {@link com.rapidminer.gui.search.model.GlobalSearchModel} when a category changes.
 *
 * @author Marco Boeck
 * @since 8.1
 *
 */
public final class GlobalSearchCategoryEvent {

	/**
	 * Defines different kind of {@link GlobalSearchCategoryEvent}s.
	 *
	 */
	public enum CategoryEvent {
		/** fired when the pending state of a category has changed */
		CATEGORY_PENDING_STATUS_CHANGED,

		/** fired when rows have been changed for a search category. This can e.g. be caused by different results or by no more results for a row */
		CATEGORY_ROWS_CHANGED,

		/** fired when more rows have been appended to a search category */
		CATEGORY_ROWS_APPENDED
	}

	private final CategoryEvent type;

	/**
	 * Creates a new {@link GlobalSearchCategoryEvent} instance for the specified {@link CategoryEvent}
	 * .
	 *
	 * @param type
	 *            the event type
	 */
	public GlobalSearchCategoryEvent(final CategoryEvent type) {
		this.type = type;
	}

	/**
	 * Returns the {@link CategoryEvent}.
	 *
	 * @return the type of the event
	 */
	public CategoryEvent getEventType() {
		return type;
	}
}
