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

import com.rapidminer.gui.flow.processrendering.annotations.AnnotationDrawUtils;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationColor;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationStyle;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.io.process.ProcessLayoutXMLFilter;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;


/**
 * Describes a workflow annotation which is located in an {@link ExecutionUnit}. It can be
 * positioned freely on the process canvas.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public class ProcessAnnotation extends WorkflowAnnotation {

	/** the minimum width an annotation can have */
	public static final int MIN_WIDTH = ProcessDrawer.OPERATOR_WIDTH;

	/** the minimum height an annotation can have */
	public static final int MIN_HEIGHT = 50;

	/** the default width for an annotation */
	public static final int DEFAULT_WIDTH = MIN_WIDTH * 2;

	/** the default height for an annotation */
	public static final int DEFAULT_HEIGHT = 105;

	/** the maximum width for an annotation */
	public static final int MAX_WIDTH = 4000;

	/** the maximum height for an annotation */
	public static final int MAX_HEIGHT = 2000;

	/** the process this annotation is located in */
	private ExecutionUnit process;

	/**
	 * Creates a new process annotation which can be freely positioned in the process.
	 *
	 * @param comment
	 *            the actual annotation as HTML
	 * @param style
	 *            the style of the annotation
	 * @param process
	 *            the process this annotation is located in
	 * @param wasResized
	 *            if the user has ever resized the annotation
	 * @param wasColored
	 *            if the user ever manually colored the annotation
	 * @param location
	 *            the location of the comment
	 */
	public ProcessAnnotation(final String comment, final AnnotationStyle style, final ExecutionUnit process,
			final boolean wasResized, final boolean wasColored, final Rectangle2D location) {
		super(comment, style, wasResized, wasColored, location);
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}
		if (location == null) {
			throw new IllegalArgumentException("location must not be null!");
		}

		this.process = process;
	}

	/**
	 * Returns the {@link ExecutionUnit} this annotation is located in.
	 *
	 * @return the process, never {@code null}
	 */
	@Override
	public ExecutionUnit getProcess() {
		return process;
	}

	/**
	 * Sets the {@link ExecutionUnit} this annotation is located in.
	 *
	 * @param process
	 *            the process the annotation will be attached to
	 *
	 */
	public void setProcess(ExecutionUnit process) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}
		this.process = process;
	}

	@Override
	public void fireUpdate() {
		// dirty hack to force an observer update on the execution unit
		getProcess().getEnclosingOperator().rename(getProcess().getEnclosingOperator().getName());
	}

	@Override
	public ProcessAnnotation createProcessAnnotation(ExecutionUnit process) {
		Rectangle2D frame = getLocation();
		return new ProcessAnnotation(getComment(), getStyle().clone(), process, false, wasColored(),
				new Rectangle2D.Double(frame.getX(), frame.getY(), frame.getWidth(), frame.getHeight()));
	}

	@Override
	public OperatorAnnotation createOperatorAnnotation(Operator attachedTo) {
		String comment = getComment();
		int x = (int) ProcessLayoutXMLFilter.lookupOperatorRectangle(attachedTo).getCenterX()
				- OperatorAnnotation.DEFAULT_WIDTH / 2;
		int y = (int) ProcessLayoutXMLFilter.lookupOperatorRectangle(attachedTo).getMaxY() + OperatorAnnotation.Y_OFFSET;
		int height = AnnotationDrawUtils.getContentHeight(AnnotationDrawUtils.createStyledCommentString(comment, getStyle()),
				OperatorAnnotation.DEFAULT_WIDTH, AnnotationDrawUtils.ANNOTATION_FONT);
		boolean overflowing = false;
		if (height > OperatorAnnotation.MAX_HEIGHT) {
			height = OperatorAnnotation.MAX_HEIGHT;
			overflowing = true;
		}
		// convert to operator annotation
		AnnotationStyle style = getStyle().clone();
		// default yellow color becomes transparent
		if (!wasColored()) {
			style.setAnnotationColor(AnnotationColor.TRANSPARENT);
		}
		OperatorAnnotation opAnno = new OperatorAnnotation(getComment(), style, attachedTo, false, wasColored(), x, y,
				OperatorAnnotation.DEFAULT_WIDTH, Math.max(OperatorAnnotation.MIN_HEIGHT, height));
		opAnno.setOverflowing(overflowing);
		return opAnno;
	}

}
