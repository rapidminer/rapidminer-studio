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
package com.rapidminer.tools.config.gui.event;

import com.rapidminer.tools.config.Configurable;
import com.rapidminer.tools.config.gui.model.ConfigurableModel;


/**
 * An event for the {@link ConfigurableModel}.
 *
 * @author Marco Boeck
 *
 */
public class ConfigurableEvent {

	/**
	 * Defines different kind of {@link ConfigurableEvent}s.
	 *
	 */
	public static enum EventType {
		/** fired when a configurable has been added */
		CONFIGURABLE_ADDED,

		/** fired when a configurable has been removed */
		CONFIGURABLE_REMOVED,

		/**
		 * fired when the existing configurables have changed and we don't know if one was
		 * added/removed
		 */
		CONFIGURABLES_CHANGED,

		/**
		 * fired when configurables are completely loaded from a repository
		 */
		LOADED_FROM_REPOSITORY;
	}

	private EventType type;

	private Configurable configurable;

	/**
	 * Creates a new {@link ConfigurableEvent} instance for the specified {@link EventType}.
	 *
	 * @param type
	 * @param configurable
	 *            the {@link Configurable} for this event, can be <code>null</code>
	 */
	public ConfigurableEvent(EventType type, Configurable configurable) {
		this.type = type;
		this.configurable = configurable;
	}

	/**
	 * Returns the {@link EventType}.
	 *
	 * @return
	 */
	public EventType getEventType() {
		return type;
	}

	/**
	 * Returns the {@link Configurable} associated with this event, or <code>null</code>.
	 *
	 * @return
	 */
	public Configurable getConfigurable() {
		return configurable;
	}
}
