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

import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationColor;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationStyle;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;


/**
 * Describes a workflow annotation which is attached to an {@link Operator}. This is displayed
 * beneath the operator. Note that if the operator is an {@link OperatorChain}, the annotation will
 * only be displayed in the process containing the operator.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public class OperatorAnnotation extends WorkflowAnnotation {

	/** offset between bottom of operator and operator annotation */
	public static final int Y_OFFSET = 7;

	/** the minimum height an annotation can have */
	public static final int MIN_HEIGHT = 30;

	/** the default width for an annotation */
	public static final int DEFAULT_WIDTH = (int) (ProcessDrawer.OPERATOR_WIDTH * 1.4f);

	/** the default height for an annotation */
	public static final int DEFAULT_HEIGHT = 50;

	/** the maximum height for an annotation */
	public static final int MAX_HEIGHT = 2000;

	/** the maximum number of characters which are allowed in an operator annotation */
	public static final int MAX_CHARACTERS = 1500;

	/** the operator this annotation is attached to */
	private Operator attachedTo;

	/**
	 * Creates a new operator annotation which is attached to an operator.
	 *
	 * @param comment
	 *            the actual annotation as HTML
	 * @param style
	 *            the style of the annotation
	 * @param attachedTo
	 *            the operator this annotation is attached to
	 * @param wasResized
	 *            if the user has ever resized the annotation
	 * @param wasColored
	 *            if the user ever manually colored the annotation
	 * @param x
	 *            the x coordinate of the annotation
	 * @param y
	 *            the y coordinate of the annotation
	 * @param width
	 *            the width of the annotation
	 * @param height
	 *            the height of the annotation
	 */
	public OperatorAnnotation(final String comment, final AnnotationStyle style, final Operator attachedTo,
			final boolean wasResized, final boolean wasColored, final double x, final double y, final double width,
			final double height) {
		super(comment, style, wasResized, wasColored, new Rectangle2D.Double(x, y, width, height));
		if (attachedTo == null) {
			throw new IllegalArgumentException("attachedTo must not be null!");
		}

		this.attachedTo = attachedTo;
	}

	/**
	 * Returns the operator this annotation is attached to.
	 *
	 * @return the operator, never {@code null}
	 */
	public Operator getAttachedTo() {
		return attachedTo;
	}

	/**
	 * Sets the operator this annotation is attached to.
	 *
	 * @param attachedTo
	 *            the operator the annotation will be attached to
	 *
	 */
	public void setAttachedTo(Operator attachedTo) {
		if (attachedTo == null) {
			throw new IllegalArgumentException("attachedTo must not be null!");
		}
		this.attachedTo = attachedTo;
	}

	@Override
	public ExecutionUnit getProcess() {
		return getAttachedTo().getExecutionUnit();
	}

	/**
	 * Operator annotations always auto resize, so we always deny past resizing here.
	 *
	 * @return always {@code false}
	 */
	@Override
	public boolean wasResized() {
		return false;
	}

	@Override
	public void fireUpdate() {
		// dirty hack to force an observer update on the operator
		getAttachedTo().rename(getAttachedTo().getName());
	}

	@Override
	public ProcessAnnotation createProcessAnnotation(ExecutionUnit process) {
		Rectangle2D frame = getLocation();
		AnnotationStyle style = getStyle().clone();
		// process annotations are always yellow by default
		if (!wasColored()) {
			style.setAnnotationColor(AnnotationColor.YELLOW);
		}
		return new ProcessAnnotation(getComment(), style, process, false, wasColored(), new Rectangle2D.Double(frame.getX(),
				frame.getY(), frame.getWidth(), Math.max(ProcessAnnotation.MIN_HEIGHT, frame.getHeight())));
	}

	@Override
	public OperatorAnnotation createOperatorAnnotation(Operator attachedTo) {
		// change owner to new parent
		return new OperatorAnnotation(getComment(), getStyle().clone(), attachedTo, wasResized(), wasColored(),
				getLocation().getX(), getLocation().getY(), getLocation().getWidth(), getLocation().getHeight());
	}
}
