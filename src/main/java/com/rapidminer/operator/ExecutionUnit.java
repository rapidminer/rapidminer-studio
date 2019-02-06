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
package com.rapidminer.operator;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;

import com.rapidminer.Process;
import com.rapidminer.operator.execution.UnitExecutionFactory;
import com.rapidminer.operator.execution.UnitExecutor;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.PortException;
import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.operator.ports.metadata.OperatorLoopError;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.DelegatingObserver;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.Tools;


/**
 * A process is a collection of operators whose ports can be wired. A process provides input and
 * output ports to its contained operators. ExecutionUnit replaces the legacy OperatorChain. This
 * class takes care of executing the operators in the correct order by sorting them topologically
 * with respect to their dependencies.
 *
 * @author Simon Fischer
 */
public class ExecutionUnit extends AbstractObservable<ExecutionUnit> {

	private final PortOwner portOwner = new PortOwner() {

		@Override
		public OperatorChain getPortHandler() {
			return getEnclosingOperator();
		}

		@Override
		public String getName() {
			return ExecutionUnit.this.getName();
		}

		@Override
		public Operator getOperator() {
			return getEnclosingOperator();
		}

		@Override
		public ExecutionUnit getConnectionContext() {
			return ExecutionUnit.this;
		}
	};

	private String name;
	private final OperatorChain enclosingOperator;

	private final InputPorts innerInputPorts;
	private final OutputPorts innerOutputPorts;
	private Vector<Operator> operators = new Vector<>();
	private Vector<Operator> executionOrder;

	/**
	 * Container for user data.
	 */
	private Map<String, UserData<Object>> userData;

	/**
	 * Lock object for user data container.
	 */
	private final Object userDataLock = new Object();

	private final Observer<Port> delegatingPortObserver = new DelegatingObserver<>(this, this);
	private final Observer<Operator> delegatingOperatorObserver = new DelegatingObserver<>(this,
			this);

	public ExecutionUnit(OperatorChain enclosingOperator, String name) {
		this.name = name;

		innerInputPorts = enclosingOperator.createInnerSinks(portOwner);
		innerOutputPorts = enclosingOperator.createInnerSources(portOwner);
		this.enclosingOperator = enclosingOperator;
		innerInputPorts.addObserver(delegatingPortObserver, false);
		innerOutputPorts.addObserver(delegatingPortObserver, false);
		int index = 0;
		do {
			char c = name.charAt(index);
			if (!(Character.isUpperCase(c) || Character.isDigit(c))) {
				// LogService.getRoot().warning("Process name does not follow naming conventions:
				// "+name+" (in "+enclosingOperator.getOperatorDescription().getName()+")");
				LogService.getRoot().log(Level.WARNING,
						"com.rapidminer.operator.ExecutionUnit.process_name_does_not_follow_name_conventions",
						new Object[]{name, enclosingOperator.getOperatorDescription().getName()});
			}
			index = name.indexOf(' ', index) + 1;
		} while (index != 0);
	}

	public InputPorts getInnerSinks() {
		return innerInputPorts;
	}

	public OutputPorts getInnerSources() {
		return innerOutputPorts;
	}

	/**
	 * Same as {@link #addOperator(Operator, boolean) addOperator(Operator, true)}.
	 */
	public int addOperator(Operator operator) {
		return addOperator(operator, true);
	}

	/**
	 * Adds the operator to this execution unit.
	 *
	 * @param registerWithProcess
	 * 		Typically true. If false, the operator will not be registered with its parent process.
	 * @return the new index of the operator.
	 * @see #addOperator(Operator, int, boolean) addOperator(Operator, -1, boolean)
	 */
	public int addOperator(Operator operator, boolean registerWithProcess) {
		addOperator(operator, -1, registerWithProcess);
		return operators.size() - 1;
	}

	/**
	 * Adds the operator to this execution unit. The operator at this index and all subsequent
	 * operators are shifted to the right. The operator is registered automatically.
	 *
	 * @see #addOperator(Operator, int, boolean) addOperator(Operator, index, true)
	 */
	public void addOperator(Operator operator, int index) {
		addOperator(operator, index, true);
	}

