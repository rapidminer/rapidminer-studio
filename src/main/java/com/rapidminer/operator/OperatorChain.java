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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.Process;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPorts;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.impl.InputPortsImpl;
import com.rapidminer.operator.ports.impl.OutputPortsImpl;
import com.rapidminer.operator.ports.metadata.MDTransformer;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.tools.DelegatingObserver;
import com.rapidminer.tools.ListenerTools;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.patterns.Visitor;


/**
 * An OperatorChain is an Operator that contains children which are again Operators and which it can
 * execute once ore several times during its own execution.<br/>
 *
 * As of RapidMiner 5.0, an OperatorChain does not directly contain nested Operators, but rather
 * nested {@link ExecutionUnit}s which in turn contain operators.
 *
 * Please refer to the RapidMiner tutorial for a description how to implement your own operator
 * chain.
 *
 * @author Simon Fischer, Ingo Mierswa
 */
public abstract class OperatorChain extends Operator {

	private ExecutionUnit[] subprocesses;

	private final Observer<ExecutionUnit> delegatingObserver = new DelegatingObserver<ExecutionUnit, Operator>(this, this);

	/**
	 * Creates an empty operator chain.
	 *
	 * @deprecated Use OpertorChain(OperatorDescription, String...) to assign names to subprocesses.
	 */
	@Deprecated
	public OperatorChain(OperatorDescription description) {
		this(description, new String[0]);
	}

	public OperatorChain(OperatorDescription description, String... subprocessNames) {
		super(description);
		subprocesses = new ExecutionUnit[subprocessNames.length];
		for (int i = 0; i < subprocesses.length; i++) {
			subprocesses[i] = new ExecutionUnit(this, subprocessNames[i]);
			subprocesses[i].addObserver(delegatingObserver, false);
			makeDirtyOnUpdate(subprocesses[i].getInnerSinks());
		}
	}

	/**
	 * Indicates whether or not the GUI may offer an option to dynamically add to the number of
	 * subprocesses. The default implementation returns false.
	 */
	public boolean areSubprocessesExtendable() {
		return false;
	}

	public ExecutionUnit removeSubprocess(int index) {
		ExecutionUnit deleted = subprocesses[index];
		ExecutionUnit[] copy = subprocesses;
		subprocesses = new ExecutionUnit[copy.length - 1];
		int j = 0;
		for (int i = 0; i < copy.length; i++) {
			if (i != index) {
				subprocesses[j++] = copy[i];
			}
		}
		deleted.removeObserver(delegatingObserver);
		fireUpdate(this);
		return deleted;
	}

	/** Creates a subprocess by making a callback to {@link createSubprocess(int)}. */
	public ExecutionUnit addSubprocess(int index) {
		ExecutionUnit[] copy = subprocesses;
		subprocesses = new ExecutionUnit[copy.length + 1];
		int j = 0;
		for (int i = 0; i < copy.length; i++) {
			if (i == index) {
				j++;
			}
			subprocesses[j++] = copy[i];
		}
		subprocesses[index] = createSubprocess(index);
		subprocesses[index].addObserver(delegatingObserver, false);
		fireUpdate(this);
		return subprocesses[index];
	}

	protected ExecutionUnit createSubprocess(int index) {
		return new ExecutionUnit(this, "Subprocess");
	}

	/**
	 * This method returns an arbitrary implementation of {@link InputPorts} for inner sink port
	 * initialization. Useful for adding an arbitrary implementation (e.g. changing port creation &
	 * (dis)connection behavior, optionally by customized {@link InputPort} instances) by overriding
	 * this method.
	 * 
	 * @param portOwner
	 *            The owner of the ports.
	 * @return The {@link InputPorts} instance, never {@code null}.
	 * @since 7.3.0
	 */
	protected InputPorts createInnerSinks(PortOwner portOwner) {
		return new InputPortsImpl(portOwner);
	}

