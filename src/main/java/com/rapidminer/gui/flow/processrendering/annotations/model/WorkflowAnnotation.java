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
package com.rapidminer.gui.flow.processrendering.annotations.model;

import java.awt.geom.Rectangle2D;
import java.util.UUID;

import com.rapidminer.gui.flow.processrendering.annotations.AnnotationsVisualizer;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationStyle;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;


/**
 * Describes a single workflow annotation shown in the {@link ProcessRendererView} via the
 * {@link AnnotationsVisualizer}. An annotation can either be set to an arbitrary location in the
 * process or it can be attached to an {@link Operator}. It mainly consists of HTML text as well as
 * some specific styling options via CSS.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public abstract class WorkflowAnnotation {

	/** the minimum x coordinate an annotation can have */
	public static final int MIN_X = 10;

	/** the minimum y coordinate an annotation can have */
	public static final int MIN_Y = 10;

	/** the actual annotation as HTML */
	private String comment;

	/** the style for this comment */
	private AnnotationStyle style;

	/** the location (coordinates and size) of the annotation */
	private Rectangle2D location;

	/** indicating if the user ever resized the annotation */
	private boolean wasResized;

	/** indicating if the user ever manually assigned a color */
	private boolean wasColored;

	/** if this annotation is overflowing on y axis */
	private boolean overflowing;

	/** a unique ID only relevant during Studio session */
	private UUID id;

	/**
	 * Creates a new workflow annotation which just has width and height.
	 *
	 * @param comment
	 *            the actual annotation as HTML
	 * @param style
	 *            the style of the annotation
	 * @param wasResized
	 *            if the user has ever resized the annotation
	 * @param wasColored
	 *            if the user ever manually colored the annotation
	 * @param width
	 *            the width of the annotation
	 * @param height
	 *            the height of the annotation
	 */
	protected WorkflowAnnotation(final String comment, final AnnotationStyle style, final boolean wasResized,
			final boolean wasColored, final double width, final double height) {
		this(comment, style, wasResized, wasColored, new Rectangle2D.Double(0, 0, width, height));
	}

	/**
	 * Creates a new workflow annotation which is freely positioned in the given
	 * {@link OperatorChain}.
	 *
	 * @param comment
	 *            the actual annotation as HTML
	 * @param style
	 *            the style of the annotation
	 * @param wasResized
	 *            if the user has ever resized the annotation
	 * @param wasColored
	 *            if the user ever manually colored the annotation
	 * @param location
	 *            the location of the annotation
	 *
	 */
	protected WorkflowAnnotation(final String comment, final AnnotationStyle style, final boolean wasResized,
			final boolean wasColored, final Rectangle2D location) {
		if (comment == null) {
			throw new IllegalArgumentException("comment must not be null!");
		}
		if (style == null) {
			throw new IllegalArgumentException("style must not be null!");
		}
		if (location == null) {
			throw new IllegalArgumentException("location must not be null!");
		}
		this.comment = comment;
		this.style = style;
		this.location = new Rectangle2D.Double();
		this.location.setFrame(location.getFrame());
		this.wasResized = wasResized;
		this.wasColored = wasColored;

		this.id = UUID.randomUUID();
	}

	/**
	 * Returns the actual annotation as HTML.
	 *
	 * @return the comment, never {@code null}
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Sets the actual annotation comment. Supports HTML.
	 *
	 * @param comment
	 *            the comment, never {@code null}
	 */
	public void setComment(final String comment) {
		if (comment == null) {
			throw new IllegalArgumentException("comment must not be null!");
		}
		this.comment = comment;
	}

	/**
	 * Returns the styling information.
	 *
	 * @return the style, never {@code null}
	 */
	public AnnotationStyle getStyle() {
		return style;
	}

	/**
	 * Returns the location of this annotation. X and Y coordinates may not be set.
	 *
	 * @return the location, never {@code null}
	 */
	public Rectangle2D getLocation() {
		return location;
	}

	/**
	 * Sets the location of this annotation.
	 *
	 * @param location
	 *            the location, never {@code null}
	 */
	public void setLocation(Rectangle2D location) {
		if (location == null) {
			throw new IllegalArgumentException("location must not be null!");
		}
		this.location = location;
	}

	/**
	 * Returns whether this annotation was ever resized by the user. An annotation that has been
	 * manually resized will never resize itself automatically again.
	 *
	 * @return {@code true} if the user has manually resized this annotation; {@code false}
	 *         otherwise
	 */
	public boolean wasResized() {
		return wasResized;
	}

	/**
	 * Call if the annotation has been manually resized by the user. After this has been called,
	 * {@link #wasResized()} will always return {@code true}.
	 */
	public void setResized() {
		wasResized = true;
	}

	/**
	 * Returns whether this annotation was ever manually colored by the user. An annotation that has
	 * been manually colored will never change its color automatically again.
	 *
	 * @return {@code true} if the user has manually colored this annotation; {@code false}
	 *         otherwise
	 */
	public boolean wasColored() {
		return wasColored;
	}

	/**
	 * Call if the annotation has been manually colored by the user. After this has been called,
	 * {@link #wasColored()} will always return {@code true}.
	 */
	public void setColored() {
		wasColored = true;
	}

	/**
	 * Return a unique ID of this annotation. Generated anew for each Studio session.
	 *
	 * @return the id, never {@code null}
	 */
	public UUID getId() {
		return id;
	}

	/**
	 * Notifies the process that something has changed and thus enables save action etc.
	 */
	public abstract void fireUpdate();

	/**
	 * Returns the {@link ExecutionUnit} this annotation is located in.
	 *
	 * @return the process, never {@code null}
	 */
	public abstract ExecutionUnit getProcess();

	/**
	 * Returns whether this annotation text is overflowing on the y axis.
	 *
	 * @return {@code true} if it is; {@code false} otherwise
	 */
	public boolean isOverflowing() {
		return overflowing;
	}

	/**
	 * Sets whether this annotation text is overflowing on the y axis.
	 *
	 * @return {@code true} if it is; {@code false} otherwise
	 */
	public void setOverflowing(boolean overflowing) {
		this.overflowing = overflowing;
	}

	/**
	 * Creates a new {@link OperatorAnnotation} based on this annotation. Can be used to either
	 * clone an operator annotation or to convert a process annotation.
	 *
	 * @param attachedTo
	 *            the operator the annotation should be attached to
	 * @return the annotation instance, never {@code null}
	 */
	public abstract OperatorAnnotation createOperatorAnnotation(Operator attachedTo);

	/**
	 * Creates a new {@link ProcessAnnotation} based on this annotation. Can be used to either clone
	 * a process annotation or to convert an operator annotation.
	 *
	 * @param process
	 *            the process the annotation should be attached to
	 * @return the annotation instance, never {@code null}
	 */
	public abstract ProcessAnnotation createProcessAnnotation(ExecutionUnit process);

	@Override
	public String toString() {
		return "WorkflowAnnotation [comment=" + comment + ", style=" + style + ", location=" + location + "]";
	}
}