	/**
	 * Adds the operator to this execution unit. If the index is non-negative, the operator at that
	 * index and all subsequent operators are shifted to the right. The operator is registered if so
	 * specified.
	 *
	 * @param operator
	 * 		the operator to be added
	 * @param index
	 * 		the position where to add the operator, or {@code -1} to simply append.
	 * @param registerWithProcess
	 * 		Typically true. If false, the operator will not be registered with its parent
	 * 		process.
	 * @since 8.2
	 */
	private void addOperator(Operator operator, int index, boolean registerWithProcess) {
		if (operator == null) {
			throw new NullPointerException("operator cannot be null!");
		}
		if (operator instanceof ProcessRootOperator) {
			throw new IllegalArgumentException(
					"'Process' operator cannot be added. It must always be the top-level operator!");
		}
		if (index == -1) {
			operators.add(operator);
		} else {
			operators.add(index, operator);
		}
		registerOperator(operator, registerWithProcess);
	}

	public int getIndexOfOperator(Operator operator) {
		return operators.indexOf(operator);
	}

	/**
	 * Looks up {@link UserData} entries. Returns null if key is unknown.
	 *
	 * @param The
	 * 		key of the user data.
	 * @return The user data.
	 */
	public UserData<Object> getUserData(String key) {
		synchronized (this.userDataLock) {
			if (this.userData == null) {
				return null;
			} else {
				return this.userData.get(key);
			}
		}
	}

	/**
	 * Stores arbitrary {@link UserData}.
	 *
	 * @param key
	 * 		The key to used to identify the data.
	 * @param data
	 * 		The user data.
	 */
	public void setUserData(String key, UserData<Object> data) {
		synchronized (this.userDataLock) {
			if (this.userData == null) {
				this.userData = new TreeMap<>();
			}
			this.userData.put(key, data);
		}
	}

	private void registerOperator(Operator operator, boolean registerWithProcess) {
		operator.setEnclosingProcess(this);
		Process process = getEnclosingOperator().getProcess();
		if (process != null && registerWithProcess) {
			operator.registerOperator(process);
		}
		fireUpdate(this);
		operator.addObserver(delegatingOperatorObserver, false);
		operator.clear(Port.CLEAR_ALL);
		if (process != null) {
			process.fireOperatorAdded(operator);
		}
	}

	private void unregister(Operator operator) {
		operator.removeObserver(delegatingOperatorObserver);
	}

	/**
	 * Removes the given operator. Don't call this method directly but call
	 * {@link Operator#remove()}.
	 */
	protected void removeOperator(Operator operator) {
		if (!operators.contains(operator)) {
			throw new NoSuchElementException("Operator " + operator.getName() + " not contained in " + getName() + "!");
		}
		int oldIndex = operators.indexOf(operator);
		int oldIndexAmongEnabled = getEnabledOperators().indexOf(operator);
		operators.remove(operator);
		unregister(operator);
		Process process = getEnclosingOperator().getProcess();
		if (process != null) {
			process.fireOperatorRemoved(operator, oldIndex, oldIndexAmongEnabled);
		}
		operator.setEnclosingProcess(null);
		fireUpdate(this);
	}

	public void clear(int clearFlags) {
		for (Operator operator : operators) {
			operator.clear(clearFlags);
		}
		getInnerSinks().clear(clearFlags);
		getInnerSources().clear(clearFlags);
	}

	/** Helper class to count the number of dependencies of an operator. */
	private static class EdgeCounter {

		private final Map<Operator, Integer> numIncomingEdges = new LinkedHashMap<>();

		private EdgeCounter(Collection<Operator> operators) {
			for (Operator op : operators) {
				numIncomingEdges.put(op, 0);
			}
		}

		private void incNumEdges(Operator op) {
			Integer num = numIncomingEdges.get(op);
			if (num == null) {
				// this can only happen if we add edges to inner ports of the enclosing operator.
				return;
			}
			num = num + 1;
			numIncomingEdges.put(op, num);
		}

		private int decNumEdges(Operator op) {
			Integer num = numIncomingEdges.get(op);
			// this can only happen if we add edges to inner ports of the enclosing operator.
			if (num == null) {
				return -1;
			}
			num = num - 1;
			assert num >= 0;
			numIncomingEdges.put(op, num);
			return num;
		}

		private LinkedList<Operator> getIndependentOperators() {
			LinkedList<Operator> independentOperators = new LinkedList<>();
			for (Map.Entry<Operator, Integer> entry : numIncomingEdges.entrySet()) {
				if (entry.getValue() == null || entry.getValue() == 0) {
					independentOperators.add(entry.getKey());
				}
			}
			return independentOperators;
		}
	}

