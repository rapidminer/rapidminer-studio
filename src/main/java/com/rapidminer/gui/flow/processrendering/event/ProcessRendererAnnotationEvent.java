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

import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;


/**
 * An event for the {@link ProcessRendererModel} which is fired when something
 * {@link WorkflowAnnotation} related changes.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public final class ProcessRendererAnnotationEvent {

	/**
	 * Defines different kind of {@link ProcessRendererAnnotationEvent}s.
	 *
	 */
	public static enum AnnotationEvent {
		/** fired when the annotation selection has changed */
		SELECTED_ANNOTATION_CHANGED,

		/** fired when annotations changed their position */
		ANNOTATIONS_MOVED,

		/** fired when something minor changes which only requires a repaint */
		MISC_CHANGED;
	}

	private final AnnotationEvent type;

	/**
	 * Creates a new {@link ProcessRendererAnnotationEvent} instance for the specified
	 * {@link AnnotationEvent}.
	 *
	 * @param type
	 *            the event type
	 */
	public ProcessRendererAnnotationEvent(final AnnotationEvent type) {
		this.type = type;
	}

	/**
	 * Returns the {@link AnnotationEvent}.
	 *
	 * @return the type of the event
	 */
	public AnnotationEvent getEventType() {
		return type;
	}
}
