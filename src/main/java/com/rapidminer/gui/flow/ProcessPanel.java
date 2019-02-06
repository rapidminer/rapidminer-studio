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
package com.rapidminer.gui.flow;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.actions.AutoWireAction;
import com.rapidminer.gui.flow.processrendering.annotations.AnnotationsVisualizer;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.background.ProcessBackgroundImageVisualizer;
import com.rapidminer.gui.flow.processrendering.connections.RemoveHoveredConnectionDecorator;
import com.rapidminer.gui.flow.processrendering.connections.RemoveSelectedConnectionDecorator;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererAnnotationEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererEventListener;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererModelEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererOperatorEvent;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.flow.processrendering.view.RenderPhase;
import com.rapidminer.gui.flow.processrendering.view.components.OperatorWarningHandler;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.ViewToolBar;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * Contains the main {@link ProcessRendererView} and a {@link ProcessButtonBar} to navigate through
 * the process.
 *
 * @author Simon Fischer, Tobias Malbrecht, Jan Czogalla
 */
public class ProcessPanel extends JPanel implements Dockable, ProcessEditor {

	/**
	 * A helper class to scroll to the view position of the currently displayed operator chain, e.g.
	 * after the displayed chain has changed.
	 *
	 * @author Jan Czogalla
	 * @see ProcessPanel#scrollToViewPosition(Point)
	 * @see ProcessPanel#scrollToProcessPosition(Point, int)
	 * @see ProcessPanel#scrollToOperator(Operator)
	 * @since 7.5
	 */
	private final class Scroller implements ChangeListener, PropertyChangeListener {

		private boolean drawn = false;
		private boolean running = false;

		private Operator op;

		@Override
		public void stateChanged(ChangeEvent e) {
			if (running) {
				return;
			}
			getViewPort().removeChangeListener(this);
			propertyChange(null);
		}

		private void scroll() {
			ProcessRendererModel model = renderer.getModel();
			OperatorChain opChain = model.getDisplayedChain();
			if (opChain == null) {
				return;
			}
			if (op != null && op != opChain) {
				// scroll to operator after selection
				// force centering (happened after displayed chain changed)
				scrollToOperator(op, true);
				drawn = true;
				return;
			}

			Point position = model.getScrollPosition(opChain);
			int index = -1;
			if (position != null) {
				model.resetScrollPosition(opChain);
				Double i = model.getScrollIndex(opChain);
				if (i != null) {
					model.resetScrollIndex(opChain);
					index = i.intValue();
				}
			} else {
				position = model.getOperatorChainPosition(opChain);
			}
			if (position == null) {
				position = new Point();
			}
			if (index == -1) {
				// scroll to zoom-specific position (e.g. re-entering the current chain)
				scrollToViewPosition(position);
			} else {
				// scroll to process-specific position (e.g. after zooming)
				scrollToProcessPosition(position, index);
			}
			drawn = true;
		}

		public void setOperator(Operator op) {
			this.op = op;
		}

		public boolean isFinished() {
			return drawn;
		}

		@Override
		public synchronized void propertyChange(PropertyChangeEvent evt) {
			running = true;
			getViewPort().removePropertyChangeListener(SCROLLER_UPDATE, this);
			scroll();
			running = false;
		}

	}

	private static final long serialVersionUID = -4419160224916991497L;

	public static final String SCROLLER_UPDATE = "rm.scroller_update";

	/** the process renderer instance */
	private final ProcessRendererView renderer;

	/** the flow visualizer instance */
	private final FlowVisualizer flowVisualizer;

	/** the workflow annotations handler instance */
	private final AnnotationsVisualizer annotationsHandler;

	/** the background image handler */
	private final ProcessBackgroundImageVisualizer backgroundImageHandler;

	/** the handler for operator warning bubbles */
	private final OperatorWarningHandler operatorWarningHandler;

	private final ProcessButtonBar processButtonBar;

	private final JButton resetZoom;
	private final JButton zoomIn;
	private final JButton zoomOut;

	private OperatorChain operatorChain;