	/**
	 * This method returns an arbitrary implementation of {@link OutputPorts} for inner source port
	 * initialization. Useful for adding an arbitrary implementation (e.g. changing port creation &
	 * (dis)connection behavior, optionally by customized {@link OutputPort} instances) by
	 * overriding this method.
	 * 
	 * @param portOwner
	 *            The owner of the ports.
	 * @return The {@link OutputPorts} instance, never {@code null}.
	 * @since 7.3.0
	 */
	protected OutputPorts createInnerSources(PortOwner portOwner) {
		return new OutputPortsImpl(portOwner);
	}

	/**
	 * Returns the maximum number of inner operators.
	 *
	 * @deprecated Use subprocesses instead.
	 */
	@Deprecated
	public int getMaxNumberOfInnerOperators() {
		return 0;
	}

	/**
	 * Returns the minimum number of inner operators. * @deprecated Use subprocesses instead.
	 */
	@Deprecated
	public int getMinNumberOfInnerOperators() {
		return 0;
	}

	/**
	 * Must return a condition of the IO behaviour of all desired inner operators.
	 *
	 * @deprecated specify input and output ports instead.
	 */
	@Deprecated
	public com.rapidminer.operator.condition.InnerOperatorCondition getInnerOperatorCondition() {
		return null;
	}

	/**
	 * Performs a deep clone of this operator chain. Use this method only if you are sure what you
	 * are doing.
	 */
	@Override
	public Operator cloneOperator(String name, boolean forParallelExcecution) {
		OperatorChain clone = (OperatorChain) super.cloneOperator(name, forParallelExcecution);
		if (areSubprocessesExtendable()) {
			while (clone.getNumberOfSubprocesses() < getNumberOfSubprocesses()) {
				clone.addSubprocess(clone.getNumberOfSubprocesses());
			}
		}
		for (int i = 0; i < subprocesses.length; i++) {
			clone.subprocesses[i].cloneExecutionUnitFrom(this.subprocesses[i], forParallelExcecution);
		}
		return clone;
	}

	/**
	 * This method checks if inner operators can handle their input and deliver the necessary
	 * output. Depending on the return value of the method {@link #shouldReturnInnerOutput()} this
	 * method returns
	 * <ul>
	 * <li>the result of <code>getDeliveredOutputClasses()</code> if the output of the inner
	 * operators should not be returned.</li>
	 * <li>the result of <code>getAllOutputClasses(Class[] innerOutput)</code> if the output of the
	 * inner operators (innerOutput) should also be returned.</li>
	 * </ul>
	 *
	 * @deprecated As of RM, checkIO is replaced by the {@link MDTransformer}.
	 */
	@Override
	@Deprecated
	public Class<?>[] checkIO(Class<?>[] input) throws IllegalInputException, WrongNumberOfInnerOperatorsException {
		getLogger().warning("As of RM 5.0, checkIO() is no longer necessary.");
		return input;
	}

	/**
	 * Indicates if inner output should be delivered by this operator chain. Default is false.
	 * Operators which want to change this default behaviour should override this method and should
	 * return true. In this case the method checkIO would not longer return the result of
	 * {@link #getDeliveredOutputClasses()} but of {@link #getAllOutputClasses(Class[])}.
	 *
	 * @deprecated As of 5.0, this method is no longer necessary.
	 */
	@Deprecated
	protected boolean shouldReturnInnerOutput() {
		return false;
	}

	protected boolean shouldAddNonConsumedInput() {
		return !shouldReturnInnerOutput();
	}

	/**
	 * Adds a new inner operator at the last position. The returned index is the position of the
	 * added operator with respect to all operators (including the disabled operators).
	 */
	@Deprecated
	public final int addOperator(Operator o) {
		for (ExecutionUnit process : subprocesses) {
			if (process.getNumberOfOperators() == 0) {
				process.addOperator(o);
				getLogger().warning(
						"OperatorChain.addOperator() is deprecated! Use getSubprocess(int).addOperator(). I have added the operator to subprocess "
								+ process.getName());
			}
		}
		throw new UnsupportedOperationException(
				"addOperator() is no longer supported. Failed to guess which subprocess was intended. Try getSubprocess(int).addOperator()");
	}

