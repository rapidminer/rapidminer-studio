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
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.flow.processrendering.annotations.AnnotationDrawUtils;
import com.rapidminer.gui.flow.processrendering.annotations.AnnotationsDecorator;
import com.rapidminer.gui.flow.processrendering.annotations.model.AnnotationResizeHelper.ResizeDirection;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationAlignment;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationColor;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.container.Pair;


/**
 * The model backing the {@link AnnotationsDecorator}.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public class AnnotationsModel {

	/** currently hovered annotation or {@code null} */
	private WorkflowAnnotation hovered;

	/** the resize direction currently being hovered or {@code null} */
	private ResizeDirection hoveredResizeDirection;

	/** currently selected annotation or {@code null} */
	private WorkflowAnnotation selected;

	/** the annotation currently being dragged or {@code null} */
	private AnnotationDragHelper dragged;

	/** the annotation currently being resized or {@code null} */
	private AnnotationResizeHelper resized;

	/** the process renderer model */
	private ProcessRendererModel model;

	/** a map between annotations (identified via ID) and their displayed hyperlink urls and bounds */
	private Map<UUID, List<Pair<String, Rectangle>>> hyperlinkBounds = new HashMap<>();

	/** the currently hovered hyperlink with url and bounds */
	private Pair<String, Rectangle> hoveredHyperLink;


	/**
	 * Creates a new model backing the workflow annotations.
	 *
	 * @param model
	 *            the process renderer model
	 */
	public AnnotationsModel(ProcessRendererModel model) {
		this.model = model;
	}

	/**
	 * Returns the hovered {@link WorkflowAnnotation}.
	 *
	 * @return the hovered annotation or {@code null}
	 */
	public WorkflowAnnotation getHovered() {
		return hovered;
	}

	/**
	 * Sets the hovered annotation. If it changes, fires a process renderer model misc event to
	 * trigger a repaint.
	 *
	 * @param hovered
	 *            the new hovered annotation, can be {@code null}
	 * @param hoveredResizeDirection
	 *            the hovered resize corner, can be {@code null}
	 */
	public void setHovered(final WorkflowAnnotation hovered, final ResizeDirection hoveredResizeDirection) {
		if (hovered == null) {
			if (this.hovered != null) {
				this.hovered = null;
				setHoveredResizeDirection(null);
				setHoveredHyperLink(null);
				model.fireAnnotationMiscChanged(null);
			}
		} else {
			if (!hovered.equals(this.hovered)) {
				this.hovered = hovered;
				setHoveredHyperLink(null);
				if (hovered.equals(selected)) {
					setHoveredResizeDirection(hoveredResizeDirection);
				} else {
					setHoveredResizeDirection(null);
				}
				model.fireAnnotationMiscChanged(hovered);
			} else {
				if (hovered.equals(selected)) {
					setHoveredResizeDirection(hoveredResizeDirection);
				}
			}
		}
	}

	/**
	 * Sets the list of hyperlink bounds for the given annotation.
	 *
	 * @param id
	 * 		the id of the annotation (see {@link WorkflowAnnotation#getId()} for which the bounds should be stored.
	 * @param bounds
	 * 		the bounds with the URL as a string. Can be empty for no bounds. Note that they are stored as if a zoom level of 100% was set,
	 * 		regardless of actual zoom level. Only bounds are stored that are actually visible (i.e. are not cut off by the
	 * 		"..." dots)
	 * @since 9.0.0
	 */
	public void setHyperlinkBoundsForAnnotation(UUID id, List<Pair<String, Rectangle>> bounds) {
		if (id == null) {
			throw new IllegalArgumentException("id must not be null!");
		}
		if (bounds == null) {
			throw new IllegalArgumentException("bounds must not be null!");
		}
		hyperlinkBounds.put(id, bounds);
	}

	/**
	 * Gets the list of hyperlink bounds for the given annotation.
	 *
	 * @param id
	 * 		the id of the annotation (see {@link WorkflowAnnotation#getId()} for which the bounds should be retrieved.
	 * @return the bounds with the URL as a string, never {@code null}. Can be empty for no bounds. Note that they are stored as if a zoom level of 100% was
	 * set, regardless of actual zoom level. Only bounds are stored that are actually visible (i.e. are not cut off by
	 * the "..." dots)
	 * @since 9.0.0
	 */
	public List<Pair<String, Rectangle>> getHyperlinkBoundsForAnnotation(UUID id) {
		if (id == null) {
			throw new IllegalArgumentException("id must not be null!");
		}
		return hyperlinkBounds.computeIfAbsent(id, notUsed -> Collections.emptyList());
	}

	/**
	 * Gets the hovered hyperlink.
	 *
	 * @return the hovered hyperlink url and its bounds, or {@code null} if no hyperlink is hovered
	 * @since 9.0.0
	 */
	public Pair<String, Rectangle> getHoveredHyperLink() {
		return hoveredHyperLink;
	}

	/**
	 * Sets the hovered hyperlink.
	 *
	 * @param hoveredHyperLink
	 * 		the hovered hyperlink url and its bounds, or {@code null} if no hyperlink is hovered
	 * @since 9.0.0
	 */
	public void setHoveredHyperLink(Pair<String, Rectangle> hoveredHyperLink) {
		if (hoveredHyperLink != null && hoveredHyperLink.getFirst() == null) {
			throw new IllegalArgumentException("url in hoveredHyperLink must not be null!");
		}
		if (hoveredHyperLink != null && hoveredHyperLink.getSecond() == null) {
			throw new IllegalArgumentException("bounds in hoveredHyperLink must not be null!");
		}
		if (this.hoveredHyperLink == hoveredHyperLink) {
			return;
		}

		this.hoveredHyperLink = hoveredHyperLink;
		model.fireAnnotationMiscChanged(null);
	}

	/**
	 * Returns the resize direction of the hovered annotation.
	 *
	 * @return the resize direction or {@code null}
	 */
	public ResizeDirection getHoveredResizeDirection() {
		return hoveredResizeDirection;
	}

	/**
	 * Sets the hovered resize direction. If it changes, fires a process renderer model misc event
	 * to trigger a repaint.
	 *
	 * @param hoveredResizeDirection
	 *            the hovered resize direction
	 */
	public void setHoveredResizeDirection(final ResizeDirection hoveredResizeDirection) {
		if (hoveredResizeDirection != null) {
			if (!hoveredResizeDirection.equals(this.hoveredResizeDirection)) {
				this.hoveredResizeDirection = hoveredResizeDirection;
				model.fireAnnotationMiscChanged(null);
			}
		} else {
			if (this.hoveredResizeDirection != null) {
				this.hoveredResizeDirection = null;
				model.fireAnnotationMiscChanged(null);
			}
		}
	}

	/**
	 * Returns the selected {@link WorkflowAnnotation}.
	 *
	 * @return the selected annotation or {@code null}
	 */
	public WorkflowAnnotation getSelected() {
		return selected;
	}

	/**
	 * Sets the selected {@link WorkflowAnnotation}. If it changes, fires a process renderer model
	 * annotation selection event to trigger a repaint.
	 *
	 * @param selected
	 *            the selected annotation or {@code null}
	 */
	public void setSelected(WorkflowAnnotation selected) {
		if (selected == null) {
			if (getSelected() != null) {
				this.selected = null;
				model.fireAnnotationSelected(null);
			}
		} else {
			if (!selected.equals(this.selected)) {
				this.selected = selected;
				model.fireAnnotationSelected(selected);
			}
		}
	}

	/**
	 * Returns the drag helper if an annotation is currently being dragged.
	 *
	 * @return the drag helper or {@code null}
	 */
	public AnnotationDragHelper getDragged() {
		return dragged;
	}

	/**
	 * Sets the drag helper if an annotation is currently being dragged.
	 *
	 * @param dragged
	 *            the drag helper or {@code null}
	 */
	public void setDragged(AnnotationDragHelper dragged) {
		this.dragged = dragged;
	}

	/**
	 * Returns the resize helper if an annotation is currently being resized.
	 *
	 * @return the resize helper or {@code null}
	 */
	public AnnotationResizeHelper getResized() {
		return resized;
	}

	/**
	 * Sets the resize helper if an annotation is currently being resized.
	 *
	 * @param resized
	 *            the resize helper or {@code null}
	 */
	public void setResized(AnnotationResizeHelper resized) {
		this.resized = resized;
	}

	/**
	 * Starts a drag or resizing of the selected annotation, depending on whether the drag starts on
	 * the annotation or one of the resize "knobs". If no annotation is selected, does nothing. If
	 * the triggering action was not a left-click, does nothing.
	 *
	 * @param e
	 *            the mouse event triggering the drag/resize
	 * @param origin
	 *            the origin of the drag/resize event
	 * @param allowResize
	 *            if {@code true}, resize is allowed. Otherwise only a drag can be started
	 */
	public void startDragOrResize(final MouseEvent e, final Point origin, boolean allowResize) {
		if (getSelected() == null) {
			return;
		}
		if (!SwingUtilities.isLeftMouseButton(e)) {
			return;
		}

		// manual resizing is NEVER permitted for operator annotations
		if (getSelected() instanceof OperatorAnnotation) {
			allowResize = false;
		}
		ResizeDirection direction = null;
		if (allowResize) {
			direction = AnnotationResizeHelper.getResizeDirectionOrNull(getSelected(), origin);
		}
		if (direction != null) {
			resized = new AnnotationResizeHelper(selected, direction, origin);
		} else {
			dragged = new AnnotationDragHelper(selected, origin, model);
		}
	}

	/**
	 * Updates the dragged position or the resizing of the selected annotation and fires a misc
	 * model change for the process renderer. If no drag and resizing is in progress, does nothing.
	 *
	 * @param point
	 *            the current location
	 */
	public void updateDragOrResize(final Point point) {
		if (dragged == null && resized == null) {
			return;
		}

		if (dragged != null) {
			dragged.handleDragEvent(point);
			// fire moved event
			model.fireAnnotationMoved(dragged.getDraggedAnnotation());
		} else if (resized != null) {
			resized.handleResizeEvent(point);
			// fire moved event
			model.fireAnnotationMoved(resized.getResized());
		}

	}

	/**
	 * Stops the drag or resizing of the selected annotation. If neither was in progress, does
	 * nothing.
	 *
	 * @param destination
	 *            the final destination of the drag/resize event. If {@code null}, dragging is
	 *            assumed to be cancelled and no conversion from operator to process annotation or
	 *            vice versa is performed.
	 */
	public void stopDragOrResize(final Point destination) {
		if (dragged == null && resized == null) {
			return;
		}

		if (destination != null) {
			updateDragOrResize(destination);
		}

		// trigger process event so it becomes dirty
		if (dragged != null) {
			WorkflowAnnotation draggedAnno = dragged.getDraggedAnnotation();
			// if we stop over an operator
			if (dragged.getHoveredOperator() != null) {
				if (draggedAnno instanceof ProcessAnnotation) {
					if (destination != null) {
						// delete process annotation if drag was not cancelled
						deleteAnnotation(draggedAnno);
						addOperatorAnnotation(draggedAnno.createOperatorAnnotation(dragged.getHoveredOperator()));
					} else {
						moveProcessAnnoToPoint((ProcessAnnotation) draggedAnno, dragged.getOrigin(),
								dragged.getStartingPoint());
					}
				} else if (draggedAnno instanceof OperatorAnnotation) {
					OperatorAnnotation opAnno = (OperatorAnnotation) draggedAnno;
					if (destination != null) {
						// remove from original operator
						model.removeOperatorAnnotation(opAnno);
						moveOperatorAnnoToOperator(opAnno, dragged.getHoveredOperator());
						// attach to new operator
						opAnno.setAttachedTo(dragged.getHoveredOperator());
						model.addOperatorAnnotation(opAnno);
					} else {
						// destination is null = cancelled dragging
						moveOperatorAnnoToOperator(opAnno, opAnno.getAttachedTo());
					}
					opAnno.fireUpdate();
				}
			} else {
				// we did not stop over an operator and dragged an annotation
				if (destination == null) {
					// we cancelled dragging via ESC -> reset to original position
					if (draggedAnno instanceof OperatorAnnotation) {
						moveOperatorAnnoToOperator((OperatorAnnotation) draggedAnno,
								((OperatorAnnotation) draggedAnno).getAttachedTo());
					} else if (draggedAnno instanceof ProcessAnnotation) {
						moveProcessAnnoToPoint((ProcessAnnotation) draggedAnno, dragged.getOrigin(),
								dragged.getStartingPoint());
					}
				} else {
					if (draggedAnno instanceof OperatorAnnotation && dragged.isUnsnapped()) {
						// an operator annotation was dragged away from an operator
						// convert to process annotation
						deleteAnnotation(draggedAnno);
						addProcessAnnotation(draggedAnno.createProcessAnnotation(draggedAnno.getProcess()));
					} else if (draggedAnno instanceof ProcessAnnotation) {
						// notify process of change
						draggedAnno.fireUpdate();
					}
				}
			}
			// otherwise we might end up with a selection rectangle here
			model.setSelectionRectangle(null);
		} else if (resized != null) {
			resized.getResized().fireUpdate();
		}

		if (resized != null) {
			WorkflowAnnotation anno = resized.getResized();
			int prefHeight = AnnotationDrawUtils.getContentHeight(AnnotationDrawUtils.createStyledCommentString(
					anno.getComment(), anno.getStyle()), (int) anno.getLocation().getWidth(), AnnotationDrawUtils.ANNOTATION_FONT);
			boolean overflowing = false;
			if (prefHeight > anno.getLocation().getHeight()) {
				overflowing = true;
			}
			anno.setOverflowing(overflowing);
		}

		// reset
		dragged = null;
		resized = null;
	}

	/**
	 * Deletes the given annotation and fires updates.
	 *
	 * @param toDelete
	 *            the annotation to delete
	 *
	 */
	public void deleteAnnotation(final WorkflowAnnotation toDelete) {
		if (toDelete == null) {
			throw new IllegalArgumentException("toDelete must not be null!");
		}

		if (toDelete instanceof OperatorAnnotation) {
			OperatorAnnotation anno = (OperatorAnnotation) toDelete;
			model.removeOperatorAnnotation(anno);
		} else if (toDelete instanceof ProcessAnnotation) {
			ProcessAnnotation anno = (ProcessAnnotation) toDelete;
			model.removeProcessAnnotation(anno);
		}
		setSelected(null);

		fireProcessUpdate(toDelete);
		model.fireAnnotationMiscChanged(null);
	}

	/**
	 * Adds the given operator annotation and fires updates.
	 *
	 * @param anno
	 *            the annotation to add
	 */
	public void addOperatorAnnotation(final OperatorAnnotation anno) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}

		model.addOperatorAnnotation(anno);
		setSelected(anno);

		fireProcessUpdate(anno);
		model.fireAnnotationMoved(anno);
	}

	/**
	 * Adds the given process annotation and fires updates.
	 *
	 * @param anno
	 *            the annotation to add
	 */
	public void addProcessAnnotation(final ProcessAnnotation anno) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}

		model.addProcessAnnotation(anno);
		setSelected(anno);

		fireProcessUpdate(anno);
		model.fireAnnotationMoved(anno);
	}

	/**
	 * Sets the color of the annotation and fires an event afterwards.
	 *
	 * @param anno
	 *            the annotation which will have its color changed
	 * @param color
	 *            the new color
	 */
	public void setAnnotationColor(final WorkflowAnnotation anno, final AnnotationColor color) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}
		if (color == null) {
			throw new IllegalArgumentException("color must not be null!");
		}

		anno.getStyle().setAnnotationColor(color);
		anno.setColored();

		fireProcessUpdate(anno);
		model.fireAnnotationMiscChanged(anno);
	}

	/**
	 * Sets the alignment of the annotation and fires an event afterwards.
	 *
	 * @param anno
	 *            the annotation which will have its alignment changed
	 * @param alignment
	 *            the new alignment
	 */
	public void setAnnotationAlignment(final WorkflowAnnotation anno, final AnnotationAlignment alignment) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}
		if (alignment == null) {
			throw new IllegalArgumentException("alignment must not be null!");
		}

		anno.getStyle().setAnnotationAlignment(alignment);

		fireProcessUpdate(anno);
		model.fireAnnotationMiscChanged(anno);
	}

	/**
	 * Sets the comment of the annotation and fires an event afterwards.
	 *
	 * @param anno
	 *            the annotation which will have its comment changed
	 * @param comment
	 *            the new comment
	 */
	public void setAnnotationComment(final WorkflowAnnotation anno, final String comment) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}
		if (comment == null) {
			throw new IllegalArgumentException("comment must not be null!");
		}

		anno.setComment(comment);

		fireProcessUpdate(anno);
		model.fireAnnotationMoved(anno);
	}

	/**
	 * Bring the given annotation to the front. That annotation will be drawn over all other
	 * annotations as well as receive events first.
	 *
	 * @param anno
	 *            the annotation to bring to the front
	 */
	public void toFront(final WorkflowAnnotation anno) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}
		model.getProcessAnnotations(anno.getProcess()).toFront(anno);

		fireProcessUpdate(anno);
		model.fireAnnotationMiscChanged(anno);
	}

	/**
	 * Brings the given annotation one layer forward.
	 *
	 * @param anno
	 *            the annotation to bring forward
	 */
	public void sendForward(final WorkflowAnnotation anno) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}
		model.getProcessAnnotations(anno.getProcess()).sendForward(anno);

		fireProcessUpdate(anno);
		model.fireAnnotationMiscChanged(anno);
	}

	/**
	 * Bring the given annotation to the back. That annotation will be drawn behind all other
	 * annotations as well as receive events last.
	 *
	 * @param anno
	 *            the annotation to bring to the front
	 */
	public void toBack(final WorkflowAnnotation anno) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}
		model.getProcessAnnotations(anno.getProcess()).toBack(anno);

		fireProcessUpdate(anno);
		model.fireAnnotationMiscChanged(anno);
	}

	/**
	 * Sends the given annotation one layer backward.
	 *
	 * @param anno
	 *            the annotation to send backward
	 */
	public void sendBack(final WorkflowAnnotation anno) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}
		model.getProcessAnnotations(anno.getProcess()).sendBack(anno);

		fireProcessUpdate(anno);
		model.fireAnnotationMiscChanged(anno);
	}

	/**
	 * Resets model status as if the model was newly created
	 */
	public void reset() {
		this.hovered = null;
		this.hoveredResizeDirection = null;
		this.selected = null;
		this.dragged = null;
		this.resized = null;
	}

	/**
	 * Moves the {@link OperatorAnnotation} to the target {@link Operator}. Only updates the
	 * location!
	 *
	 * @param opAnno
	 *            the annotation to move
	 * @param target
	 *            the operator to which the annotation should be moved
	 */
	private void moveOperatorAnnoToOperator(final OperatorAnnotation opAnno, final Operator target) {
		int x = (int) (model.getOperatorRect(target).getCenterX() - opAnno.getLocation().getWidth() / 2);
		int y = (int) model.getOperatorRect(target).getMaxY() + OperatorAnnotation.Y_OFFSET;
		opAnno.setLocation(new Rectangle2D.Double(x, y, opAnno.getLocation().getWidth(), opAnno.getLocation().getHeight()));
	}

	/**
	 * Moves the {@link ProcessAnnotation} to the target {@link Point}. Only updates the location!
	 *
	 * @param processAnno
	 *            the annotation to move
	 * @param current
	 *            the current absolute point of the annotation
	 * @param target
	 *            the new absolute point of the annotation
	 */
	private void moveProcessAnnoToPoint(final ProcessAnnotation processAnno, final Point current, final Point target) {
		processAnno.setLocation(new Rectangle2D.Double(target.getX(), target.getY(), processAnno.getLocation().getWidth(),
				processAnno.getLocation().getHeight()));
	}

	/**
	 * Fires an update for a process. If the annotation is not attached to any process, does
	 * nothing.
	 *
	 * @param anno
	 *            the annotation which triggered the update
	 */
	private void fireProcessUpdate(final WorkflowAnnotation anno) {
		ExecutionUnit process = anno.getProcess();

		if (process != null) {
			// dirty hack to trigger a process update
			process.getEnclosingOperator().rename(process.getEnclosingOperator().getName());
		}
	}
}
