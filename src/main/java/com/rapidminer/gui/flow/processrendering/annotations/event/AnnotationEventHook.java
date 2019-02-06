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
package com.rapidminer.gui.flow.processrendering.annotations.event;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.flow.processrendering.annotations.AnnotationDrawer;
import com.rapidminer.gui.flow.processrendering.annotations.AnnotationsDecorator;
import com.rapidminer.gui.flow.processrendering.annotations.AnnotationsVisualizer;
import com.rapidminer.gui.flow.processrendering.annotations.model.AnnotationResizeHelper;
import com.rapidminer.gui.flow.processrendering.annotations.model.AnnotationsModel;
import com.rapidminer.gui.flow.processrendering.annotations.model.OperatorAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.ProcessAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotations;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationStyle;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererAnnotationEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererEventListener;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererModelEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererOperatorEvent;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessEventDecorator;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.flow.processrendering.view.RenderPhase;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.RMUrlHandler;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;
import com.rapidminer.tools.container.Pair;


/**
 * This class handles event hooks registered to the {@link ProcessRendererView} for workflow
 * annotations.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public final class AnnotationEventHook {

	/** the annotations decorator */
	private final AnnotationsDecorator decorator;

	/** the annotation handler */
	private final AnnotationsVisualizer visualizer;

	/** the annotations model */
	private final AnnotationsModel model;

	/** the annotation drawer */
	private final AnnotationDrawer drawer;

	/** the process renderer */
	private final ProcessRendererView view;

	/** the process renderer model */
	private final ProcessRendererModel rendererModel;

	/** handles events for non-selected process annotations */
	private ProcessEventDecorator processAnnotationEvents = new ProcessEventDecorator() {

		@Override
		public void processMouseEvent(final ExecutionUnit process, final MouseEventType type, final MouseEvent e) {
			if (!visualizer.isActive()) {
				return;
			}

			Point point = rendererModel.getMousePositionRelativeToProcess();
			if (point == null) {
				point = e.getPoint();
			}

			switch (type) {
				case MOUSE_CLICKED:
					if (!SwingUtilities.isLeftMouseButton(e)) {
						break;
					}
					if (process != null && e.getClickCount() >= 2) {
						if (!AnnotationDrawer.isProcessInteractionHappening(rendererModel)) {
							double x = Math.max(WorkflowAnnotation.MIN_X, point.getX());
							double y = Math.max(WorkflowAnnotation.MIN_Y, point.getY());
							ProcessAnnotation anno = new ProcessAnnotation(
									I18N.getGUILabel("workflow.annotation.default_text.label"), new AnnotationStyle(),
									process, false, false, new Rectangle2D.Double(x, y, ProcessAnnotation.DEFAULT_WIDTH,
											ProcessAnnotation.DEFAULT_HEIGHT));
							model.addProcessAnnotation(anno);
							decorator.editSelected();
							e.consume();
						}
					}
					break;
				case MOUSE_ENTERED:
				case MOUSE_MOVED:
					if (process != null) {
						WorkflowAnnotations annotations = rendererModel.getProcessAnnotations(process);
						if (updateHoveredStatus(point, process, annotations)) {
							updateHyperlinkHoverStatus(point);
							e.consume();
						} else {
							model.setHovered(null, null);
						}
					}
					break;
				case MOUSE_EXITED:
					if (!SwingTools.isMouseEventExitedToChildComponents(view, e)) {
						model.setHovered(null, null);
					}
					break;
				case MOUSE_DRAGGED:
					model.setHovered(null, null);
					break;
				case MOUSE_PRESSED:
					if ((SwingTools.isControlOrMetaDown(e) || e.isShiftDown()) && e.getButton() == 1) {
						return;
					}
					if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)) {
						handleMousePressedForUnselectedAnnotations(e, point);
					}
					break;
				case MOUSE_RELEASED:
				default:
					break;
			}
		}

		@Override
		public void processKeyEvent(final ExecutionUnit process, final KeyEventType type, final KeyEvent e) {
			// not interested
		}
	};

	/** handles events for non-selected operator annotations */
	private ProcessEventDecorator operatorAnnotationEvents = new ProcessEventDecorator() {

		@Override
		public void processMouseEvent(final ExecutionUnit process, final MouseEventType type, final MouseEvent e) {
			if (!visualizer.isActive()) {
				return;
			}

			Point point = rendererModel.getMousePositionRelativeToProcess();
			if (point == null) {
				point = e.getPoint();
			}

			switch (type) {
				case MOUSE_CLICKED:
					break;
				case MOUSE_ENTERED:
				case MOUSE_MOVED:
					if (process != null) {
						List<Operator> selectedOperators = rendererModel.getSelectedOperators();
						// selected operators annotations are drawn over non selected ones, so
						// handle them first
						for (Operator selOp : selectedOperators) {
							WorkflowAnnotations annotations = rendererModel.getOperatorAnnotations(selOp);
							if (updateHoveredStatus(point, process, annotations)) {
								updateHyperlinkHoverStatus(point);
								e.consume();
								return;
							}
						}
						for (Operator op : process.getOperators()) {
							if (selectedOperators.contains(op)) {
								continue;
							}
							WorkflowAnnotations annotations = rendererModel.getOperatorAnnotations(op);
							if (updateHoveredStatus(point, process, annotations)) {
								updateHyperlinkHoverStatus(point);
								e.consume();
								return;
							}
						}
					}
					break;
				case MOUSE_EXITED:
					break;
				case MOUSE_DRAGGED:
					break;
				case MOUSE_PRESSED:
					if (SwingTools.isControlOrMetaDown(e) || e.isShiftDown()) {
						return;
					}
					if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)) {
						if (model.getHovered() instanceof ProcessAnnotation) {
							return;
						}
						handleMousePressedForUnselectedAnnotations(e, point);
					}
					break;
				case MOUSE_RELEASED:
				default:
					break;
			}
		}

		@Override
		public void processKeyEvent(final ExecutionUnit process, final KeyEventType type, final KeyEvent e) {
			// not interested
		}
	};

	/** handles events for selected annotations */
	private ProcessEventDecorator workflowAnnotationSelectedEvents = new ProcessEventDecorator() {

		@Override
		public void processMouseEvent(final ExecutionUnit process, final MouseEventType type, final MouseEvent e) {
			if (!visualizer.isActive()) {
				return;
			}
			WorkflowAnnotation selected = model.getSelected();
			if (selected == null) {
				return;
			}

			Point point = rendererModel.getMousePositionRelativeToProcess();
			if (point == null) {
				point = e.getPoint();
			}

			switch (type) {
				case MOUSE_ENTERED:
				case MOUSE_EXITED:
				case MOUSE_MOVED:
					// only handle events over the selected annotation
					if (!selected.getLocation().contains(point)
							|| !selected.getProcess().equals(process)) {
						return;
					}
					// always consume
					e.consume();

					if (process != null) {
						WorkflowAnnotations annotations = rendererModel.getProcessAnnotations(process);
						if (!updateHoveredStatus(point, process, annotations)) {
							model.setHovered(null, null);
						} else {
							updateHyperlinkHoverStatus(point);
						}
					}
					break;
				case MOUSE_DRAGGED:
					if (model.getDragged() != null || model.getResized() != null) {
						model.updateDragOrResize(point);
						// only consume if we actually started a drag
						if (model.getDragged() != null && model.getDragged().isDragInProgress()) {
							e.consume();
						}
					} else {
						if (process != null) {
							WorkflowAnnotations annotations = rendererModel.getProcessAnnotations(process);
							if (!updateHoveredStatus(point, process, annotations)) {
								model.setHovered(null, null);
							}
						}
					}
					break;
				case MOUSE_CLICKED:
					// only handle events over the selected annotation
					if (!selected.getLocation().contains(point)
							|| !selected.getProcess().equals(process)) {
						return;
					}

					// always consume if we have a selected annotation
					if (e.getClickCount() >= 2) {
						decorator.editSelected();
						e.consume();
					}
					break;
				case MOUSE_PRESSED:
					// only handle events over the selected annotation
					if (!selected.getLocation().contains(point)
							|| !selected.getProcess().equals(process)) {
						return;
					}

					if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)) {
						if (SwingUtilities.isLeftMouseButton(e) && model.getHoveredResizeDirection() == null && model.getHoveredHyperLink() != null) {
							RMUrlHandler.handleUrl(model.getHoveredHyperLink().getFirst());
							e.consume();
							return;
						}
						// only allow popup trigger to pass through
						if (e.isPopupTrigger()) {
							if (visualizer.showPopupMenu(e)) {
								e.consume();
							}
							return;
						} else {
							model.startDragOrResize(e, point, true);
							e.consume();
						}
					}
					break;
				case MOUSE_RELEASED:
					view.setCursor(Cursor.getDefaultCursor());
					// always stop drag or resize at this point
					model.stopDragOrResize(point);

					// apart from that, only handle events over the selected annotation
					if (!selected.getLocation().contains(point)
							|| !selected.getProcess().equals(process)) {
						return;
					}

					// only allow popup trigger to pass through
					if (e.isPopupTrigger()) {
						if (visualizer.showPopupMenu(e)) {
							e.consume();
						}
						return;
					} else {
						e.consume();
					}
					break;
				default:
					break;
			}
		}

		@Override
		public void processKeyEvent(final ExecutionUnit process, final KeyEventType type, final KeyEvent e) {
			if (!visualizer.isActive()) {
				return;
			}
			if (type != KeyEventType.KEY_PRESSED) {
				return;
			}
			if (model.getSelected() == null) {
				return;
			}

			switch (e.getKeyCode()) {
				case KeyEvent.VK_F2:
					decorator.editSelected();
					e.consume();
					break;
				case KeyEvent.VK_BACK_SPACE:
					if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX) {
						model.deleteAnnotation(model.getSelected());
						model.setResized(null);
						model.setDragged(null);
						e.consume();
					}
					break;
				case KeyEvent.VK_DELETE:
					model.deleteAnnotation(model.getSelected());
					model.setResized(null);
					model.setDragged(null);
					e.consume();
					break;
				case KeyEvent.VK_ESCAPE:
					model.setSelected(null);
					model.stopDragOrResize(null);
					e.consume();
					break;
				default:
					break;
			}

		}

	};

	/** listener to be notified of process renderer model events, e.g. operator movements */
	private ProcessRendererEventListener modelListener = new ProcessRendererEventListener() {

		@Override
		public void operatorsChanged(final ProcessRendererOperatorEvent e, final Collection<Operator> operators) {
			switch (e.getEventType()) {
				case OPERATORS_MOVED:
				case PORTS_CHANGED:
					List<WorkflowAnnotation> movedAnnos = positionOperatorAnnotations(operators);
					rendererModel.fireAnnotationsMoved(movedAnnos);
					break;
				case SELECTED_OPERATORS_CHANGED:
					model.setSelected(null);
					break;
				default:
					break;

			}
		}

		@Override
		public void modelChanged(final ProcessRendererModelEvent e) {
			switch (e.getEventType()) {
				case DISPLAYED_CHAIN_CHANGED:
				case DISPLAYED_PROCESSES_CHANGED:
					model.reset();
					decorator.reset();
					drawer.reset();
					List<WorkflowAnnotation> movedAnnos = positionOperatorAnnotations(
							rendererModel.getDisplayedChain().getAllInnerOperators());
					rendererModel.fireAnnotationsMoved(movedAnnos);
					break;
				case PROCESS_ZOOM_CHANGED:
					decorator.reset();
					break;
				case MISC_CHANGED:
				case PROCESS_SIZE_CHANGED:
				case DISPLAYED_CHAIN_WILL_CHANGE:
				default:
					break;

			}
		}

		@Override
		public void annotationsChanged(final ProcessRendererAnnotationEvent e,
				final Collection<WorkflowAnnotation> annotations) {
			// ignore
		}
	};

	public AnnotationEventHook(final AnnotationsDecorator decorator, final AnnotationsModel model,
			final AnnotationsVisualizer visualizer, final AnnotationDrawer drawer, final ProcessRendererView view,
			final ProcessRendererModel rendererModel) {
		this.decorator = decorator;
		this.model = model;
		this.visualizer = visualizer;
		this.drawer = drawer;
		this.view = view;
		this.rendererModel = rendererModel;
	}

	/**
	 * Registers the event hooks and draw decorators to the process renderer.
	 */
	public void registerDecorators() {
		view.addEventDecorator(processAnnotationEvents, RenderPhase.ANNOTATIONS);
		view.addEventDecorator(operatorAnnotationEvents, RenderPhase.OPERATOR_ANNOTATIONS);
		view.addEventDecorator(workflowAnnotationSelectedEvents, RenderPhase.OVERLAY);

		rendererModel.registerEventListener(modelListener);
	}

	/**
	 * Removes the event hooks and draw decorators from the process renderer.
	 */
	public void unregisterEventHooks() {
		view.removeEventDecorator(processAnnotationEvents, RenderPhase.ANNOTATIONS);
		view.removeEventDecorator(operatorAnnotationEvents, RenderPhase.OPERATOR_ANNOTATIONS);
		view.removeEventDecorator(workflowAnnotationSelectedEvents, RenderPhase.OVERLAY);

		rendererModel.removeEventListener(modelListener);
	}

	/**
	 * Updates the hovered annotation.
	 *
	 * @param point
	 *            the location of the mouse
	 * @param process
	 *            the process being hovered
	 * @param annotations
	 *            the annotations container, can be {@code null}
	 * @return {@code true} if we are hovering over an annotation; {@code false} otherwise
	 */
	private boolean updateHoveredStatus(final Point point, final ExecutionUnit process,
			final WorkflowAnnotations annotations) {
		if (annotations != null) {
			// if we have a selected annotation, always check that first for hovering
			if (model.getSelected() != null && model.getSelected().getProcess().equals(process)) {
				if (model.getSelected().getLocation().contains(point)) {
					model.setHovered(model.getSelected(),
							AnnotationResizeHelper.getResizeDirectionOrNull(model.getSelected(), point));
					return true;
				}
			}

			// non-selected annotations
			for (WorkflowAnnotation anno : annotations.getAnnotationsEventOrder()) {
				// first one we find is hovered
				if (anno.getLocation().contains(point)) {
					model.setHovered(anno, AnnotationResizeHelper.getResizeDirectionOrNull(anno, point));
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Updates the positions of all operator annotations for the given operators.
	 *
	 * @param operators
	 *            the operators for which to reposition the annotations
	 * @return the list of annotations that have actually changed position
	 */
	private List<WorkflowAnnotation> positionOperatorAnnotations(final Collection<Operator> operators) {
		List<WorkflowAnnotation> movedAnnos = new LinkedList<>();
		for (Operator op : operators) {
			WorkflowAnnotations annotations = rendererModel.getOperatorAnnotations(op);
			if (annotations != null) {
				Rectangle2D opRect = rendererModel.getOperatorRect(op);
				for (WorkflowAnnotation anno : annotations.getAnnotationsDrawOrder()) {
					Rectangle2D loc = anno.getLocation();
					double annoCenter = loc.getCenterX();
					double opCenter = opRect.getCenterX();
					double newX = loc.getX() + (opCenter - annoCenter);
					double newY = opRect.getMaxY() + OperatorAnnotation.Y_OFFSET;

					// move if they really changed
					if (loc.getX() != newX || loc.getY() != newY) {
						anno.setLocation(new Rectangle2D.Double(newX, newY, loc.getWidth(), loc.getHeight()));
						movedAnnos.add(anno);
					}
				}
			}
		}
		return movedAnnos;
	}

	/**
	 * Handles a MousePressed event for annotations which are not selected.
	 *
	 * @param e
	 * 		the mouse event
	 * @param point
	 * 		the mouse position relative to the clicked process
	 */
	private void handleMousePressedForUnselectedAnnotations(MouseEvent e, Point point) {
		if (model.getHovered() != null) {
			model.setSelected(model.getHovered());
			if (SwingUtilities.isLeftMouseButton(e) && model.getHoveredHyperLink() != null) {
				RMUrlHandler.handleUrl(model.getHoveredHyperLink().getFirst());
				return;
			}
			model.startDragOrResize(e, point, false);
			e.consume();

			// linux/mac only, otherwise the first click will only select
			if (e.isPopupTrigger()) {
				visualizer.showPopupMenu(e);
			}
		} else {
			if (model.getSelected() != null) {
				model.setSelected(null);
				if (!e.isPopupTrigger()) {
					e.consume();
				}
			}
		}
	}

	/**
	 * Update hovering status of hyperlinks in annotations. Only call after a directly previous call to {@link
	 * #updateHoveredStatus(Point, ExecutionUnit, WorkflowAnnotations)} returned {@code true}!
	 *
	 * @param relativeMousePosition
	 * 		the current relative mouse position, already includes zoom level math
	 */
	private void updateHyperlinkHoverStatus(Point relativeMousePosition) {
		WorkflowAnnotation hoveredAnnotation = model.getHovered();
		Rectangle2D loc = hoveredAnnotation.getLocation();
		List<Pair<String, Rectangle>> hyperlinkBoundsForAnnotation = model.getHyperlinkBoundsForAnnotation(hoveredAnnotation.getId());
		for (Pair<String, Rectangle> pair : hyperlinkBoundsForAnnotation) {
			Rectangle hyperLinkRect = pair.getSecond();
			// adapt to annotation location
			int x = (int) (loc.getX() + hyperLinkRect.getX());
			int y = (int) (loc.getY() + hyperLinkRect.getY());
			int w = (int) hyperLinkRect.getWidth();
			int h = (int) hyperLinkRect.getHeight();
			// sanity checks so that neither width nor height exceeds actual shown content
			if (x + w > loc.getMaxX()) {
				w = (int) (loc.getMaxX() - x);
			}
			if (y + h > loc.getMaxY()) {
				h = (int) (loc.getMaxY() - y);
			}
			hyperLinkRect = new Rectangle(x, y, w, h);
			if (hyperLinkRect.contains(relativeMousePosition)) {
				model.setHoveredHyperLink(pair);
				return;
			} else {
				model.setHoveredHyperLink(null);
			}
		}
	}

}
