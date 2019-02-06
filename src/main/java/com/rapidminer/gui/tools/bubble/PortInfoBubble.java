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
import com.rapidminer.operator.ports.Port;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.vlsolutions.swing.docking.RelativeDockablePosition;


/**
 * This class creates a speech bubble-shaped JDialog, which can be attached to a {@link Port}. See
 * {@link BubbleWindow} for more details.
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

public class PortInfoBubble extends BubbleWindow {

	/**
	 * Builder for {@link PortInfoBubble}s. After calling all relevant setters, call
	 * {@link #build()} to create the actual dialog instance.
	 *
	 * @author Marco Boeck
	 * @since 6.5.0
	 *
	 */
	public static class PortBubbleBuilder extends BubbleWindowBuilder<PortInfoBubble, PortBubbleBuilder> {

		private Port attachTo;
		private boolean hideOnConnection;
		private boolean hideOnDisable;
		private boolean hideOnRun;
		private boolean ensureVisible;
		private boolean killOnPerspectiveChange;

		public PortBubbleBuilder(final Window owner, final Port attachTo, final String i18nKey, final Object... arguments) {
			super(owner, i18nKey, arguments);
			this.attachTo = attachTo;
			this.killOnPerspectiveChange = true;
		}

		/**
		 * Sets whether to hide the bubble when the port is connected. Defaults to {@code false}.
		 *
		 * @param hideOnConnection
		 *            {@code true} if the bubble should be hidden upon connection; {@code false}
		 *            otherwise
		 * @return the builder instance
		 */
		public PortBubbleBuilder setHideOnConnection(final boolean hideOnConnection) {
			this.hideOnConnection = hideOnConnection;
			return this;
		}

		/**
		 * Sets whether to hide the bubble when the operator the port is attached to is disabled.
		 * Defaults to {@code false}.
		 *
		 * @param hideOnDisable
		 *            {@code true} if the bubble should be hidden upon disable; {@code false}
		 *            otherwise
		 * @return the builder instance
		 */
		public PortBubbleBuilder setHideOnDisable(final boolean hideOnDisable) {
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
		public PortBubbleBuilder setHideOnProcessRun(final boolean hideOnRun) {
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
		public PortBubbleBuilder setEnsureVisible(final boolean ensureVisible) {
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
		public PortBubbleBuilder setKillOnPerspectiveChange(final boolean killOnPerspectiveChange) {
			this.killOnPerspectiveChange = killOnPerspectiveChange;
			return this;
		}

		@Override
		public PortInfoBubble build() {
			return new PortInfoBubble(owner, style, alignment, i18nKey, attachTo, componentsToAdd, hideOnConnection,
					hideOnDisable, hideOnRun, ensureVisible, moveable, showCloseButton, killOnPerspectiveChange, arguments);
		}

		@Override
		public PortBubbleBuilder getThis() {
			return this;
		}
	}

	private static final long serialVersionUID = 1L;

	private Port port;
	private OperatorChain portChain;
	private boolean hideOnConnection;
	private boolean hideOnDisable;
	private boolean hideOnRun;
	private final boolean killOnPerspectiveChange;
	private ProcessRendererView renderer = RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer();
	private JViewport viewport = RapidMinerGUI.getMainFrame().getProcessPanel().getViewPort();
	private ProcessRendererEventListener rendererModelListener;
	private ExtendedProcessEditor processEditor;
	private ProcessStateListener processStateListener;
	private ChangeListener viewPortListener;
	private Observer<Port> portObserver;

	/**
	 * Creates a BubbleWindow which points to a {@link Port}.
	 *
	 * @param owner
	 *            the {@link Window} on which this {@link BubbleWindow} should be shown.
	 * @param preferredAlignment
	 *            offer for alignment but the Class will calculate by itself whether the position is
	 *            usable.
	 * @param i18nKey
	 *            of the message which should be shown
	 * @param toAttach
	 *            the port the bubble should be attached to
	 * @param style
	 *            the bubble style
	 * @param componentsToAdd
	 *            array of JComponents which will be added to the Bubble or {@code null}
	 * @param hideOnConnection
	 *            if {@code true}, the bubble will be removed once the port is connected
	 * @param hideOnDisable
	 *            if {@code true}, the bubble will be removed once the operator of the port becomes
	 *            disabled
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
	private PortInfoBubble(Window owner, BubbleStyle style, AlignedSide preferredAlignment, String i18nKey, Port toAttach,
			JComponent[] componentsToAdd, boolean hideOnConnection, boolean hideOnDisable, boolean hideOnRun,
			boolean ensureVisible, boolean moveable, boolean showCloseButton, boolean killOnPerspectiveChange,
			Object... arguments) {
		super(owner, style, preferredAlignment, i18nKey, ProcessPanel.PROCESS_PANEL_DOCK_KEY, null, null, moveable,
				showCloseButton, componentsToAdd, arguments);
		if (toAttach == null) {
			throw new IllegalArgumentException("toAttach must not be null!");
		}

		this.port = toAttach;
		this.portChain = toAttach.getPorts().getOwner().getPortHandler();
		this.hideOnConnection = hideOnConnection;
		this.hideOnDisable = hideOnDisable;
		this.hideOnRun = hideOnRun;
		this.killOnPerspectiveChange = killOnPerspectiveChange;

		// if we need to ensure that the bubble is visible:
		if (ensureVisible) {
			OperatorChain portParent = portChain;
			while (portParent.getParent() != null) {
				portParent = portParent.getParent();
			}
			OperatorChain displayedParent = renderer.getModel().getDisplayedChain();
			while (displayedParent.getParent() != null) {
				displayedParent = displayedParent.getParent();
			}
			// switch to correct subprocess (if the bubble is part of a subprocess!)
			// if it belongs to a different process, don't switch displayed process
			boolean belongsToSameRoot = portParent == null || portParent.equals(displayedParent);
			if (belongsToSameRoot && !renderer.getModel().getDisplayedChain().equals(portChain)) {
				renderer.getModel().setDisplayedChainAndFire(portChain);
			}
			// switch to correct perspective
			if (!RapidMinerGUI.getMainFrame().getPerspectiveController().getModel().getSelectedPerspective().getName()
					.equals(PerspectiveModel.DESIGN)) {
				RapidMinerGUI.getMainFrame().getPerspectiveController().showPerspective(PerspectiveModel.DESIGN);
				this.myPerspective = PerspectiveModel.DESIGN;
			}
		}

		// keyboard accessibility
		ActionListener closeOnEscape = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				PortInfoBubble.this.killBubble(true);
			}
		};
		getRootPane().registerKeyboardAction(closeOnEscape, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		// try to scroll to operator. If not possible, scroll to port
		ProcessPanel processPanel = RapidMinerGUI.getMainFrame().getProcessPanel();
		if (!processPanel.scrollToOperator(port.getPorts().getOwner().getOperator())) {
			processPanel.scrollToViewPosition(getObjectLocation());
		}
		super.paint(false);
	}

	@Override
	protected void registerSpecificListener() {
		viewPortListener = new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				PortInfoBubble.this.paint(false);
			}
		};
		rendererModelListener = new ProcessRendererEventListener() {

			@Override
			public void modelChanged(ProcessRendererModelEvent e) {
				switch (e.getEventType()) {
					case DISPLAYED_CHAIN_CHANGED:
					case DISPLAYED_PROCESSES_CHANGED:
						if (!renderer.getModel().getDisplayedChain().equals(portChain)) {
							killBubble(true);
						}
						break;
					case PROCESS_ZOOM_CHANGED:
					case PROCESS_SIZE_CHANGED:
						PortInfoBubble.this.paint(false);
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
							if (op.equals(port.getPorts().getOwner().getOperator())) {
								PortInfoBubble.this.paint(false);
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
					if (!port.getPorts().getOwner().getOperator().isEnabled()) {
						killBubble(true);
					}
					// check if operator was removed from process and kill bubble
					if (port.getPorts().getOwner().getOperator().getExecutionUnit() == null) {
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
		portObserver = new Observer<Port>() {

			@Override
			public void update(Observable<Port> observable, Port arg) {
				if (port.isConnected()) {
					killBubble(true);
				}
			}
		};

		if (hideOnConnection) {
			port.addObserver(portObserver, false);
		}
		if (hideOnRun) {
			RapidMinerGUI.getMainFrame().getProcess().addProcessStateListener(processStateListener);
		}
		viewport.addChangeListener(viewPortListener);
		renderer.getModel().registerEventListener(rendererModelListener);
		RapidMinerGUI.getMainFrame().addExtendedProcessEditor(processEditor);
	}

	@Override
	protected void unregisterSpecificListeners() {
		if (hideOnConnection) {
			port.removeObserver(portObserver);
		}
		if (hideOnRun) {
			RapidMinerGUI.getMainFrame().getProcess().removeProcessStateListener(processStateListener);
		}
		renderer.getModel().removeEventListener(rendererModelListener);
		viewport.removeChangeListener(viewPortListener);
		RapidMinerGUI.getMainFrame().removeExtendedProcessEditor(processEditor);
	}

	@Override
	protected Point getObjectLocation() {
		if (!viewport.isShowing()) {
			return new Point(0, 0);
		}
		// get all necessary parameters
		if (!getDockable().getComponent().isShowing()) {
			return new Point(0, 0);
		}

		Point portLoc = ProcessDrawUtils.createPortLocation(port, renderer.getModel());
		if (portLoc == null) {
			return new Point(0, 0);
		}
		portLoc.x = (int) (portLoc.x * renderer.getModel().getZoomFactor());
		portLoc.y = (int) (portLoc.y * renderer.getModel().getZoomFactor());
		portLoc = ProcessDrawUtils.convertToAbsoluteProcessPoint(portLoc,
				renderer.getModel().getProcessIndex(port.getPorts().getOwner().getConnectionContext()), renderer.getModel());
		if (portLoc == null) {
			return new Point(0, 0);
		}
		portLoc.translate(-getObjectWidth() / 2, -getObjectHeight() / 2);

		// calculate actual on screen loc of the port loc and return it
		Point rendererLoc = renderer.getVisibleRect().getLocation();
		Point absoluteLoc = new Point((int) (viewport.getLocationOnScreen().x + (portLoc.getX() - rendererLoc.getX())),
				(int) (viewport.getLocationOnScreen().y + (portLoc.getY() - rendererLoc.getY())));

		// return validated Point
		return this.validatePointForBubbleInViewport(absoluteLoc);

	}

	@Override
	protected int getObjectWidth() {
		// double x width because we want a bit of distance from a port
		return (int) (ProcessDrawer.PORT_SIZE * 2 * renderer.getModel().getZoomFactor());
	}

	@Override
	protected int getObjectHeight() {
		return (int) (ProcessDrawer.PORT_SIZE * renderer.getModel().getZoomFactor());
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
}
