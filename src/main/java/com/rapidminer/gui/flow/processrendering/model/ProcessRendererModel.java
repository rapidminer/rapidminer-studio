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
package com.rapidminer.gui.flow.processrendering.model;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.event.EventListenerList;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.annotations.model.OperatorAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.ProcessAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotations;
import com.rapidminer.gui.flow.processrendering.background.ProcessBackgroundImage;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererAnnotationEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererAnnotationEvent.AnnotationEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererEventListener;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererModelEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererModelEvent.ModelEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererOperatorEvent;
import com.rapidminer.gui.flow.processrendering.event.ProcessRendererOperatorEvent.OperatorEvent;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.io.process.GUIProcessXMLFilter;
import com.rapidminer.io.process.ProcessXMLFilterRegistry;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.parameter.ParameterChangeListener;


/**
 * The model backing the process renderer view. It contains all the data necessary to draw the
 * current process. The minimal configuration which is required to draw a process via the
 * {@link ProcessDrawer} can be achieved by calling the following setters:
 * <ul>
 * <li>{@link #setDisplayedChain(OperatorChain)}</li>
 * <li>{@link #setProcesses(ExecutionUnit[])}</li>
 * <li>{@link #setProcessSize(ExecutionUnit, Dimension)}</li>
 * </ul>
 * <p>
 * Note that the model itself does not fire any events. To trigger events, call any of the fireXYZ
 * methods. This is done for performance reasons and to support batch updates and only trigger
 * events when really needed.
 * </p>
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public final class ProcessRendererModel {

	/** the font for the operator name */
	public static final Font OPERATOR_FONT = new Font(Font.DIALOG, Font.BOLD, 11);

	/** the height of the operator name header */
	public static final int HEADER_HEIGHT = OPERATOR_FONT.getSize() + 7;

	/** the width of each operator */
	public static final int OPERATOR_WIDTH = 5 * 16 + 2 * 5;  // 5 mini icons + padding

	/** the minimum height of an operator */
	public static final int MIN_OPERATOR_HEIGHT = 50 + HEADER_HEIGHT;

	/** the size of each operator/process port */
	public static final int PORT_SIZE = 14;

	/** event listener for this model */
	private final EventListenerList eventListener;

	/** a list of the currently selected operators */
	private List<Operator> selectedOperators;

	/** a list of the currently dragged operators */
	private List<Operator> draggedOperators;

	/** the displayed processes */
	private List<ExecutionUnit> processes;

	/** the currently displayed operator chain */
	private OperatorChain displayedChain;

	/** whether snap to grid is enabled */
	private boolean snapToGrid;

	/** source port of the current connection */
	private OutputPort selectedConnectionSource;

	/** source port of the connection currently being created */
	private Port connectingPortSource;

	/** index of the process under the mouse */
	private int hoveringProcessIndex;

	/** port under the mouse cursor */
	private Port hoveringPort;

	/** operator under the mouse cursor */
	private Operator hoveringOperator;

	/** source port of the currently hovered connector */
	private OutputPort hoveringConnectionSource;

	/** the currently selected area */
	private Rectangle2D selectionRectangle;

	/** the size of the individual subprocesses */
	private final Map<ExecutionUnit, Dimension> processSizes;

	/** the number of ports for each operator */
	private final Map<Operator, Integer> portNumbers;

	/** if an operator is dragged from the operator tree or if a repository entry is dragged. */
	private boolean dragStarted;

	/** indicates if the droptarget could be set */
	private boolean dropTargetSet;

	/** indicates if an operator source (tree, WoC, ...) is hovered */
	private boolean operatorSourceHovered;

	/** the position the mouse is currently at */
	private Point currentMousePosition;

	/**
	 * if canImport of transfer handler has returned <code>true</code>. Will be set to false if
	 * mouse has exited the process renderer
	 */
	private boolean importDragged;

	/** the current mouse position relative to the process the mouse is over */
	private Point mousePositionRelativeToProcess;

	// initialize the filter responsible for reading/writing operator coordinates from/to XML
	static {
		ProcessXMLFilterRegistry.registerFilter(new GUIProcessXMLFilter());
	}

	public ProcessRendererModel() {
		this.eventListener = new EventListenerList();

		this.processes = Collections.unmodifiableList(Collections.<ExecutionUnit> emptyList());
		this.selectedOperators = Collections.unmodifiableList(Collections.<Operator> emptyList());
		this.draggedOperators = Collections.unmodifiableList(Collections.<Operator> emptyList());
		this.processSizes = new WeakHashMap<>();
		this.portNumbers = new WeakHashMap<>();
		this.snapToGrid = Boolean.parseBoolean(ParameterService
				.getParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_SNAP_TO_GRID));
		this.hoveringProcessIndex = -1;

		// listen for snapToGrid changes
		ParameterService.registerParameterChangeListener(new ParameterChangeListener() {

			@Override
			public void informParameterSaved() {
				// ignore
			}

			@Override
			public void informParameterChanged(String key, String value) {
				if (RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_SNAP_TO_GRID.equals(key)) {
					setSnapToGrid(Boolean.parseBoolean(value));
				}
			}
		});
	}

	/**
	 * Returns the currently displayed processes.
	 *
	 * @return the immutable list of currently displayed processes. Never returns {@code null}.
	 */
	public List<ExecutionUnit> getProcesses() {
		return processes;
	}

	/**
	 * Returns the process at the specified index.
	 *
	 * @param index
	 *            the index of the process to return. If index is invalid, throws
	 * @return the currently displayed process at the specified index
	 * @throws IndexOutOfBoundsException
	 *             if index < 0 or index >= length
	 */
	public ExecutionUnit getProcess(int index) throws ArrayIndexOutOfBoundsException {
		return getProcesses().get(index);
	}

	/**
	 * Returns the index of the given process.
	 *
	 * @param process
	 *            the process for which the index should be retrieved
	 * @return the index of the process starting with {@code 0} or {@code -1} if the process is not
	 *         part of {@link #getProcesses()}
	 */
	public int getProcessIndex(ExecutionUnit process) {
		return getProcesses().indexOf(process);
	}

	/**
	 * Sets the currently displayed processes.
	 *
	 * @param processes
	 *            the new processes to display
	 */
	public void setProcesses(List<ExecutionUnit> processes) {
		if (processes == null) {
			throw new IllegalArgumentException("processes must not be null!");
		}
		this.processes = Collections.unmodifiableList(processes);
	}

	/**
	 * Returns the currently displayed {@link OperatorChain}.
	 *
	 * @return the operator chain, never {@code null}
	 */
	public OperatorChain getDisplayedChain() {
		return displayedChain;
	}

	/**
	 * Sets the currently displayed {@link OperatorChain}. Call {@link #fireDisplayedChainChanged()}
	 * to trigger the event.
	 *
	 * @param displayedChain
	 *            the new operator chain to display
	 */
	public void setDisplayedChain(OperatorChain displayedChain) {
		if (displayedChain == null) {
			throw new IllegalArgumentException("displayedChain must not be null!");
		}
		this.displayedChain = displayedChain;
	}

	/**
	 * Returns the currently in the process view selected {@link Operator}s.
	 *
	 * @return the immutable list of currently selected operators
	 */
	public List<Operator> getSelectedOperators() {
		return selectedOperators;
	}

	/**
	 * Clears the operator selection.
	 */
	public void clearOperatorSelection() {
		this.selectedOperators = Collections.unmodifiableList(Collections.<Operator> emptyList());
	}

	/**
	 * Adds the given operator to the currently selected operators.
	 *
	 * @param selectedOperator
	 *            this operator is added to the list of currently selected operators
	 */
	public void addOperatorToSelection(Operator selectedOperator) {
		List<Operator> newList = new ArrayList<>(getSelectedOperators().size() + 1);
		newList.addAll(getSelectedOperators());
		newList.add(selectedOperator);
		this.selectedOperators = Collections.unmodifiableList(newList);
	}

	/**
	 * Removes the given operator from the currently selected operators. If the given operator is
	 * not selected, does nothing.
	 *
	 * @param selectedOperator
	 *            this operator is removed from the list of currently selected operators
	 */
	public void removeOperatorFromSelection(Operator selectedOperator) {
		List<Operator> newList = new ArrayList<>(getSelectedOperators());
		newList.remove(selectedOperator);
		this.selectedOperators = Collections.unmodifiableList(newList);
	}

	/**
	 * Adds the given operators to the currently selected operators.
	 *
	 * @param selectedOperators
	 *            these operators are added to the list of currently selected operators
	 */
	public void addOperatorsToSelection(List<Operator> selectedOperators) {
		List<Operator> newList = new ArrayList<>(getSelectedOperators().size() + selectedOperators.size());
		newList.addAll(getSelectedOperators());
		newList.addAll(selectedOperators);
		this.selectedOperators = Collections.unmodifiableList(newList);
	}

	/**
	 * Returns the currently in the process view dragged {@link Operator}s.
	 *
	 * @return the immutable list of currently dragged operators
	 */
	public List<Operator> getDraggedOperators() {
		return draggedOperators;
	}

	/**
	 * Sets the given operators as the currently dragged operators.
	 *
	 * @param draggedOperators
	 *            these operators are set as the currently dragged operators
	 */
	public void setDraggedOperators(Collection<Operator> draggedOperators) {
		List<Operator> newList = new ArrayList<>(draggedOperators.size());
		newList.addAll(draggedOperators);
		this.draggedOperators = Collections.unmodifiableList(newList);
	}

	/**
	 * Clears the dragged operators.
	 */
	public void clearDraggedOperators() {
		this.draggedOperators = Collections.unmodifiableList(Collections.<Operator> emptyList());
	}

	/**
	 * Whether operators snap to a grid or not.
	 *
	 * @return {@code true} if they do; {@code false} otherwise
	 */
	public boolean isSnapToGrid() {
		return snapToGrid;
	}

	/**
	 * Sets whether operators snap to a grid or not.
	 *
	 * @param snapToGrid
	 *            whether operators should snap to a grid or not
	 */
	public void setSnapToGrid(boolean snapToGrid) {
		this.snapToGrid = snapToGrid;
	}

	/**
	 * Whether a drag operation (operator or repository entry) is in progress.
	 *
	 * @return {@code true} if dragging is in progress; {@code false} otherwise
	 */
	public boolean isDragStarted() {
		return dragStarted;
	}

	/**
	 * Sets whether a drag operation (operator or repository entry) is in progress.
	 *
	 * @param dragStarted
	 *            {@code true} if dragging is in progress; {@code false} otherwise
	 */
	public void setDragStarted(boolean dragStarted) {
		this.dragStarted = dragStarted;
	}

	/**
	 * Whether a drop target was set or not. If this is not set to {@code true}, drag&drop is not
	 * supported.
	 *
	 * @return {@code true} if a valid drop target was set; {@code false} otherwise
	 */
	public boolean isDropTargetSet() {
		return dropTargetSet;
	}

	/**
	 * Sets whether a drop target was set or not. If this is not set to {@code true}, drag&drop is
	 * not supported.
	 *
	 * @param dropTargetSet
	 *            {@code true} if a valid drop target was set; {@code false} otherwise
	 */
	public void setDropTargetSet(boolean dropTargetSet) {
		this.dropTargetSet = dropTargetSet;
	}

	/**
	 * Whether an an operator source (tree, WoC, ...) is hovered or not.
	 *
	 * @return {@code true} if an operator source (tree, WoC, ...); {@code false} otherwise
	 */
	public boolean isOperatorSourceHovered() {
		return operatorSourceHovered;
	}

	/**
	 * Sets whether an an operator source (tree, WoC, ...) is hovered or not.
	 *
	 * @param operatorSourceHovered
	 *            {@code true} if a an operator source (tree, WoC, ...) is hovered; {@code false}
	 *            otherwise
	 */
	public void setOperatorSourceHovered(boolean operatorSourceHovered) {
		this.operatorSourceHovered = operatorSourceHovered;
	}

	/**
	 * Sets the current mouse position over the process renderer. Can be {@code null}.
	 *
	 * @param currentMousePosition
	 *            the position or {@code null} if it is not over the renderer
	 */
	public void setCurrentMousePosition(Point currentMousePosition) {
		this.currentMousePosition = currentMousePosition;
	}

	/**
	 * The current mouse position over the process renderer. Can be {@code null}.
	 *
	 * @return the mouse position or {@code null}
	 */
	public Point getCurrentMousePosition() {
		return currentMousePosition;
	}

	/**
	 * Whether the currently dragged import was accepted by the transfer handler, i.e. if the import
	 * is accepted.
	 *
	 * @return {@code true} if the import would be accepted; {@code false} otherwise
	 */
	public boolean isImportDragged() {
		return importDragged;
	}

	/**
	 * Sets whether the currently dragged import was accepted by the transfer handler, i.e. if the
	 * import is accepted.
	 *
	 * @param importDragged
	 *            {@code true} if the import would be accepted; {@code false} otherwise
	 */
	public void setImportDragged(boolean importDragged) {
		this.importDragged = importDragged;
	}

	/**
	 * The selected (via click) connection source port.
	 *
	 * @return the port or {@code null}
	 */
	public OutputPort getSelectedConnectionSource() {
		return selectedConnectionSource;
	}

	/**
	 * Sets the selected connection source port.
	 *
	 * @param selectedConnectionSource
	 *            the connection source port or {@code null}
	 */
	public void setSelectedConnectionSource(OutputPort selectedConnectionSource) {
		this.selectedConnectionSource = selectedConnectionSource;
	}

	/**
	 * The connection source port of the connection currently being created.
	 *
	 * @return the port or {@code null}
	 */
	public Port getConnectingPortSource() {
		return connectingPortSource;
	}

	/**
	 * Sets the connection source port of the connection currently being created.
	 *
	 * @param connectingPortSource
	 *            the source port of the connection currently being created or {@code null}
	 */
	public void setConnectingPortSource(Port connectingPortSource) {
		this.connectingPortSource = connectingPortSource;
	}

	/**
	 * Returns the index of the process over which the mouse currently hovers.
	 *
	 * @return the hovered process index or -1 if not hovering over any
	 */
	public int getHoveringProcessIndex() {
		return hoveringProcessIndex;
	}

	/**
	 * Sets the index of the process over which the mosue currently hovers.
	 *
	 * @param hoveringProcessIndex
	 *            the hovered process index
	 */
	public void setHoveringProcessIndex(int hoveringProcessIndex) {
		this.hoveringProcessIndex = hoveringProcessIndex;
	}

	/**
	 * Gets the port over which the mouse hovers.
	 *
	 * @return the port or {@code null}
	 */
	public Port getHoveringPort() {
		return hoveringPort;
	}

	/**
	 * Sets the operator over which the mouse hovers.
	 *
	 * @param hoveringOperator
	 *            the operator under the mouse or {@code null}
	 */
	public void setHoveringOperator(Operator hoveringOperator) {
		this.hoveringOperator = hoveringOperator;
	}

	/**
	 * Gets the {@link Operator} over which the mouse hovers.
	 *
	 * @return the operator or {@code null}
	 */
	public Operator getHoveringOperator() {
		return hoveringOperator;
	}

	/**
	 * Sets the {@link OutputPort} of the connection over which the mouse hovers.
	 *
	 * @param hoveringConnectionSource
	 *            the output port of the connection under the mouse or {@code null}
	 */
	public void setHoveringConnectionSource(OutputPort hoveringConnectionSource) {
		this.hoveringConnectionSource = hoveringConnectionSource;
	}

	/**
	 * Gets the {@link OutputPort} of the connection over which the mouse hovers.
	 *
	 * @return the output port or {@code null}
	 */
	public OutputPort getHoveringConnectionSource() {
		return hoveringConnectionSource;
	}

	/**
	 * Sets the port over which the mouse hovers.
	 *
	 * @param hoveringPort
	 *            the port under the mouse or {@code null}
	 */
	public void setHoveringPort(Port hoveringPort) {
		this.hoveringPort = hoveringPort;
	}

	/**
	 * Gets the {@link Rectangle2D} which represents the current selection box of the user.
	 *
	 * @return the rectangle or {@code null}
	 */
	public Rectangle2D getSelectionRectangle() {
		return selectionRectangle;
	}

	/**
	 * Sets the rectangle which represents the current selection box of the user.
	 *
	 * @param selectionRectangle
	 *            the selection rectangle or {@code null}
	 */
	public void setSelectionRectangle(Rectangle2D selectionRectangle) {
		this.selectionRectangle = selectionRectangle;
	}

	/**
	 * Returns the size of the given process.
	 *
	 * @param process
	 *            the size of this process is returned
	 * @return the size of the specified process or {@code null}
	 */
	public Dimension getProcessSize(ExecutionUnit process) {
		return processSizes.get(process);
	}

	/**
	 * Returns the width of the given process. Convenience method which simply returns the width of
	 * {@link #getProcessSize(ExecutionUnit)}.
	 *
	 * @param process
	 *            the process for which the width should be returned
	 * @return the width or -1 if no process size has been stored
	 */
	public double getProcessWidth(ExecutionUnit process) {
		Dimension dim = processSizes.get(process);
		if (dim == null) {
			return -1;
		}
		return dim.getWidth();
	}

	/**
	 * Sets the width for the given process. If {@link #getProcessSize(ExecutionUnit)} returns
	 * {@code null} for the specified process, does nothing.
	 *
	 * @param process
	 *            the process for which the height should be set
	 * @param width
	 *            the new width
	 */
	public void setProcessWidth(ExecutionUnit process, double width) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}
		Dimension dim = processSizes.get(process);
		if (dim == null) {
			return;
		}
		dim.setSize(width, dim.getHeight());
	}

	/**
	 * Returns the height of the given process. Convenience method which simply returns the height
	 * of {@link #getProcessSize(ExecutionUnit)}.
	 *
	 * @param process
	 *            the process for which the height should be returned
	 * @return the height or -1 if no process size has been stored
	 */
	public double getProcessHeight(ExecutionUnit process) {
		Dimension dim = processSizes.get(process);
		if (dim == null) {
			return -1;
		}
		return dim.getHeight();
	}

	/**
	 * Sets the height for the given process. If {@link #getProcessSize(ExecutionUnit)} returns
	 * {@code null} for the specified process, does nothing.
	 *
	 * @param process
	 *            the process for which the height should be set
	 * @param height
	 *            the new height
	 */
	public void setProcessHeight(ExecutionUnit process, double height) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}
		Dimension dim = processSizes.get(process);
		if (dim == null) {
			return;
		}
		dim.setSize(dim.getWidth(), height);
	}

	/**
	 * Sets the size of the given process.
	 *
	 * @param process
	 *            the size of this process is stored
	 * @param size
	 *            the size of the specified process
	 */
	public void setProcessSize(ExecutionUnit process, Dimension size) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}
		if (size == null) {
			throw new IllegalArgumentException("size must not be null!");
		}
		this.processSizes.put(process, size);
	}

	/**
	 * Returns a {@link Rectangle2D} representing the given {@link Operator}.
	 *
	 * @param op
	 *            the operator in question
	 * @return the rectangle. Can return {@code null} but only if the operator has not been added to
	 *         the {@link ProcessRendererView}.
	 */
	public Rectangle2D getOperatorRect(Operator op) {
		return GUIProcessXMLFilter.lookupOperatorRectangle(op);
	}

	/**
	 * Returns the {@link WorkflowAnnotations} container for the given {@link Operator}.
	 *
	 * @param op
	 *            the operator in question
	 * @return the container. Can be {@code null} if no annotations exist for this operator
	 */
	public WorkflowAnnotations getOperatorAnnotations(Operator op) {
		return GUIProcessXMLFilter.lookupOperatorAnnotations(op);
	}

	/**
	 * Removes the given {@link OperatorAnnotation}.
	 *
	 * @param annotation
	 *            the annotation to remove
	 */
	public void removeOperatorAnnotation(OperatorAnnotation anno) {
		GUIProcessXMLFilter.removeOperatorAnnotation(anno);
	}

	/**
	 * Adds the given {@link OperatorAnnotation}.
	 *
	 * @param annotation
	 *            the annotation to add
	 */
	public void addOperatorAnnotation(OperatorAnnotation anno) {
		GUIProcessXMLFilter.addOperatorAnnotation(anno);
	}

	/**
	 * Returns the {@link WorkflowAnnotations} container for the given {@link ExecutionUnit}.
	 *
	 * @param process
	 *            the process in question
	 * @return the container. Can be {@code null} if no annotations exist for this process
	 */
	public WorkflowAnnotations getProcessAnnotations(ExecutionUnit process) {
		return GUIProcessXMLFilter.lookupProcessAnnotations(process);
	}

	/**
	 * Removes the given {@link ProcessAnnotation}.
	 *
	 * @param annotation
	 *            the annotation to remove
	 */
	public void removeProcessAnnotation(ProcessAnnotation anno) {
		GUIProcessXMLFilter.removeProcessAnnotation(anno);
	}

	/**
	 * Adds the given {@link ProcessAnnotation}.
	 *
	 * @param annotation
	 *            the annotation to add
	 */
	public void addProcessAnnotation(ProcessAnnotation anno) {
		GUIProcessXMLFilter.addProcessAnnotation(anno);
	}

	/**
	 * Returns the {@link ProcessBackgroundImage} for the given {@link ExecutionUnit}.
	 *
	 * @param process
	 *            the process in question
	 * @return the background image. Can be {@code null} if none is set for this process
	 */
	public ProcessBackgroundImage getBackgroundImage(ExecutionUnit process) {
		return GUIProcessXMLFilter.lookupBackgroundImage(process);
	}

	/**
	 * Removes the given {@link ProcessBackgroundImage}.
	 *
	 * @param process
	 *            the process for which to remove the background image
	 */
	public void removeBackgroundImage(ExecutionUnit process) {
		GUIProcessXMLFilter.removeBackgroundImage(process);
	}

	/**
	 * Sets the given {@link ProcessBackgroundImage}.
	 *
	 * @param image
	 *            the image to add
	 */
	public void setBackgroundImage(ProcessBackgroundImage image) {
		GUIProcessXMLFilter.setBackgroundImage(image);
	}

	/**
	 * Returns the number of ports for the given {@link Operator}.
	 *
	 * @param op
	 *            the operator in question
	 * @return the number of ports or {@code null} if they have not yet been stored
	 */
	public Integer getNumberOfPorts(Operator op) {
		return portNumbers.get(op);
	}

	/**
	 * Sets the number of ports for the given {@link Operator}.
	 *
	 * @param op
	 *            the operator in question
	 * @param number
	 *            the number of ports or {@code null}
	 */
	public Integer setNumberOfPorts(Operator op, Integer number) {
		return portNumbers.put(op, number);
	}

	/**
	 * Sets the {@link Operator} {@link Rectangle2D}. Calculates and sets the height of the operator
	 * to match the existing ports!
	 *
	 * @param op
	 *            the operator for which the rectangle should be set
	 * @param rect
	 *            the rectangle representing position and size of operator
	 */
	public void setOperatorRect(Operator op, Rectangle2D rect) {
		if (op == null) {
			throw new IllegalArgumentException("op must not be null!");
		}
		if (rect == null) {
			throw new IllegalArgumentException("rect must not be null!");
		}

		// make sure operator is neither too tall nor too short
		double height = ProcessDrawUtils.calcHeighForOperator(op);
		if (rect.getHeight() != height) {
			rect.setRect(rect.getX(), rect.getY(), rect.getWidth(), height);
		}

		GUIProcessXMLFilter.setOperatorRectangle(op, rect);
	}

	/**
	 * Returns the spacing of the specified {@link Port}.
	 *
	 * @param port
	 *            the port in question
	 * @return the additional spacing before this port
	 */
	public int getPortSpacing(Port port) {
		return GUIProcessXMLFilter.lookupPortSpacing(port);
	}

	/**
	 * Sets the spacing of the specified {@link Port}.
	 *
	 * @param port
	 *            the port in question
	 * @param spacing
	 *            the additional spacing before the port
	 */
	public void setPortSpacing(Port port, int spacing) {
		if (port == null) {
			throw new IllegalArgumentException("port must not be null!");
		}
		GUIProcessXMLFilter.setPortSpacing(port, spacing);
	}

	/**
	 * Resets the spacing of the specified {@link Port} to the default value.
	 *
	 * @param port
	 *            the port in question
	 */
	public void resetPortSpacing(Port port) {
		if (port == null) {
			throw new IllegalArgumentException("port must not be null!");
		}
		GUIProcessXMLFilter.resetPortSpacing(port);
	}

	/**
	 * Returns the {@link Point} the mouse is at relative to the process it currently is over.
	 *
	 * @return the location or {@code null}
	 */
	public Point getMousePositionRelativeToProcess() {
		return mousePositionRelativeToProcess;
	}

	/**
	 * Sets the {@link Point} the mouse is at relative to the process it currently is over.
	 *
	 * @param mousePositionRelativeToProcess
	 *            the point or {@code null}
	 */
	public void setMousePositionRelativeToProcess(Point mousePositionRelativeToProcess) {
		this.mousePositionRelativeToProcess = mousePositionRelativeToProcess;
	}

	/**
	 * Adds a {@link ProcessRendererEventListener} which will be informed of all changes to this
	 * model.
	 *
	 * @param listener
	 *            the listener instance to add
	 */
	public void registerEventListener(final ProcessRendererEventListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}
		eventListener.add(ProcessRendererEventListener.class, listener);
	}

	/**
	 * Removes the {@link ProcessRendererEventListener} from this model.
	 *
	 * @param listener
	 *            the listener instance to remove
	 */
	public void removeEventListener(final ProcessRendererEventListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}
		eventListener.remove(ProcessRendererEventListener.class, listener);
	}

	/**
	 * Fire when the displayed operator chain has changed.
	 */
	public void fireDisplayedChainChanged() {
		fireModelChanged(ModelEvent.DISPLAYED_CHAIN_CHANGED);
	}

	/**
	 * Fire when the displayed processes have changed.
	 */
	public void fireProcessesChanged() {
		fireModelChanged(ModelEvent.DISPLAYED_PROCESSES_CHANGED);
	}

	/**
	 * Fire when a process size has changed.
	 */
	public void fireProcessSizeChanged() {
		fireModelChanged(ModelEvent.PROCESS_SIZE_CHANGED);
	}

	/**
	 * Fire when the something minor has changed which only requires a repaint.
	 */
	public void fireMiscChanged() {
		fireModelChanged(ModelEvent.MISC_CHANGED);
	}

	/**
	 * Fire when an operator has been moved.
	 *
	 * @param operator
	 *            the moved operator
	 */
	public void fireOperatorMoved(Operator operator) {
		List<Operator> list = new LinkedList<>();
		list.add(operator);
		fireOperatorsMoved(list);
	}

	/**
	 * Fire when operators have been moved.
	 *
	 * @param operators
	 *            a collection of moved operators
	 */
	public void fireOperatorsMoved(Collection<Operator> operators) {
		fireOperatorsChanged(OperatorEvent.OPERATORS_MOVED, operators);
	}

	/**
	 * Fire when the operator selection has changed.
	 *
	 * @param operators
	 *            a collection of selected operators
	 */
	public void fireOperatorSelectionChanged(Collection<Operator> operators) {
		fireOperatorsChanged(OperatorEvent.SELECTED_OPERATORS_CHANGED, operators);
	}

	/**
	 * Fire when the number of ports for operators has changed.
	 *
	 * @param operators
	 *            a collection of operators which had their ports changed
	 */
	public void firePortsChanged(Collection<Operator> operators) {
		fireOperatorsChanged(OperatorEvent.PORTS_CHANGED, operators);
	}

	/**
	 * Fire when an annotation has been moved.
	 *
	 * @param anno
	 *            the moved annotation
	 */
	public void fireAnnotationMoved(WorkflowAnnotation anno) {
		List<WorkflowAnnotation> list = new LinkedList<>();
		list.add(anno);
		fireAnnotationsMoved(list);
	}

	/**
	 * Fire when annotations have been moved.
	 *
	 * @param annotations
	 *            the moved annotations
	 */
	public void fireAnnotationsMoved(Collection<WorkflowAnnotation> annotations) {
		fireAnnotationsChanged(AnnotationEvent.ANNOTATIONS_MOVED, annotations);
	}

	/**
	 * Fire when an annotation has been selected.
	 *
	 * @param anno
	 *            the selected annotation
	 */
	public void fireAnnotationSelected(WorkflowAnnotation anno) {
		List<WorkflowAnnotation> list = new LinkedList<>();
		list.add(anno);
		fireAnnotationsChanged(AnnotationEvent.SELECTED_ANNOTATION_CHANGED, list);
	}

	/**
	 * Fire when the something minor with workflow annotations has changed which only requires a
	 * repaint.
	 *
	 * @param anno
	 *            the changed annotation, can be {@code null}
	 */
	public void fireAnnotationMiscChanged(WorkflowAnnotation anno) {
		List<WorkflowAnnotation> list = new LinkedList<>();
		list.add(anno);
		fireAnnotationsChanged(AnnotationEvent.MISC_CHANGED, list);
	}

	/**
	 * Fires the given {@link ModelEvent}.
	 *
	 * @param type
	 *            the event type
	 */
	private void fireModelChanged(final ModelEvent type) {
		Object[] listeners = eventListener.getListenerList();
		// Process the listeners last to first
		for (int i = 0; i < listeners.length - 1; i += 2) {
			if (listeners[i] == ProcessRendererEventListener.class) {
				ProcessRendererModelEvent e = new ProcessRendererModelEvent(type);
				((ProcessRendererEventListener) listeners[i + 1]).modelChanged(e);
			}
		}
	}

	/**
	 * Fires the given {@link OperatorEvent} with the affected {@link Operator}s.
	 *
	 * @param type
	 *            the event type
	 * @param operators
	 *            the affected operators
	 */
	private void fireOperatorsChanged(final OperatorEvent type, Collection<Operator> operators) {
		Object[] listeners = eventListener.getListenerList();
		// Process the listeners last to first
		for (int i = 0; i < listeners.length - 1; i += 2) {
			if (listeners[i] == ProcessRendererEventListener.class) {
				ProcessRendererOperatorEvent e = new ProcessRendererOperatorEvent(type);
				((ProcessRendererEventListener) listeners[i + 1]).operatorsChanged(e, operators);
			}
		}
	}

	/**
	 * Fires the given {@link AnnotationEvent} with the affected {@link WorkflowAnnotation}s.
	 *
	 * @param type
	 *            the event type
	 * @param annotations
	 *            the affected annotations
	 */
	private void fireAnnotationsChanged(final AnnotationEvent type, Collection<WorkflowAnnotation> annotations) {
		Object[] listeners = eventListener.getListenerList();
		// Process the listeners last to first
		for (int i = 0; i < listeners.length - 1; i += 2) {
			if (listeners[i] == ProcessRendererEventListener.class) {
				ProcessRendererAnnotationEvent e = new ProcessRendererAnnotationEvent(type);
				((ProcessRendererEventListener) listeners[i + 1]).annotationsChanged(e, annotations);
			}
		}
	}
}