	/**
	 * Adds the given operator at the given position. Please note that all operators (including the
	 * disabled operators) are used for position calculations.
	 */
	public final int addOperator(Operator operator, int index) {
		if (index < subprocesses.length) {
			subprocesses[index].addOperator(operator);
			getLogger().warning(
					"OperatorChain.addOperator() is deprecated! Use getSubprocess(int).addOperator(). I have added the operator to subprocess "
							+ subprocesses[index].getName());
			return index;
		} else {
			throw new UnsupportedOperationException(
					"addOperator() is no longer supported. Try getSubprocess(int).addOperator()");
		}
	}

	/**
	 * Register this operator chain and all of its children in the given process. This might change
	 * the name of the operator.
	 */
	@Override
	protected void registerOperator(Process process) {
		super.registerOperator(process);
		for (ExecutionUnit subprocess : subprocesses) {
			for (Operator child : subprocess.getOperators()) {
				child.registerOperator(process);
			}
		}
	}

	/** Unregisters this chain and all of its children from the given process. */
	@Override
	protected void unregisterOperator(Process process) {
		super.unregisterOperator(process);
		for (ExecutionUnit subprocess : subprocesses) {
			for (Operator child : subprocess.getOperators()) {
				child.unregisterOperator(process);
			}
		}
	}

	/**
	 * Removes the given operator from this operator chain. Do not use this method to actually
	 * remove an operator from an operator chain. Use operator.remove() instead. This method will be
	 * invoked by the remove() method (which also performs some other actions).
	 */
	@Deprecated
	protected final void removeOperator(Operator operator) {
		throw new UnsupportedOperationException("removeOperator is deprecated. Use getSubprocess(int).removeOperator()");
	}

	/** Returns the i-th inner operator. */
	@Deprecated
	public Operator getOperator(int i) {
		throw new UnsupportedOperationException("getOperator(int) is deprecated. Try getSubprocess(int).");
	}

	/** Returns an iterator over all Operators. */
	@Deprecated
	public Iterator<Operator> getOperators() {
		throw new UnsupportedOperationException(
				"OperatorChain.getNumberOfOperators() is deprecated. Try getSubprocesses(int).getOperators()");
	}

	/** Returns all operators contained in the subprocesses of this chain (non-recursive). */
	public List<Operator> getImmediateChildren() {
		List<Operator> children = new LinkedList<Operator>();
		for (ExecutionUnit executionUnit : subprocesses) {
			children.addAll(executionUnit.getOperators());
		}
		return children;
	}

	/** Returns recursively all child operators independently if they are activated or not. */
	public List<Operator> getAllInnerOperators() {
		List<Operator> children = new LinkedList<Operator>();
		for (ExecutionUnit executionUnit : subprocesses) {
			children.addAll(executionUnit.getAllInnerOperators());
		}
		return children;
	}

	public List<Operator> getAllInnerOperatorsAndMe() {
		List<Operator> children = getAllInnerOperators();
		children.add(this);
		return children;
	}

	/**
	 * As a workaround, returns the number of subprocesses.
	 *
	 * @deprecated as of RM replaced by subprocesses.
	 */
	@Deprecated
	public int getNumberOfOperators() {
		return subprocesses.length;
	}

	/**
	 * Returns the number of all inner operators (including the disabled operators). Mainly used for
	 * GUI purposes. Operators should use {@link #getNumberOfOperators()}.
	 *
	 * @deprecated Try getSubprocess(int).getNumberOfOperators()
	 */
	@Deprecated
	public int getNumberOfAllOperators() {
		return subprocesses.length;
	}

	/**
	 * Returns the i-th operator. In contrast to the method {@link #getOperator(int i)} this method
	 * also uses disabled operators. Mainly used for GUI purposes. Other operators should use the
	 * method {@link #getOperator(int i)} which only delivers enabled inner operators.
	 */
	@Deprecated
	public Operator getOperatorFromAll(int i) {
		throw new UnsupportedOperationException(
				"OperatorChain.getOperatorFromAll(int) is deprecated. Try getSubprocess(int).getOperators()");
	}

