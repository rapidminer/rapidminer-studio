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
package com.rapidminer.gui.flow.processrendering.event;

import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;


/**
 * An event for the {@link ProcessRendererModel}.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public final class ProcessRendererOperatorEvent {

	/**
	 * Defines different kind of {@link ProcessRendererOperatorEvent}s.
	 *
	 */
	public static enum OperatorEvent {
		/** fired when the operator selection has changed */
		SELECTED_OPERATORS_CHANGED,

		/** fired when operators changed their position */
		OPERATORS_MOVED,

		/** fired when operators have changed their number of ports */
		PORTS_CHANGED;
	}

	private final OperatorEvent type;

	/**
	 * Creates a new {@link ProcessRendererOperatorEvent} instance for the specified
	 * {@link OperatorEvent}.
	 *
	 * @param type
	 *            the event type
	 */
	public ProcessRendererOperatorEvent(final OperatorEvent type) {
		this.type = type;
	}

	/**
	 * Returns the {@link OperatorEvent}.
	 *
	 * @return the type of the event
	 */
	public OperatorEvent getEventType() {
		return type;
	}
}
