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

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.dnd.RepositoryLocationList;
import com.rapidminer.gui.dnd.TransferableOperator;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotations;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererModelEvent;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.I18N;


/**
 * The controller for the {@link ProcessRendererView} which is responsible for intercepting and
 * reacting to user interaction with the view.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public class ProcessRendererController {

	/** used to detect connection hovering */
	private static final Stroke CONNECTION_HOVER_DETECTION_STROKE = new BasicStroke(30);

	/** the view this controller manipulates */
	private ProcessRendererView view;

	/** the model behind the process renderer */
	private ProcessRendererModel model;

	/**
	 * Creates a new process renderer controller which is responsible to intercept and react to user
	 * interaction with the view and manipulating the model.
	 *
	 * @param view
	 * @param model
	 */
	public ProcessRendererController(ProcessRendererView view, ProcessRendererModel model) {
		this.view = view;
		this.model = model;
	}

	/**
	 * Automatically arranges the operators in the specified process according to a layouting
	 * algorithm.
	 *
	 * @param process
	 * 		the operators of this process will be arranged
	 */
	public void autoArrange(final ExecutionUnit process) {
		List<ExecutionUnit> list = new ArrayList<>(1);
		list.add(0, process);
		autoArrange(list);
	}

	/**
	 * Automatically arranges the {@link Operator Operators} in the specified {@link ExecutionUnit ExecutionUnits} according to a layouting algorithm.
	 *
	 * @param processes
	 * 		the operators of these processes will be arranged
	 */
	public void autoArrange(final List<ExecutionUnit> processes) {
		int unitNumber = processes.size();
		List<Map<Operator, Rectangle2D>> newPositions = new ArrayList<>(unitNumber);

		for (int i = 0; i < unitNumber; i++) {

			if (processes.get(i) == null) {
				throw new IllegalArgumentException("process must not be null!");
			}

			Collection<Operator> sorted = processes.get(i).getOperators();

			mxGraphModel graphModel = new mxGraphModel();
			mxGraph graph = new mxGraph(graphModel);

			Map<Operator, Object> vertexMap = new HashMap<>();
			List<Operator> unconnectedOps = new LinkedList<>();
			List<Operator> connectedOps = new LinkedList<>();

			// insert vertices
			for (Operator op : sorted) {

				// skip unconnected operators
				if (!isOperatorConnected(op)) {
					unconnectedOps.add(op);
					continue;
				}

				connectedOps.add(op);

				Rectangle2D operatorRect = model.getOperatorRect(op);
				Object opVert = graph.insertVertex(null, null, op.getName(), operatorRect.getX(), operatorRect.getY(),
						operatorRect.getWidth(), operatorRect.getHeight(), null);
				vertexMap.put(op, opVert);
			}

			// connect vertices
			for (Operator source : sorted) {
				for (OutputPort out : source.getOutputPorts().getAllPorts()) {
					if (out.isConnected()) {
						Operator dest = out.getDestination().getPorts().getOwner().getOperator();
						if (!(dest instanceof ProcessRootOperator)) {
							String value = source.getName() + " to " + dest.getName();
							graph.insertEdge(null, null, value, vertexMap.get(source), vertexMap.get(dest), null);
						}
					}
				}
			}

			// calculate new layout
			mxHierarchicalLayout layout = new mxHierarchicalLayout(graph, SwingConstants.WEST);
			layout.setInterRankCellSpacing(ProcessDrawer.GRID_X_OFFSET);

			layout.execute(graph.getDefaultParent());

			newPositions.add(i, new HashMap<Operator, Rectangle2D>());

			for (Operator op : connectedOps) {
				mxRectangle cellBounds = graph.getCellBounds(vertexMap.get(op));

				double x = cellBounds.getX() + ProcessDrawer.GRID_X_OFFSET;
				double y = cellBounds.getY() + ProcessDrawer.GRID_Y_OFFSET;
				if (!unconnectedOps.isEmpty()) {
					y += ProcessDrawer.OPERATOR_MIN_HEIGHT + ProcessDrawer.GRID_Y_OFFSET;
				}
				if (model.isSnapToGrid()) {
					Point snappedPoint = ProcessDrawUtils.snap(new Point2D.Double(x, y));
					newPositions.get(i).put(op, new Rectangle2D.Double(snappedPoint.getX(), snappedPoint.getY(),
							cellBounds.getWidth(), cellBounds.getHeight()));
				} else {
					newPositions.get(i).put(op, new Rectangle2D.Double(x, y, cellBounds.getWidth(), cellBounds.getHeight()));
				}
			}

			int index = 0;
			for (Operator op : unconnectedOps) {
				newPositions.get(i).put(op, autoPosition(op, index, false));
				++index;
			}

		}

		moveOperators(processes, newPositions, 10, 100);
	}

	/**
	 * Automatically adapts the size of all processes to fit the available space.
	 */
	public synchronized void autoFit() {
		for (ExecutionUnit unit : model.getProcesses()) {
			ensureOperatorsHaveLocation(unit);
			autoFit(unit, false);
		}

		balance();
		view.updateExtensionButtons();
	}

	/**
	 * Calculates the position of an operator.
	 *
	 * @param index
	 * 		the operators index in the surrounding {@link ExecutionUnit}
	 * @param setPosition
	 * 		If {@code true}, will actually change the position of the operator, otherwise will not move the operator
	 * @return the position and size of the operator, never {@code null}
	 * @see ProcessDrawUtils#autoPosition(Operator, int, ProcessRendererModel) ProcessDrawUtils#autoPosition(op, index, model)
	 */
	public Rectangle2D autoPosition(Operator op, int index, boolean setPosition) {
		Rectangle2D rect = ProcessDrawUtils.autoPosition(op, index, model);
		if (setPosition) {
			model.setOperatorRect(op, rect);
			model.fireOperatorMoved(op);
		}
		return rect;
	}

	/**
	 * Opens a rename textfield at the location of the specified operator.
	 *
	 * @param op
	 * 		the operator to be renamed
	 */
	public void rename(final Operator op) {
		view.rename(op);
	}

	/**
	 * Select the given operator.
	 *
	 * @param op
	 * 		the operator to select
	 * @param clear
	 * 		if {@code true}, an existing selection will be cleared
	 */
	void selectOperator(final Operator op, final boolean clear) {
		selectOperator(op, clear, false);
	}

	/**
	 * Makes sure the current processes are up to date when the displayed chain changes. Also auto
	 * fits in case the view is actually being shown right now.
	 */
	void processDisplayedChainChanged() {
		List<ExecutionUnit> processes;
		OperatorChain op = model.getDisplayedChain();
		if (op == null) {
			processes = Collections.emptyList();
		} else {
			processes = new LinkedList<>(op.getSubprocesses());
		}
		model.setProcesses(processes);
		model.fireProcessesChanged();

		// only auto fit when the view when is actually displayed
		if (view.isShowing()) {
			autoFit();
		}
	}

	/**
	 * Makes sure the process still fits the new height of the operator whose ports have changed.
	 * Also triggers height recalculation of the operator itself.
	 *
	 * @param op
	 * 		the operator which had his ports changed
	 */
	void processPortsChanged(Operator op) {
		if (model.getOperatorRect(op) != null) {
			model.setOperatorRect(op, model.getOperatorRect(op));
			// make sure that process size fits new size of operators
			ensureProcessSizeFits(op.getExecutionUnit(), model.getOperatorRect(op));
		}
	}

	/**
	 * Select the given operator.
	 *
	 * @param op
	 * 		the operator to select
	 * @param clear
	 * 		if {@code true}, an existing selection will be cleared
	 * @param range
	 * 		if true, select interval from last already selected operator to now selected operator
	 */
	void selectOperator(final Operator op, final boolean clear, final boolean range) {
		boolean changed = false;
		LinkedList<Operator> selectedOperators = new LinkedList<>(model.getSelectedOperators());
		if (clear || op == null) {
			if (!selectedOperators.isEmpty()) {
				changed = true;
				if (!range) {
					selectedOperators.clear();
				} else {
					Operator last = null;
					if (!selectedOperators.isEmpty()) {
						last = selectedOperators.getLast();
					}
					selectedOperators.clear();
					if (last != null && last != model.getDisplayedChain()) {
						selectedOperators.add(last);
					}
				}
			}
		} else if (selectedOperators.contains(model.getDisplayedChain())) {
			selectedOperators.remove(model.getDisplayedChain());
		}
		if (range) {
			int lastIndex = -1;
			boolean sameUnit = true;
			if (!selectedOperators.isEmpty()) {
				Operator lastSelected = selectedOperators.getLast();
				if (lastSelected.getExecutionUnit() == null) { // happens if last == Root
					sameUnit = false;
				} else {
					lastIndex = lastSelected.getExecutionUnit().getOperators().indexOf(lastSelected);
					if (lastSelected.getExecutionUnit() != op.getExecutionUnit()) {
						sameUnit = false;
					}
				}
			}
			if (sameUnit) {
				int index = op.getExecutionUnit().getOperators().indexOf(op);
				if (lastIndex < index) {
					for (int i = lastIndex + 1; i <= index; i++) {
						selectedOperators.add(op.getExecutionUnit().getOperators().get(i));
					}
				} else if (lastIndex > index) {
					for (int i = lastIndex - 1; i >= index; i--) {
						selectedOperators.add(op.getExecutionUnit().getOperators().get(i));
					}
				}
			}
		} else {
			boolean contains = selectedOperators.contains(op);
			if (op != null) {
				if (!contains) {
					selectedOperators.add(op);
					changed = true;
				} else if (!clear) {
					selectedOperators.remove(op);
					changed = true;
				}
			}
		}
		if (changed) {
			RapidMinerGUI.getMainFrame().selectOperators(selectedOperators);
		}
	}

	/**
	 * Starting from the current selection, selects the first operator in the given direction.
	 *
	 * @param e
	 * 		the key event which triggered the selection
	 */
	void selectInDirection(final KeyEvent e) {
		int keyCode = e.getKeyCode();
		if (model.getSelectedOperators().isEmpty() ||
				model.getSelectedOperators().size() == 1 && model.getSelectedOperators().get(0) == model.getDisplayedChain()) {
			for (ExecutionUnit unit : model.getProcesses()) {
				if (unit.getNumberOfOperators() > 0) {
					selectOperator(unit.getOperators().get(0), true);
				}
			}
		} else {
			Operator current = model.getSelectedOperators().get(0);
			if (current.getParent() != model.getDisplayedChain()) {
				return;
			}
			Rectangle2D pos = model.getOperatorRect(current);
			ExecutionUnit unit = current.getExecutionUnit();
			if (unit == null) {
				return;
			}
			double smallestDistance = Double.POSITIVE_INFINITY;
			Operator closest = null;
			for (Operator other : unit.getOperators()) {
				Rectangle2D otherPos = model.getOperatorRect(other);
				boolean ok = false;
				switch (keyCode) {
					case KeyEvent.VK_LEFT:
						ok = otherPos.getMinX() < pos.getMinX();
						break;
					case KeyEvent.VK_RIGHT:
						ok = otherPos.getMaxX() > pos.getMaxX();
						break;
					case KeyEvent.VK_UP:
						ok = otherPos.getMinY() < pos.getMinY();
						break;
					case KeyEvent.VK_DOWN:
						ok = otherPos.getMaxY() > pos.getMaxY();
						break;
					default:
				}
				if (ok) {
					double dx = otherPos.getCenterX() - pos.getCenterX();
					double dy = otherPos.getCenterY() - pos.getCenterY();
					double dist = dx * dx + dy * dy;
					if (dist < smallestDistance) {
						smallestDistance = dist;
						closest = other;
					}
				}
			}
			if (closest != null) {
				selectOperator(closest, !e.isShiftDown());
			}
		}
	}

	/**
	 * Returns whether an operator has is connected to either output or input ports.
	 *
	 * @param op
	 * 		the operator in question
	 * @return {@code true} if the operator has a connection; {@code false} otherwise
	 */
	boolean hasConnections(final Operator op) {
		for (Port port : op.getInputPorts().getAllPorts()) {
			if (port.isConnected()) {
				return true;
			}
		}
		for (Port port : op.getOutputPorts().getAllPorts()) {
			if (port.isConnected()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns whether the given {@link Transferable} can be accepted as a drop or not.
	 *
	 * @param t
	 * 		the transferable
	 * @return {@code true} if the process renderer can handle the drop; {@code false} otherwise
	 */
	boolean canImportTransferable(final Transferable t) {
		// check if folder is being dragged. Folders cannot be dropped on the process panel
		Stream<RepositoryLocation> repositoryLocations = null;
		if (t.isDataFlavorSupported(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR)) {
			try {
				Object transferData = t.getTransferData(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_LIST_FLAVOR);
				if (transferData instanceof RepositoryLocationList) {
					repositoryLocations = ((RepositoryLocationList) transferData).getAll().stream();
				} else if (transferData instanceof RepositoryLocation[]) {
					repositoryLocations = Arrays.stream((RepositoryLocation[]) transferData);
				}
			} catch (UnsupportedFlavorException | IOException ignored) {
				// ignore
			}
		} else if (t.isDataFlavorSupported(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR)) {
			try {
				repositoryLocations = Stream.of(
						(RepositoryLocation) t.getTransferData(TransferableOperator.LOCAL_TRANSFERRED_REPOSITORY_LOCATION_FLAVOR));
			} catch (UnsupportedFlavorException | IOException ignored) {
				// ignore
			}
		}
		// if not representing any repo locations or at least one none-folder present, return true
		return repositoryLocations == null || !repositoryLocations.map(l -> {
			try {
				return l.locateEntry();
			} catch (RepositoryException e) {
				return null;
			}
		}).allMatch(e -> e instanceof Folder);
	}

	/**
	 * Connects the operators specified by the output and input port and enables them.
	 *
	 * @param out
	 * 		the output port
	 * @param in
	 * 		the input port
	 */
	void connect(final OutputPort out, final InputPort in) {
		Operator inOp = in.getPorts().getOwner().getOperator();
		if (!inOp.isEnabled()) {
			inOp.setEnabled(true);
		}
		Operator outOp = out.getPorts().getOwner().getOperator();
		if (!outOp.isEnabled()) {
			outOp.setEnabled(true);
		}
		out.connectTo(in);
	}

	/**
	 * Ensures that the process is at least width wide.
	 *
	 * @param executionUnit
	 * 		the process to ensure the minimum width
	 * @param width
	 * 		the mininum width
	 */
	void ensureWidth(final ExecutionUnit executionUnit, final int width) {
		Dimension old = new Dimension((int) model.getProcessWidth(executionUnit),
				(int) model.getProcessHeight(executionUnit));
		if (width > old.getWidth()) {
			model.setProcessWidth(executionUnit, width);
			balance();
			model.fireProcessSizeChanged();
		}
	}

	/**
	 * Ensures that the process is at least height heigh.
	 *
	 * @param executionUnit
	 * 		the process to ensure the minimum height
	 * @param height
	 * 		the minimum height
	 */
	void ensureHeight(final ExecutionUnit executionUnit, final int height) {
		Dimension old = new Dimension((int) model.getProcessWidth(executionUnit),
				(int) model.getProcessHeight(executionUnit));
		if (height > old.getHeight()) {
			model.setProcessHeight(executionUnit, height);
			balance();
			model.fireProcessSizeChanged();
		}
	}

	/**
	 * Increases the process size if necessary for the given operator.
	 *
	 * @param process
	 * 		the process for which the size should be checked
	 * @param rect
	 * 		the location which must fit inside the process
	 * @return {@code true} if process was resized; {@code false} otherwise
	 */
	boolean ensureProcessSizeFits(final ExecutionUnit process, final Rectangle2D rect) {
		Dimension processSize = model.getProcessSize(process);
		if (processSize == null) {
			return false;
		}
		if (rect == null) {
			return false;
		}

		boolean needsResize = false;

		double processWidth = processSize.getWidth() * (1 / model.getZoomFactor());
		double processHeight = processSize.getHeight() * (1 / model.getZoomFactor());
		double width = processWidth;
		double height = processHeight;
		if (processWidth < rect.getMaxX() + ProcessDrawer.GRID_X_OFFSET) {
			double diff = rect.getMaxX() + ProcessDrawer.GRID_X_OFFSET - processWidth;
			if (diff > ProcessDrawer.GRID_X_OFFSET) {
				width += diff;
			} else {
				width += ProcessDrawer.GRID_X_OFFSET;
			}
			needsResize = true;
		}
		if (processHeight < rect.getMaxY() + ProcessDrawer.GRID_Y_OFFSET) {
			double diff = rect.getMaxY() + ProcessDrawer.GRID_Y_OFFSET - processHeight;
			if (diff > ProcessDrawer.GRID_Y_OFFSET) {
				height += diff;
			} else {
				height += ProcessDrawer.GRID_Y_OFFSET;
			}
			needsResize = true;
		}
		if (!needsResize) {
			return false;
		}
		model.setProcessWidth(process, width);
		model.setProcessHeight(process, height);
		balance();
		model.fireProcessSizeChanged();
		return true;
	}

	/**
	 * Checks whether we have a port under the given point (in process space) and, as a side effect,
	 * remembers the hovering port and potentially resets the hovering operator.
	 *
	 * @param ports
	 * 		the ports to be checked
	 * @param x
	 * 		the x coordinate
	 * @param y
	 * 		the y coordinate
	 * @return {@code true} if a port of the given list lies under the coordinates; {@code false}
	 * otherwise
	 */
	boolean checkPortUnder(final Ports<? extends Port> ports, final int x, final int y) {
		for (Port port : ports.getAllPorts()) {
			Point2D location = ProcessDrawUtils.createPortLocation(port, model);
			if (location == null) {
				continue;
			}
			int dx = (int) location.getX() - x;
			int dy = (int) location.getY() - y;
			if (dx * dx + dy * dy < 3 * ProcessDrawer.PORT_SIZE * ProcessDrawer.PORT_SIZE / 2) {
				if (model.getHoveringPort() != port) {
					model.setHoveringPort(port);
					Port connectingPort = model.getConnectingPortSource();
					Port hoveringPort = model.getHoveringPort();
					PortOwner hoveringOwner = hoveringPort.getPorts().getOwner();
					if (connectingPort == null) {
						if (hoveringOwner.getOperator() == model.getDisplayedChain()) {
							showStatus(I18N.getGUILabel("processRenderer.displayChain.port.hover"));
						} else {
							showStatus(I18N.getGUILabel("processRenderer.operator.port.hover"));
						}
					} else {
						PortOwner connectingOwner = connectingPort.getPorts().getOwner();
						// different type of ports, aka input/output or output/input
						if (connectingPort instanceof InputPort && hoveringPort instanceof OutputPort ||
								connectingPort instanceof OutputPort && hoveringPort instanceof InputPort) {
							// only if ports are in the same subprocess, and either we connect (sub)process input and output ports or ports of different operators
							if ((connectingOwner.getOperator() != hoveringOwner.getOperator() || model.getDisplayedChain() == connectingOwner.getOperator())
									&& connectingOwner.getConnectionContext() == hoveringOwner.getConnectionContext()) {
								showStatus(I18N.getGUILabel("processRenderer.operator.port.hover_connect"));
							} else if (hoveringOwner.getOperator() == connectingOwner.getOperator() &&
									model.getDisplayedChain() != connectingOwner.getOperator()) {
								// if input/output ports of same operator should be connected which is NOT currently displayed chain
								showStatus(I18N.getGUILabel("processRenderer.operator.port.hover_switch_source"));
							}
						} else if (connectingPort != hoveringPort && (connectingPort instanceof InputPort && hoveringPort instanceof InputPort ||
								connectingPort instanceof OutputPort && hoveringPort instanceof OutputPort)) {
							// if port is different but both are either input/output, display "switch source of connection"
							showStatus(I18N.getGUILabel("processRenderer.operator.port.hover_switch_source"));
						}
					}
					view.setHoveringOperator(null);
					model.fireMiscChanged();
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the index of the specified process in the current list of processes.
	 *
	 * @param executionUnit
	 * 		the process we want to get the index of
	 * @return the index of the process or -1 if it is not currently displayed
	 */
	int getIndex(final ExecutionUnit executionUnit) {
		for (int i = 0; i < model.getProcesses().size(); i++) {
			if (model.getProcess(i) == executionUnit) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the total height the process renderer occupies.
	 *
	 * @return the height including the padding to the top and bottom
	 */
	double getTotalHeight() {
		double height = 0;
		for (ExecutionUnit u : model.getProcesses()) {
			double h = model.getProcessHeight(u);
			if (h > height) {
				height = h;
			}
		}
		return height;
	}

	/**
	 * Returns the total width the process renderer occupies.
	 *
	 * @return the width including the walls to the left and right
	 */
	double getTotalWidth() {
		double width = 0;
		int count = 0;
		for (ExecutionUnit u : model.getProcesses()) {
			if (count > 0) {
				width += 2 * ProcessDrawer.WALL_WIDTH;
			}
			double w = model.getProcessWidth(u);
			width += w;

			count++;
		}
		return width;
	}

	/**
	 * Sets the initial sizes of all processes if the model does not already contain them.
	 */
	void setInitialSizes() {
		List<ExecutionUnit> units = model.getProcesses();
		for (ExecutionUnit unit : units) {
			Dimension size = model.getProcessSize(unit);
			if (size == null) {
				size = createInitialSize(unit);
				model.setProcessSize(unit, size);
			}
		}
	}

	/**
	 * Returns the first connected {@link OutputPort} for which the specified point lies inside the
	 * connector shape.
	 *
	 * @param p
	 * 		the point in question
	 * @param unit
	 * 		the process for which to check
	 * @return an output port for which the point lies inside the connector or {@code null}
	 */
	OutputPort getPortForConnectorNear(final Point p, final ExecutionUnit unit) {
		List<OutputPort> candidates = new LinkedList<>();
		candidates.addAll(unit.getInnerSources().getAllPorts());
		for (Operator op : unit.getOperators()) {
			candidates.addAll(op.getOutputPorts().getAllPorts());
		}
		for (OutputPort port : candidates) {
			if (port.isConnected()) {
				Shape connector = ProcessDrawUtils.createConnector(port, port.getDestination(), model);
				if (connector == null) {
					return null;
				}
				Shape thick = CONNECTION_HOVER_DETECTION_STROKE.createStrokedShape(connector);
				if (thick.contains(p)) {
					return port;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the closest operator to the left of the given point and process.
	 *
	 * @param p
	 * 		looks for an operator to the left of this location
	 * @param unit
	 * 		the process for the location
	 * @return the closest operator or {@code null}
	 */
	Operator getClosestLeftNeighbour(final Point2D p, final ExecutionUnit unit) {
		Operator closest = null;
		double minDist = Double.POSITIVE_INFINITY;
		for (Operator op : unit.getOperators()) {
			Rectangle2D rect = model.getOperatorRect(op);
			if (rect.getMaxX() >= p.getX()) {
				continue;
			}
			double dx = rect.getMaxX() - p.getX();
			double dy = rect.getMaxY() - p.getY();
			double dist = dx * dx + dy * dy;
			if (dist < minDist) {
				minDist = dist;
				closest = op;
			}
		}
		return closest;
	}

	/**
	 * Insert the specified operator into the currently hovered connection.
	 *
	 * @param operator
	 * 		the operator to be inserted
	 */
	@SuppressWarnings("deprecation")
	void insertIntoHoveringConnection(final Operator operator) {
		OutputPort hoveringConnectionSource = model.getHoveringConnectionSource();
		if (hoveringConnectionSource == null) {
			return;
		}
		InputPort oldDest = hoveringConnectionSource.getDestination();
		oldDest.lock();
		hoveringConnectionSource.lock();
		try {
			// no IndexOutOfBoundsException since checked above
			InputPort bestInputPort = null;
			MetaData md = hoveringConnectionSource.getMetaData();
			if (md != null) {
				for (InputPort inCandidate : operator.getInputPorts().getAllPorts()) {
					if (!inCandidate.isConnected() && inCandidate.isInputCompatible(md, CompatibilityLevel.PRE_VERSION_5)) {
						bestInputPort = inCandidate;
						break;
					}
				}
			} else {
				for (InputPort inCandidate : operator.getInputPorts().getAllPorts()) {
					if (!inCandidate.isConnected()) {
						bestInputPort = inCandidate;
						break;
					}
				}
			}
			if (bestInputPort != null) {
				hoveringConnectionSource.disconnect();
				connect(hoveringConnectionSource, bestInputPort);
				if (RapidMinerGUI.getMainFrame().VALIDATE_AUTOMATICALLY_ACTION.isSelected()) {
					hoveringConnectionSource.getPorts().getOwner().getOperator().transformMetaData();
					operator.transformMetaData();
				}

				OutputPort bestOutput = null;
				for (OutputPort outCandidate : operator.getOutputPorts().getAllPorts()) {
					if (!outCandidate.isConnected()) {
						md = outCandidate.getMetaData();
						if (md != null && oldDest.isInputCompatible(md, CompatibilityLevel.PRE_VERSION_5)) {
							bestOutput = outCandidate;
							break;
						}
					}
				}
				if (bestOutput == null) {
					for (OutputPort outCandidate : operator.getOutputPorts().getAllPorts()) {
						if (!outCandidate.isConnected()) {
							bestOutput = outCandidate;
							break;
						}
					}
				}
				if (bestOutput != null) {
					connect(bestOutput, oldDest);
				}
				model.fireOperatorsMoved(Collections.singleton(operator));
			}
		} finally {
			oldDest.unlock();
			hoveringConnectionSource.unlock();
			model.setHoveringConnectionSource(null);
		}
	}

	/**
	 * Set spacing and reduce spacing for successor if possible.
	 *
	 * @param port
	 * 		the port to be moved
	 * @param delta
	 * 		by how much the port spacing should be changed
	 * @return how much the port was moved
	 */
	double shiftPortSpacing(final Port port, final double delta) {
		// remember old spacing
		final Ports<? extends Port> ports = port.getPorts();
		final int myIndex = ports.getAllPorts().indexOf(port);
		final Double old = (double) model.getPortSpacing(port);

		double newY = old + delta;
		if (model.isSnapToGrid()) {
			newY = Math.floor(newY / (ProcessDrawer.PORT_SIZE * 3d / 2d)) * (ProcessDrawer.PORT_SIZE * 3 / 2);
		}
		double diff = newY - old;

		if (diff == 0) {
			return 0;
		} else if (diff > 0) {
			// find ports which this port will "push" down
			for (int i = myIndex + 1; i < ports.getNumberOfPorts(); i++) {
				Port other = ports.getPortByIndex(i);
				double otherSpacing = model.getPortSpacing(other);
				if (otherSpacing < diff) {
					model.resetPortSpacing(other);
				} else {
					model.setPortSpacing(other, (int) (otherSpacing - diff));
					break;
				}
			}
			// see if it still fits into process frame
			model.setPortSpacing(port, (int) (old + diff));
			Point bottomPortPos = ProcessDrawUtils.createPortLocation(ports.getPortByIndex(ports.getNumberOfPorts() - 1),
					model);
			// if it doesn't, revert
			double height = model.getProcessHeight(ports.getOwner().getConnectionContext());
			if (bottomPortPos != null && bottomPortPos.getY() > height) {
				double tooMuch = bottomPortPos.getY() - height;
				diff -= tooMuch;
				model.setPortSpacing(port, (int) (old + diff));
			}
			return diff;
		} else if (diff < 0) {
			// find ports which this port will "push" up
			double actuallyRemoved = 0;
			for (int i = myIndex; i >= 0; i--) {
				Port other = ports.getPortByIndex(i);
				double otherSpacing = model.getPortSpacing(other);
				if (otherSpacing < -diff) {
					actuallyRemoved += model.getPortSpacing(other);
					model.resetPortSpacing(other);
				} else {
					model.setPortSpacing(other, (int) (otherSpacing + diff));
					actuallyRemoved = -diff;
					break;
				}
			}
			if (ports.getNumberOfPorts() > myIndex + 1) {
				Port other = ports.getPortByIndex(myIndex + 1);
				model.setPortSpacing(other, (int) (model.getPortSpacing(other) + actuallyRemoved));
			}
			return -actuallyRemoved;
		} else {
			// cannot happen
			return 0;
		}
	}

	/**
	 * Returns a {@link Rectangle2D} representing an {@link Operator}. First tries to look up the
	 * data from the model, if that fails determines a position it automatically.
	 *
	 * @param op
	 * 		the operator for which a rectangle should be created
	 * @return the rectangle representing the operator, never {@code null}
	 * @see ProcessDrawUtils#createOperatorPosition(Operator, ProcessRendererModel, BiFunction)
	 */
	Rectangle2D createOperatorPosition(Operator op) {
		return ProcessDrawUtils.createOperatorPosition(op, model, ProcessRendererController::isDependenciesOk);
	}

	/**
	 * A helper method used when {@link #createOperatorPosition(Operator) creating operator positions} to avoid that
	 * the method is called again from {@link ProcessDrawUtils#createPortLocation(Port, ProcessRendererModel) ProcessDrawUtils.createPortLocation}.
	 */
	private static boolean isDependenciesOk(Operator op, ProcessRendererModel model) {
		boolean dependenciesOk = true;
		// we check whether all children know where they are
		Operator sourceOp = op.getInputPorts().getPortByIndex(0).getSource().getPorts().getOwner().getOperator();
		Operator destOp = op.getOutputPorts().getPortByIndex(0).getDestination().getPorts().getOwner().getOperator();
		dependenciesOk &= sourceOp == model.getDisplayedChain() || model.getOperatorRect(sourceOp) != null;
		dependenciesOk &= destOp == model.getDisplayedChain() || model.getOperatorRect(destOp) != null;
		return dependenciesOk;
	}

	/**
	 * Ensures that each operator in the given {@link ExecutionUnit} has a location.
	 *
	 * @param unit
	 * 		the process in question
	 * @return the list of operators that did not have a location and now have one
	 */
	List<Operator> ensureOperatorsHaveLocation(ExecutionUnit unit) {
		return ProcessDrawUtils.ensureOperatorsHaveLocation(unit, model);
	}

	/**
	 * Shows the given message in the main GUI status bar.
	 *
	 * @param msg
	 * 		the message
	 */
	void showStatus(final String msg) {
		RapidMinerGUI.getMainFrame().getStatusBar().setSpecialText(msg);
	}

	/**
	 * Clears the main GUI status bar message.
	 */
	void clearStatus() {
		RapidMinerGUI.getMainFrame().getStatusBar().clearSpecialText();
	}

	/**
	 * Creates the initial size for a process.
	 *
	 * @param unit
	 * 		the process for which the initial size should be created
	 * @return the size, never {@code null}
	 */
	private Dimension createInitialSize(final ExecutionUnit unit) {
		Dimension frameSize;
		if (view.getParent() instanceof JViewport) {
			frameSize = view.getParent().getSize();
		} else {
			frameSize = view.getSize();
		}
		return ProcessDrawUtils.calculatePreferredSize(model, unit, frameSize.width, frameSize.height);
	}

	/**
	 * Moves the operators of the specified process to their new positions with an animation.
	 *
	 * @param processes
	 * 		the processes of the operators to be moved
	 * @param newPositions
	 * 		the new position for each operator
	 * @param steps
	 * 		the number of movement steps to reach their new position
	 * @param time
	 * 		the time in ms for the animation
	 */
	private void moveOperators(final List<ExecutionUnit> processes, final List<Map<Operator, Rectangle2D>> newPositions,
			final int steps, final int time) {
		// store current position, dx, and dy for each operator
		final int listSize = processes.size();
		final List<Map<Operator, Rectangle2D>> current = new ArrayList<>(listSize);
		final List<Map<Operator, Double>> dxs = new ArrayList<>(listSize);
		final List<Map<Operator, Double>> dys = new ArrayList<>(listSize);
		for (int i = 0; i < listSize; i++) {
			current.add(i, new HashMap<Operator, Rectangle2D>());
			dxs.add(i, new HashMap<Operator, Double>());
			dys.add(i, new HashMap<Operator, Double>());

			for (Operator op : newPositions.get(i).keySet()) {
				Rectangle2D currentPos = model.getOperatorRect(op);
				Rectangle2D endPos = newPositions.get(i).get(op);
				current.get(i).put(op, currentPos);
				dxs.get(i).put(op, Math.floor((endPos.getX() - currentPos.getX()) / steps));
				dys.get(i).put(op, Math.floor((endPos.getY() - currentPos.getY()) / steps));
			}
		}

		final Timer operatorMoverTimer = new Timer((int) (time / (double) steps), null);

		ActionListener operatorMover = new ActionListener() {

			private int count = 0;

			@Override
			public void actionPerformed(ActionEvent e) {

				// iterate over all ExecutionUnits
				for (int i = 0; i < listSize; i++) {

					// iterate over all moving operators
					Iterator<Operator> iterator = current.get(i).keySet().iterator();
					while (iterator.hasNext()) {
						Operator op = iterator.next();

						// check if display rect equals current stored rect
						Rectangle2D displayRect = model.getOperatorRect(op);
						Rectangle2D currentRect = current.get(i).get(op);
						if (currentRect.getX() != displayRect.getX() || currentRect.getY() != displayRect.getY()) {
							// remove operator as it has been moved by the user
							iterator.remove();
						}

						// calculate new display position for operator
						double dx = dxs.get(i).get(op);
						double dy = dys.get(i).get(op);
						double x;
						double y;
						// during animation, we don't really care about exact positioning
						if (count < steps - 1) {
							x = currentRect.getX() + dx;
							y = currentRect.getY() + dy;
						} else {
							// this is the final position, it has to be exact
							x = newPositions.get(i).get(op).getX();
							y = newPositions.get(i).get(op).getY();
						}
						currentRect = new Rectangle2D.Double(x, y, currentRect.getWidth(), currentRect.getHeight());

						// update current rect in map and also set position as display position
						current.get(i).put(op, currentRect);
						model.setOperatorRect(op, currentRect);
						model.fireOperatorMoved(op);
					}
				}

				// after moving all operators, update UI, increase counter, and reset timer
				// use {@link ProcessRendererView#doRepaint()} instead of {@link
				// ProcessRendererView#repaint()} since repaint must happen instantly
				view.doRepaint();
				++count;
				if (count == steps) {
					operatorMoverTimer.stop();
					autoFit();
					processes.get(0).getEnclosingOperator().getProcess().updateNotify();
				}
			}
		};

		operatorMoverTimer.addActionListener(operatorMover);
		operatorMoverTimer.start();
	}

	/**
	 * Returns whether the specified operator is connected or not. An operator is considered
	 * connected if any of his input or output ports are connected.
	 *
	 * @param op
	 * @return {@code true} if it is connected; {@code false} otherwise
	 */
	private boolean isOperatorConnected(Operator op) {
		for (InputPort port : op.getInputPorts().getAllPorts()) {
			if (port.isConnected()) {
				return true;
			}
		}
		for (OutputPort port : op.getOutputPorts().getAllPorts()) {
			if (port.isConnected()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Balance the display of all processes so they are of equal size.
	 */
	private void balance() {
		balanceHeights();
	}

	/**
	 * Balance the display height of all processes so they are of equal size.
	 */
	private void balanceHeights() {
		double height = 0;
		for (ExecutionUnit p : model.getProcesses()) {
			double h = model.getProcessHeight(p);
			if (h > height) {
				height = h;
			}
		}
		for (ExecutionUnit p : model.getProcesses()) {
			setHeight(p, height * model.getZoomFactor());
		}
	}

	/**
	 * Set the height of the given process. Fires a
	 * {@link ProcessRendererModelEvent.ModelEvent#PROCESS_SIZE_CHANGED} event if the height has
	 * changed.
	 *
	 * @param executionUnit
	 * 		the process for which to set the height
	 * @param height
	 * 		the new height
	 */
	private void setHeight(final ExecutionUnit executionUnit, final double height) {
		if (model.getProcessHeight(executionUnit) != height) {
			model.setProcessHeight(executionUnit, height * (1 / model.getZoomFactor()));
			model.fireProcessSizeChanged();
		}
	}

	/**
	 * Automatically adapts the size of the given process to fit the available space.
	 *
	 * @param process
	 * 		the size of this process will be adapted
	 * @param balance
	 * 		if {@code true}, will balance the size of all processes
	 */
	private void autoFit(ExecutionUnit process, boolean balance) {
		double w = 0;
		double h = 0;
		for (Operator op : process.getOperators()) {
			// operator location
			Rectangle2D bounds = model.getOperatorRect(op);
			if (bounds.getMaxX() > w) {
				w = bounds.getMaxX();
			}
			if (bounds.getMaxY() > h) {
				h = bounds.getMaxY();
			}

			// operator annotations
			WorkflowAnnotations annotations = model.getOperatorAnnotations(op);
			if (annotations != null) {
				for (WorkflowAnnotation anno : annotations.getAnnotationsDrawOrder()) {
					bounds = anno.getLocation();
					if (bounds.getMaxX() > w) {
						w = bounds.getMaxX();
					}
					if (bounds.getMaxY() > h) {
						h = bounds.getMaxY();
					}
				}
			}
		}
		// process annotations
		WorkflowAnnotations annotations = model.getProcessAnnotations(process);
		if (annotations != null) {
			for (WorkflowAnnotation anno : annotations.getAnnotationsDrawOrder()) {
				Rectangle2D bounds = anno.getLocation();
				if (bounds.getMaxX() > w) {
					w = bounds.getMaxX();
				}
				if (bounds.getMaxY() > h) {
					h = bounds.getMaxY();
				}
			}
		}

		for (Port port : process.getInnerSources().getAllPorts()) {
			Point pLoc = ProcessDrawUtils.createPortLocation(port, model);
			if (pLoc != null) {
				h = Math.max(h, pLoc.getY());
			}
		}
		for (Port port : process.getInnerSinks().getAllPorts()) {
			Point pLoc = ProcessDrawUtils.createPortLocation(port, model);
			if (pLoc != null) {
				h = Math.max(h, pLoc.getY());
			}
		}

		double minWidth = ProcessDrawer.OPERATOR_WIDTH * 2;
		double subprocessWidth = w + ProcessDrawer.GRID_X_OFFSET;
		double subprocessHeight = h + ProcessDrawer.GRID_Y_OFFSET;

		// consider zoom factor
		if (model.getZoomFactor() < 1) {
			subprocessWidth *= model.getZoomFactor();
			subprocessHeight *= model.getZoomFactor();
		}

		double height = subprocessHeight;
		double width = subprocessWidth > minWidth ? subprocessWidth : minWidth;

		// if less than original size, set to original size
		Dimension initialSize = createInitialSize(process);

		if (width < initialSize.getWidth()) {
			width = initialSize.getWidth();
		}
		if (height < initialSize.getHeight()) {
			height = initialSize.getHeight();
		}

		// at this point we have might have scrollbars. Check if we a) have them and b) need them.
		// If not, add scrollbar size / number of processes to width/height
		// if this code is removed, process size is smaller than it could be after first autofit
		if (JScrollPane.class.isAssignableFrom(view.getParent().getParent().getClass())) {
			JScrollPane sp = (JScrollPane) view.getParent().getParent();
			if (sp.getHorizontalScrollBar().isVisible() && sp.getVerticalScrollBar().isVisible()) {
				double targetWidth = sp.getSize().getWidth() / model.getProcesses().size();
				double targetHeight = sp.getSize().getHeight();
				double sbWidth = sp.getVerticalScrollBar().getSize().getWidth() / model.getProcesses().size();
				double sbHeight = sp.getHorizontalScrollBar().getSize().getHeight();
				if (width < targetWidth && subprocessWidth < targetWidth) {
					width += sbWidth;
				}
				if (height < targetHeight && subprocessHeight < targetHeight) {
					height += sbHeight;
				}
			}
		}

		Dimension newDim = new Dimension();
		newDim.setSize(width, height);
		model.setProcessSize(process, newDim);

		if (balance) {
			balance();
		}

		model.fireProcessSizeChanged();
	}
}
