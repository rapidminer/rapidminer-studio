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
package com.rapidminer.operator;

/**
 * Describes an event which can occur on a {@link Process} {@link IOObjectMap}.
 *
 * @author Marco Boeck
 *
 */
public class IOObjectMapEvent {

	/**
	 * All available event types for {@link IOObjectMap} changes.
	 */
	public static enum IOObjectMapEventType {
		/** a single {@link IOObject} has been added to the map */
		ADDED,

		/** a single {@link IOObject} has been removed from the map */
		REMOVED,

		/** a single {@link IOObject} that was already in the map has changed */
		CHANGED,

		/**
		 * either multiple elements have been added or multiple elements have been removed at the
		 * same time
		 */
		STRUCTURE_CHANGED;
	}

	private final IOObjectMapEventType type;
	private final String ioobjectName;

	/**
	 * Creates a new event with the specified type for the given ioobject.
	 *
	 * @param type
	 *            the type of the event
	 * @param name
	 *            the name of the ioobject which has changed. Can be <code>null</code> for type
	 *            {@link IOObjectMapEventType#STRUCTURE_CHANGED}
	 */
	public IOObjectMapEvent(IOObjectMapEventType type, String name) {
		if (type == null) {
			throw new IllegalArgumentException("type must not be null!");
		}

		this.type = type;
		this.ioobjectName = name;
	}

	/**
	 * The name of the {@link IOObject} which has changed in the {@link IOObjectMap}.
	 *
	 * @return the name of the changed ioobject or <code>null</code> if {@link #getType()} is
	 *         {@link IOObjectMapEventType#STRUCTURE_CHANGED}
	 */
	public String getName() {
		return ioobjectName;
	}

	/**
	 * The {@link IOObjectMapEventType} which occured on the map.
	 *
	 * @return the type of the event
	 */
	public IOObjectMapEventType getType() {
		return type;
	}
}
