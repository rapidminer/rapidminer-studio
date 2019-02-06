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

import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.ProcessStorageListener;
import com.rapidminer.RapidMiner;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.NewProcessUndoManager;
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
import com.rapidminer.gui.processeditor.ExtendedProcessEditor;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.io.process.AnnotationProcessXMLFilter;
import com.rapidminer.io.process.ProcessOriginProcessXMLFilter;
import com.rapidminer.io.process.BackgroundImageProcessXMLFilter;
import com.rapidminer.io.process.GUIProcessXMLFilter;
import com.rapidminer.io.process.ProcessLayoutXMLFilter;
import com.rapidminer.io.process.ProcessXMLFilterRegistry;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.FlagUserData;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.UserData;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.parameter.ParameterChangeListener;
import com.rapidminer.tutorial.Tutorial;


/**
 * The model backing the process renderer view. It contains all the data necessary to draw the
 * current process. The minimal configuration which is required to draw a process via the
 * {@link ProcessDrawer} can be achieved by calling the following setters:
 * <ul>
 * <li>{@link #setDisplayedChain(OperatorChain)}</li>
 * <li>{@link #setProcesses(List)}</li>
 * <li>{@link #setProcessSize(ExecutionUnit, Dimension)} (see
 * {@link ProcessDrawUtils#calculatePreferredSize(ProcessRendererModel, ExecutionUnit, int, int)} to
 * automatically create the correct dimensions for each {@link ExecutionUnit} of a process)</li>
 * </ul>
 * <p>
 * Note that the model itself does not fire any events. To trigger events, call any of the fireXYZ
 * methods. This is done for performance reasons and to support batch updates and only trigger
 * events when really needed.
 * </p>
 * <p>
 * The model should be disposed of if it is no longer needed using the {@link #dispose()} method.
 * Take note that it can not be reliably used after that call.
 * </p>
 *
 * @author Marco Boeck, Jan Czogalla
 * @since 6.4.0
 *
 */
public final class ProcessRendererModel {

	/** available process zoom factors */
	private static final double[] ZOOM_FACTORS = new double[] { 0.25, 0.33, 0.5, 0.67, 0.75, 0.9, 1.0, 1.1, 1.25, 1.5, 1.75,
			2.0 };

	/** the starting zoom index equaling no zoom */
	private static final int ORIGINAL_ZOOM_INDEX = 6;

	/** the font for the operator name */
	public static final Font OPERATOR_FONT = FontTools.getFont(Font.DIALOG, Font.BOLD, 11);

	/** the height of the operator name header */
	public static final int HEADER_HEIGHT = OPERATOR_FONT.getSize() + 7;

	/** the width of each operator */
	public static final int OPERATOR_WIDTH = 5 * 16 + 2 * 5;  // 5 mini icons + padding

	/** the minimum height of an operator */
	public static final int MIN_OPERATOR_HEIGHT = 50 + HEADER_HEIGHT;

	/** the size of each operator/process port */
	public static final int PORT_SIZE = 14;

	/** The default maximum number of available undo steps */
	private static final int DEFAULT_UNDO_LIST_SIZE = 20;

	/** event listener for this model */
	private final EventListenerList eventListener;

	/**
	 * list of {@link ProcessEditor ProcessEditors}; can also contain {@link ExtendedProcessEditor}
	 * instances
	 */
	private final EventListenerList processEditors = new EventListenerList();

	/** list of {@link ProcessStorageListener ProcessStorageListeners} */
	private final LinkedList<ProcessStorageListener> storageListeners = new LinkedList<>();

	/** underlying process for this model */
	private Process process;

	/** {@link NewProcessUndoManager} associated with the underlying process */
	private NewProcessUndoManager undoManager = new NewProcessUndoManager();

	/** the current position in the undo stack */
	private int undoIndex = 0;

	/** indicator whether the process has changed */
	private boolean hasChanged = false;

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

	private ParameterChangeListener paramListener;

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

	/** the current zoom index */
	private int zoomIndex = ORIGINAL_ZOOM_INDEX;

	/**
	 * if canImport of transfer handler has returned <code>true</code>. Will be set to false if
	 * mouse has exited the process renderer
	 */
	private boolean importDragged;

	/**
	 * the current mouse position relative to the process the mouse is over
	 */
	private Point mousePositionRelativeToProcess;

	// initialize the filter responsible for reading/writing operator coordinates from/to XML
	static {
		if (!RapidMiner.getExecutionMode().isHeadless()) {
			ProcessXMLFilterRegistry.registerFilter(new GUIProcessXMLFilter());
			ProcessXMLFilterRegistry.registerFilter(new ProcessOriginProcessXMLFilter());
		}
	}

	public ProcessRendererModel() {
		this.eventListener = new EventListenerList();

		this.processes = Collections.emptyList();
		this.selectedOperators = Collections.emptyList();
		this.draggedOperators = Collections.emptyList();
		this.processSizes = new WeakHashMap<>();
		this.portNumbers = new WeakHashMap<>();
		this.snapToGrid = Boolean
				.parseBoolean(ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_SNAP_TO_GRID));
		this.hoveringProcessIndex = -1;

