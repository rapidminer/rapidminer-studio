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
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import com.rapidminer.gui.flow.processrendering.annotations.AnnotationDrawUtils;


/**
 * Simple workflow annotation resize helper class.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public final class AnnotationResizeHelper {

	/**
	 * The direction in which a resize is happening for a process annotation.
	 */
	public static enum ResizeDirection {
		/** started in top left corner */
		TOP_LEFT,

		/** started in top right corner */
		TOP_RIGHT,

		/** started in bottom left corner */
		BOTTOM_LEFT,

		/** started in bottom right corner */
		BOTTOM_RIGHT;
	}

	/** the resized annotation */
	private final WorkflowAnnotation resized;

	/** the direction in which to resize */
	private final ResizeDirection direction;

	/** resize starting point */
	private Point origin;

	/** indicates if actual resizing has taken place */
	private boolean resizeStarted;

	/**
	 * Creates a new resize helper which keeps track of the resizing state.
	 *
	 * @param resized
	 *            the annotation being resized
	 * @param direction
	 *            the resize direction
	 * @param origin
	 *            the location of the annotation before the resize
	 */
	public AnnotationResizeHelper(final WorkflowAnnotation resized, final ResizeDirection direction, final Point origin) {
		if (resized == null) {
			throw new IllegalArgumentException("resized must not be null!");
		}
		if (origin == null) {
			throw new IllegalArgumentException("origin must not be null!");
		}

		this.resized = resized;
		this.direction = direction;
		this.origin = origin;
	}

	/**
	 * Returns the origin point of the resize.
	 *
	 * @return the starting origin, never {@code null}
	 */
	public Point getOrigin() {
		return origin;
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
	 * Returns the resized annotation.
	 *
	 * @return the annotation, never {@code null}
	 */
	public WorkflowAnnotation getResized() {
		return resized;
	}

	/**
	 * Returns the direction in which to resize.
	 *
	 * @return the direction, never {@code null}
	 */
	public ResizeDirection getDirection() {
		return direction;
	}

	/**
	 * Returns whether actual resizing has taken place.
	 *
	 * @return {@code true} if {@link #handleResizeEvent(Point)} has been called at least once;
	 *         {@code false} otherwise
	 */
	public boolean isResizeInProgress() {
		return resizeStarted;
	}

	/**
	 * Handles a resize event based on the given {@link Point}.
	 *
	 * @param point
	 *            the new point
	 */
	public void handleResizeEvent(final Point point) {
		WorkflowAnnotation resizedAnno = getResized();
		if (resizedAnno instanceof OperatorAnnotation) {
			// should not happen
			return;
		}

		Rectangle2D startFrame = resizedAnno.getLocation();
		double xOffset = point.getX() - getOrigin().getX();
		double yOffset = point.getY() - getOrigin().getY();
		double xIncrease = 0;
		double yIncrease = 0;
		double widthIncrease = 0;
		double heightIncrease = 0;
		// calculate new x,y, width, height while keeping min/max width/height in mind
		if (resizedAnno instanceof ProcessAnnotation) {
			switch (getDirection()) {
				case BOTTOM_LEFT:
					if (xOffset >= 0) {
						double newWidth = Math.max(startFrame.getWidth() - xOffset, ProcessAnnotation.MIN_WIDTH);
						widthIncrease = newWidth - startFrame.getWidth();
						xIncrease = -widthIncrease;
					} else {
						double newWidth = Math.min(startFrame.getWidth() - xOffset, ProcessAnnotation.MAX_WIDTH);
						widthIncrease = newWidth - startFrame.getWidth();
						xIncrease = -widthIncrease;
					}
					if (yOffset >= 0) {
						double newHeight = Math.min(startFrame.getHeight() + yOffset, ProcessAnnotation.MAX_HEIGHT);
						heightIncrease = newHeight - startFrame.getHeight();
					} else {
						double newHeight = Math.max(startFrame.getHeight() + yOffset, ProcessAnnotation.MIN_HEIGHT);
						heightIncrease = newHeight - startFrame.getHeight();
					}
					break;
				case BOTTOM_RIGHT:
					if (xOffset >= 0) {
						double newWidth = Math.min(startFrame.getWidth() + xOffset, ProcessAnnotation.MAX_WIDTH);
						widthIncrease = newWidth - startFrame.getWidth();
					} else {
						double newWidth = Math.max(startFrame.getWidth() + xOffset, ProcessAnnotation.MIN_WIDTH);
						widthIncrease = newWidth - startFrame.getWidth();
					}
					if (yOffset >= 0) {
						double newHeight = Math.min(startFrame.getHeight() + yOffset, ProcessAnnotation.MAX_HEIGHT);
						heightIncrease = newHeight - startFrame.getHeight();
					} else {
						double newHeight = Math.max(startFrame.getHeight() + yOffset, ProcessAnnotation.MIN_HEIGHT);
						heightIncrease = newHeight - startFrame.getHeight();
					}
					break;
				case TOP_LEFT:
					if (xOffset >= 0) {
						double newWidth = Math.max(startFrame.getWidth() - xOffset, ProcessAnnotation.MIN_WIDTH);
						widthIncrease = newWidth - startFrame.getWidth();
						xIncrease = -widthIncrease;
					} else {
						double newWidth = Math.min(startFrame.getWidth() - xOffset, ProcessAnnotation.MAX_WIDTH);
						widthIncrease = newWidth - startFrame.getWidth();
						xIncrease = -widthIncrease;
					}
					if (yOffset >= 0) {
						double newHeight = Math.max(startFrame.getHeight() - yOffset, ProcessAnnotation.MIN_HEIGHT);
						heightIncrease = newHeight - startFrame.getHeight();
						yIncrease = -heightIncrease;
					} else {
						double newHeight = Math.min(startFrame.getHeight() - yOffset, ProcessAnnotation.MAX_HEIGHT);
						heightIncrease = newHeight - startFrame.getHeight();
						yIncrease = -heightIncrease;
					}
					break;
				case TOP_RIGHT:
					if (xOffset >= 0) {
						double newWidth = Math.min(startFrame.getWidth() + xOffset, ProcessAnnotation.MAX_WIDTH);
						widthIncrease = newWidth - startFrame.getWidth();
					} else {
						double newWidth = Math.max(startFrame.getWidth() + xOffset, ProcessAnnotation.MIN_WIDTH);
						widthIncrease = newWidth - startFrame.getWidth();
					}
					if (yOffset >= 0) {
						double newHeight = Math.max(startFrame.getHeight() - yOffset, ProcessAnnotation.MIN_HEIGHT);
						heightIncrease = newHeight - startFrame.getHeight();
						yIncrease = -heightIncrease;
					} else {
						double newHeight = Math.min(startFrame.getHeight() - yOffset, ProcessAnnotation.MAX_HEIGHT);
						heightIncrease = newHeight - startFrame.getHeight();
						yIncrease = -heightIncrease;
					}
					break;
				// $CASES-OMITTED$
				default:
					break;
			}
		}

		double newX = startFrame.getX() + xIncrease;
		double newY = startFrame.getY() + yIncrease;
		double newWidth = startFrame.getWidth() + widthIncrease;
		double newHeight = startFrame.getHeight() + heightIncrease;

		// cannot relocate annotation to less than min x/y values
		if (newX < WorkflowAnnotation.MIN_X) {
			// prevent width increase if resize x was less than min value
			if (resizedAnno instanceof ProcessAnnotation) {
				newWidth = newWidth - (WorkflowAnnotation.MIN_X - newX);
				newX = WorkflowAnnotation.MIN_X;
			}
		}
		if (newY < WorkflowAnnotation.MIN_Y) {
			// prevent height increase if resize x was less than min value
			newHeight = newHeight - (WorkflowAnnotation.MIN_Y - newY);
			newY = WorkflowAnnotation.MIN_Y;
		}

		Rectangle2D newLoc = new Rectangle2D.Double(newX, newY, newWidth, newHeight);
		resizedAnno.setLocation(newLoc);
		setOrigin(point);
		resizeStarted = true;
		resizedAnno.setResized();
	}

	/**
	 * Determines if the origin point is in one of resize starting areas, e.g. the corners.
	 *
	 * @param anno
	 *            the annotation which should be checked
	 * @param point
	 *            the location of the mouse
	 * @return the direction or {@code null} if the point was in none of the resize starting areas
	 */
	public static ResizeDirection getResizeDirectionOrNull(final WorkflowAnnotation anno, final Point point) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}
		if (point == null) {
			throw new IllegalArgumentException("point must not be null!");
		}

		int x = (int) anno.getLocation().getX();
		int y = (int) anno.getLocation().getY();
		int maxX = (int) anno.getLocation().getMaxX();
		int maxY = (int) anno.getLocation().getMaxY();

		if (anno instanceof ProcessAnnotation) {
			Shape triangle = new Polygon(new int[] { x, x, x + 20 }, new int[] { y + 20, y, y }, 3);
			if (triangle.contains(point)) {
				return ResizeDirection.TOP_LEFT;
			}
			triangle = new Polygon(new int[] { maxX - 20, maxX, maxX }, new int[] { y, y, y + 20 }, 3);
			if (triangle.contains(point)) {
				return ResizeDirection.TOP_RIGHT;
			}
			triangle = new Polygon(new int[] { x, x, x + 20 }, new int[] { maxY, maxY - 20, maxY }, 3);
			if (triangle.contains(point)) {
				return ResizeDirection.BOTTOM_LEFT;
			}
			triangle = new Polygon(new int[] { maxX, maxX, maxX - 20 }, new int[] { maxY - 20, maxY, maxY }, 3);
			if (triangle.contains(point)) {
				return ResizeDirection.BOTTOM_RIGHT;
			}
		}

		return null;
	}
}
