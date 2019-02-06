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
package com.rapidminer.gui.tools.bubble;

import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.Process;
import com.rapidminer.ProcessStateListener;
import com.rapidminer.gui.PerspectiveModel;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererAnnotationEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererEventListener;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererModelEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererOperatorEvent;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.processeditor.ExtendedProcessEditor;
import com.rapidminer.gui.tools.DockingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.vlsolutions.swing.docking.RelativeDockablePosition;


/**
 * This class creates a speech bubble-shaped JDialog, which can be attached to an {@link Operator}.
 * See {@link BubbleWindow} for more details. In contrast to the {@link OperatorBubble}, this bubble
 * contains no assistants and behaves exactly like a {@link PortInfoBubble}.
 * <p>
 * If the perspective is incorrect, the dockable not shown or the subprocess currently viewed is
 * wrong, automatically corrects everything to ensure the bubble is shown if the
 * {@code ensureVisibility} parameter is set.
 * </p>
 *
 * @author Marco Boeck
 * @since 6.5.0
 *
 */

public class OperatorInfoBubble extends BubbleWindow {

	/**
	 * Builder for {@link OperatorInfoBubble}s. After calling all relevant setters, call
	 * {@link #build()} to create the actual dialog instance.
	 *
	 * @author Marco Boeck
	 * @since 6.5.0
	 *
	 */
	public static class OperatorBubbleBuilder extends BubbleWindowBuilder<OperatorInfoBubble, OperatorBubbleBuilder> {

		private Operator attachTo;
		private boolean hideOnDisable;
		private boolean hideOnRun;
		private boolean ensureVisible;
		private boolean killOnPerspectiveChange;

		public OperatorBubbleBuilder(final Window owner, final Operator attachTo, final String i18nKey,
				final Object... arguments) {
			super(owner, i18nKey, arguments);
			this.attachTo = attachTo;
			this.killOnPerspectiveChange = true;
		}

		/**
		 * Sets whether to hide the bubble when the operator is disabled. Defaults to {@code false}.
		 *
		 * @param hideOnDisable
		 *            {@code true} if the bubble should be hidden upon disable; {@code false}
		 *            otherwise
		 * @return the builder instance
		 */
		public OperatorBubbleBuilder setHideOnDisable(final boolean hideOnDisable) {
			this.hideOnDisable = hideOnDisable;
			return this;
		}

		/**
		 * Sets whether to hide the bubble when the process is run. Defaults to {@code false}.
		 *
		 * @param hideOnRun
		 *            {@code true} if the bubble should be hidden upon running a process;
		 *            {@code false} otherwise
		 * @return the builder instance
		 */
		public OperatorBubbleBuilder setHideOnProcessRun(final boolean hideOnRun) {
			this.hideOnRun = hideOnRun;
			return this;
		}

		/**
		 * Sets whether to make sure the bubble is visible by automatically switching perspective,
		 * opening/showing the process dockable and changing the subprocess. Defaults to
		 * {@code false}.
		 *
		 * @param ensureVisible
		 *            {@code true} if the bubble should be hidden upon disable; {@code false}
		 *            otherwise
		 * @return the builder instance
		 */
		public OperatorBubbleBuilder setEnsureVisible(final boolean ensureVisible) {
			this.ensureVisible = ensureVisible;
			return this;
		}

		/**
		 * Sets whether the bubble should be automatically killed by switching perspective. Defaults
		 * to {@code true}.
		 *
		 * @param killOnPerspectiveChange
		 *            {@code true} if the bubble should be killed on perspective change;
		 *            {@code false} otherwise
		 * @return the builder instance
		 */
		public OperatorBubbleBuilder setKillOnPerspectiveChange(final boolean killOnPerspectiveChange) {
			this.killOnPerspectiveChange = killOnPerspectiveChange;
			return this;
		}

