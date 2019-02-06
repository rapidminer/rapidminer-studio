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
package com.rapidminer.gui.flow.processrendering.view;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.properties.celleditors.value.PropertyValueCellEditor;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.PortException;
import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * This class handles mouse events for the {@link ProcessRendererView}.
 *
 * @author Simon Fischer, Marco Boeck
 * @since 6.4.0
 *
 */
public class ProcessRendererMouseHandler {

	private boolean pressHasSelected;

	/** if the user has been dragging operators around */
	private boolean hasDragged;

	/** port currently being dragged (within view only!) */
	private Port draggedPort;

	/** a mapping between dragged operators and their original position */
	private Map<Operator, Rectangle2D> draggedOperatorsOrigins;

	/** a mapping between dragged operators and their last position */
	private Map<Operator, Rectangle2D> lastFixedOperatorsPosition;

	/** if connection dragging was canceled */
	private boolean connectionDraggingCanceled;

	private Point mousePositionAtDragStart;
	private Point mousePositionAtLastEvaluation;
	private int pressedMouseButton;

	/** the view instance */
	private final ProcessRendererView view;

	/** the model instance */
	private final ProcessRendererModel model;

	/** the controller instance */
	private final ProcessRendererController controller;

	public ProcessRendererMouseHandler(ProcessRendererView view, ProcessRendererModel model,
			ProcessRendererController controller) {
		this.view = view;
		this.model = model;
		this.controller = controller;
	}

	/**
	 * Call when the mouse has moved in the {@link ProcessRendererView}.
	 *
	 * @param e
	 */
	public void mouseMoved(final MouseEvent e) {
		if (model.getConnectingPortSource() != null) {
			view.repaint();
		}
		updateHoveringState(e);
	}

	/**
	 * Call when the mouse has been dragged in the {@link ProcessRendererView}.
	 *
	 * @param e
	 */
	public void mouseDragged(final MouseEvent e) {
		// Pan viewport
		if ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0 && view.getParent() instanceof JViewport) {
			JViewport jv = (JViewport) view.getParent();
			Point p = jv.getViewPosition();
			int newX = p.x - (e.getX() - mousePositionAtDragStart.x);
			int newY = p.y - (e.getY() - mousePositionAtDragStart.y);
			int maxX = view.getWidth() - jv.getWidth();
			int maxY = view.getHeight() - jv.getHeight();
			if (newX < 0) {
				newX = 0;
			}
			if (newX > maxX) {
				newX = maxX;
			}
			if (newY < 0) {
				newY = 0;
			}
			if (newY > maxY) {
				newY = maxY;
			}
			jv.setViewPosition(new Point(newX, newY));
			e.consume();
			return;
		}

		// drag ports
		if (model.getConnectingPortSource() != null) {
			view.repaint();

			// We cannot drag when it is an inner sink: dragging means moving the port.
			if (model.getConnectingPortSource().getPorts().getOwner().getOperator() == model.getDisplayedChain()
					&& e.isShiftDown()) {
				cancelConnectionDragging();
			}
			e.consume();
		}