	/**
	 * Sorts the operators topologically, i.e. such that operator <var>i</var> in the returned
	 * ordering has dependencies (i.e. connected {@link InputPort}s) only from operators
	 * <var>0..i-1</var>.
	 */
	public Vector<Operator> topologicalSort() {
		final Map<Operator, Integer> originalIndices = new HashMap<>();
		for (int i = 0; i < operators.size(); i++) {
			originalIndices.put(operators.get(i), i);
		}
		EdgeCounter counter = new EdgeCounter(operators);
		for (Operator child : getOperators()) {
			for (OutputPort out : child.getOutputPorts().getAllPorts()) {
				InputPort dest = out.getDestination();
				if (dest != null) {
					counter.incNumEdges(dest.getPorts().getOwner().getOperator());
				}
			}
		}
		Vector<Operator> sorted = new Vector<>();
		PriorityQueue<Operator> independentOperators = new PriorityQueue<>(Math.max(1, operators.size()),
				Comparator.comparingInt(originalIndices::get));
		independentOperators.addAll(counter.getIndependentOperators());
		while (!independentOperators.isEmpty()) {
			Operator first = independentOperators.poll();
			sorted.add(first);
			for (OutputPort out : first.getOutputPorts().getAllPorts()) {
				InputPort dest = out.getDestination();
				if (dest != null) {
					Operator destOp = dest.getPorts().getOwner().getOperator();
					if (counter.decNumEdges(destOp) == 0) {
						// independentOperators.addFirst(destOp);
						independentOperators.add(destOp);
					}
				}
			}
		}
		return sorted;
	}

	protected void updateExecutionOrder() {
		this.executionOrder = topologicalSort();
		if (!this.executionOrder.equals(operators)) {
			if (operators.size() != executionOrder.size()) {
				// we have a circle. without a check, operator vanishes.
				return;
			}
			this.operators = this.executionOrder;
			getEnclosingOperator().getProcess().fireExecutionOrderChanged(this);
		}
		for (Operator operator : this.operators) {
			operator.updateExecutionOrder();
		}
	}

	public void transformMetaData() {
		List<Operator> sorted = topologicalSort();
		for (Operator op : sorted) {
			op.transformMetaData();
		}
		if (sorted.size() != operators.size()) {
			List<Operator> remainder = new LinkedList<>(operators);
			remainder.removeAll(sorted);
			for (Operator nodeInCircle : remainder) {
				for (OutputPort outputPort : nodeInCircle.getOutputPorts().getAllPorts()) {
					InputPort destination = outputPort.getDestination();
					if (destination != null && remainder.contains(destination.getPorts().getOwner().getOperator())) {
						if (destination.getSource() != null) {
							// (source can be null *during* a disconnect in which case
							// both the source and the destination fire an update
							// which leads to this inconsistent state)
							destination.addError(new OperatorLoopError(destination));
						}
						outputPort.addError(new OperatorLoopError(outputPort));
					}
				}
			}
		}
		getInnerSinks().checkPreconditions();
	}

	/** Returns an unmodifiable view of the operators contained in this process. */
	public List<Operator> getOperators() {
		return Collections.unmodifiableList(new ArrayList<>(operators));
	}

	/**
	 * Use this method only in cases where you are sure that you don't want a
	 * ConcurrentModificationException to occur when the list of operators is modified.
	 */
	public Enumeration<Operator> getOperatorEnumeration() {
		return operators.elements();
	}

