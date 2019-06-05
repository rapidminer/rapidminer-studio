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
package com.rapidminer.connection.util;

import java.util.EventObject;

/**
 * A simple {@link EventObject} with a source and {@link RegistrationEventType}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class RegistrationEvent extends EventObject {

	public enum RegistrationEventType {
		REGISTERED, UNREGISTERED
	}

	private final RegistrationEventType type;

	/**
	 * Constructs a registration event.
	 *
	 * @param source
	 * 		The object on which the Event initially occurred.
	 * @throws IllegalArgumentException
	 * 		if source is null.
	 */
	public RegistrationEvent(Object source, RegistrationEventType type) {
		super(source);
		this.type = type;
	}

	/** Gets the type of event, one of {@link RegistrationEventType} */
	public RegistrationEventType getType() {
		return type;
	}
}
