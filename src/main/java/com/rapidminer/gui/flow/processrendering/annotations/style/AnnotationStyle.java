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

import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;


/**
 * Contains all style information for a {@link WorkflowAnnotation}.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public final class AnnotationStyle {

	/** the color of the annotation background */
	private AnnotationColor color;

	/** the horizontal alignment of the annotation */
	private AnnotationAlignment alignment;

	/** the padding between text and border */
	private int padding;

	/**
	 * Creates a workflow annotation style with a yellow color and left alignement.
	 */
	public AnnotationStyle() {
		this(AnnotationColor.YELLOW, AnnotationAlignment.CENTER);
	}

	/**
	 * Creates a workflow annotation style with a yellow color and left alignement.
	 */
	public AnnotationStyle(AnnotationColor color, AnnotationAlignment alignment) {
		if (color == null) {
			throw new IllegalArgumentException("color must not be null!");
		}
		this.color = color;
		this.alignment = alignment;
		this.padding = 5;
	}

	/**
	 * Sets the annotation color.
	 *
	 * @param color
	 *            the new color
	 */
	public void setAnnotationColor(AnnotationColor color) {
		if (color == null) {
			throw new IllegalArgumentException("color must not be null!");
		}
		this.color = color;
	}

	/**
	 * Returns the annotation color.
	 *
	 * @return the color, never {@code null}
	 */
	public AnnotationColor getAnnotationColor() {
		return color;
	}

	/**
	 * Sets the annotation horizontal alignment.
	 *
	 * @param alignment
	 *            the new alignment
	 */
	public void setAnnotationAlignment(AnnotationAlignment alignment) {
		if (alignment == null) {
			throw new IllegalArgumentException("alignment must not be null!");
		}
		this.alignment = alignment;
	}

	/**
	 * Returns the annotation alignment.
	 *
	 * @return the alignment, never {@code null}
	 */
	public AnnotationAlignment getAnnotationAlignment() {
		return alignment;
	}

	/**
	 * Returns the inner padding between text and border.
	 *
	 * @return the padding, never < {@code 0}
	 */
	public int getPadding() {
		return padding;
	}

	/**
	 * Creates a new annotation style object with no shared references (except for enum constants).
	 */
	@Override
	public AnnotationStyle clone() {
		return new AnnotationStyle(getAnnotationColor(), getAnnotationAlignment());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (alignment == null ? 0 : alignment.hashCode());
		result = prime * result + (color == null ? 0 : color.hashCode());
		result = prime * result + padding;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		AnnotationStyle other = (AnnotationStyle) obj;
		if (alignment != other.alignment) {
			return false;
		}
		if (color != other.color) {
			return false;
		}
		if (padding != other.padding) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "AnnotationStyle [color=" + color + ", alignment=" + alignment + "]";
	}
}
