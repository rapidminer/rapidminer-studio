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

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.Action;

import com.rapidminer.gui.flow.processrendering.annotations.model.AnnotationsModel;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.tools.ResourceAction;


/**
 * Available colors for {@link WorkflowAnnotation}s.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public enum AnnotationColor {

	/** slight yellow color */
	YELLOW("yellow", new Color(247, 244, 130, 217), new Color(252, 249, 135)),

	/** slight orange color */
	ORANGE("orange", new Color(252, 245, 205, 217), new Color(255, 250, 210)),

	/** red color */
	RED("red", new Color(253, 228, 215, 217), new Color(255, 233, 220)),

	/** purple color */
	PURPLE("purple", new Color(241, 217, 232, 217), new Color(246, 222, 237)),

	/** green color */
	GREEN("green", new Color(216, 233, 205, 217), new Color(221, 238, 210)),

	/** blue color */
	BLUE("blue", new Color(222, 218, 236, 217), new Color(227, 223, 241)),

	/** gray color */
	GRAY("gray", new Color(204, 204, 204, 217), new Color(209, 209, 209)),

	/** transparent background */
	TRANSPARENT("transparent", new Color(255, 255, 255, 0), new Color(255, 255, 255, 0));

	private final String key;
	private final Color color;
	private final Color colorHighlight;
	private final Color colorTransparent;

	private AnnotationColor(final String key, final Color color, final Color colorHighlight) {
		this.key = key;
		this.color = color;
		this.colorHighlight = colorHighlight;
		if ("transparent".equals(key)) {
			this.colorTransparent = new Color(colorHighlight.getRed() - 25, colorHighlight.getGreen() - 25,
					colorHighlight.getBlue() - 25, 100);
		} else {
			this.colorTransparent = new Color(colorHighlight.getRed(), colorHighlight.getGreen(), colorHighlight.getBlue(),
					100);
		}
	}

	/**
	 * Returns the color as an AWT color.
	 *
	 * @return the color, never {@code null}
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * Returns the highlight color as an AWT color.
	 *
	 * @return the highlight color, never {@code null}
	 */
	public Color getColorHighlight() {
		return colorHighlight;
	}

	/**
	 * Returns the transparent color as an AWT color.
	 *
	 * @return the transparent color, never {@code null}
	 */
	public Color getColorTransparent() {
		return colorTransparent;
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
	 * Creates an action to change the color of the given annotation to the color of this instance.
	 *
	 * @param model
	 *            the annotation model instance
	 * @param anno
	 *            the annotation which should have its color changed
	 * @return the menu item instance, never {@code null}
	 */
	public Action makeColorChangeAction(final AnnotationsModel model, final WorkflowAnnotation anno) {
		ResourceAction action = new ResourceAction(true, "workflow.annotation.color." + getKey()) {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				model.setAnnotationColor(anno, AnnotationColor.this);
			}
		};

		return action;
	}

	/**
	 * Looks up the correct instance for the given identifier key.
	 *
	 * @param key
	 *            the identifier key
	 * @return the matching instance or {@link #YELLOW} if no match is found
	 */
	public static AnnotationColor fromKey(final String key) {
		for (AnnotationColor color : values()) {
			if (color.getKey().equals(key)) {
				return color;
			}
		}
		return YELLOW;
	}
}