	private final JScrollPane scrollPane;

	public ProcessPanel(final MainFrame mainFrame) {
		setOpaque(true);
		setBackground(Colors.PANEL_BACKGROUND);
		processButtonBar = new ProcessButtonBar(mainFrame);

		final ProcessRendererModel model = new ProcessRendererModel();
		// listen for display chain changes and update breadcrumbs accordingly
		model.registerEventListener(new ProcessRendererEventListener() {

			@Override
			public void operatorsChanged(ProcessRendererOperatorEvent e, Collection<Operator> operators) {
				// don't care
			}

			@Override
			public void modelChanged(ProcessRendererModelEvent e) {
				switch (e.getEventType()) {
					case DISPLAYED_CHAIN_CHANGED:
					case DISPLAYED_PROCESSES_CHANGED:
						processButtonBar.setSelectedNode(renderer.getModel().getDisplayedChain());
						break;
					case PROCESS_ZOOM_CHANGED:
						zoomIn.setEnabled(model.canZoomIn());
						zoomOut.setEnabled(model.canZoomOut());
						resetZoom.setText((int) (model.getZoomFactor() * 100) + "%");
						break;
					case PROCESS_SIZE_CHANGED:
					case MISC_CHANGED:
					case DISPLAYED_CHAIN_WILL_CHANGE:
					default:
						break;
				}
			}

			@Override
			public void annotationsChanged(ProcessRendererAnnotationEvent e, Collection<WorkflowAnnotation> annotations) {
				// don't care
			}
		});

		renderer = new ProcessRendererView(model, this, mainFrame);

		// listen for operator selection and view changes (displayed chain/zoom) to adjust view
		// position as needed
		model.registerEventListener(new ProcessRendererEventListener() {

			Scroller scroller;

			@Override
			public void operatorsChanged(ProcessRendererOperatorEvent e, Collection<Operator> operators) {
				switch (e.getEventType()) {
					case SELECTED_OPERATORS_CHANGED:
						if (operators.isEmpty()) {
							return;
						}
						Operator operator = operators.iterator().next();
						Rectangle2D opRect = model.getOperatorRect(operator);
						OperatorChain parent = operator.getParent();
						if (opRect == null || parent == null || model.getDisplayedChain() != parent) {
							break;
						}
						if (model.getRestore(operator)) {
							// don't scroll further after undo/redo
							model.resetRestore(operator);
							break;
						}
						if (operators.size() != 1) {
							// only scroll to operator if it is the single one selected;
							// there is no sensible behavior for scrolling to multiple operators yet
							return;
						}
						if (scroller != null && !scroller.isFinished()) {
							// scroll to operator after new chain is painted
							scroller.setOperator(operator);
						} else {
							// scroll to operator directly (chain is displayed)
							scrollToOperator(operator);
						}
						break;
					case OPERATORS_MOVED:
					case PORTS_CHANGED:
					default:
						break;
				}
			}

			@Override
			public void modelChanged(ProcessRendererModelEvent e) {
				switch (e.getEventType()) {
					case DISPLAYED_CHAIN_WILL_CHANGE:
					// before change: save zoom lvl & center point
					{
						OperatorChain opChain = model.getDisplayedChain();
						if (opChain == null) {
							return;
						}
						// always save zoom, even if no scrollbars exist
						model.setOperatorChainZoom(opChain, model.getZoomFactor());

						// only need to save scroll position if scrollbars exist
						if (scroller != null && !scroller.isFinished()) {
							return;
						} else {
							scroller = null;
						}
						Point position = getCurrentViewCenter();
						model.setOperatorChainPosition(opChain, position);
						break;
					}
					case DISPLAYED_CHAIN_CHANGED:
						// after change: restore zoom lvl first
						OperatorChain opChain = model.getDisplayedChain();
						if (opChain == null) {
							break;
						}
						Double zoom = model.getOperatorChainZoom(opChain);
						if (zoom != null) {
							if (zoom != model.getZoomFactor()) {
								model.setZoomFactor(zoom);
								model.fireProcessZoomChanged();
							}
						} else if (model.getZoomFactor() != 1) {
							model.resetZoom();
							model.fireProcessZoomChanged();
						}
						//$FALL-THROUGH$
					case PROCESS_ZOOM_CHANGED:
						if (scroller == null || scroller.isFinished()) {
							// scroll to position as soon as view port has adjusted
							getViewPort().addChangeListener(scroller = new Scroller());
							getViewPort().addPropertyChangeListener(SCROLLER_UPDATE, scroller);
						}
						break;
					case DISPLAYED_PROCESSES_CHANGED:
					case MISC_CHANGED:
					case PROCESS_SIZE_CHANGED:
					default:
						break;
				}
			}

			@Override
			public void annotationsChanged(ProcessRendererAnnotationEvent e, Collection<WorkflowAnnotation> annotations) {
				// don't care
			}
		});

		flowVisualizer = new FlowVisualizer(renderer);
		annotationsHandler = new AnnotationsVisualizer(renderer, flowVisualizer);
		backgroundImageHandler = new ProcessBackgroundImageVisualizer(renderer);

		RemoveSelectedConnectionDecorator removeSelectedConnectionDecorator = new RemoveSelectedConnectionDecorator(
				renderer.getModel());
		renderer.addDrawDecorator(removeSelectedConnectionDecorator, RenderPhase.CONNECTIONS);
		renderer.addEventDecorator(removeSelectedConnectionDecorator, RenderPhase.CONNECTIONS);

		RemoveHoveredConnectionDecorator removeHoveredConnectionDecorator = new RemoveHoveredConnectionDecorator(
				renderer.getModel());
		renderer.addDrawDecorator(removeHoveredConnectionDecorator, RenderPhase.CONNECTIONS);
		// event decorator must be in phase OVERLAY such that it comes before selecting of
		// connections which is done in between phases OPERATOR_ADDITIONS and OPERATORS
		renderer.addEventDecorator(removeHoveredConnectionDecorator, RenderPhase.OVERLAY);

		ViewToolBar toolBar = new ViewToolBar(ViewToolBar.LEFT);

		zoomIn = new JButton(new ResourceActionAdapter(true, "processpanel.zoom_in") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				fireProcessZoomWillChange();
				model.zoomIn();
				model.fireProcessZoomChanged();
			}
		});
		zoomOut = new JButton(new ResourceActionAdapter(true, "processpanel.zoom_out") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				fireProcessZoomWillChange();
				model.zoomOut();
				model.fireProcessZoomChanged();
			}
		});
		resetZoom = new JButton(new ResourceActionAdapter(true, "processpanel.reset_zoom") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				fireProcessZoomWillChange();
				model.resetZoom();
				model.fireProcessZoomChanged();
			}
		});
		resetZoom.setHorizontalTextPosition(SwingConstants.LEADING);

		toolBar.add(resetZoom);
		toolBar.add(zoomIn);
		toolBar.add(zoomOut);

		toolBar.add(annotationsHandler.makeAddAnnotationAction(null), ViewToolBar.RIGHT);
		toolBar.add(new AutoWireAction(), ViewToolBar.RIGHT);

		toolBar.add(flowVisualizer.SHOW_ORDER_TOGGLEBUTTON, ViewToolBar.RIGHT);
		toolBar.add(renderer.getAutoFitAction(), ViewToolBar.RIGHT);

		setLayout(new BorderLayout());

		JLayeredPane processLayeredPane = new JLayeredPane();
		processLayeredPane.setLayout(new BorderLayout());
		processLayeredPane.add(processButtonBar, BorderLayout.WEST, 1);
		processLayeredPane.add(toolBar, BorderLayout.EAST, 0);
		processLayeredPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		add(processLayeredPane, BorderLayout.NORTH);

		scrollPane = new ExtendedJScrollPane(renderer);
		scrollPane.getVerticalScrollBar().setUnitIncrement(10);
		scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.TEXTFIELD_BORDER));

		add(scrollPane, BorderLayout.CENTER);

		new ProcessPanelScroller(renderer, scrollPane);

		// add event decorator for operator warning icons
		operatorWarningHandler = new OperatorWarningHandler(model);
		renderer.addEventDecorator(operatorWarningHandler, RenderPhase.OPERATOR_ADDITIONS);
	}

	/**
	 * Fires update before zoom changes, keeping the view centered
	 *
	 * @see ProcessRendererModel#prepareProcessZoomWillChange(Point, int)
	 * @since 7.5
	 */
	private void fireProcessZoomWillChange() {
		Point center = getCurrentViewCenter();
		int index = renderer.getProcessIndexUnder(center);
		center = renderer.toProcessSpace(center, index);
		renderer.getModel().prepareProcessZoomWillChange(center, index);
	}

	/**
	 * Shows the specified {@link OperatorChain} in the {@link ProcessRendererView}.
	 *
	 * @param operatorChain
	 *            the operator chain to show, must not be {@code null}
	 * @deprecated use {@link ProcessRendererModel#setDisplayedChain(OperatorChain)} and
	 *             {@link ProcessRendererModel#fireDisplayedChainChanged()} instead
	 */
	@Deprecated
	public void showOperatorChain(OperatorChain operatorChain) {
		if (operatorChain == null) {
			throw new IllegalArgumentException("operatorChain must not be null!");
		}

		this.operatorChain = operatorChain;

		renderer.getModel().setDisplayedChainAndFire(operatorChain);
	}

	@Override
	public void setSelection(List<Operator> selection) {
		Operator first = selection.isEmpty() ? null : selection.get(0);
		if (first != null) {
			processButtonBar.addToHistory(first);
		}
	}

	@Override
	public void processChanged(Process process) {
		processButtonBar.clearHistory();
	}

	@Override
	public void processUpdated(Process process) {
		renderer.processUpdated();
		if (operatorChain != null) {
			processButtonBar.setSelectedNode(operatorChain);
		}
	}

	/**
	 * The {@link ProcessRendererView} which is responsible for displaying the current process as
	 * well as interaction with it
	 *
	 * @return the instance, never {@code null}
	 */
	public ProcessRendererView getProcessRenderer() {
		return renderer;
	}

	/**
	 * The {@link FlowVisualizer} instance tied to the process renderer.
	 *
	 * @return the instance, never {@code null}
	 */
	public FlowVisualizer getFlowVisualizer() {
		return flowVisualizer;
	}

	/**
	 * The {@link AnnotationsVisualizer} instance tied to the process renderer.
	 *
	 * @return the instance, never {@code null}
	 */
	public AnnotationsVisualizer getAnnotationsHandler() {
		return annotationsHandler;
	}

	/**
	 * The {@link ProcessBackgroundImageVisualizer} instance tied to the process renderer.
	 *
	 * @return the instance, never {@code null}
	 */
	public ProcessBackgroundImageVisualizer getBackgroundImageHandler() {
		return backgroundImageHandler;
	}

	public static final String PROCESS_PANEL_DOCK_KEY = "process_panel";
	private final DockKey DOCK_KEY = new ResourceDockKey(PROCESS_PANEL_DOCK_KEY);

	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	public JViewport getViewPort() {
		return scrollPane.getViewport();
	}

	/** Returns the center position of the current view. */
	public Point getCurrentViewCenter() {
		Rectangle viewRect = getViewPort().getViewRect();
		Point center = new Point((int) viewRect.getCenterX(), (int) viewRect.getCenterY());
		return center;
	}

	/**
	 * Scrolls the view to the specified {@link Operator}, making it visible. Will not force
	 * centering
	 *
	 * @param operator
	 *            the operator to focus on
	 * @return whether the scrolling was successful
	 * @since 7.5
	 * @see #scrollToOperator(Operator, boolean)
	 */
	public boolean scrollToOperator(Operator operator) {
		return scrollToOperator(operator, false);
	}

	/**
	 * Scrolls the view to the specified {@link Operator}, making it the center of the view if
	 * necessary, indicated by the flag.
	 *
	 * @param operator
	 *            the operator to focus on
	 * @param toCenter
	 *            flag to indicate whether to force centering
	 * @return whether the scrolling was successful
	 * @since 7.5
	 * @see #scrollToViewPosition(Point)
	 */
	public boolean scrollToOperator(Operator operator, boolean toCenter) {
		Rectangle2D opRect = renderer.getModel().getOperatorRect(operator);
		if (opRect == null) {
			return false;
		}
		int pIndex = renderer.getProcessIndexOfOperator(operator);
		if (pIndex == -1) {
			return false;
		}
		Rectangle opViewRect = getOpViewRect(opRect, pIndex);
		Rectangle viewRect = getViewPort().getViewRect();
		if (!toCenter) {
			if (viewRect.contains(opViewRect)) {
				// if operator visible, do nothing
				return false;
			}
			if (viewRect.intersects(opViewRect)) {
				// if partially visible, just scroll it into view
				opViewRect.translate(-viewRect.x, -viewRect.y);
				getViewPort().scrollRectToVisible(opViewRect);
				// return false nonetheless, see PortInfoBubble
				return false;
			}
		}
		Point opCenter = new Point((int) opViewRect.getCenterX(), (int) opViewRect.getCenterY());
		scrollToViewPosition(opCenter);
		return true;
	}

	/**
	 * Calculates the view rectangle of the given process rectangle, adding in some border padding.
	 *
	 * @param opRect
	 *            the operator rectangle in the process
	 * @param pIndex
	 *            the process index of the corresponding operator
	 * @return the view rectangle
	 * @since 7.5
	 */
	private Rectangle getOpViewRect(Rectangle2D opRect, int pIndex) {
		Rectangle target = new Rectangle();
		target.setLocation(renderer.fromProcessSpace(opRect.getBounds().getLocation(), pIndex));
		double zoomFactor = renderer.getModel().getZoomFactor();
		target.setSize((int) (opRect.getWidth() * zoomFactor), (int) (opRect.getHeight() * zoomFactor));
		target.grow(ProcessDrawer.PORT_SIZE, ProcessDrawer.WALL_WIDTH * 2);
		return target;
	}

	/**
	 * Scrolls the view to the specified {@link Point center point}.
	 *
	 * @param center
	 *            the point to focus on
	 * @since 7.5
	 * @see #scrollToProcessPosition(Point, int)
	 */
	public void scrollToViewPosition(Point center) {
		getViewPort().scrollRectToVisible(getScrollRectangle(center));
	}

	/**
	 * Scrolls the view to the specified {@link Point process point}.
	 *
	 * @param center
	 *            the point to focus on
	 * @param processIndex
	 *            the index of the process to focus on
	 * @since 7.5
	 * @see #scrollToViewPosition(Point)
	 */
	public void scrollToProcessPosition(Point center, int processIndex) {
		scrollToViewPosition(renderer.fromProcessSpace(center, processIndex));
	}

	/**
	 * Calculates the relative scroll rectangle from the current view, so that the specified
	 * {@link Point} is in the center if possible.
	 *
	 * @param center
	 *            the point to focus on
	 * @return the relative scroll rectangle
	 * @since 7.5
	 */
	private Rectangle getScrollRectangle(Point center) {
		Point newViewPoint = new Point(center);
		Rectangle currentViewRect = getViewPort().getViewRect();
		newViewPoint.translate((int) -currentViewRect.getCenterX(), (int) -currentViewRect.getCenterY());
		// Don't scroll outside the viewport
		if (newViewPoint.x < 0) {
			newViewPoint.x = 0;
		}
		if (newViewPoint.y < 0) {
			newViewPoint.y = 0;
		}
		return new Rectangle(newViewPoint, currentViewRect.getSize());
	}

	/**
	 * Returns the handler for operator warning bubbles.
	 *
	 * @return the handler for operator warnings, never {@code null}
	 */
	public OperatorWarningHandler getOperatorWarningHandler() {
		return operatorWarningHandler;
	}

}