		@Override
		public OperatorInfoBubble build() {
			return new OperatorInfoBubble(owner, style, alignment, i18nKey, attachTo, componentsToAdd, hideOnDisable,
					hideOnRun, ensureVisible, moveable, showCloseButton, killOnPerspectiveChange, arguments);
		}

		@Override
		public OperatorBubbleBuilder getThis() {
			return this;
		}

	}

	private static final long serialVersionUID = 1L;

	private final Operator operator;
	private final OperatorChain operatorChain;
	private final boolean hideOnDisable;
	private final boolean hideOnRun;
	private final boolean killOnPerspectiveChange;
	private final ProcessRendererView renderer = RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer();
	private final JViewport viewport = RapidMinerGUI.getMainFrame().getProcessPanel().getViewPort();

	private ExtendedProcessEditor processEditor;
	private ProcessRendererEventListener rendererModelListener;
	private ProcessStateListener processStateListener;
	private ChangeListener viewPortListener;

	/**
	 * Creates a BubbleWindow which points to an {@link Operator}.
	 *
	 * @param owner
	 *            the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which should be shown
	 * @param toAttach
	 *            the operator the bubble should be attached to
	 * @param style
	 *            the bubble style
	 * @param componentsToAdd
	 *            array of JComponents which will be added to the Bubble or {@code null}
	 * @param hideOnDisable
	 *            if {@code true}, the bubble will be removed once the operator becomes disabled
	 * @param hideOnRun
	 *            if {@code true}, the bubble will be removed once the process is executed
	 * @param ensureVisible
	 *            if {@code true}, will automatically make sure the bubble will be visible by
	 *            manipulating the GUI
	 * @param moveable
	 *            if {@code true} the user can drag the bubble around on screen
	 * @param showCloseButton
	 *            if {@code true} the user can close the bubble via an "x" button in the top right
	 *            corner
	 * @param killOnPerspectiveChange
	 *            if {@code true} the bubble will be automatically killed if the perspective changes
	 * @param arguments
	 *            arguments to pass thought to the I18N Object
	 */
	OperatorInfoBubble(Window owner, BubbleStyle style, AlignedSide preferredAlignment, String i18nKey, Operator toAttach,
			JComponent[] componentsToAdd, boolean hideOnDisable, boolean hideOnRun, boolean ensureVisible, boolean moveable,
			boolean showCloseButton, boolean killOnPerspectiveChange, Object... arguments) {
		super(owner, style, preferredAlignment, i18nKey, ProcessPanel.PROCESS_PANEL_DOCK_KEY, null, null, moveable,
				showCloseButton, componentsToAdd, arguments);
		if (toAttach == null) {
			throw new IllegalArgumentException("toAttach must not be null!");
		}

		this.operator = toAttach;
		if (operator.getParent() != null) {
			operatorChain = operator.getParent();
		} else {
			this.operatorChain = operator instanceof OperatorChain ? (OperatorChain) operator : null;
		}
		this.hideOnDisable = hideOnDisable;
		this.hideOnRun = hideOnRun;
		this.killOnPerspectiveChange = killOnPerspectiveChange;

		// if we need to ensure that the bubble is visible:
		if (ensureVisible) {
			// switch to correct subprocess
			if (operatorChain != null && !renderer.getModel().getDisplayedChain().equals(operatorChain)) {
				renderer.getModel().setDisplayedChainAndFire(operatorChain);
			}
			// switch to correct perspective
			if (!RapidMinerGUI.getMainFrame().getPerspectiveController().getModel().getSelectedPerspective().getName()
					.equals(PerspectiveModel.DESIGN)) {
				RapidMinerGUI.getMainFrame().getPerspectiveController().showPerspective(PerspectiveModel.DESIGN);
				this.myPerspective = PerspectiveModel.DESIGN;
			}

			// make sure the operator has a parent (which could be the ProcessRootOperator)
			// if the operator has no parent, e.g. because it is used internally by another
			// operator, it can not be selected
			if (operatorChain != null) {
				RapidMinerGUI.getMainFrame().selectOperator(operator);
			}
		}

		// keyboard accessibility
		ActionListener closeOnEscape = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				OperatorInfoBubble.this.killBubble(true);
			}
		};
		getRootPane().registerKeyboardAction(closeOnEscape, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		super.paint(false);
	}

	@Override
	protected void registerSpecificListener() {
		viewPortListener = new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				OperatorInfoBubble.this.paint(false);
			}
		};
		rendererModelListener = new ProcessRendererEventListener() {

			@Override
			public void modelChanged(ProcessRendererModelEvent e) {
				switch (e.getEventType()) {
					case DISPLAYED_CHAIN_CHANGED:
					case DISPLAYED_PROCESSES_CHANGED:
						if (!renderer.getModel().getDisplayedChain().equals(operatorChain)) {
							killBubble(true);
						}
						break;
					case PROCESS_ZOOM_CHANGED:
					case PROCESS_SIZE_CHANGED:
						OperatorInfoBubble.this.paint(false);
						break;
					case MISC_CHANGED:
					case DISPLAYED_CHAIN_WILL_CHANGE:
					default:
						break;
				}
			}

			@Override
			public void operatorsChanged(ProcessRendererOperatorEvent e, Collection<Operator> operators) {
				switch (e.getEventType()) {
					case OPERATORS_MOVED:
					case PORTS_CHANGED:
						for (Operator op : operators) {
							if (op.equals(operator)) {
								OperatorInfoBubble.this.paint(false);
								break;
							}
						}
						break;
					case SELECTED_OPERATORS_CHANGED:
					default:
						break;
				}
			}

			@Override
			public void annotationsChanged(ProcessRendererAnnotationEvent e, Collection<WorkflowAnnotation> annotations) {
				// don't care
			}

		};
		processEditor = new ExtendedProcessEditor() {

			@Override
			public void setSelection(List<Operator> selection) {
				// don't care
			}

			@Override
			public void processUpdated(Process process) {
				if (hideOnDisable) {
					// check if operator was disabled and kill bubble if that is desired
					if (!operator.isEnabled()) {
						killBubble(true);
					}
					// check if operator was removed from process and kill bubble
					if (operator.getExecutionUnit() == null) {
						killBubble(true);
					}
				}
				// don't care
			}

			@Override
			public void processChanged(Process process) {
				killBubble(true);
			}

			@Override
			public void processViewChanged(Process process) {
				// handled by model listener
			}
		};
		processStateListener = new ProcessStateListener() {

			@Override
			public void started(Process process) {
				killBubble(true);
			}

			@Override
			public void paused(Process process) {
				// ignore
			}

			@Override
			public void resumed(Process process) {
				// ignore
			}

			@Override
			public void stopped(Process process) {
				// ignore
			}

		};
		if (hideOnRun) {
			RapidMinerGUI.getMainFrame().getProcess().addProcessStateListener(processStateListener);
		}
		viewport.addChangeListener(viewPortListener);
		renderer.getModel().registerEventListener(rendererModelListener);
		RapidMinerGUI.getMainFrame().addExtendedProcessEditor(processEditor);
	}

	@Override
	protected void unregisterSpecificListeners() {
		renderer.getModel().removeEventListener(rendererModelListener);
		viewport.removeChangeListener(viewPortListener);
		RapidMinerGUI.getMainFrame().removeExtendedProcessEditor(processEditor);
	}

	@Override
	protected Point getObjectLocation() {
		// get all necessary parameters
		if (!getDockable().getComponent().isShowing()) {
			return new Point(0, 0);
		}

		Point rendererLoc = renderer.getVisibleRect().getLocation();
		Rectangle2D targetRect = renderer.getModel().getOperatorRect(operator);
		if (targetRect == null) {
			return rendererLoc;
		}
		Point loc = new Point((int) targetRect.getX(), (int) targetRect.getY());
		loc.x = (int) (loc.x * renderer.getModel().getZoomFactor());
		loc.y = (int) (loc.y * renderer.getModel().getZoomFactor());
		loc = ProcessDrawUtils.convertToAbsoluteProcessPoint(loc,
				renderer.getModel().getProcessIndex(operator.getExecutionUnit()), renderer.getModel());
		if (loc == null) {
			return rendererLoc;
		}

		if (!viewport.isShowing()) {
			return new Point(0, 0);
		}
		// calculate actual on screen loc of the operator and return it
		Point absoluteLoc = new Point((int) (viewport.getLocationOnScreen().x + (loc.getX() - rendererLoc.getX())),
				(int) (viewport.getLocationOnScreen().y + (loc.getY() - rendererLoc.getY())));

		// return validated Point
		return this.validatePointForBubbleInViewport(absoluteLoc);

	}

	@Override
	protected int getObjectWidth() {
		return (int) (ProcessDrawer.OPERATOR_WIDTH * renderer.getModel().getZoomFactor());
	}

	@Override
	protected int getObjectHeight() {
		Rectangle2D rect = renderer.getModel().getOperatorRect(operator);
		int height = rect != null ? (int) rect.getHeight() : ProcessDrawer.OPERATOR_MIN_HEIGHT;
		return (int) (height * renderer.getModel().getZoomFactor());
	}

	@Override
	protected void changeToAssistant(final AssistantType type) {
		if (AssistantType.WRONG_PERSPECTIVE == type && !killOnPerspectiveChange) {
			setVisible(false);
		} else {
			killBubble(true);
		}
	}

	/**
	 * validates the position of a Bubble and manipulates the position so that the Bubble won't
	 * point to a Point outside of the Viewport if the Operator is not in the Viewport (the
	 * Alignment of the Bubble is considered).
	 *
	 * @param position
	 *            Point to validate
	 * @return returns a Point inside the {@link JViewport}
	 */
	private Point validatePointForBubbleInViewport(Point position) {
		// calculate Offset which is necessary to consider the Alignment
		int xOffset = 0;
		int yOffset = 0;
		int x = position.x;
		int y = position.y;
		if (getRealAlignment() != null) {
			switch (getRealAlignment()) {
				case LEFTBOTTOM:
				case LEFTTOP:
					xOffset = this.getObjectWidth();
					//$FALL-THROUGH$
				case RIGHTBOTTOM:
				case RIGHTTOP:
					yOffset = (int) (this.getObjectHeight() * 0.5);
					break;
				case TOPLEFT:
				case TOPRIGHT:
					yOffset = this.getObjectHeight();
					//$FALL-THROUGH$
				case BOTTOMLEFT:
				case BOTTOMRIGHT:
					xOffset = (int) (this.getObjectWidth() * 0.5);
					break;
				// $CASES-OMITTED$
				default:
			}
		}
		// manipulate invalid coordinates
		if (!(position.x + xOffset >= viewport.getLocationOnScreen().x)) {
			// left
			x = viewport.getLocationOnScreen().x - xOffset;
		}
		if (!(position.x + xOffset <= viewport.getLocationOnScreen().x + viewport.getSize().width)) {
			// right
			x = viewport.getLocationOnScreen().x + viewport.getSize().width - xOffset;
		}
		if (!(position.y + yOffset >= viewport.getLocationOnScreen().y)) {
			// top
			y = viewport.getLocationOnScreen().y - yOffset;
		}
		if (!(position.y + yOffset <= viewport.getLocationOnScreen().y + viewport.getSize().height)) {
			// bottom
			y = viewport.getLocationOnScreen().y + viewport.getSize().height - yOffset;
		}

		return new Point(x, y);
	}

	/**
	 * @return the {@link Operator} for this bubble
	 */
	final Operator getOperator() {
		return operator;
	}

}