	/** Returns an unmodifiable view of the operators contained in this process. */
	public List<Operator> getEnabledOperators() {
		return new EnabledOperatorView(operators);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/** Returns the operator that contains this process as a subprocess. */
	public OperatorChain getEnclosingOperator() {
		return enclosingOperator;
	}

	private void unwire(boolean recursive) {
		getInnerSources().disconnectAll();
		for (Operator op : getOperators()) {
			unwire(op, recursive);
		}
	}

	private void unwire(Operator op, boolean recursive) throws PortException {
		op.getOutputPorts().disconnectAll();
		if (recursive && op instanceof OperatorChain) {
			for (ExecutionUnit subprocess : ((OperatorChain) op).getSubprocesses()) {
				subprocess.unwire(recursive);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void autoWire(CompatibilityLevel level, InputPorts inputPorts, LinkedList<OutputPort> readyOutputs)
			throws PortException {
		boolean success = false;
		do {
			Set<InputPort> complete = new HashSet<>();
			for (InputPort in : inputPorts.getAllPorts()) {
				success = false;
				if (!in.isConnected() && !complete.contains(in)
						&& in.getPorts().getOwner().getOperator().shouldAutoConnect(in)) {
					Iterator<OutputPort> outIterator;
					// TODO: Simon: Does the same in both cases. Check again.
					if (in.simulatesStack()) {
						outIterator = readyOutputs.descendingIterator();
					} else {
						outIterator = readyOutputs.descendingIterator();
					}
					while (outIterator.hasNext()) {
						OutputPort outCandidate = outIterator.next();
						// TODO: Remove shouldAutoConnect() in later versions
						Operator owner = outCandidate.getPorts().getOwner().getOperator();
						if (owner.shouldAutoConnect(outCandidate) && outCandidate.getMetaData() != null &&
								in.isInputCompatible(outCandidate.getMetaData(), level)) {
							readyOutputs.remove(outCandidate);
							outCandidate.connectTo(in);
							// we cannot continue with the remaining input ports
							// since connecting may have triggered the creation of new input
							// ports
							// which would result in undefined behavior and a
							// ConcurrentModificationException
							success = true;
							break;
						}
					}
					// no port found.
					complete.add(in);
					if (success) {
						break;
					}
				}
			}
		} while (success);
	}

	/**
	 * Transforms the meta data of the enclosing operator. Required in {@link #autoWire()} after
	 * each Operator that has been wired.
	 */
	private void transformMDNeighbourhood() {
		getEnclosingOperator().transformMetaData();
	}

	/**
	 * Connects the ports automatically in a first-fit approach. Operators are connected in their
	 * ordering within the {@link #operators} list. Every input of every operator is connected to
	 * the first compatible output of an operator "left" of this operator. This corresponds to the
	 * way, IOObjects were consumed in the pre-5.0 version. Disabled operators are skipped.
	 *
	 * @param level
	 * 		If level is {@link CompatibilityLevel#VERSION_5}, an input is considered
	 * 		compatible only if it satisfies all meta data constraints. For
	 * 		{@link CompatibilityLevel#PRE_VERSION_5} we only consider the classes.
	 * @param keepConnections
	 * 		if true, don't unwire old connections before rewiring.
	 */
	public void autoWire(CompatibilityLevel level, boolean keepConnections, boolean recursive) throws PortException {
		if (!keepConnections) {
			unwire(recursive);
		}
		// store all outputs. Scan them to find matching inputs.
		LinkedList<OutputPort> readyOutputs = new LinkedList<>();
		addReadyOutputs(readyOutputs, getInnerSources());
		List<Operator> enabled = new LinkedList<>();
		for (Operator op : getOperators()) {
			if (op.isEnabled()) {
				enabled.add(op);
			}
		}
		autoWire(level, enabled, readyOutputs, recursive, true);
	}

	/**
	 * @param wireNew
	 * 		If true, OutputPorts of operators will be added to readyOutputs once they are
	 * 		wired.
	 */
	private void autoWire(CompatibilityLevel level, List<Operator> operators, LinkedList<OutputPort> readyOutputs,
						  boolean recursive, boolean wireNew) throws PortException {
		transformMDNeighbourhood();

		for (Operator op : operators) {
			try {
				readyOutputs = op.preAutoWire(readyOutputs);
			} catch (OperatorException e) {
				getEnclosingOperator().getLogger().log(Level.WARNING, "During auto-wiring: " + e, e);
			}
			autoWire(level, op.getInputPorts(), readyOutputs);
			transformMDNeighbourhood();
			if (recursive && op instanceof OperatorChain) {
				for (ExecutionUnit subprocess : ((OperatorChain) op).getSubprocesses()) {
					// we have already removed all connections, so keepConnections=true in
					// recursive call
					subprocess.autoWire(level, true, recursive);
				}
			}
			if (wireNew) {
				addReadyOutputs(readyOutputs, op.getOutputPorts());
			}
		}
		autoWire(level, getInnerSinks(), readyOutputs);
		transformMDNeighbourhood();
	}

	/**
	 * Automatically wires inputs and outputs of a single operator in this execution unit.
	 *
	 * @param inputs
	 * 		Wire inputs?
	 * @param outputs
	 * 		Wire outputs?
	 */
	public void autoWireSingle(Operator operator, CompatibilityLevel level, boolean inputs, boolean outputs) {
		// auto wire inputs
		if (inputs) {
			transformMDNeighbourhood();
			// store all outputs. Scan them to find matching inputs.
			LinkedList<OutputPort> readyOutputs = new LinkedList<>();
			// add the ports, oldest first. Simulate pre-5.0-like stack by taking
			// the last out of this list when consuming input.
			addReadyOutputs(readyOutputs, getInnerSources());
			boolean found = false;
			for (Operator other : operators) {
				if (other == operator) {
					found = true;
					break;
				} else {
					addReadyOutputs(readyOutputs, other.getOutputPorts());
				}
			}
			if (!found) {
				throw new IllegalArgumentException(
						"Operator " + operator.getName() + " does not belong to this subprocess " + getName() + ".");
			}
			getEnclosingOperator().getLogger()
					.fine("Wiring: " + operator + "." + operator.getInputPorts().getAllPorts() + " to " + readyOutputs);
			autoWire(level, operator.getInputPorts(), readyOutputs);
		}

		// auto wire outputs
		if (outputs) {
			LinkedList<OutputPort> readyOutputs = new LinkedList<>();
			addReadyOutputs(readyOutputs, operator.getOutputPorts());
			List<Operator> successors = new LinkedList<>();
			boolean foundMe = false;
			for (Operator other : getOperators()) {
				if (foundMe) {
					successors.add(other);
				} else if (other == operator) {
					foundMe = true;
				}
			}
			autoWire(level, successors, readyOutputs, false, false);
		}
	}

	private void addReadyOutputs(LinkedList<OutputPort> readyOutputs, OutputPorts ports) {
		// add the parameters in a stack-like fashion like in pre-5.0
		Iterator<OutputPort> i = new LinkedList<OutputPort>(ports.getAllPorts()).descendingIterator();
		while (i.hasNext()) {
			OutputPort port = i.next();
			if (!port.isConnected() && port.shouldAutoConnect()) {
				readyOutputs.addLast(port);
			}
		}
	}

	/**
	 * Returns a list of all available output ports within this process, including inner sources and
	 * output ports of enclosed operators.
	 */
	public Collection<OutputPort> getAllOutputPorts() {
		Collection<OutputPort> outputPorts = new LinkedList<>();
		outputPorts.addAll(getInnerSources().getAllPorts());
		for (Operator operator : operators) {
			outputPorts.addAll(operator.getOutputPorts().getAllPorts());
		}
		return outputPorts;
	}

	public Operator getOperatorByName(String toOp) {
		for (Operator op : operators) {
			if (op.getName().equals(toOp)) {
				return op;
			}
		}
		return null;
	}

	public int getNumberOfOperators() {
		return operators.size();
	}

	/**
	 * Clones operators contained in <code>original</code>, adds them to this execution unit and
	 * wires them as they were originally.
	 *
	 * @param forParallelExecution
	 * 		Indicates whether this clone is supposed to be executed in parallel. If yes, the
	 * 		clone will not be registered with the parent process and will share its
	 * 		{@link Operator#applyCount} with the original.
	 */
	public void cloneExecutionUnitFrom(ExecutionUnit original, boolean forParallelExecution) {
		// Clone operators
		Map<String, Operator> clonedOperatorsByName = new HashMap<>();
		for (Operator originalChild : original.operators) {
			Operator clonedOperator = originalChild.cloneOperator(originalChild.getName(), forParallelExecution);
			addOperator(clonedOperator, !forParallelExecution);
			clonedOperatorsByName.put(originalChild.getName(), clonedOperator);
		}

		// Restore connections
		cloneConnections(original.getInnerSources(), original, clonedOperatorsByName);
		for (Operator op : original.operators) {
			cloneConnections(op.getOutputPorts(), original, clonedOperatorsByName);
		}

		// Unlock
		original.getInnerSources().unlockPortExtenders();
		original.getInnerSinks().unlockPortExtenders();
		for (Operator op : this.operators) {
			op.getInputPorts().unlockPortExtenders();
			op.getOutputPorts().unlockPortExtenders();
		}

		// copy user data entries
		if (original.userData != null) {
			for (String key : original.userData.keySet()) {
				UserData<Object> data = original.userData.get(key);
				if (data != null) {
					setUserData(key, data.copyUserData(this));
				}
			}
		}

		// Other:
		this.expanded = original.expanded;
	}

	private void cloneConnections(OutputPorts originalPorts, ExecutionUnit originalExecutionUnit,
								  Map<String, Operator> clonedOperatorsByName) {
		for (OutputPort originalSource : originalPorts.getAllPorts()) {
			if (originalSource.isConnected()) {

				OutputPort mySource;
				if (originalPorts.getOwner().getOperator() == originalExecutionUnit.getEnclosingOperator()) {
					// this is an inner source
					mySource = getInnerSources().getPortByName(originalSource.getName());
					if (mySource == null) {
						throw new RuntimeException("Error during clone: Corresponding source for " + originalSource
								+ " not found (no such inner source).");
					}
				} else {
					// this is an output port
					Operator myOperator = clonedOperatorsByName
							.get(originalSource.getPorts().getOwner().getOperator().getName());
					if (myOperator == null) {
						throw new RuntimeException("Error during clone: Corresponding source for " + originalSource
								+ " not found (no such operator).");
					}
					mySource = myOperator.getOutputPorts().getPortByName(originalSource.getName());
					if (mySource == null) {
						throw new RuntimeException("Error during clone: Corresponding source for " + originalSource
								+ " not found (no such output port).");
					}
				}

				InputPort originalDestination = originalSource.getDestination();
				InputPort myDestination;
				if (originalDestination.getPorts().getOwner().getOperator() == originalExecutionUnit
						.getEnclosingOperator()) {
					// this is an inner sink
					myDestination = getInnerSinks().getPortByName(originalDestination.getName());
					if (myDestination == null) {
						throw new RuntimeException("Error during clone: Corresponding destination for " + originalDestination
								+ " not found (no such inner sink).");
					}
				} else {
					// this is an input port
					Operator myOperator = clonedOperatorsByName
							.get(originalDestination.getPorts().getOwner().getOperator().getName());
					if (myOperator == null) {
						throw new RuntimeException("Error during clone: Corresponding destination for " + originalDestination
								+ " not found (no such operator).");
					}
					myDestination = myOperator.getInputPorts().getPortByName(originalDestination.getName());
					if (myDestination == null) {
						throw new RuntimeException("Error during clone: Corresponding destination for " + originalDestination
								+ " not found (no such input port).");
					}
				}
				mySource.connectTo(myDestination);
			}
		}
	}

	/** Returns all nested operators. */
	public Collection<Operator> getChildOperators() {
		List<Operator> children = new LinkedList<>();
		for (Operator operator : operators) {
			children.add(operator);
		}
		return children;
	}

	/** Recursively returns all nested operators. */
	public List<Operator> getAllInnerOperators() {
		List<Operator> children = new LinkedList<>();
		for (Operator operator : operators) {
			children.add(operator);
			if (operator instanceof OperatorChain) {
				children.addAll(((OperatorChain) operator).getAllInnerOperators());
			}
		}
		return children;
	}

	protected List<String> createProcessTreeList(int indent, String selfPrefix, String childPrefix,
												 Operator markOperator, String mark) {
		List<String> treeList = new LinkedList<>();
		treeList.add(Tools.indent(indent) + " subprocess '" + getName() + "'");
		Iterator<Operator> i = operators.iterator();
		while (i.hasNext()) {
			treeList.addAll(i.next().createProcessTreeList(indent, childPrefix + "+- ",
					childPrefix + (i.hasNext() ? "|  " : "   "), markOperator, mark));
		}
		return treeList;
	}

	/** Executes the inner operators. */
	public void execute() throws OperatorException {
		UnitExecutor executor = UnitExecutionFactory.getInstance().getExecutor(this);
		// check only the callstack of nested operators, otherwise execution units of
		// unsigned extensions might not be able to execute trusted operators
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

				@Override
				public Void run() throws OperatorException {

					executor.execute(ExecutionUnit.this);
					return null;
				}
			});
		} catch (PrivilegedActionException e) {
			// e.getException() can either be an instance of OperatorException,
			// or an unsafe Exception,
			// as only checked exceptions will be wrapped in a
			// PrivilegedActionException.
			if (e.getException() instanceof OperatorException) {
				throw (OperatorException) e.getException();
			} else {
				// Wrap unsafe Exceptions
				throw new OperatorException(e.getException().getMessage(), e.getException());
			}
		}
	}

	/** Frees memory used by inner sinks. */
	public void freeMemory() {
		getInnerSources().freeMemory();
		getInnerSinks().freeMemory();
	}

	private boolean expanded = true;

	/** Sets the expansion mode which indicates if this operator is drawn expanded or not. */
	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	/** Returns true if this operator should be painted expanded. */
	public boolean isExpanded() {
		return expanded;
	}

	public void processStarts() throws OperatorException {
		for (Operator operator : operators) {
			operator.processStarts();
		}
		updateExecutionOrder();
	}

	public void processFinished() throws OperatorException {
		for (Operator operator : operators) {
			operator.processFinished();
		}
	}

	/**
	 * Moves the operators from this process to another process, keeping all connections intact.
	 * TODO: Test more rigorously. Do we register/unregister everything correctly?
	 *
	 * @return the number of ports the connections of which could not be restored
	 */
	public int stealOperatorsFrom(ExecutionUnit otherUnit) {
		int failedReconnects = 0;

		// remember source and sink connections so we can reconnect them later.
		Map<String, InputPort> sourceMap = new HashMap<>();
		Map<String, OutputPort> sinkMap = new HashMap<>();
		for (OutputPort source : otherUnit.getInnerSources().getAllPorts()) {
			if (source.isConnected()) {
				sourceMap.put(source.getName(), source.getDestination());
			}
		}
		otherUnit.getInnerSources().disconnectAll();
		for (InputPort sink : otherUnit.getInnerSinks().getAllPorts()) {
			if (sink.isConnected()) {
				sinkMap.put(sink.getName(), sink.getSource());
			}
		}
		otherUnit.getInnerSinks().disconnectAll();

		// Move operators
		Iterator<Operator> i = otherUnit.operators.iterator();
		while (i.hasNext()) {
			Operator operator = i.next();
			i.remove();
			otherUnit.unregister(operator);
			Process otherProcess = operator.getProcess();
			if (otherProcess != null) {
				operator.unregisterOperator(otherProcess);
			}
			this.operators.add(operator);
			operator.setEnclosingProcess(null);
			registerOperator(operator, true);
		}

		// Rewire sources and sinks
		for (Map.Entry<String, InputPort> entry : sourceMap.entrySet()) {
			OutputPort mySource = getInnerSources().getPortByName(entry.getKey());
			if (mySource != null) {
				mySource.connectTo(entry.getValue());
			} else {
				failedReconnects++;
			}
		}
		getInnerSources().unlockPortExtenders();

		for (Map.Entry<String, OutputPort> entry : sinkMap.entrySet()) {
			InputPort mySink = getInnerSinks().getPortByName(entry.getKey());
			if (mySink != null) {
				entry.getValue().connectTo(mySink);
			} else {
				failedReconnects++;
			}
		}
		getInnerSinks().unlockPortExtenders();

		fireUpdate(this);
		return failedReconnects;
	}

	/**
	 * Moves an operator to the given index. (If the old index is smaller than the new one, the new
	 * one will automatically be reduced by one.)
	 */
	public void moveToIndex(Operator op, int newIndex) {
		int oldIndex = operators.indexOf(op);
		Process process = getEnclosingOperator().getProcess();
		if (oldIndex != -1) {
			operators.remove(op);
			if (process != null) {
				int oldIndexAmongEnabled = getEnabledOperators().indexOf(op);
				process.fireOperatorRemoved(op, oldIndex, oldIndexAmongEnabled);
			}
			if (oldIndex < newIndex) {
				newIndex--;
			}
			operators.add(newIndex, op);
			if (process != null) {
				process.fireOperatorAdded(op);
			}
			fireUpdate();
			updateExecutionOrder();
		}
	}

	/**
	 * Re-arranges the execution order such that the specified operators immediately follow
	 * <code>insertAfter</code>.
	 */
	public void bringToFront(Collection<Operator> movedOperators, Operator insertAfter) {
		this.operators.removeAll(movedOperators);
		int index = this.operators.indexOf(insertAfter) + 1;
		for (Operator op : movedOperators) {
			this.operators.add(index++, op);
		}
		updateExecutionOrder();
		fireUpdate();
	}
}
