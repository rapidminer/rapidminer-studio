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
package com.rapidminer.gui.flow.processrendering.annotations.style;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import com.rapidminer.gui.flow.processrendering.annotations.model.AnnotationsModel;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.tools.ResourceAction;


/**
 * Available horizontal alignments for {@link WorkflowAnnotation}s.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public enum AnnotationAlignment {

	/** left align */
	LEFT("left", "text-align: left"),

	/** center align */
	CENTER("center", "text-align: center"),

	/** right align */
	RIGHT("right", "text-align: right");

	private final String key;
	private final String css;

	private AnnotationAlignment(final String key, final String css) {
		this.key = key;
		this.css = css;
	}

	/**
	 * Returns the alignment as a CSS alignment string.
	 *
	 * @return the alignment string representation, never {@code null}
	 */
	public String getCSS() {
		return css;
	}

	/**
	 * Returns the identifier key for the alignment.
	 *
	 * @return the key, never {@code null}
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Creates an action to change the alignment of the given annotation to the alignment of this
	 * instance.
	 *
	 * @param model
	 *            the annotation model instance
	 * @param anno
	 *            the annotation which should have its alignment changed
	 * @return the menu item instance, never {@code null}
	 */
	public Action makeAlignmentChangeAction(final AnnotationsModel model, final WorkflowAnnotation anno) {
		ResourceAction action = new ResourceAction(true, "workflow.annotation.alignment." + getKey()) {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				model.setAnnotationAlignment(anno, AnnotationAlignment.this);
			}
		};

		return action;
	}

	/**
	 * Looks up the correct instance for the identifier key value.
	 *
	 * @param key
	 *            the identifier key
	 * @return the matching instance or {@link #LEFT} if no match is found
	 */
	public static AnnotationAlignment fromKey(String key) {
		for (AnnotationAlignment alignment : values()) {
			if (alignment.getKey().equals(key)) {
				return alignment;
			}
		}
		return LEFT;
	}
}
