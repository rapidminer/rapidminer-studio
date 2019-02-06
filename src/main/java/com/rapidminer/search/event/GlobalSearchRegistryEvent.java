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
package com.rapidminer.search.event;

import com.rapidminer.search.GlobalSearchRegistry;


/**
 * Events for registrations/unregistrations at the {@link GlobalSearchRegistry}.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public final class GlobalSearchRegistryEvent {


	/**
	 * Defines different kind of {@link RegistrationEvent}s.
	 */
	public enum RegistrationEvent {
		/**
		 * fired when a searchable has been registered
		 */
		SEARCH_CATEGORY_REGISTERED,

		/**
		 * fired when a searchable has been unregistered
		 */
		SEARCH_CATEGORY_UNREGISTERED
	}

	private final RegistrationEvent type;

	/**
	 * Creates a new {@link GlobalSearchRegistryEvent} instance for the specified {@link RegistrationEvent}.
	 *
	 * @param type
	 * 		the event type
	 */
	public GlobalSearchRegistryEvent(final RegistrationEvent type) {
		this.type = type;
	}

	/**
	 * Returns the {@link RegistrationEvent}.
	 *
	 * @return the type of the event
	 */
	public RegistrationEvent getEventType() {
		return type;
	}

}
