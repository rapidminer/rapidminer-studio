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
package com.rapidminer.gui.viewer.metadata.event;

import com.rapidminer.gui.viewer.metadata.model.AbstractAttributeStatisticsModel;


/**
 * An event for the {@link AbstractAttributeStatisticsModel}.
 * 
 * @author Marco Boeck
 * 
 */
public class AttributeStatisticsEvent {

	/**
	 * Defines different kind of {@link AttributeStatisticsEvent}s.
	 * 
	 */
	public static enum EventType {
		ALTERNATING_CHANGED, ENLARGED_CHANGED, SHOW_CONSTRUCTION_CHANGED, STATISTICS_CHANGED;
	}

	private EventType type;

	/**
	 * Creates a new {@link AttributeStatisticsEvent} instance for the specified {@link EventType}.
	 * 
	 * @param type
	 */
	public AttributeStatisticsEvent(EventType type) {
		this.type = type;
	}

	/**
	 * Returns the {@link EventType}.
	 * 
	 * @return
	 */
	public EventType getEventType() {
		return type;
	}
}