		// listen for snapToGrid changes
		paramListener = new ParameterChangeListener() {

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
		};
		ParameterService.registerParameterChangeListener(paramListener);

		// listen for selection changes in the ProcessRendererView and notify all registered process
		// editors
		registerEventListener(new ProcessRendererEventListener() {

			@Override
			public void modelChanged(ProcessRendererModelEvent e) {
				// ignore
			}

			@Override
			public void operatorsChanged(ProcessRendererOperatorEvent e, Collection<Operator> operators) {
				if (e.getEventType() == OperatorEvent.SELECTED_OPERATORS_CHANGED) {
					for (ProcessEditor editor : processEditors.getListeners(ProcessEditor.class)) {
						editor.setSelection(new LinkedList<>(operators));
					}
					for (ExtendedProcessEditor editor : processEditors.getListeners(ExtendedProcessEditor.class)) {
						editor.setSelection(new LinkedList<>(operators));
					}
				}
			}

			@Override
			public void annotationsChanged(ProcessRendererAnnotationEvent e, Collection<WorkflowAnnotation> annotations) {
				// ignore
			}

		});

		// listen for process change and set the GUI flag
		addProcessEditor(new ProcessEditor() {

			@Override
			public void setSelection(List<Operator> selection) {
				// don't care

			}

			@Override
			public void processUpdated(Process process) {
				// don't care

			}

			@Override
			public void processChanged(Process process) {
				if (process != null && !RapidMiner.getExecutionMode().isHeadless()) {
					process.getRootOperator().setUserData(RapidMinerGUI.IS_GUI_PROCESS, new FlagUserData());
				}
			}
		});
	}

	/**
	 * Returns the underlying {@link Process} or {@code null} if none is set.
	 *
	 * @since 7.5
	 */
	public Process getProcess() {
		return process;
	}

	/**
	 * Sets the underlying process and informs listeners that the displayed chain and the process
	 * have changed. If the given process should be handled as a new one, the undo list will be
	 * reset. Will also inform listeners that the process was loaded if so indicated.
	 *
	 * @param process
	 * 		the process to be set
	 * @param isNew
	 * 		indicates if the process should be handled as a new process
	 * @param open
	 * 		whether the process was newly opened e.g. from a file
	 * @since 7.5
	 */
	public void setProcess(Process process, boolean isNew, boolean open) {
		this.process = process;
		if (isNew) {
			// prevent addToUndoList from process editor
			undoManager.clearSnapshot();
		}
		fireProcessChanged();

		displayedChain = process.getRootOperator();
		fireDisplayedChainChanged();

		this.selectedOperators = Collections.singletonList(displayedChain);
		fireOperatorSelectionChanged(getSelectedOperators());

		if (isNew) {
			// reset undo manager because this is a new process
			resetUndo();
		}
		if (open) {
			fireProcessLoaded();
		}
	}

	/**
	 * Informs listeners that the process was saved.
	 *
	 * @since 7.5
	 */
	public void processHasBeenSaved() {
		hasChanged = false;
		fireProcessStored();
	}

	/**
	 * Returns whether the process has been altered since the last save.
	 *
	 * @since 7.5
	 */
	public boolean hasChanged() {
		return hasChanged;
	}

	/**
	 * Restores the previous state of the process if there are previous steps. Returns an Exception
	 * if a problem occurred, {@code null} otherwise.
	 *
	 * @return an exception if a problem occurred
	 * @see #setToStep(int)
	 * @since 7.5
	 */
	public Exception undo() {
		if (!hasUndoSteps()) {
			return null;
		}
		if (!hasRedoSteps()) {
			// add redo step from current state
			takeSnapshot();
			undoManager.add(true);
		}
		undoIndex--;
		return setToStep(undoIndex);
	}

	/**
	 * Returns whether there are undo steps available.
	 *
	 * @since 7.5
	 */
	public boolean hasUndoSteps() {
		return undoIndex > 0;
	}

	/**
	 * Restores the next state of the process if there are next steps. Returns an Exception if a
	 * problem occurred, {@code null} otherwise.
	 *
	 * @return an exception if a problem occurred
	 * @see #setToStep(int)
	 * @since 7.5
	 */
	public Exception redo() {
		if (!hasRedoSteps()) {
			return null;
		}
		undoIndex++;
		Exception result = setToStep(undoIndex);
		if (!hasRedoSteps()) {
			// remove last step since we are at the end of stack
			// undo will create a redo step that represents the state at the time of undo
			undoManager.removeLast();
		}
		return result;
	}

	/**
	 * Returns whether there are redo steps available.
	 *
	 * @since 7.5
	 */
	public boolean hasRedoSteps() {
		return undoIndex < undoManager.getNumberOfUndos() - 1;
	}

	/**
	 * Checks whether there was a change in the process and if so, adds a new undo step. Will return
	 * true if a new undo step was created.
	 *
	 * @since 7.5
	 */
	public boolean checkForNewUndoStep() {
		takeSnapshot();
		if (undoManager.snapshotDiffers()) {
			addToUndoList(false);
			return true;
		}
		return false;
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
	 * 		the index of the process to return. If index is invalid, throws
	 * @return the currently displayed process at the specified index
	 * @throws IndexOutOfBoundsException
	 * 		if index < 0 or index >= length
	 */
	public ExecutionUnit getProcess(int index) throws ArrayIndexOutOfBoundsException {
		return getProcesses().get(index);
	}

	/**
	 * Returns the index of the given process.
	 *
	 * @param process
	 * 		the process for which the index should be retrieved
	 * @return the index of the process starting with {@code 0} or {@code -1} if the process is not part of {@link #getProcesses()}
	 */
	public int getProcessIndex(ExecutionUnit process) {
		return getProcesses().indexOf(process);
	}

	/**
	 * Sets the currently displayed processes.
	 *
	 * @param processes
	 * 		the new processes to display
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
	 * 		the new operator chain to display
	 */
	public void setDisplayedChain(OperatorChain displayedChain) {
		if (displayedChain == null) {
			throw new IllegalArgumentException("displayedChain must not be null!");
		}
		addViewSwitchToUndo();
		this.displayedChain = displayedChain;
		fireProcessViewChanged();
	}

	/**
	 * Sets the displayed operator chain to the specified one and triggers updates before and after
	 * the change. Will do nothing if the operator chain is already displayed. Convenience method.
	 *
	 * @param displayedChain
	 * 		the new chain to display
	 * @since 7.5
	 */
	public void setDisplayedChainAndFire(OperatorChain displayedChain) {
		if (getDisplayedChain() == displayedChain) {
			return;
		}
		fireDisplayedChainWillChange();
		setDisplayedChain(displayedChain);
		fireDisplayedChainChanged();
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
		this.selectedOperators = Collections.emptyList();
	}

	/**
	 * Adds the given operator to the currently selected operators.
	 *
	 * @param selectedOperator
	 * 		this operator is added to the list of currently selected operators
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
	 * 		this operator is removed from the list of currently selected operators
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
	 * 		these operators are added to the list of currently selected operators
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
	 * 		these operators are set as the currently dragged operators
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
		this.draggedOperators = Collections.emptyList();
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
	 * 		whether operators should snap to a grid or not
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
	 * 		{@code true} if dragging is in progress; {@code false} otherwise
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
	 * 		{@code true} if a valid drop target was set; {@code false} otherwise
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
	 * 		{@code true} if a an operator source (tree, WoC, ...) is hovered; {@code false}
	 * 		otherwise
	 */
	public void setOperatorSourceHovered(boolean operatorSourceHovered) {
		this.operatorSourceHovered = operatorSourceHovered;
	}

	/**
	 * Sets the current mouse position over the process renderer. Can be {@code null}.
	 *
	 * @param currentMousePosition
	 * 		the position or {@code null} if it is not over the renderer
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
	 * 		{@code true} if the import would be accepted; {@code false} otherwise
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
	 * 		the connection source port or {@code null}
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
	 * 		the source port of the connection currently being created or {@code null}
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
	 * 		the hovered process index
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
	 * 		the operator under the mouse or {@code null}
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
	 * Checks if the mouse cursor is hovering over the header (i.e. name) portion of the hovered operator.
	 * Will also return {@code false} if no operator is currently hovered.
	 *
	 * @return whether the cursor is hovering over the operator name
	 * @since 9.0.0
	 */
	public boolean isHoveringOperatorName() {
		if (hoveringOperator == null) {
			return false;
		}
		Rectangle2D operatorRect = getOperatorRect(hoveringOperator);
		if (operatorRect == null) {
			// should not happen
			return false;
		}
		Point mousePosition = getMousePositionRelativeToProcess();
		if (mousePosition == null) {
			// should not happen because then the operator would not be hovered
			return false;
		}
		return mousePosition.y - operatorRect.getY() < HEADER_HEIGHT - 1;
	}

	/**
	 * Sets the {@link OutputPort} of the connection over which the mouse hovers.
	 *
	 * @param hoveringConnectionSource
	 * 		the output port of the connection under the mouse or {@code null}
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
	 * 		the port under the mouse or {@code null}
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
	 * 		the selection rectangle or {@code null}
	 */
	public void setSelectionRectangle(Rectangle2D selectionRectangle) {
		this.selectionRectangle = selectionRectangle;
	}

	/**
	 * Returns the size of the given process.
	 *
	 * @param process
	 * 		the size of this process is returned
	 * @return the size of the specified process or {@code null}
	 */
	public Dimension getProcessSize(ExecutionUnit process) {
		Dimension dim = processSizes.get(process);
		if (dim == null) {
			return null;
		}

		// copy dim to not allow altering of dim in map
		dim = new Dimension(dim);
		if (getZoomFactor() > 1.0) {
			dim.width *= getZoomFactor();
			dim.height *= getZoomFactor();
		}
		return dim;
	}

	/**
	 * Returns the width of the given process. Convenience method which simply returns the width of
	 * {@link #getProcessSize(ExecutionUnit)}.
	 *
	 * @param process
	 * 		the process for which the width should be returned
	 * @return the width or -1 if no process size has been stored
	 */
	public double getProcessWidth(ExecutionUnit process) {
		Dimension dim = processSizes.get(process);
		if (dim == null) {
			return -1;
		}
		if (getZoomFactor() > 1.0) {
			return dim.getWidth() * getZoomFactor();
		}
		return dim.getWidth();
	}

	/**
	 * Returns the zoom factor of the process where {@code 1.0} means no zoom, values smaller equal
	 * zooming out and values greater than {@code 1.0} equal zooming in.
	 *
	 * @return the zoom factor
	 */
	public double getZoomFactor() {
		return ZOOM_FACTORS[zoomIndex];
	}

	/**
	 * Sets the zoom factor. If not a valid zoom factor or identical to the current factor, does
	 * nothing.
	 *
	 * @param zoomFactor
	 * 		factor in {@link #ZOOM_FACTORS}
	 */
	public void setZoomFactor(double zoomFactor) {
		if (getZoomFactor() == zoomFactor) {
			return;
		}

		int index = 0;
		for (double d : ZOOM_FACTORS) {
			if (d == zoomFactor) {
				zoomIndex = index;
				break;
			}
			index++;
		}
	}

	/**
	 * @return {@code true} if it is still possible to zoom in
	 */
	public boolean canZoomIn() {
		return zoomIndex < ZOOM_FACTORS.length - 1;
	}

	/**
	 * @return {@code true} if it is still possible to zoom out
	 */
	public boolean canZoomOut() {
		return zoomIndex > 0;
	}

	/**
	 * @return {@code true} if it is possible to reset the zoom (aka the process is currently zoomed in/out)
	 */
	public boolean canZoomReset() {
		return zoomIndex != ORIGINAL_ZOOM_INDEX;
	}

	/**
	 * Tries to zoom into the process. If the largest zoom factor has already been reached, does
	 * nothing.
	 */
	public void zoomIn() {
		if (canZoomIn()) {
			this.zoomIndex += 1;
		}
	}

	/**
	 * Tries to zoom out of the process. If the smallest zoom factor has already been reached, does
	 * nothing.
	 */
	public void zoomOut() {
		if (canZoomOut()) {
			this.zoomIndex -= 1;
		}
	}

	public void resetZoom() {
		if (canZoomReset()) {
			this.zoomIndex = ORIGINAL_ZOOM_INDEX;
		}
	}

	/**
	 * Sets the width for the given process. If {@link #getProcessSize(ExecutionUnit)} returns
	 * {@code null} for the specified process, does nothing.
	 *
	 * @param process
	 * 		the process for which the height should be set
	 * @param width
	 * 		the new width
	 */
	public void setProcessWidth(ExecutionUnit process, double width) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}
		Dimension dim = processSizes.get(process);
		if (dim == null) {
			return;
		}
		// execution unit dimensions should not use sub-pixels
		dim.setSize(Math.round(width), dim.getHeight());
	}

	/**
	 * Returns the height of the given process. Convenience method which simply returns the height
	 * of {@link #getProcessSize(ExecutionUnit)}.
	 *
	 * @param process
	 * 		the process for which the height should be returned
	 * @return the height or -1 if no process size has been stored
	 */
	public double getProcessHeight(ExecutionUnit process) {
		Dimension dim = processSizes.get(process);
		if (dim == null) {
			return -1;
		}
		if (getZoomFactor() > 1.0) {
			return dim.getHeight() * getZoomFactor();
		}
		return dim.getHeight();
	}

	/**
	 * Sets the height for the given process. If {@link #getProcessSize(ExecutionUnit)} returns
	 * {@code null} for the specified process, does nothing.
	 *
	 * @param process
	 * 		the process for which the height should be set
	 * @param height
	 * 		the new height
	 */
	public void setProcessHeight(ExecutionUnit process, double height) {
		if (process == null) {
			throw new IllegalArgumentException("process must not be null!");
		}
		Dimension dim = processSizes.get(process);
		if (dim == null) {
			return;
		}
		// execution unit dimensions should not use subpixels
		dim.setSize(dim.getWidth(), Math.round(height));
	}

	/**
	 * Sets the size of the given process.
	 *
	 * @param process
	 * 		the size of this process is stored
	 * @param size
	 * 		the size of the specified process
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
	 * 		the operator in question
	 * @return the rectangle. Can return {@code null} but only if the operator has not been added to
	 * the {@link com.rapidminer.gui.flow.processrendering.view.ProcessRendererView
	 * ProcessRendererView}.
	 */
	public Rectangle2D getOperatorRect(Operator op) {
		return ProcessLayoutXMLFilter.lookupOperatorRectangle(op);
	}

	/**
	 * Returns the {@link WorkflowAnnotations} container for the given {@link Operator}.
	 *
	 * @param op
	 * 		the operator in question
	 * @return the container. Can be {@code null} if no annotations exist for this operator
	 */
	public WorkflowAnnotations getOperatorAnnotations(Operator op) {
		return AnnotationProcessXMLFilter.lookupOperatorAnnotations(op);
	}

	/**
	 * Removes the given {@link OperatorAnnotation}.
	 *
	 * @param anno
	 * 		the annotation to remove
	 */
	public void removeOperatorAnnotation(OperatorAnnotation anno) {
		AnnotationProcessXMLFilter.removeOperatorAnnotation(anno);
	}

	/**
	 * Adds the given {@link OperatorAnnotation}.
	 *
	 * @param anno
	 * 		the annotation to add
	 */
	public void addOperatorAnnotation(OperatorAnnotation anno) {
		AnnotationProcessXMLFilter.addOperatorAnnotation(anno);
	}

	/**
	 * Returns the {@link WorkflowAnnotations} container for the given {@link ExecutionUnit}.
	 *
	 * @param process
	 * 		the process in question
	 * @return the container. Can be {@code null} if no annotations exist for this process
	 */
	public WorkflowAnnotations getProcessAnnotations(ExecutionUnit process) {
		return AnnotationProcessXMLFilter.lookupProcessAnnotations(process);
	}

	/**
	 * Removes the given {@link ProcessAnnotation}.
	 *
	 * @param anno
	 * 		the annotation to remove
	 */
	public void removeProcessAnnotation(ProcessAnnotation anno) {
		AnnotationProcessXMLFilter.removeProcessAnnotation(anno);
	}

	/**
	 * Adds the given {@link ProcessAnnotation}.
	 *
	 * @param anno
	 * 		the annotation to add
	 */
	public void addProcessAnnotation(ProcessAnnotation anno) {
		AnnotationProcessXMLFilter.addProcessAnnotation(anno);
	}

	/**
	 * Returns the {@link ProcessBackgroundImage} for the given {@link ExecutionUnit}.
	 *
	 * @param process
	 * 		the process in question
	 * @return the background image. Can be {@code null} if none is set for this process
	 */
	public ProcessBackgroundImage getBackgroundImage(ExecutionUnit process) {
		return BackgroundImageProcessXMLFilter.lookupBackgroundImage(process);
	}

	/**
	 * Removes the given {@link ProcessBackgroundImage}.
	 *
	 * @param process
	 * 		the process for which to remove the background image
	 */
	public void removeBackgroundImage(ExecutionUnit process) {
		BackgroundImageProcessXMLFilter.removeBackgroundImage(process);
	}

	/**
	 * Sets the given {@link ProcessBackgroundImage}.
	 *
	 * @param image
	 * 		the image to add
	 */
	public void setBackgroundImage(ProcessBackgroundImage image) {
		BackgroundImageProcessXMLFilter.setBackgroundImage(image);
	}

	/**
	 * Returns the number of ports for the given {@link Operator}.
	 *
	 * @param op
	 * 		the operator in question
	 * @return the number of ports or {@code null} if they have not yet been stored
	 */
	public Integer getNumberOfPorts(Operator op) {
		return portNumbers.get(op);
	}

	/**
	 * Sets the number of ports for the given {@link Operator}.
	 *
	 * @param op
	 * 		the operator in question
	 * @param number
	 * 		the number of ports or {@code null}
	 */
	public Integer setNumberOfPorts(Operator op, Integer number) {
		return portNumbers.put(op, number);
	}

	/**
	 * Sets the {@link Operator} {@link Rectangle2D}. Calculates and sets the height of the operator
	 * to match the existing ports!
	 *
	 * @param op
	 * 		the operator for which the rectangle should be set
	 * @param rect
	 * 		the rectangle representing position and size of operator
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

		ProcessLayoutXMLFilter.setOperatorRectangle(op, rect);
	}

	/**
	 * Returns the spacing of the specified {@link Port}.
	 *
	 * @param port
	 * 		the port in question
	 * @return the additional spacing before this port
	 */
	public int getPortSpacing(Port port) {
		return ProcessLayoutXMLFilter.lookupPortSpacing(port);
	}

	/**
	 * Sets the spacing of the specified {@link Port}.
	 *
	 * @param port
	 * 		the port in question
	 * @param spacing
	 * 		the additional spacing before the port
	 */
	public void setPortSpacing(Port port, int spacing) {
		if (port == null) {
			throw new IllegalArgumentException("port must not be null!");
		}
		ProcessLayoutXMLFilter.setPortSpacing(port, spacing);
	}

	/**
	 * Resets the spacing of the specified {@link Port} to the default value.
	 *
	 * @param port
	 * 		the port in question
	 */
	public void resetPortSpacing(Port port) {
		if (port == null) {
			throw new IllegalArgumentException("port must not be null!");
		}
		ProcessLayoutXMLFilter.resetPortSpacing(port);
	}

	/**
	 * Looks up the view position of the specified {@link OperatorChain}.
	 *
	 * @param chain
	 * 		The operator chain.
	 * @return The position or null.
	 * @since 7.5
	 */
	public Point getOperatorChainPosition(OperatorChain chain) {
		return ProcessLayoutXMLFilter.lookupOperatorChainPosition(chain);
	}

	/**
	 * Sets the view position of the specified {@link OperatorChain}.
	 *
	 * @param chain
	 * 		The operator chain.
	 * @param position
	 * 		The center position.
	 * @since 7.5
	 */
	public void setOperatorChainPosition(OperatorChain chain, Point position) {
		if (chain == null) {
			throw new IllegalArgumentException("operator chain must not be null!");
		}
		ProcessLayoutXMLFilter.setOperatorChainPosition(chain, position);
	}

	/**
	 * Resets the view position of the specified {@link OperatorChain}.
	 *
	 * @param chain
	 * 		The operator chain.
	 * @since 7.5
	 */
	public void resetOperatorChainPosition(OperatorChain chain) {
		if (chain == null) {
			throw new IllegalArgumentException("operator chain must not be null!");
		}
		ProcessLayoutXMLFilter.resetOperatorChainPosition(chain);
	}

	/**
	 * Looks up the zoom of the specified {@link OperatorChain}.
	 *
	 * @param chain
	 * 		The operator chain.
	 * @return The position or null.
	 * @since 7.5
	 */
	public Double getOperatorChainZoom(OperatorChain chain) {
		return ProcessLayoutXMLFilter.lookupOperatorChainZoom(chain);
	}

	/**
	 * Sets the zoom of the specified {@link OperatorChain}.
	 *
	 * @param chain
	 * 		The operator chain.
	 * @param zoom
	 * 		The zoom.
	 * @since 7.5
	 */
	public void setOperatorChainZoom(OperatorChain chain, Double zoom) {
		if (chain == null) {
			throw new IllegalArgumentException("operator chain must not be null!");
		}
		ProcessLayoutXMLFilter.setOperatorChainZoom(chain, zoom);
	}

	/**
	 * Resets the zoom of the specified {@link OperatorChain}.
	 *
	 * @param chain
	 * 		The operator chain.
	 * @since 7.5
	 */
	public void resetOperatorChainZoom(OperatorChain chain) {
		if (chain == null) {
			throw new IllegalArgumentException("operator chain must not be null!");
		}
		ProcessLayoutXMLFilter.resetOperatorChainZoom(chain);
	}

	/**
	 * Looks up the scroll position of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain.
	 * @return The scroll position or null
	 * @since 7.5
	 */
	public Point getScrollPosition(OperatorChain operatorChain) {
		return ProcessLayoutXMLFilter.lookupScrollPosition(operatorChain);
	}

	/**
	 * Sets the scroll position of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator.
	 * @param scrollPos
	 * 		The scroll position.
	 * @since 7.5
	 */
	public void setScrollPosition(OperatorChain operatorChain, Point scrollPos) {
		ProcessLayoutXMLFilter.setScrollPosition(operatorChain, scrollPos);
	}

	/**
	 * Resets the scroll position of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain.
	 * @since 7.5
	 */
	public void resetScrollPosition(OperatorChain operatorChain) {
		ProcessLayoutXMLFilter.resetScrollPosition(operatorChain);
	}

	/**
	 * Looks up the scroll process index of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain.
	 * @return The index or null
	 * @since 7.5
	 */
	public Double getScrollIndex(OperatorChain operatorChain) {
		return ProcessLayoutXMLFilter.lookupScrollIndex(operatorChain);
	}

	/**
	 * Sets the scroll process index of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator.
	 * @param index
	 * 		The process index.
	 * @since 7.5
	 */
	public void setScrollIndex(OperatorChain operatorChain, Double index) {
		ProcessLayoutXMLFilter.setScrollIndex(operatorChain, index);
	}

	/**
	 * Resets the scroll process index of the specified {@link OperatorChain}.
	 *
	 * @param operatorChain
	 * 		The operator chain.
	 * @since 7.5
	 */
	public void resetScrollIndex(OperatorChain operatorChain) {
		ProcessLayoutXMLFilter.resetScrollIndex(operatorChain);
	}

	/**
	 * Looks up the restore flag of the specified {@link Operator}. Indicates if this operator was
	 * restored via undo/redo and should not be scrolled to.
	 *
	 * @param operator
	 * 		the operator
	 * @return if the flag was set
	 * @since 7.5
	 */
	public boolean getRestore(Operator operator) {
		return ProcessLayoutXMLFilter.lookupRestore(operator);
	}

	/**
	 * Sets the restore flag of the specified {@link Operator}. Indicates that this operator was
	 * restored via undo/redo and should not be scrolled to.
	 *
	 * @param operator
	 * 		the operator
	 * @since 7.5
	 */
	public void setRestore(Operator operator) {
		ProcessLayoutXMLFilter.setRestore(operator);
	}

	/**
	 * Resets the restore flag of the specified {@link OperatorChain}. Indicates that this operator
	 * should be scrolled to when selected.
	 *
	 * @param operator
	 * 		The operator
	 * @since 7.5
	 */
	public void resetRestore(Operator operator) {
		ProcessLayoutXMLFilter.resetRestore(operator);
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
	 * 		the point or {@code null}
	 */
	public void setMousePositionRelativeToProcess(Point mousePositionRelativeToProcess) {
		this.mousePositionRelativeToProcess = mousePositionRelativeToProcess;
	}

	/**
	 * Adds a {@link ProcessRendererEventListener} which will be informed of all changes to this
	 * model.
	 *
	 * @param listener
	 * 		the listener instance to add
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
	 * 		the listener instance to remove
	 */
	public void removeEventListener(final ProcessRendererEventListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be null!");
		}
		eventListener.remove(ProcessRendererEventListener.class, listener);
	}

	/**
	 * Adds the given {@link ProcessEditor} listener. Automatically detects
	 * {@link ExtendedProcessEditor}.
	 */
	public void addProcessEditor(final ProcessEditor p) {
		if (p instanceof ExtendedProcessEditor) {
			processEditors.add(ExtendedProcessEditor.class, (ExtendedProcessEditor) p);
		} else {
			processEditors.add(ProcessEditor.class, p);
		}
	}

	/**
	 * Removes the given {@link ProcessEditor} listener. Automatically detects
	 * {@link ExtendedProcessEditor}.
	 */
	public void removeProcessEditor(final ProcessEditor p) {
		if (p instanceof ExtendedProcessEditor) {
			processEditors.remove(ExtendedProcessEditor.class, (ExtendedProcessEditor) p);
		} else {
			processEditors.remove(ProcessEditor.class, p);
		}
	}

	/**
	 * Adds the given {@link ProcessStorageListener}.
	 *
	 * @since 7.5
	 */
	public void addProcessStorageListener(final ProcessStorageListener listener) {
		synchronized (storageListeners) {
			storageListeners.add(listener);
		}
	}

	/**
	 * Removes the given {@link ProcessStorageListener}.
	 *
	 * @since 7.5
	 */
	public void removeProcessStorageListener(final ProcessStorageListener listener) {
		synchronized (storageListeners) {
			storageListeners.remove(listener);
		}
	}

	/**
	 * Informs the {@link ProcessEditor ProcessEditors} that the process was updated. Usually fired
	 * on validation or when the undo stack was altered or accessed.
	 *
	 * @since 7.5
	 */
	public void fireProcessUpdated() {
		Process process = getProcess();
		for (ProcessEditor editor : processEditors.getListeners(ProcessEditor.class)) {
			editor.processUpdated(process);
		}
		for (ExtendedProcessEditor editor : processEditors.getListeners(ExtendedProcessEditor.class)) {
			editor.processUpdated(process);
		}
	}

	/**
	 * Fire before displayed operator chain will changed.
	 *
	 * @since 7.5
	 */
	public void fireDisplayedChainWillChange() {
		fireModelChanged(ModelEvent.DISPLAYED_CHAIN_WILL_CHANGE);
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
	 * Called before the process zoom level will change. The given point and index indicate which
	 * process position should be the (new) center position.
	 *
	 * @param center
	 * 		the (new) center point
	 * @param index
	 * 		the (new) process index
	 * @since 7.5
	 */
	public void prepareProcessZoomWillChange(Point center, int index) {
		setScrollIndex(displayedChain, (double) index);
		setScrollPosition(displayedChain, center);
	}

	/**
	 * Fire when the process zoom level has changed.
	 */
	public void fireProcessZoomChanged() {
		fireModelChanged(ModelEvent.PROCESS_ZOOM_CHANGED);
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
	 * 		the moved operator
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
	 * 		a collection of moved operators
	 */
	public void fireOperatorsMoved(Collection<Operator> operators) {
		fireOperatorsChanged(OperatorEvent.OPERATORS_MOVED, operators);
	}

	/**
	 * Fire when the operator selection has changed.
	 *
	 * @param operators
	 * 		a collection of selected operators
	 */
	public void fireOperatorSelectionChanged(Collection<Operator> operators) {
		fireOperatorsChanged(OperatorEvent.SELECTED_OPERATORS_CHANGED, operators);
	}

	/**
	 * Fire when the number of ports for operators has changed.
	 *
	 * @param operators
	 * 		a collection of operators which had their ports changed
	 */
	public void firePortsChanged(Collection<Operator> operators) {
		fireOperatorsChanged(OperatorEvent.PORTS_CHANGED, operators);
	}

	/**
	 * Fire when an annotation has been moved.
	 *
	 * @param anno
	 * 		the moved annotation
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
	 * 		the moved annotations
	 */
	public void fireAnnotationsMoved(Collection<WorkflowAnnotation> annotations) {
		fireAnnotationsChanged(AnnotationEvent.ANNOTATIONS_MOVED, annotations);
	}

	/**
	 * Fire when an annotation has been selected.
	 *
	 * @param anno
	 * 		the selected annotation
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
	 * 		the changed annotation, can be {@code null}
	 */
	public void fireAnnotationMiscChanged(WorkflowAnnotation anno) {
		List<WorkflowAnnotation> list = new LinkedList<>();
		list.add(anno);
		fireAnnotationsChanged(AnnotationEvent.MISC_CHANGED, list);
	}

	/**
	 * Adds the last snapshot state of the process to the undo list. Sets the model to changed if it
	 * was not a simple view switch.
	 */
	private void addToUndoList(final boolean viewSwitch) {
		while (undoIndex < undoManager.getNumberOfUndos()) {
			undoManager.removeLast();
		}
		if (!undoManager.add(viewSwitch)) {
			return;
		}
		String maxSizeProperty = ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_UNDOLIST_SIZE);
		int maxSize = DEFAULT_UNDO_LIST_SIZE;
		try {
			if (maxSizeProperty != null) {
				maxSize = Integer.parseInt(maxSizeProperty);
			}
		} catch (NumberFormatException e) {
			LogService.getRoot().warning("com.rapidminer.gui.main_frame_warning");
		}
		while (undoManager.getNumberOfUndos() > maxSize) {
			undoManager.removeFirst();
		}
		undoIndex = undoManager.getNumberOfUndos();

		// mark as changed only if the XML has changed
		if (!viewSwitch) {
			hasChanged = true;
		}
		fireProcessUpdated();
	}

	/**
	 * Adds the current view to the undo stack. Called before the actual displayed chain will change.
	 */
	private void addViewSwitchToUndo() {
		takeSnapshot();
		addToUndoList(true);
	}

	/**
	 * Takes a snapshot of the current process.
	 */
	private void takeSnapshot() {
		Process process = getProcess();
		if (process == null) {
			return;
		}
		fireDisplayedChainWillChange();
		undoManager.takeSnapshot(process.getRootOperator().getXML(true), getDisplayedChain(), getSelectedOperators(),
				process.getAllOperators());
	}

	/**
	 * Restores the state of the process according to the given index. Will return a thrown
	 * exception if a problem occurs.
	 */
	private Exception setToStep(int index) {
		String stateXML = undoManager.getXML(index);
		synchronized (process) {
			undoManager.clearSnapshot();
			try {
				ProcessRootOperator rootOperator = process.getRootOperator();
				UserData<Object> isTutorialProcess = rootOperator.getUserData(Tutorial.KEY_USER_DATA_FLAG);
				String currentXML = rootOperator.getXML(true);
				ProcessLocation procLoc = process.getProcessLocation();
				if (!stateXML.equals(currentXML)) {
					process = undoManager.restoreProcess(index);
					rootOperator = process.getRootOperator();
					// keep tutorial flag between undo steps
					if (isTutorialProcess != null) {
						rootOperator.setUserData(Tutorial.KEY_USER_DATA_FLAG, isTutorialProcess);
					}
					process.setProcessLocation(procLoc);
					// check whether the current xml corresponds to the saved one
					hasChanged = procLoc == null || !rootOperator.getXML(false).equals(procLoc.getRawXML());
					fireProcessChanged();
				}

				// restore displayed chain
				OperatorChain restoredOperatorChain = undoManager.restoreDisplayedChain(process, index);
				if (restoredOperatorChain != null) {
					displayedChain = restoredOperatorChain;
					fireDisplayedChainChanged();
				}

				// restore selected operator
				List<Operator> restoredOperators = undoManager.restoreSelectedOperators(process, index);
				if (restoredOperators != null) {
					setRestore(restoredOperators.get(0));
					selectedOperators = Collections.unmodifiableList(restoredOperators);
					fireOperatorSelectionChanged(selectedOperators);
				}
				fireProcessUpdated();
			} catch (Exception e) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Resets the undo stack.
	 */
	private void resetUndo() {
		undoManager.reset();
		takeSnapshot();
		undoIndex = 0;
		hasChanged = false;
	}

	/**
	 * Fires the given {@link ModelEvent}.
	 *
	 * @param type
	 * 		the event type
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
	 * 		the event type
	 * @param operators
	 * 		the affected operators
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
	 * 		the event type
	 * @param annotations
	 * 		the affected annotations
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

	/**
	 * Informs the {@link ProcessEditor ProcessEditors} that the process was updated. Fired when the
	 * process view has changed, e.g. when the user enters/leaves a subprocess in the process design
	 * panel.
	 *
	 * @see #setDisplayedChain(OperatorChain)
	 */
	private void fireProcessViewChanged() {
		Process process = getProcess();
		for (ExtendedProcessEditor editor : processEditors.getListeners(ExtendedProcessEditor.class)) {
			editor.processViewChanged(process);
		}
	}

	/**
	 * Informs the {@link ProcessEditor ProcessEditors} that the process was changed. Fired when the
	 * process was replaced.
	 *
	 * @see #setProcess(Process, boolean, boolean)
	 */
	private void fireProcessChanged() {
		Process process = getProcess();
		for (ProcessEditor editor : processEditors.getListeners(ProcessEditor.class)) {
			editor.processChanged(process);
		}
		for (ExtendedProcessEditor editor : processEditors.getListeners(ExtendedProcessEditor.class)) {
			editor.processChanged(process);
		}
	}

	/**
	 * Informs the {@link ProcessStorageListener ProcessStorageListeners} that the process was
	 * loaded.
	 *
	 * @see #setProcess(Process, boolean, boolean)
	 */
	private void fireProcessLoaded() {
		Process process = getProcess();
		LinkedList<ProcessStorageListener> list;
		synchronized (storageListeners) {
			list = new LinkedList<>(storageListeners);
		}
		for (ProcessStorageListener l : list) {
			l.opened(process);
		}
	}

	/**
	 * Informs the {@link ProcessStorageListener ProcessStorageListeners} that the process was
	 * saved.
	 *
	 * @see #processHasBeenSaved()
	 */
	private void fireProcessStored() {
		Process process = getProcess();
		LinkedList<ProcessStorageListener> list;
		synchronized (storageListeners) {
			list = new LinkedList<>(storageListeners);
		}
		for (ProcessStorageListener l : list) {
			l.stored(process);
		}
	}

	/**
	 * Disposes of this model. Removes the global {@link ParameterChangeListener} and clears the
	 * {@link NewProcessUndoManager}. This should be called if the model is no longer used and is
	 * expected to be garbage collected. It can not be used reliably after this call.
	 *
	 * @since 8.2
	 */
	public void dispose() {
		undoManager.reset();
		ParameterService.removeParameterChangeListener(paramListener);
	}
}
