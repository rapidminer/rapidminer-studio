/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.PortException;
import com.rapidminer.tools.I18N;


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
		if ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0) {
			if (view.getParent() instanceof JViewport) {
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
		}

		// drag ports
		if (model.getConnectingPortSource() != null) {
			view.repaint();

			// We cannot drag when it is an inner sink: dragging means moving the port.
			if (model.getConnectingPortSource().getPorts().getOwner().getOperator() == model.getDisplayedChain()
					&& e.isShiftDown()) {
				cancelConnectionDragging();
				connectionDraggingCanceled = true;
			}
			e.consume();
		}

		hasDragged = true;
		if (draggedOperatorsOrigins != null && !draggedOperatorsOrigins.isEmpty()) {
			ExecutionUnit draggingInSubprocess = draggedOperatorsOrigins.keySet().iterator().next().getExecutionUnit();
			Operator hoveringOperator = model.getHoveringOperator();
			if (hoveringOperator != null) {
				if (draggedOperatorsOrigins.size() == 1) {
					if (ProcessDrawUtils.hasOperatorFreePorts(hoveringOperator)) {
						int pid = controller.getIndex(draggingInSubprocess);
						Point processSpace = view.toProcessSpace(e.getPoint(), pid);
						if (processSpace != null) {
							model.setHoveringConnectionSource(controller.getPortForConnectorNear(processSpace,
									draggingInSubprocess));
						}
					}
				}

				double difX = e.getX() - mousePositionAtDragStart.getX();
				double difY = e.getY() - mousePositionAtDragStart.getY();

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

				// shift
				for (Operator op : draggedOperatorsOrigins.keySet()) {
					Rectangle2D origin = draggedOperatorsOrigins.get(op);
					if (origin.getMaxX() + difX >= unitWidth) {
						if (origin.getMaxX() + difX > maxX) {
							maxX = origin.getMaxX() + difX;
						}
					}
					if (origin.getMaxY() + difY >= unitHeight) {
						if (origin.getMaxY() + difY > maxY) {
							maxY = origin.getMaxY() + difY;
						}
					}

					if (origin.getMinY() + difY < 0) {
						difY = -origin.getMinY() + ProcessDrawer.GRID_Y_OFFSET;
					}
					if (origin.getMinX() + difX < 0) {
						difX = -origin.getMinX() + ProcessDrawer.GRID_X_OFFSET;
					}
					Rectangle2D opPos = new Rectangle2D.Double(Math.floor(origin.getX() + difX), Math.floor(origin.getY()
							+ difY), origin.getWidth(), origin.getHeight());
					model.setOperatorRect(op, opPos);
				}
				model.fireOperatorsMoved(draggedOperatorsOrigins.keySet());
				e.consume();
			}
		} else {
			// ports are draggeable only if they belong to the displayed chain <->
			// they are innersinks of our sources
			if (isDisplayChainPortDragged() &&
					// furthermore they can only be dragged with left mouse button + shift key pressed
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

		if (SwingUtilities.isLeftMouseButton(e)) {
			if (model.getHoveringOperator() == null && model.getHoveringPort() == null
					&& model.getSelectedConnectionSource() != model.getHoveringConnectionSource()) {
				model.setSelectedConnectionSource(model.getHoveringConnectionSource());
				model.fireMiscChanged();
				e.consume();
			}
		}

		if (e.getButton() == MouseEvent.BUTTON2) {
			view.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			return;
		}

		// disconnect when clicking with alt + left mouse on connection
		if (model.getHoveringConnectionSource() != null && model.getHoveringOperator() == null
				&& SwingUtilities.isLeftMouseButton(e) && e.isAltDown()) {
			OutputPort port = model.getHoveringConnectionSource();
			if (port.isConnected()) {
				port.disconnect();
				model.setHoveringConnectionSource(null);
				if (model.getSelectedConnectionSource() != null && model.getSelectedConnectionSource().equals(port)) {
					model.setSelectedConnectionSource(null);
				}
				model.fireMiscChanged();
			}
			e.consume();
		}

		// If mouse pressed while connecting, check if connecting ports should be canceled
		Port connectingPortSource = model.getConnectingPortSource();
		if (connectingPortSource != null) {

			// cancel if right mouse button is pressed
			if (SwingUtilities.isRightMouseButton(e)) {
				cancelConnectionDragging();
				connectionDraggingCanceled = true;
				e.consume();
				return;
			}

			// cancel if any button is pressed but not over hovering port
			if (model.getHoveringPort() == null) {
				cancelConnectionDragging();
				connectionDraggingCanceled = true;
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
				if (hoveringPort instanceof OutputPort) {
					if (((OutputPort) hoveringPort).isConnected()) {
						((OutputPort) hoveringPort).disconnect();
					}
				} else if (hoveringPort instanceof InputPort) {
					if (((InputPort) hoveringPort).isConnected()) {
						((InputPort) hoveringPort).getSource().disconnect();
					}
				}
				view.repaint();
			} else {
				// Left mouse button pressed on port -> start connecting ports
				if (hoveringPort instanceof OutputPort) {
					if (connectingPortSource != null && connectingPortSource instanceof InputPort) {
						connectConnectingPortSourceWithHoveringPort((InputPort) connectingPortSource,
								(OutputPort) hoveringPort, hoveringPort);
					} else {
						if (!e.isShiftDown()) {
							model.setConnectingPortSource(hoveringPort);
						}
					}
				} else if (hoveringPort instanceof InputPort) {
					if (connectingPortSource != null && connectingPortSource instanceof OutputPort) {
						connectConnectingPortSourceWithHoveringPort((InputPort) hoveringPort,
								(OutputPort) connectingPortSource, hoveringPort);
					} else {
						if (!e.isShiftDown()) {
							model.setConnectingPortSource(hoveringPort);
						}
					}
				}
			}
			e.consume();
		} else if (model.getHoveringOperator() == null) {
			// deselect unless shift is pressed
			if (!e.isShiftDown() && !(SwingTools.isControlOrMetaDown(e) && e.getButton() == 1)) {
				if (model.getSelectedOperators().isEmpty() || model.getSelectedOperators().size() >= 1
						&& !model.getSelectedOperators().get(0).equals(model.getDisplayedChain())) {
					controller.selectOperator(model.getDisplayedChain(), true);
				}
			}
		}

		if (hoveringPort != null) {
			controller.selectOperator(hoveringPort.getPorts().getOwner().getOperator(), true);
			pressHasSelected = true;
			e.consume();
		} else {
			if (model.getHoveringOperator() == null) {
				if (!e.isShiftDown() && !(SwingTools.isControlOrMetaDown(e) && e.getButton() == 1)) {
					if (model.getSelectedOperators().isEmpty() || model.getSelectedOperators().size() >= 1
							&& !model.getSelectedOperators().get(0).equals(model.getDisplayedChain())) {
						controller.selectOperator(model.getDisplayedChain(), true);
						pressHasSelected = true;
					}
				}
			}
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
		Operator clickedOperator = null;
		if (model.getHoveringPort() != null) {
			clickedOperator = model.getHoveringPort().getPorts().getOwner().getOperator();
		} else if (model.getHoveringOperator() != null) {
			clickedOperator = model.getHoveringOperator();
		} else {
			clickedOperator = model.getDisplayedChain();
		}
		if (e.isPopupTrigger()
				&& (!model.getDisplayedChain().equals(clickedOperator) || model.getHoveringConnectionSource() != null)) {
			if (!connectionDraggingCanceled) {
				if (view.showPopupMenu(e)) {
					e.consume();
					return;
				}
			}
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
		Operator clickedOperator = null;
		if (model.getHoveringPort() != null) {
			clickedOperator = model.getHoveringPort().getPorts().getOwner().getOperator();
		} else if (model.getHoveringOperator() != null) {
			clickedOperator = model.getHoveringOperator();
		} else {
			clickedOperator = model.getDisplayedChain();
		}
		if (e.isPopupTrigger() && model.getDisplayedChain().equals(clickedOperator)) {
			if (!connectionDraggingCanceled) {
				if (view.showPopupMenu(e)) {
					e.consume();
					return;
				}
			}
		}
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
				connectionDraggingCanceled = true;
			}

			Port hoveringPort = model.getHoveringPort();
			// cancel if any button is released but not over hovering port
			if (hoveringPort == null) {
				cancelConnectionDragging();
				connectionDraggingCanceled = true;
			}

			// connect when released over hovering port
			if (SwingUtilities.isLeftMouseButton(e) && hoveringPort != null && !e.isAltDown()) {
				if (hoveringPort instanceof InputPort && connectingPortSource instanceof OutputPort) {
					connectConnectingPortSourceWithHoveringPort((InputPort) hoveringPort, (OutputPort) connectingPortSource,
							hoveringPort);
				} else if (hoveringPort instanceof OutputPort && connectingPortSource instanceof InputPort) {
					connectConnectingPortSourceWithHoveringPort((InputPort) connectingPortSource, (OutputPort) hoveringPort,
							hoveringPort);
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
						processIndex = view.getProcessIndexUnder(new Point((int) selectionRectangle.getCenterX(),
								(int) selectionRectangle.getCenterY()));
					}
					Point offset = view.toProcessSpace(new Point(0, 0), processIndex);
					if (offset != null) {
						model.getSelectionRectangle().setFrame(selectionRectangle.getX() + offset.getX(),
								selectionRectangle.getY() + offset.getY(), selectionRectangle.getWidth(),
								selectionRectangle.getHeight());
						if (!e.isShiftDown() && !SwingTools.isControlOrMetaDown(e)
								|| model.getSelectedOperators().size() == 1
								&& model.getSelectedOperators().get(0) == model.getDisplayedChain()) { // if
							// we have only selected the parent, we ignore SHIFT and CTRL
							model.clearOperatorSelection();
						}
						for (Operator op : model.getProcess(processIndex).getOperators()) {
							Rectangle2D opRect = model.getOperatorRect(op);
							if (model.getSelectionRectangle().contains(opRect)) {
								controller.selectOperator(op, false);
							}
						}
					}
				}
				model.setSelectionRectangle(null);
				e.consume();
			} else {
				if (hasDragged && draggedOperatorsOrigins != null && draggedOperatorsOrigins.size() == 1) {
					controller.insertIntoHoveringConnection(model.getHoveringOperator());
					e.consume();
				} else if (!hasDragged
						&& model.getHoveringOperator() != null
						&& !e.isPopupTrigger()
						&& SwingUtilities.isLeftMouseButton(e)
						&& (SwingTools.isControlOrMetaDown(e) || model.getSelectedOperators().contains(
								model.getHoveringOperator())
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
			hasDragged = false;
			model.clearDraggedOperators();
		}

		// Popup will only be triggered if mouse has been released and no dragging was done
		// CAUTION: Mac&Linux / Windows do different popup trigger handling. Because of this the
		// popup trigger has to be checked in mousePressed AND mouseReleased
		// WINDOWS: mouseReleased
		// LINUX: mousePressed
		// DO NOT HANDLE BACKGROUND CLICKS HERE, they are handled in a later RenderPhase
		Operator clickedOperator = null;
		if (model.getHoveringPort() != null) {
			clickedOperator = model.getHoveringPort().getPorts().getOwner().getOperator();
		} else if (model.getHoveringOperator() != null) {
			clickedOperator = model.getHoveringOperator();
		} else {
			clickedOperator = model.getDisplayedChain();
		}
		if (e.isPopupTrigger()
				&& (!model.getDisplayedChain().equals(clickedOperator) || model.getHoveringConnectionSource() != null)) {
			if (!connectionDraggingCanceled) {
				if (view.showPopupMenu(e)) {
					e.consume();
					return;
				}
			}
		}

		view.repaint();
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
		Operator clickedOperator = null;
		if (model.getHoveringPort() != null) {
			clickedOperator = model.getHoveringPort().getPorts().getOwner().getOperator();
		} else if (model.getHoveringOperator() != null) {
			clickedOperator = model.getHoveringOperator();
		} else {
			clickedOperator = model.getDisplayedChain();
		}
		if (e.isPopupTrigger() && model.getDisplayedChain().equals(clickedOperator)) {
			if (!connectionDraggingCanceled) {
				if (view.showPopupMenu(e)) {
					e.consume();
					return;
				}
			}
		}
	}

	/**
	 * Call when the mouse has been clicked in the {@link ProcessRendererView}.
	 *
	 * @param e
	 */
	public void mouseClicked(final MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (e.getClickCount() == 2) {
				if (model.getHoveringOperator() != null) {
					if (model.getHoveringOperator() instanceof OperatorChain) {
						model.setDisplayedChain((OperatorChain) model.getHoveringOperator());
						model.fireDisplayedChainChanged();
						RapidMinerGUI.getMainFrame().addViewSwitchToUndo();
					}
					e.consume();
				}
			}
		} else if (SwingUtilities.isRightMouseButton(e)) {
			if (model.getConnectingPortSource() != null) {
				cancelConnectionDragging();
				connectionDraggingCanceled = true;
				e.consume();
			}
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
	};

	/**
	 * Updates the currently hovered element
	 *
	 * @param e
	 */
	private void updateHoveringState(final MouseEvent e) {
		int hoveringProcessIndex = model.getHoveringProcessIndex();
		if (model.getHoveringProcessIndex() != -1) {
			int relativeX = (int) model.getMousePositionRelativeToProcess().getX();
			int relativeY = (int) model.getMousePositionRelativeToProcess().getY();

			OutputPort connectionSourceUnderMouse = controller.getPortForConnectorNear(
					model.getMousePositionRelativeToProcess(), model.getProcess(hoveringProcessIndex));
			if (connectionSourceUnderMouse != model.getHoveringConnectionSource()) {
				model.setHoveringConnectionSource(connectionSourceUnderMouse);
				model.fireMiscChanged();
				e.consume();
			}

			// find inner sinks/sources under mouse
			if (controller.checkPortUnder(model.getProcess(hoveringProcessIndex).getInnerSinks(), relativeX, relativeY)
					|| controller.checkPortUnder(model.getProcess(hoveringProcessIndex).getInnerSources(), relativeX,
							relativeY)) {
				e.consume();
				return;
			}

			// find operator under mouse
			List<Operator> operators = model.getProcess(hoveringProcessIndex).getOperators();
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
				if (rect.contains(new Point2D.Double(relativeX, relativeY))) {
					if (model.getHoveringOperator() != op) {
						model.setHoveringPort(null);
						view.setHoveringOperator(op);
						if (model.getHoveringOperator() instanceof OperatorChain) {
							controller.showStatus(I18N.getGUILabel("processRenderer.displayChain.hover"));
						} else {
							controller.showStatus(I18N.getGUILabel("processRenderer.operator.hover"));
						}
					}
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
		if (model.getHoveringConnectionSource() != null) {
			controller.showStatus(I18N.getGUILabel("processRenderer.connection.hover"));
			e.consume();
		} else {
			controller.clearStatus();
		}
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
							popupPosition = new Point(
									(int) (popupPosition.x + model.getProcessWidth(unit) + ProcessDrawer.WALL_WIDTH),
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
			connectionDraggingCanceled = true;
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
	}
}
