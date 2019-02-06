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
 * @author Marco Boeck, Jan Czogalla
 * @since 6.4.0
 *
 */
public final class ProcessRendererModelEvent {

	/**
	 * Defines different kind of {@link ProcessRendererModelEvent}s.
	 *
	 */
	public static enum ModelEvent {
		/** fired when the displayed processes have changed */
		DISPLAYED_PROCESSES_CHANGED,

		/** fired when the displayed operator chain has changed */
		DISPLAYED_CHAIN_CHANGED,

		/** fired when a process size has changed */
		PROCESS_SIZE_CHANGED,

		/** fired when the process zoom level has changed */
		PROCESS_ZOOM_CHANGED,

		/** fired when something minor changes which only requires a repaint */
		MISC_CHANGED,

		/** fired before the displayed operator chain will change */
		DISPLAYED_CHAIN_WILL_CHANGE;
	}

	private final ModelEvent type;

	/**
	 * Creates a new {@link ProcessRendererModelEvent} instance for the specified {@link ModelEvent}
	 * .
	 *
	 * @param type
	 *            the event type
	 */
	public ProcessRendererModelEvent(final ModelEvent type) {
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