	/**
	 * Returns the index of the given operator in the list of children. If useDisabled is true,
	 * disabled operators are also used for index calculations.
	 */
	@Deprecated
	public int getIndexOfOperator(Operator operator, boolean useDisabled) {
		throw new UnsupportedOperationException(
				"OperatorChain.getOperatorFromAll(int) is deprecated. Try getSubprocess(int).getOperators()");
	}

	/**
	 * Returns the result of the super method if this operator does not have a parent. Otherwise
	 * this method returns true if it is enabled and the parent is also enabled.
	 */
	@Override
	public boolean isEnabled() {
		return super.isEnabled();
	}

	/** Invokes the super method and the method for all children. */
	@Override
	public void processStarts() throws OperatorException {
		ListenerTools.informAllAndThrow(x -> super.processStarts(), Arrays.asList(subprocesses), ExecutionUnit::processStarts);
	}

	/** Invokes the super method and the method for all children. */
	@Override
	public void processFinished() throws OperatorException {
		ListenerTools.informAllAndThrow(x -> super.processFinished(), Arrays.asList(subprocesses), ExecutionUnit::processFinished);
	}

	// -------------------- implemented abstract methods

	/**
	 * Clears all sinks of all inner processes
	 */
	protected void clearAllInnerSinks() {
		for (ExecutionUnit subprocess : subprocesses) {
			subprocess.getInnerSinks().clear(Port.CLEAR_DATA);
		}
	}

	@Override
	public void doWork() throws OperatorException {
		for (ExecutionUnit subprocess : subprocesses) {
			subprocess.execute();
		}
	}

	@Override
	public void freeMemory() {
		super.freeMemory();
		for (ExecutionUnit unit : subprocesses) {
			unit.freeMemory();
		}
	}

	// --------------------------------------------------------------------------------

	/**
	 * This method invokes the additional check method for each child. Subclasses which override
	 * this method to perform a check should also invoke super.performAdditionalChecks()!
	 */
	@Override
	protected void performAdditionalChecks() {
		super.performAdditionalChecks();
		for (ExecutionUnit subprocess : subprocesses) {
			for (Operator o : subprocess.getOperators()) {
				if (o.isEnabled()) {
					o.performAdditionalChecks();
				}
			}
		}
	}

	/**
	 * Will throw an exception if a non optional property has no default value and is not defined by
	 * user.
	 */
	@Override
	public int checkProperties() {
		int errorCount = super.checkProperties();
		for (ExecutionUnit subprocess : subprocesses) {
			for (Operator o : subprocess.getOperators()) {
				if (o.isEnabled()) {
					errorCount += o.checkProperties();
				}
			}
		}
		return errorCount;
	}

	/**
	 * Will count an the number of deprecated operators, i.e. the operators which
	 * {@link #getDeprecationInfo()} method does not return null. Returns the total number of
	 * deprecations.
	 */
	@Override
	public int checkDeprecations() {
		int deprecationCount = super.checkDeprecations();
		for (ExecutionUnit subprocess : subprocesses) {
			for (Operator o : subprocess.getOperators()) {
				deprecationCount += o.checkDeprecations();
			}
		}
		return deprecationCount;
	}

	/**
	 * Checks if the number of inner operators lies between MinInnerOps and MaxInnerOps. Performs
	 * the check for all operator chains which are children of this operator chain.
	 *
	 * @deprecated As of RM, this is implicit in the subprocesses.
	 */
	@Deprecated
	public int checkNumberOfInnerOperators() {
		return 0;
	}

	/**
	 * Returns this OperatorChain's name and class and the process trees of the inner operators.
	 */
	@Override
	protected String createProcessTree(int indent, String selfPrefix, String childPrefix, Operator markOperator, String mark) {
		StringBuilder treeBuilder = new StringBuilder(super.createProcessTree(indent, selfPrefix, childPrefix, markOperator,
				mark));
		for (int i = 0; i < subprocesses.length; i++) {
			List<String> processTreeList = subprocesses[i].createProcessTreeList(indent, childPrefix + "+- ", childPrefix
					+ (i < subprocesses.length - 1 ? "|  " : "   "), markOperator, mark);
			for (String entry : processTreeList) {
				treeBuilder.append(Tools.getLineSeparator());
				treeBuilder.append(entry);
			}
		}
		return treeBuilder.toString();
	}

