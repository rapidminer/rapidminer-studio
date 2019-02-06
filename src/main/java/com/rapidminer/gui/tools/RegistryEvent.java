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
package com.rapidminer.gui.tools;

import java.util.EventObject;


/**
 * 
 * Event for the {@link Registry} which contains the type and the object which
 * registered/unregistered.
 * 
 * @author Marco Boeck
 * 
 */
public class RegistryEvent<T> extends EventObject {

	/**
	 * The type of a {@link RegistryEvent}.
	 * 
	 */
	public static enum RegistryEventType {
		/** an object registered itself to the registry */
		REGISTERED,

		/** an object unregistered itself from the registry */
		UNREGISTERED;
	}

	private static final long serialVersionUID = 8034757100709544142L;

	private T registerObject;

	private RegistryEventType type;

	/**
	 * Create a new {@link RegistryEvent} of the specified {@link RegistryEventType} and the given
	 * object which was registered or unregistered.
	 * 
	 * @param source
	 *            the "source" of the event, typically the {@link Registry}
	 * @param type
	 *            whether it was a register or unregister event
	 * @param registerObject
	 *            the object which was registered/unregistered from the registry
	 */
	public RegistryEvent(Object source, RegistryEventType type, T registerObject) {
		super(source);
		this.type = type;
		this.registerObject = registerObject;
	}

	/**
	 * Returns the object which registered/unregistered itself from the {@link Registry}.
	 * 
	 * @return
	 */
	public T getObject() {
		return registerObject;
	}

	/**
	 * Returns whether this was register or an unregister event.
	 * 
	 * @return
	 */
	public RegistryEventType getType() {
		return type;
	}
}
