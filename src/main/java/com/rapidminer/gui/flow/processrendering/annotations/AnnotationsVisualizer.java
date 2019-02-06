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
package com.rapidminer.gui.flow.processrendering.annotations;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.dnd.OperatorTransferHandler;
import com.rapidminer.gui.flow.FlowVisualizer;
import com.rapidminer.gui.flow.processrendering.annotations.model.AnnotationsModel;
import com.rapidminer.gui.flow.processrendering.annotations.model.OperatorAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.ProcessAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotations;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationAlignment;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationColor;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationStyle;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.I18N;


/**
 * This class manages process annotations which can be added/edited in the
 * {@link ProcessRendererView}.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public final class AnnotationsVisualizer {

	/** margin which removes the space reserved for checkbox icon on menu items */
	private static final Insets MENU_ITEM_MARGIN = new Insets(2, -10, 2, 2);

	/** the process renderer */
	private final ProcessRendererView view;

	private final FlowVisualizer flowVisualizer;

	/** the event hook and draw decorator */
	private final AnnotationsDecorator decorator;

	/** the model backing the annotation decorator */
	private final AnnotationsModel model;

	/** whether annotations are active or not */
	private boolean active;

	/** action to toggle visibility of all notes */
	private final ToggleAction toggleAnnotations = new ToggleAction(true, "workflow.annotation.toggle_visibility") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionToggled(ActionEvent e) {
			setActive(isSelected());
			view.requestFocusInWindow();
		}

	};

	/** action to edit selected note */
	private final ResourceAction editAnnotation = new ResourceAction(true, "workflow.annotation.edit") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			if (model.getSelected() != null) {
				decorator.editSelected();
			}
		}
	};

	/**
	 * Creates the visualizer for {@link WorkflowAnnotation}s.
	 *
	 * @param view
	 *            the proces renderer instance
	 * @param flowVisualizer
	 *            the flow visualizer instance
	 */
	public AnnotationsVisualizer(final ProcessRendererView view, final FlowVisualizer flowVisualizer) {
		this.view = view;
		this.model = new AnnotationsModel(view.getModel());
		this.decorator = new AnnotationsDecorator(view, this, model);
		this.flowVisualizer = flowVisualizer;

		// start annotation decorators
		decorator.registerEventHooks();

		// always show annotations by default
		toggleAnnotations.actionPerformed(new ActionEvent(view, 0, ""));
	}

	/**
	 * Returns the workflow annotation backing model.
	 *
	 * @return the model instance, never {@code null}
	 */
	public AnnotationsModel getModel() {
		return model;
	}

	/**
	 * Whether the annotations are active, i.e. are displayed and can be edited.
	 *
	 * @return {@code true} if they are active; {@code false} otherwise
	 */
	public boolean isActive() {
		return active && !flowVisualizer.isActive();
	}

	/**
	 * Sets whether the annotations are active or not.
	 *
	 * @param active
	 *            {@code true} if they are active; {@code false} otherwise
	 */
	public void setActive(final boolean active) {
		if (this.active != active) {
			this.active = active;
			if (!active) {
				model.reset();
				decorator.reset();
			}
			view.getModel().fireMiscChanged();
		}
	}

	/**
	 * Deletes the selected {@link WorkflowAnnotation}. Has no effect if no annotation has been
	 * selected.
	 *
	 */
	public void deleteSelected() {
		if (model.getSelected() != null) {
			model.deleteAnnotation(model.getSelected());
		}
	}

	/**
	 * Returns the toggle action for workflow annotations.
	 *
	 * @return the action, never {@code null}
	 */
	public ToggleAction getToggleAnnotationsAction() {
		return toggleAnnotations;
	}

	/**
	 * Returns the action to edit the currently selected workflow annotation.
	 *
	 * @return the action, never {@code null}
	 */
	public ResourceAction getEditAnnotationAction() {
		return editAnnotation;
	}

	/**
	 * Creates an action which can be used to add a new {@link OperatorAnnotation} (if an operator
	 * is selected which does not yet have one) or a {@link ProcessAnnotation} at the top left
	 * corner.
	 *
	 * @param process
	 *            the process for which to create the annotation. Can be {@code null} for first
	 *            process at action event time
	 * @return the action, never {@code null}
	 */
	public ResourceAction makeAddAnnotationAction(final ExecutionUnit process) {
		ResourceAction addProcessAnnotation = new ResourceAction(true, "workflow.annotation.add") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				// do nothing if flow visualizer is active
				if (flowVisualizer.isActive()) {
					return;
				}
				// activate annotations if they are not active yet
				if (!isActive()) {
					getToggleAnnotationsAction().actionPerformed(null);
				}

				ExecutionUnit targetProcess = process;
				if (process == null) {
					targetProcess = view.getModel().getProcess(0);
				}

				// if we have a valid selected operator and it does not yet have an annotation
				if (!view.getModel().getSelectedOperators().isEmpty()) {
					Operator selOp = view.getModel().getSelectedOperators().get(0);
					if (!selOp.equals(view.getModel().getDisplayedChain())) {
						if (view.getModel().getOperatorAnnotations(selOp) == null
								|| view.getModel().getOperatorAnnotations(selOp).isEmpty()) {
							Rectangle2D opRect = view.getModel().getOperatorRect(selOp);
							int x = (int) opRect.getCenterX() - OperatorAnnotation.DEFAULT_WIDTH / 2;
							int y = (int) opRect.getMaxY() + OperatorAnnotation.Y_OFFSET;
							AnnotationStyle style = new AnnotationStyle(AnnotationColor.TRANSPARENT,
									AnnotationAlignment.CENTER);
							OperatorAnnotation anno = new OperatorAnnotation(
									I18N.getGUILabel("workflow.annotation.default_text.label"), style, selOp, false, false,
									x, y, OperatorAnnotation.DEFAULT_WIDTH, OperatorAnnotation.DEFAULT_HEIGHT);
							model.addOperatorAnnotation(anno);
							decorator.editSelected();
							return;
						} else {
							// the operator has anno so we want to add a process anno to its process
							targetProcess = selOp.getExecutionUnit();
						}
					}
				}

				// not a valid operator selected or it is already annotated, create process anno
				ProcessAnnotation anno = new ProcessAnnotation(I18N.getGUILabel("workflow.annotation.default_text.label"),
						new AnnotationStyle(), targetProcess, false, false, new Rectangle2D.Double(ProcessAnnotation.MIN_X,
								ProcessAnnotation.MIN_Y, ProcessAnnotation.DEFAULT_WIDTH, ProcessAnnotation.DEFAULT_HEIGHT));
				model.addProcessAnnotation(anno);
				decorator.editSelected();
			}
		};

		return addProcessAnnotation;
	}

	/**
	 * Creates an action which can be used to add a new {@link ProcessAnnotation} at the given
	 * point.
	 *
	 * @param process
	 *            the process for which to create the annotation. Can be {@code null} for first
	 *            process at action event time
	 * @param origin
	 *            the x/y coordinates of the annotation. Can be {@code null} for default location
	 * @return the action, never {@code null}
	 */
	public ResourceAction makeAddProcessAnnotationAction(final ExecutionUnit process, final Point origin) {
		ResourceAction addProcessAnnotation = new ResourceAction(true, "workflow.annotation.add") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				// activate annotations if they are not active yet
				if (!isActive()) {
					getToggleAnnotationsAction().actionPerformed(null);
				}

				ExecutionUnit targetProcess = process;
				Point point = origin;
				if (process == null) {
					targetProcess = view.getModel().getProcess(0);
				}
				if (origin == null) {
					point = new Point(WorkflowAnnotation.MIN_X, WorkflowAnnotation.MIN_Y);
				}
				ProcessAnnotation anno = new ProcessAnnotation(I18N.getGUILabel("workflow.annotation.default_text.label"),
						new AnnotationStyle(), targetProcess, false, false, new Rectangle2D.Double(point.getX(),
								point.getY(), ProcessAnnotation.DEFAULT_WIDTH, ProcessAnnotation.DEFAULT_HEIGHT));
				model.addProcessAnnotation(anno);
				decorator.editSelected();
			}
		};

		return addProcessAnnotation;
	}

	/**
	 * Creates an action which can be used to add a new {@link OperatorAnnotation} to the hovered
	 * operator.
	 *
	 * @param operator
	 *            the operator for which to create the annotation
	 * @return the action, never {@code null}
	 */
	public ResourceAction makeAddOperatorAnnotationAction(final Operator operator) {
		if (operator == null) {
			throw new IllegalArgumentException("operator must not be null!");
		}

		ResourceAction addOperatorAnnotation = new ResourceAction(true, "workflow.annotation.attach") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				// activate annotations if they are not active yet
				if (!isActive()) {
					getToggleAnnotationsAction().actionPerformed(null);
				}

				Rectangle2D opRect = view.getModel().getOperatorRect(operator);
				int x = (int) opRect.getCenterX() - OperatorAnnotation.DEFAULT_WIDTH / 2;
				int y = (int) opRect.getMaxY() + OperatorAnnotation.Y_OFFSET;
				AnnotationStyle style = new AnnotationStyle(AnnotationColor.TRANSPARENT, AnnotationAlignment.CENTER);
				OperatorAnnotation anno = new OperatorAnnotation(I18N.getGUILabel("workflow.annotation.default_text.label"),
						style, operator, false, false, x, y, OperatorAnnotation.DEFAULT_WIDTH,
						OperatorAnnotation.DEFAULT_HEIGHT);
				model.addOperatorAnnotation(anno);
				decorator.editSelected();
			}
		};

		return addOperatorAnnotation;
	}

	/**
	 * Creates an action which can be used to detach an existing {@link OperatorAnnotation} from the
	 * hovered operator.
	 *
	 * @param operator
	 *            the operator for which to detach the annotation
	 * @return the action, never {@code null}
	 */
	public ResourceAction makeDetachOperatorAnnotationAction(final Operator operator) {
		if (operator == null) {
			throw new IllegalArgumentException("operator must not be null!");
		}

		ResourceAction detachOperatorAnnotation = new ResourceAction(true, "workflow.annotation.detach") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				// activate annotations if they are not active yet
				if (!isActive()) {
					getToggleAnnotationsAction().actionPerformed(null);
				}

				WorkflowAnnotations annotations = view.getModel().getOperatorAnnotations(operator);
				if (annotations != null) {
					for (WorkflowAnnotation anno : annotations.getAnnotationsDrawOrder()) {
						model.deleteAnnotation(anno);
						model.addProcessAnnotation(anno.createProcessAnnotation(anno.getProcess()));
					}
				}
			}
		};

		return detachOperatorAnnotation;
	}

	/**
	 * Creates an action which can be used to add bring an annotation to the front.
	 *
	 * @param anno
	 *            the annotation which should be brought to the front
	 * @return the action, never {@code null}
	 */
	public ResourceAction makeToFrontAction(final WorkflowAnnotation anno) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}

		ResourceAction toFrontAnnotation = new ResourceAction(true, "workflow.annotation.order_to_front") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				model.toFront(anno);
			}
		};

		return toFrontAnnotation;
	}

	/**
	 * Creates an action which can be used to add send an annotation one layer forward.
	 *
	 * @param anno
	 *            the annotation which should be sent one layer forward
	 * @return the action, never {@code null}
	 */
	public ResourceAction makeSendForwardAction(final WorkflowAnnotation anno) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}

		ResourceAction sendForwardAnnotation = new ResourceAction(true, "workflow.annotation.order_one_forward") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				model.sendForward(anno);
			}
		};

		return sendForwardAnnotation;
	}

	/**
	 * Creates an action which can be used to add send an annotation to the back.
	 *
	 * @param anno
	 *            the annotation which should be sent to the back
	 * @return the action, never {@code null}
	 */
	public ResourceAction makeToBackAction(final WorkflowAnnotation anno) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}

		ResourceAction toBackAnnotation = new ResourceAction(true, "workflow.annotation.order_to_back") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				model.toBack(anno);
			}
		};

		return toBackAnnotation;
	}

	/**
	 * Creates an action which can be used to add send an annotation one layer back.
	 *
	 * @param anno
	 *            the annotation which should be sent one layer back
	 * @return the action, never {@code null}
	 */
	public ResourceAction makeSendBackAction(final WorkflowAnnotation anno) {
		if (anno == null) {
			throw new IllegalArgumentException("anno must not be null!");
		}

		ResourceAction sendBackAnnotation = new ResourceAction(true, "workflow.annotation.order_one_backward") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				model.sendBack(anno);
			}
		};

		return sendBackAnnotation;
	}

	/**
	 * Creates and displays the annotation popup menu if applicable.
	 *
	 * @param e
	 *            the mouse event
	 * @return {@code true} if the context menu was shown; {@code false} otherwise
	 */
	public boolean showPopupMenu(final MouseEvent e) {
		if (e == null) {
			throw new IllegalArgumentException("e must not be null!");
		}
		if (!isActive()) {
			return false;
		}
		if (model.getSelected() == null) {
			return false;
		}

		JPopupMenu menu = new JPopupMenu();

		// edit action
		menu.add(new JMenuItem(getEditAnnotationAction()));
		// detach action (if applicable)
		if (model.getSelected() instanceof OperatorAnnotation) {
			menu.add(new JMenuItem(makeDetachOperatorAnnotationAction(((OperatorAnnotation) model.getSelected())
					.getAttachedTo())));
		}
		menu.addSeparator();

		OperatorTransferHandler.installMenuItems(menu, true);
		menu.addSeparator();

		// color change menu
		JMenu colorMenu = new JMenu(I18N.getGUILabel("workflow.annotation.color_select.label"));
		colorMenu.setIcon(SwingTools.createIcon("16/" + I18N.getGUILabel("workflow.annotation.color_select.icon")));
		for (AnnotationColor color : AnnotationColor.values()) {
			Action action = color.makeColorChangeAction(model, model.getSelected());
			JMenuItem item = new JMenuItem(action);
			Color borderColor = color.getColor();
			if (color == AnnotationColor.TRANSPARENT) {
				borderColor = Color.LIGHT_GRAY;
			}
			item.setIcon(SwingTools.createIconFromColor(color.getColor(), borderColor, 16, 16, new Ellipse2D.Double(2, 2,
					12, 12)));
			// this removes the space otherwise reserved for a checkbox
			item.setMargin(MENU_ITEM_MARGIN);
			colorMenu.add(item);
		}
		menu.add(colorMenu);

		// alignment change menu
		JMenu alignmentMenu = new JMenu(I18N.getGUILabel("workflow.annotation.alignment_select.label"));
		alignmentMenu.setIcon(SwingTools.createIcon("16/" + I18N.getGUILabel("workflow.annotation.alignment_select.icon")));
		for (AnnotationAlignment align : AnnotationAlignment.values()) {
			Action action = align.makeAlignmentChangeAction(model, model.getSelected());
			JMenuItem item = new JMenuItem(action);
			// this removes the space otherwise reserved for a checkbox
			item.setMargin(MENU_ITEM_MARGIN);
			alignmentMenu.add(item);
		}
		menu.add(alignmentMenu);

		// order menu
		if (model.getSelected() instanceof ProcessAnnotation) {
			JMenu orderMenu = new JMenu(I18N.getGUILabel("workflow.annotation.order_notes.label"));
			orderMenu.setIcon(SwingTools.createIcon("16/" + I18N.getGUILabel("workflow.annotation.order_notes.icon")));

			Action action = makeToFrontAction(model.getSelected());
			JMenuItem item = new JMenuItem(action);
			// this removes the space otherwise reserved for a checkbox
			item.setMargin(MENU_ITEM_MARGIN);
			orderMenu.add(item);

			action = makeToBackAction(model.getSelected());
			item = new JMenuItem(action);
			// this removes the space otherwise reserved for a checkbox
			item.setMargin(MENU_ITEM_MARGIN);
			orderMenu.add(item);

			orderMenu.addSeparator();

			action = makeSendForwardAction(model.getSelected());
			item = new JMenuItem(action);
			// this removes the space otherwise reserved for a checkbox
			item.setMargin(MENU_ITEM_MARGIN);
			orderMenu.add(item);

			action = makeSendBackAction(model.getSelected());
			item = new JMenuItem(action);
			// this removes the space otherwise reserved for a checkbox
			item.setMargin(MENU_ITEM_MARGIN);
			orderMenu.add(item);

			menu.add(orderMenu);
		}

		menu.addSeparator();
		menu.add(getToggleAnnotationsAction().createMenuItem());

		menu.show(view, e.getX(), e.getY());
		return true;
	}
}