	@Override
	public List<String> createProcessTreeList(int indent, String selfPrefix, String childPrefix, Operator markOperator,
			String mark) {
		List<String> treeList = super.createProcessTreeList(indent, selfPrefix, childPrefix, markOperator, mark);
		for (int i = 0; i < subprocesses.length; i++) {
			treeList.addAll(subprocesses[i].createProcessTreeList(indent, childPrefix + "+- ", childPrefix
					+ (i < subprocesses.length - 1 ? "|  " : "   "), markOperator, mark));
		}
		return treeList;
	}

	public ExecutionUnit getSubprocess(int index) {
		return subprocesses[index];
	}

	public int getNumberOfSubprocesses() {
		return subprocesses.length;
	}

	/** Returns an immutable view of all subprocesses */
	public List<ExecutionUnit> getSubprocesses() {
		return Arrays.asList(subprocesses);
	}

	@Override
	protected void collectErrors(List<ProcessSetupError> errors) {
		super.collectErrors(errors);
		for (ExecutionUnit executionUnit : subprocesses) {
			for (Operator op : executionUnit.getOperators()) {
				op.collectErrors(errors);
			}
			for (Port port : executionUnit.getInnerSinks().getAllPorts()) {
				errors.addAll(port.getErrors());
			}
			for (Port port : executionUnit.getInnerSources().getAllPorts()) {
				errors.addAll(port.getErrors());
			}
		}
	}

	@Override
	public void clear(int clearFlags) {
		super.clear(clearFlags);
		for (ExecutionUnit executionUnit : subprocesses) {
			executionUnit.clear(clearFlags);
		}
	}

	@Override
	public void assumePreconditionsSatisfied() {
		super.assumePreconditionsSatisfied();
		for (ExecutionUnit executionUnit : subprocesses) {
			for (InputPort inputPort : executionUnit.getInnerSinks().getAllPorts()) {
				for (Precondition precondition : inputPort.getAllPreconditions()) {
					precondition.assumeSatisfied();
				}
			}
		}
	}

	@Override
	public void notifyRenaming(String oldName, String newName) {
		Arrays.stream(subprocesses).forEach(unit -> unit.getOperators().forEach(op -> op.notifyRenaming(oldName, newName)));
		super.notifyRenaming(oldName, newName);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Also all inner operators will be notified.
	 */
	@Override
	public void notifyReplacing(String oldName, Operator oldOp, String newName, Operator newOp) {
		Arrays.stream(subprocesses).forEach(unit -> unit.getOperators().forEach(op -> op.notifyReplacing(oldName, oldOp, newName, newOp)));
		super.notifyReplacing(oldName, oldOp, newName, newOp);
	}

	@Override
	protected void propagateDirtyness() {
		for (ExecutionUnit unit : subprocesses) {
			for (Operator op : unit.getOperators()) {
				op.propagateDirtyness();
			}
		}
	}

	@Override
	public void updateExecutionOrder() {
		for (ExecutionUnit unit : subprocesses) {
			unit.updateExecutionOrder();
		}
	}

	@Override
	protected Operator lookupOperator(String operatorName) {
		Operator result = super.lookupOperator(operatorName);
		if (result != null) {
			return result;
		} else {
			for (Operator child : getAllInnerOperators()) {
				if (child.getName().equals(operatorName)) {
					return child;
				}
			}
			return null;
		}
	}

	@Override
	public void walk(Visitor<Operator> visitor) {
		super.walk(visitor);
		for (ExecutionUnit unit : subprocesses) {
			for (Operator op : unit.getOperators()) {
				op.walk(visitor);
			}
		}
	}

}