		hasDragged = true;
		if (draggedOperatorsOrigins != null && !draggedOperatorsOrigins.isEmpty()) {
			ExecutionUnit draggingInSubprocess = draggedOperatorsOrigins.keySet().iterator().next().getExecutionUnit();
			Operator hoveringOperator = model.getHoveringOperator();
			if (hoveringOperator != null) {
				if (draggedOperatorsOrigins.size() == 1 && ProcessDrawUtils.hasOperatorFreePorts(hoveringOperator)) {
					int pid = controller.getIndex(draggingInSubprocess);
					Point processSpace = view.toProcessSpace(e.getPoint(), pid);
					if (processSpace != null) {
						model.setHoveringConnectionSource(
								controller.getPortForConnectorNear(processSpace, draggingInSubprocess));
					}
				}

				double difX = e.getX() - mousePositionAtDragStart.getX();
				double difY = e.getY() - mousePositionAtDragStart.getY();
				difX *= 1 / model.getZoomFactor();
				difY *= 1 / model.getZoomFactor();

				// hoveringOperator is always included in draggedOperators
				if (!draggedOperatorsOrigins.containsKey(hoveringOperator)) {
					draggedOperatorsOrigins.put(hoveringOperator, model.getOperatorRect(hoveringOperator));
				}
				double targetX = draggedOperatorsOrigins.get(hoveringOperator).getX() + difX;
				double targetY = draggedOperatorsOrigins.get(hoveringOperator).getY() + difY;
				if (targetX < ProcessDrawer.GRID_X_OFFSET) {
					targetX = ProcessDrawer.GRID_X_OFFSET;
				}

				if (targetY < ProcessDrawer.GRID_Y_OFFSET) {
					targetY = ProcessDrawer.GRID_Y_OFFSET;
				}
				// use only hovering operator for snapping
				if (model.isSnapToGrid()) {
					Point snapped = ProcessDrawUtils.snap(new Point2D.Double(targetX, targetY));
					targetX = snapped.getX();
					targetY = snapped.getY();
				}

				// now, set difX and difY to shift /after/ snapped and clipped
				difX = targetX - draggedOperatorsOrigins.get(hoveringOperator).getX();
				difY = targetY - draggedOperatorsOrigins.get(hoveringOperator).getY();

				// bound to subprocess

				double unitWidth = model.getProcessWidth(draggingInSubprocess);
				double unitHeight = model.getProcessHeight(draggingInSubprocess);
				double maxX = 0;
				double maxY = 0;

				Set<Operator> movedOperators = new HashSet<>();
				// shift
				for (Entry<Operator, Rectangle2D> opAndRectangle : draggedOperatorsOrigins.entrySet()) {
					Rectangle2D origin = opAndRectangle.getValue();
					if (origin.getMaxX() + difX >= unitWidth && origin.getMaxX() + difX > maxX) {
						maxX = origin.getMaxX() + difX;
					}
					if (origin.getMaxY() + difY >= unitHeight && origin.getMaxY() + difY > maxY) {
						maxY = origin.getMaxY() + difY;
					}

					if (origin.getMinY() + difY < 0) {
						difY = -origin.getMinY() + ProcessDrawer.GRID_Y_OFFSET;
					}
					if (origin.getMinX() + difX < 0) {
						difX = -origin.getMinX() + ProcessDrawer.GRID_X_OFFSET;
					}
					final Rectangle2D lastFixedPosition = lastFixedOperatorsPosition.get(opAndRectangle.getKey());
					Rectangle2D opPos = new Rectangle2D.Double(Math.floor(origin.getX() + difX),
							Math.floor(origin.getY() + difY), origin.getWidth(), origin.getHeight());
					model.setOperatorRect(opAndRectangle.getKey(), opPos);
					if (!opPos.equals(lastFixedPosition)) {
						movedOperators.add(opAndRectangle.getKey());
						lastFixedOperatorsPosition.put(opAndRectangle.getKey(), opPos);
					}
				}
				if (!movedOperators.isEmpty()) {
					model.fireOperatorsMoved(movedOperators);
				}
				e.consume();
			}
		} else {
			// ports are draggeable only if they belong to the displayed chain <->
			// they are innersinks of our sources
			if (isDisplayChainPortDragged() &&
			// furthermore they can only be dragged with left mouse button + shift key
			// pressed
					pressedMouseButton == MouseEvent.BUTTON1 && e.isShiftDown()) {

				double diff = e.getY() - mousePositionAtLastEvaluation.getY();
				double shifted = controller.shiftPortSpacing(draggedPort, diff);
				mousePositionAtLastEvaluation.setLocation(mousePositionAtLastEvaluation.getX(),
						mousePositionAtLastEvaluation.getY() + shifted);

				view.repaint();
				e.consume();
			} else if (model.getSelectionRectangle() != null) {
				model.setSelectionRectangle(ProcessDrawUtils.createRectangle(mousePositionAtDragStart, e.getPoint()));
				model.fireMiscChanged();
				e.consume();
			} else if (model.getConnectingPortSource() != null) {
				updateHoveringState(e);
			}
		}
	}

	/**
	 * Call when the mouse has been pressed in the {@link ProcessRendererView}.
	 *
	 * @param e
	 */
	public void mousePressed(final MouseEvent e) {
		pressHasSelected = false;
		mousePositionAtDragStart = e.getPoint();
		mousePositionAtLastEvaluation = e.getPoint();
		hasDragged = false;
		pressedMouseButton = e.getButton();
		connectionDraggingCanceled = false;

		if (SwingUtilities.isLeftMouseButton(e) && model.getHoveringOperator() == null && model.getHoveringPort() == null
				&& model.getSelectedConnectionSource() != model.getHoveringConnectionSource()) {
			model.setSelectedConnectionSource(model.getHoveringConnectionSource());
			model.fireMiscChanged();
			e.consume();
		}

		if (e.getButton() == MouseEvent.BUTTON2) {
			view.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			return;
		}

		// disconnect when clicking with alt + left mouse on connection
		if (model.getHoveringConnectionSource() != null && model.getHoveringOperator() == null
				&& SwingUtilities.isLeftMouseButton(e) && e.isAltDown()) {
			ProcessRendererView.disconnectHoveredConnection(model);
			e.consume();
		}

		// If mouse pressed while connecting, check if connecting ports should be canceled
		Port connectingPortSource = model.getConnectingPortSource();
		if (connectingPortSource != null) {

			// cancel if right mouse button is pressed
			if (SwingUtilities.isRightMouseButton(e)) {
				cancelConnectionDragging();
				e.consume();
				return;
			}

			// cancel if any button is pressed but not over hovering port
			if (model.getHoveringPort() == null) {
				cancelConnectionDragging();
				e.consume();
				return;
			}
			e.consume();
		}
		updateHoveringState(e);

		Port hoveringPort = model.getHoveringPort();
		if (SwingUtilities.isLeftMouseButton(e) && hoveringPort != null) {

			// Left mouse button pressed on port with alt pressed -> remove connection
			if (e.isAltDown()) {
				if (hoveringPort.isConnected()) {
					if (hoveringPort instanceof OutputPort) {
						((OutputPort) hoveringPort).disconnect();
					} else if (hoveringPort instanceof InputPort) {
						((InputPort) hoveringPort).getSource().disconnect();
					}
				}
				view.repaint();
			} else {
				// Left mouse button pressed on port -> start connecting ports
				try {
					verifyAndConnectConnectingPortSourceWithHoveringPort();
				} catch (IncompatiblePortsException ex) {
					if (!e.isShiftDown()) {
						model.setConnectingPortSource(hoveringPort);
					}
				}
			}
			e.consume();
		} else if (model.getHoveringOperator() == null && !e.isShiftDown() && !(SwingTools.isControlOrMetaDown(e) && e.getButton() == 1) &&
				(model.getSelectedOperators().isEmpty() || !model.getSelectedOperators().get(0).equals(model.getDisplayedChain()))) {
			// deselect unless shift is pressed
			controller.selectOperator(model.getDisplayedChain(), true);
		}

		if (hoveringPort != null) {
			controller.selectOperator(hoveringPort.getPorts().getOwner().getOperator(), true);
			pressHasSelected = true;
			e.consume();
		} else if (model.getHoveringOperator() == null && !e.isShiftDown() && !(SwingTools.isControlOrMetaDown(e) && e.getButton() == 1) &&
				(model.getSelectedOperators().isEmpty() || !model.getSelectedOperators().get(0).equals(model.getDisplayedChain()))) {
			controller.selectOperator(model.getDisplayedChain(), true);
			pressHasSelected = true;
		}

		if (model.getHoveringOperator() != null) {
			// control down and reducing selection from {A,B,C} to {A} is delayed to
			// mouseReleased
			if (!(SwingTools.isControlOrMetaDown(e) && SwingUtilities.isLeftMouseButton(e))
					&& !model.getSelectedOperators().contains(model.getHoveringOperator())) {
				controller.selectOperator(model.getHoveringOperator(), true, e.isShiftDown());
				pressHasSelected = true;
			}
			// start dragging
			draggedOperatorsOrigins = new HashMap<>();
			for (Operator op : model.getSelectedOperators()) {
				if (op.getExecutionUnit() == model.getHoveringOperator().getExecutionUnit()) {
					draggedOperatorsOrigins.put(op, (Rectangle2D) model.getOperatorRect(op).clone());
				}
			}
			model.setDraggedOperators(draggedOperatorsOrigins.keySet());
			lastFixedOperatorsPosition = new HashMap<>();

			e.consume();
		} else if (hoveringPort != null) {
			draggedPort = hoveringPort;
			e.consume();
		} else if (SwingUtilities.isLeftMouseButton(e)) {
			// start selection if we actually start selecting in a process
			if (view.getProcessIndexUnder(e.getPoint()) == -1) {
				return;
			}
			model.setSelectionRectangle(ProcessDrawUtils.createRectangle(mousePositionAtDragStart, e.getPoint()));
		}

		// Popup will only be triggered if mouse has been released and no dragging was done
		// CAUTION: Mac&Linux / Windows do different popup trigger handling. Because of this the
		// popup trigger has to be checked in mousePressed AND mouseReleased
		// WINDOWS: mouseReleased
		// LINUX: mousePressed
		// DO NOT HANDLE BACKGROUND CLICKS HERE, they are handled in a later RenderPhase
		showOperatorPopup(e, !model.getDisplayedChain().equals(getClickedOperator()) || model.getHoveringConnectionSource() != null);
	}

	private Operator getClickedOperator() {
		if (model.getHoveringPort() != null) {
			return model.getHoveringPort().getPorts().getOwner().getOperator();
		}
		if (model.getHoveringOperator() != null) {
			return model.getHoveringOperator();
		}
		return model.getDisplayedChain();
	}

	private void showOperatorPopup(MouseEvent e, boolean condition) {
		if (e.isPopupTrigger() && condition && !connectionDraggingCanceled && view.showPopupMenu(e)) {
			e.consume();
		}
	}

	/**
	 * Call when the mouse has been pressed in the {@link ProcessRendererView} and no
	 * {@link RenderPhase} before the {@link RenderPhase#BACKGROUND} has consumed the event.
	 *
	 * @param e
	 */
	public void mousePressedBackground(MouseEvent e) {
		// Popup will only be triggered if mouse has been released and no dragging was done
		// CAUTION: Mac&Linux / Windows do different popup trigger handling. Because of this the
		// popup trigger has to be checked in mousePressed AND mouseReleased
		// WINDOWS: mouseReleased
		// LINUX: mousePressed
		// ONLY HANDLE BACKGROUND CLICKS HERE
		showOperatorPopup(e, model.getDisplayedChain().equals(getClickedOperator()));
	}

	/**
	 * Call when the mouse has been released in the {@link ProcessRendererView}.
	 *
	 * @param e
	 */
	public void mouseReleased(final MouseEvent e) {
		if ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0) {
			view.setCursor(Cursor.getDefaultCursor());
			e.consume();
			return;
		}

		Port connectingPortSource = model.getConnectingPortSource();
		if (connectingPortSource != null) {

			// cancel if right mouse button is released
			if (SwingUtilities.isRightMouseButton(e)) {
				cancelConnectionDragging();
			}

			Port hoveringPort = model.getHoveringPort();
			// cancel if any button is released but not over hovering port
			if (hoveringPort == null) {
				cancelConnectionDragging();
			}

			// connect when released over hovering port
			if (SwingUtilities.isLeftMouseButton(e) && hoveringPort != null && !e.isAltDown()) {
				try {
					verifyAndConnectConnectingPortSourceWithHoveringPort();
				} catch (IncompatiblePortsException ex) {
					if (!e.isShiftDown() && !hoveringPort.equals(connectingPortSource)) {
						cancelConnectionDragging();
					}
				}
			}

			e.consume();
		}

		try {
			Rectangle2D selectionRectangle = model.getSelectionRectangle();
			if (selectionRectangle != null) {
				if (selectionRectangle.getWidth() > 3 && selectionRectangle.getHeight() > 3) {
					int processIndex = view.getProcessIndexUnder(mousePositionAtDragStart);
					if (processIndex == -1) {
						processIndex = view.getProcessIndexUnder(e.getPoint());
					}
					if (processIndex == -1) {
						processIndex = view.getProcessIndexUnder(
								new Point((int) selectionRectangle.getCenterX(), (int) selectionRectangle.getCenterY()));
					}

					Point offset = view.toProcessSpace(new Point(0, 0), processIndex);
					if (offset != null) {
						if (!e.isShiftDown() && !SwingTools.isControlOrMetaDown(e)
								|| model.getSelectedOperators().size() == 1
										&& model.getSelectedOperators().get(0) == model.getDisplayedChain()) { // if
							// we have only selected the parent, we ignore SHIFT and CTRL
							model.clearOperatorSelection();
						}
						for (Operator op : model.getProcess(processIndex).getOperators()) {
							Rectangle2D opRect = model.getOperatorRect(op);
							double zoomFactor = model.getZoomFactor();
							Rectangle2D selRect = new Rectangle2D.Double(
									model.getSelectionRectangle().getX() * (1 / zoomFactor) + offset.getX(),
									model.getSelectionRectangle().getY() * (1 / zoomFactor) + offset.getY(),
									model.getSelectionRectangle().getWidth() * (1 / zoomFactor),
									model.getSelectionRectangle().getHeight() * (1 / zoomFactor));
							if (selRect.contains(opRect)) {
								controller.selectOperator(op, false);
							}
						}
					}
				}
				model.setSelectionRectangle(null);
				model.fireMiscChanged();
				e.consume();
			} else {
				if (hasDragged && draggedOperatorsOrigins != null && draggedOperatorsOrigins.size() == 1) {
					if (ProcessDrawUtils.canOperatorBeInsertedIntoConnection(model, model.getHoveringOperator())) {
						controller.insertIntoHoveringConnection(model.getHoveringOperator());
					}
					e.consume();
				} else if (!hasDragged && model.getHoveringOperator() != null && !e.isPopupTrigger()
						&& SwingUtilities.isLeftMouseButton(e)
						&& (SwingTools.isControlOrMetaDown(e)
								|| model.getSelectedOperators().contains(model.getHoveringOperator())
										&& !pressHasSelected)) {
					// control and deselection was delayed to mouseReleased
					controller.selectOperator(model.getHoveringOperator(), !SwingTools.isControlOrMetaDown(e),
							e.isShiftDown());
					e.consume();
				}
			}

			if (draggedOperatorsOrigins != null || draggedPort != null) {
				model.getDisplayedChain().getProcess().updateNotify();
			}
		} finally {
			mousePositionAtDragStart = null;
			draggedPort = null;
			draggedOperatorsOrigins = null;
			lastFixedOperatorsPosition = null;
			hasDragged = false;
			model.clearDraggedOperators();
		}

		// Popup will only be triggered if mouse has been released and no dragging was done
		// CAUTION: Mac&Linux / Windows do different popup trigger handling. Because of this the
		// popup trigger has to be checked in mousePressed AND mouseReleased
		// WINDOWS: mouseReleased
		// LINUX: mousePressed
		// DO NOT HANDLE BACKGROUND CLICKS HERE, they are handled in a later RenderPhase
		showOperatorPopup(e, !model.getDisplayedChain().equals(getClickedOperator()) || model.getHoveringConnectionSource() != null);
		if (!e.isConsumed()) {
			view.repaint();
		}
	}

	/**
	 * Call when the mouse has been released in the {@link ProcessRendererView} and no
	 * {@link RenderPhase} before the {@link RenderPhase#BACKGROUND} has consumed the event.
	 *
	 * @param e
	 */
	public void mouseReleasedBackground(final MouseEvent e) {
		// Popup will only be triggered if mouse has been released and no dragging was done
		// CAUTION: Mac&Linux / Windows do different popup trigger handling. Because of this the
		// popup trigger has to be checked in mousePressed AND mouseReleased
		// WINDOWS: mouseReleased
		// LINUX: mousePressed
		// ONLY HANDLE BACKGROUND CLICKS HERE
		showOperatorPopup(e, model.getDisplayedChain().equals(getClickedOperator()));
	}

	/**
	 * Call when the mouse has been clicked in the {@link ProcessRendererView}.
	 *
	 * @param e
	 */
	public void mouseClicked(final MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (e.getClickCount() != 2) {
				return;
			}
			Operator hoveringOperator = model.getHoveringOperator();
			if (hoveringOperator != null) {
				if (model.isHoveringOperatorName()) {
					ActionStatisticsCollector.getInstance().logOperatorDoubleClick(hoveringOperator, ActionStatisticsCollector.OPERATOR_ACTION_RENAME);
					controller.rename(hoveringOperator);
					e.consume();
					return;
				}
				if (hoveringOperator instanceof OperatorChain && e.getModifiersEx() != InputEvent.ALT_DOWN_MASK) {
					// dive into operator chain, unless user has pressed ALT key. ALT + double-click = activate primary parameter
					ActionStatisticsCollector.getInstance().logOperatorDoubleClick(hoveringOperator, ActionStatisticsCollector.OPERATOR_ACTION_OPEN);
					model.setDisplayedChainAndFire((OperatorChain) hoveringOperator);
					e.consume();
					return;
				}
				// look for a primary parameter, and activate it if found
				ParameterType primaryParameter = hoveringOperator.getPrimaryParameter();
				ActionStatisticsCollector.getInstance().logOperatorDoubleClick(model.getHoveringOperator(), ActionStatisticsCollector.OPERATOR_ACTION_PRIMARY_PARAMETER);
				if (primaryParameter != null) {
					PropertyValueCellEditor editor = RapidMinerGUI.getMainFrame().getPropertyPanel().getEditorForKey(primaryParameter.getKey());
					if (editor != null) {
						editor.activate();
						e.consume();
						return;
					}
				}
			}
			if (model.getHoveringPort() == null) {
				return;
			}
			Port hoveringPort = model.getHoveringPort();
			PortOwner hoveringPortOwner = hoveringPort.getPorts().getOwner();
			// should only work for yet unconnected outer ports
			if (hoveringPortOwner.getPortHandler().equals(hoveringPortOwner.getOperator()) || hoveringPort.isConnected()) {
				return;
			}
			ExecutionUnit surroundingUnit = hoveringPortOwner.getOperator().getExecutionUnit();
			if (hoveringPort instanceof OutputPort) {
				OutputPort hoveringOutputPort = (OutputPort) hoveringPort;
				for (InputPort in : surroundingUnit.getInnerSinks().getAllPorts()) {
					if (attemptConnection(hoveringOutputPort, in)) {
						e.consume();
						return;
					}
				}
			} else {
				InputPort hoveringInputPort = (InputPort) hoveringPort;
				for (OutputPort out : surroundingUnit.getInnerSources().getAllPorts()) {
					if (attemptConnection(out, hoveringInputPort)) {
						e.consume();
						return;
					}
				}
			}
			return;
		}
		if (SwingUtilities.isRightMouseButton(e) && model.getConnectingPortSource() != null) {
			cancelConnectionDragging();
			e.consume();
		}
	}

	/**
	 * Call when the mouse has entered the {@link ProcessRendererView}.
	 *
	 * @param e
	 */
	public void mouseEntered(final MouseEvent e) {}

	/**
	 * Call when the mouse has exited the {@link ProcessRendererView}.
	 *
	 * @param e
	 */
	public void mouseExited(final MouseEvent e) {
		controller.clearStatus();
	}

	/**
	 * Updates the currently hovered element
	 *
	 * @param e
	 */
	private void updateHoveringState(final MouseEvent e) {
		int hoveringProcessIndex = model.getHoveringProcessIndex();
		if (hoveringProcessIndex != -1) {
			ExecutionUnit hoveringProcess = model.getProcess(hoveringProcessIndex);
			Point relativeMousePosition = model.getMousePositionRelativeToProcess();
			int relativeX = (int) relativeMousePosition.getX();
			int relativeY = (int) relativeMousePosition.getY();

			OutputPort connectionSourceUnderMouse = controller.getPortForConnectorNear(relativeMousePosition, hoveringProcess);
			if (connectionSourceUnderMouse != model.getHoveringConnectionSource()) {
				model.setHoveringConnectionSource(connectionSourceUnderMouse);
				model.fireMiscChanged();
				e.consume();
			}

			// find inner sinks/sources under mouse
			if (controller.checkPortUnder(hoveringProcess.getInnerSinks(), relativeX, relativeY)
					|| controller.checkPortUnder(hoveringProcess.getInnerSources(), relativeX, relativeY)) {
				e.consume();
				return;
			}

			// find operator under mouse
			List<Operator> operators = hoveringProcess.getOperators();
			List<Operator> selectedOperators = model.getSelectedOperators();
			// if there are selected operators, they take precedence
			if (!operators.isEmpty() && !selectedOperators.isEmpty()) {
				operators = new ArrayList<>(operators);
				operators.sort((o1, o2) -> {
					int index1 = selectedOperators.indexOf(o1);
					int index2 = selectedOperators.indexOf(o2);
					if (index1 == index2) {
						return 0;
					}
					if (index1 == -1) {
						return -1;
					}
					if (index2 == -1) {
						return 1;
					}
					return index2 - index1;
				});
			}

			ListIterator<Operator> iterator = operators.listIterator(operators.size());
			while (iterator.hasPrevious()) {
				Operator op = iterator.previous();
				// first, check whether we are over a port
				if (controller.checkPortUnder(op.getInputPorts(), relativeX, relativeY)
						|| controller.checkPortUnder(op.getOutputPorts(), relativeX, relativeY)) {
					e.consume();
					return;
				}
				// If not, check operator.
				Rectangle2D rect = model.getOperatorRect(op);
				if (rect == null) {
					continue;
				}
				if (rect.contains(relativeMousePosition)) {
					if (model.getHoveringOperator() != op) {
						model.setHoveringPort(null);
						view.setHoveringOperator(op);
						if (model.getConnectingPortSource() == null) {
							if (model.getHoveringOperator() instanceof OperatorChain) {
								controller.showStatus(I18N.getGUILabel("processRenderer.displayChain.hover"));
							} else {
								controller.showStatus(I18N.getGUILabel("processRenderer.operator.hover"));
							}
						} else {
							controller.showStatus(I18N.getGUILabel("processRenderer.connection.hover_cancel"));
						}
					}
					view.updateCursor();
					e.consume();
					return;
				}
			}
		}
		if (model.getHoveringOperator() != null) {
			view.setHoveringOperator(null);
		}
		if (model.getHoveringPort() != null) {
			model.setHoveringPort(null);
			view.updateCursor();
			model.fireMiscChanged();
		}
		if (model.getHoveringConnectionSource() != null && model.getConnectingPortSource() == null) {
			controller.showStatus(I18N.getGUILabel("processRenderer.connection.hover"));
		} else if (model.getConnectingPortSource() != null) {
			controller.showStatus(I18N.getGUILabel("processRenderer.connection.hover_cancel"));
		} else {
			controller.clearStatus();
		}
	}

	/**
	 * Verifies the ports and connect with the connection source port if possible
	 *
	 * @throws IncompatiblePortsException in case the connection is not possible
	 */
	private void verifyAndConnectConnectingPortSourceWithHoveringPort() throws IncompatiblePortsException {
		final Port connectingPortSource = model.getConnectingPortSource();
		final Port hoveringPort = model.getHoveringPort();
		final InputPort input;
		final OutputPort output;
		if (hoveringPort instanceof InputPort && connectingPortSource instanceof OutputPort) {
			input = (InputPort) hoveringPort;
			output = (OutputPort) connectingPortSource;
		} else if (hoveringPort instanceof OutputPort && connectingPortSource instanceof InputPort) {
			input = (InputPort)  connectingPortSource;
			output = (OutputPort) hoveringPort;
		} else {
			throw new IncompatiblePortsException();
		}

		Operator destOp = input.getPorts().getOwner().getOperator();
		Operator sourceOp = output.getPorts().getOwner().getOperator();
		// outer ports of an operator should not connect to each other
		if (!destOp.equals(model.getDisplayedChain()) && destOp.equals(sourceOp)) {
			throw new IncompatiblePortsException();
		}
		connectConnectingPortSourceWithHoveringPort(input, output, hoveringPort);
	}
	/**
	 * Connects the clicked port with the connection source port.
	 *
	 * @param input
	 * @param output
	 * @param hoveringPort
	 */
	private void connectConnectingPortSourceWithHoveringPort(final InputPort input, final OutputPort output,
			final Port hoveringPort) {
		try {
			Operator destOp = input.getPorts().getOwner().getOperator();
			boolean hasConnections = controller.hasConnections(destOp);
			controller.connect(output, input);
			// move directly after source if first connection
			if (!hasConnections) {
				Operator sourceOp = output.getPorts().getOwner().getOperator();
				if (destOp != model.getDisplayedChain() && sourceOp != model.getDisplayedChain()) {
					destOp.getExecutionUnit().moveToIndex(destOp,
							destOp.getExecutionUnit().getOperators().indexOf(sourceOp) + 1);
				}
			}
		} catch (PortException e1) {
			if (e1.hasRepairOptions()) {

				// calculate popup position
				Point popupPosition = ProcessDrawUtils.createPortLocation(hoveringPort, model);
				// correct by zoomFactor
				double zoomFactor = model.getZoomFactor();
				popupPosition = new Point((int) (popupPosition.getX() * zoomFactor),
						(int) (popupPosition.getY() * zoomFactor));

				// take splitted process pane into account and add offset for each process we
				// have to the left of our current one
				if (hoveringPort.getPorts() != null) {
					ExecutionUnit process;
					if (hoveringPort.getPorts().getOwner().getOperator() == model.getDisplayedChain()) {
						// this is an inner port
						process = hoveringPort.getPorts().getOwner().getConnectionContext();
					} else {
						// this is an outer port of a nested operator
						process = hoveringPort.getPorts().getOwner().getOperator().getExecutionUnit();
					}
					// iterate over all processes and add widths of processes to the left
					int counter = 0;
					for (ExecutionUnit unit : model.getProcesses()) {
						if (unit == process) {
							// only add process widths until we have the process which contains
							// the port
							break;
						} else {
							counter++;
							popupPosition = new Point((int) (popupPosition.x + model.getProcessWidth(unit)),
									popupPosition.y);
						}
					}
					// add another wall width as offset if we have multiple processes
					if (counter > 0) {
						popupPosition = new Point(popupPosition.x + ProcessDrawer.WALL_WIDTH, popupPosition.y);
					}
				}

				if (hoveringPort instanceof InputPort) {
					popupPosition.setLocation(popupPosition.getX() + 28, popupPosition.getY() - 2);
				} else {
					popupPosition.setLocation(popupPosition.getX() - 18, popupPosition.getY() - 2);
				}

				e1.showRepairPopup(view, popupPosition);
			} else {
				JOptionPane.showMessageDialog(null, e1.getMessage(), "Cannot connect", JOptionPane.ERROR_MESSAGE);
			}
			view.repaint();
		} finally {
			cancelConnectionDragging();
		}
	}

	/**
	 * Returns if a display chain port is being dragged.
	 *
	 * @return {@code true} if one is dragged; {@code false} otherwise
	 */
	private boolean isDisplayChainPortDragged() {
		return draggedPort != null && draggedPort.getPorts().getOwner().getOperator() == model.getDisplayedChain();
	}

	/**
	 * Cancels the ongoing connection dragging.
	 */
	private void cancelConnectionDragging() {
		model.setConnectingPortSource(null);
		model.fireMiscChanged();
		connectionDraggingCanceled = true;
	}

	/**
	 * Connects the given ports if they are both not connected yet and the {@link MetaData} matches. If the ports were
	 * connected, this will also call {@link #cancelConnectionDragging()}.
	 * Will return {@code true} iff the ports were connected.
	 *
	 * @param out
	 * 		the output port
	 * @param in
	 * 		the input port
	 * @return {@code true} iff the ports were connected.
	 * @since 8.2
	 */
	private boolean attemptConnection(OutputPort out, InputPort in) {
		MetaData outMetaData;
		try {
			outMetaData = out.getMetaData(MetaData.class);
		} catch (IncompatibleMDClassException e){
			//Should not happen
			return false;
		}
		if (!in.isConnected() && !out.isConnected() && outMetaData != null && in.isInputCompatible(outMetaData, CompatibilityLevel.VERSION_5)) {
			out.connectTo(in);
			cancelConnectionDragging();
			return true;
		}
		return false;
	}

	/**
	 * Used to identify incompatible port combinations
	 *
	 * @author Jonas Wilms-Pfau
	 * @see #verifyAndConnectConnectingPortSourceWithHoveringPort
	 * @since 8.2.0
	 */
	private static class IncompatiblePortsException extends Exception {
		// marker exception
	}

}
