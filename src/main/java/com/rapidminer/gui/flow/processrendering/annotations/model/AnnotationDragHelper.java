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

import java.awt.Point;
import java.awt.geom.Rectangle2D;

import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;


/**
 * Simple workflow annotation drag helper class.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public final class AnnotationDragHelper {

	/** move an operator annotation at least that much to unsnap it */
	private static final int OPERATOR_ANNOTATION_UNSNAP_DISTANCE = 75;

	/** the dragged annotation */
	private final WorkflowAnnotation dragged;

	/** the process renderer model instance */
	private final ProcessRendererModel model;

	/** the original location of the annotation before dragging started */
	private final Point startingPoint;

	/** drag starting point */
	private Point origin;

	/** operator under the mouse while dragging */
	private Operator hoveredOperator;

	/** indicates if actual dragging has taken place */
	private boolean dragStarted;

	/** indicates if an operator annotation was "unsnapped" from its operator */
	private boolean unsnapped;

	/**
	 * Creates a new drag helper which keeps track of the drag state.
	 *
	 * @param dragged
	 *            the annotation being dragged
	 * @param origin
	 *            the location of the annotation before the drag
	 * @param model
	 *            the process renderer model instance
	 */
	public AnnotationDragHelper(final WorkflowAnnotation dragged, final Point origin, final ProcessRendererModel model) {
		if (dragged == null) {
			throw new IllegalArgumentException("dragged must not be null!");
		}
		if (origin == null) {
			throw new IllegalArgumentException("origin must not be null!");
		}
		if (model == null) {
			throw new IllegalArgumentException("model must not be null!");
		}

		this.dragged = dragged;
		this.origin = origin;
		this.startingPoint = new Point((int) dragged.getLocation().getX(), (int) dragged.getLocation().getY());
		this.model = model;
		// process annotations are always unsnapped
		this.unsnapped = dragged instanceof ProcessAnnotation;
	}

	/**
	 * Returns the last intermediate point of the drag.
	 *
	 * @return the last intermediate point, never {@code null}
	 */
	public Point getOrigin() {
		return origin;
	}

	/**
	 * Returns the absolute starting point of the drag.
	 *
	 * @return the starting point, never {@code null}
	 */
	public Point getStartingPoint() {
		return startingPoint;
	}

	/**
	 * Returns the operator the mouse is currently over while dragging.
	 *
	 * @return the operator or {@code null}
	 */
	public Operator getHoveredOperator() {
		return hoveredOperator;
	}

	/**
	 * Updates the origin.
	 *
	 * @param origin
	 *            the new origin
	 */
	public void setOrigin(final Point origin) {
		this.origin = origin;
	}

	/**
	 * Returns the dragged annotation.
	 *
	 * @return the annotation, never {@code null}
	 */
	public WorkflowAnnotation getDraggedAnnotation() {
		return dragged;
	}

	/**
	 * Returns whether actual dragging has taken place.
	 *
	 * @return {@code true} if {@link #handleDragEvent(Point)} has been called at least once;
	 *         {@code false} otherwise
	 */
	public boolean isDragInProgress() {
		return dragStarted;
	}

	/**
	 * Returns whether an operator annotation was unsnapped from its operator.
	 *
	 * @return {@code true} if it was; {@code false} otherwise
	 */
	public boolean isUnsnapped() {
		return unsnapped;
	}

	/**
	 * Handles a drag event based on the given {@link Point}.
	 *
	 * @param point
	 *            the new point
	 */
	public void handleDragEvent(final Point point) {
		WorkflowAnnotation draggedAnno = getDraggedAnnotation();
		double xOffset = point.getX() - getOrigin().getX();
		double yOffset = point.getY() - getOrigin().getY();

		// we set the drag flag even if not yet unsnapped
		if (xOffset != 0 || yOffset != 0) {
			dragStarted = true;
		}

		// operator annotations need to be dragged a certain amount before they come "loose"
		if (dragged instanceof OperatorAnnotation) {
			if (!unsnapped && Math.abs(xOffset) < OPERATOR_ANNOTATION_UNSNAP_DISTANCE
					&& Math.abs(yOffset) < OPERATOR_ANNOTATION_UNSNAP_DISTANCE) {
				return;
			} else {
				unsnapped = true;
			}
		}

		double newX = draggedAnno.getLocation().getX() + xOffset;
		double newY = draggedAnno.getLocation().getY() + yOffset;
		double width = draggedAnno.getLocation().getWidth();
		double height = draggedAnno.getLocation().getHeight();

		// make sure dragging out of process is not allowed
		if (newX < WorkflowAnnotation.MIN_X) {
			newX = WorkflowAnnotation.MIN_X;
		}
		if (newY < WorkflowAnnotation.MIN_Y) {
			newY = WorkflowAnnotation.MIN_Y;
		}

		// check if we are hovering over an operator which does NOT have an annotation
		if (model.getHoveringProcessIndex() != -1) {
			ExecutionUnit process = model.getProcess(model.getHoveringProcessIndex());
			if (process != null) {
				hoveredOperator = null;
				for (Operator op : process.getOperators()) {
					if (model.getOperatorRect(op) != null
							&& model.getOperatorRect(op).contains(model.getMousePositionRelativeToProcess())) {
						if (model.getOperatorAnnotations(op) == null || model.getOperatorAnnotations(op).isEmpty()) {
							hoveredOperator = op;
							break;
						} else if (model.getOperatorAnnotations(op).getAnnotationsDrawOrder().contains(draggedAnno)) {
							// our own origin is a valid target as well
							hoveredOperator = op;
							break;
						}
					}
				}
			}
		}

		Rectangle2D newLoc = new Rectangle2D.Double(newX, newY, width, height);
		draggedAnno.setLocation(newLoc);
		setOrigin(point);
	}

}
