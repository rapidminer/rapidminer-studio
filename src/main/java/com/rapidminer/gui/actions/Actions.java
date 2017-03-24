/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.rapidminer.BreakpointListener;
import com.rapidminer.Process;
import com.rapidminer.ProcessListener;
import com.rapidminer.gui.ConditionalAction;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.actions.OperatorActionFactory.ResourceEntry;
import com.rapidminer.gui.dnd.OperatorTransferHandler;
import com.rapidminer.gui.flow.AutoWireThread;
import com.rapidminer.gui.operatormenu.OperatorMenu;
import com.rapidminer.gui.operatortree.actions.DeleteOperatorAction;
import com.rapidminer.gui.operatortree.actions.InfoOperatorAction;
import com.rapidminer.gui.operatortree.actions.ToggleActivationItem;
import com.rapidminer.gui.operatortree.actions.ToggleAllBreakpointsItem;
import com.rapidminer.gui.operatortree.actions.ToggleBreakpointItem;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.EditBlockingProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ProcessRootOperator;


/**
 * A process editor that enables/disables actions depending on the selection of operators.
 *
 * @author Simon Fischer, Tobias Malbrecht
 */
public class Actions implements ProcessEditor {

	public final Action INFO_OPERATOR_ACTION = new InfoOperatorAction() {

		private static final long serialVersionUID = 6758272768665592429L;

		@Override
		protected Operator getOperator() {
			return getFirstSelectedOperator();
		}
	};

	public final ToggleActivationItem TOGGLE_ACTIVATION_ITEM = new ToggleActivationItem(this);

	public final Action RENAME_OPERATOR_ACTION = new ResourceAction(true, "rename_in_processrenderer") {

		{
			setCondition(OPERATOR_SELECTED, MANDATORY);
		}

		private static final long serialVersionUID = -3104160320178045540L;

		@Override
		public void actionPerformed(ActionEvent e) {
			mainFrame.getProcessPanel().getProcessRenderer().rename(getFirstSelectedOperator());
		}
	};

	public final Action DELETE_OPERATOR_ACTION = new DeleteOperatorAction();

	public final ToggleBreakpointItem TOGGLE_BREAKPOINT[] = {
			new ToggleBreakpointItem(this, BreakpointListener.BREAKPOINT_BEFORE),
			new ToggleBreakpointItem(this, BreakpointListener.BREAKPOINT_AFTER) };

	public transient final ToggleAllBreakpointsItem TOGGLE_ALL_BREAKPOINTS = new ToggleAllBreakpointsItem(this);

	public transient final Action MAKE_DIRTY_ACTION = new ResourceAction(true, "make_dirty") {

		private static final long serialVersionUID = -1260942717363137733L;

		{
			setCondition(OPERATOR_SELECTED, MANDATORY);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			for (Operator selectedOperator : new LinkedList<Operator>(getSelectedOperators())) {
				selectedOperator.makeDirty();
			}
		}
	};

	private transient final Action SHOW_PROBLEM_ACTION = new ResourceAction(true, "show_potential_problem") {

		private static final long serialVersionUID = -1260942717363137733L;

		{
			setCondition(OPERATOR_SELECTED, MANDATORY);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			mainFrame.getProcessPanel().getOperatorWarningHandler().showOperatorWarning(getFirstSelectedOperator());
		}
	};

	private List<Operator> selection;

	private Process process;

	private final List<OperatorActionFactory> factories = new ArrayList<>();

	private final OperatorActionContext context = new OperatorActionContext() {

		@Override
		public List<Operator> getOperators() {
			return Collections.unmodifiableList(getSelectedOperators());
		}

		@Override
		public Operator getDisplayedChain() {
			return mainFrame.getProcessPanel().getProcessRenderer().getModel().getDisplayedChain();
		}

	};

	private final MainFrame mainFrame;

	private final BreakpointListener breakpointListener = new BreakpointListener() {

		@Override
		public void breakpointReached(Process process, Operator op, IOContainer iocontainer, int location) {
			enableActions();
		}

		@Override
		public void resume() {
			enableActions();
		}
	};
	private final ProcessListener processListener = new ProcessListener() {

		@Override
		public void processEnded(Process process) {
			enableActions();
			mainFrame.RUN_ACTION.setState(process.getProcessState());
		}

		@Override
		public void processFinishedOperator(Process process, Operator op) {}

		@Override
		public void processStartedOperator(Process process, Operator op) {}

		@Override
		public void processStarts(Process process) {
			enableActions();
		}
	};

	public Actions(MainFrame mainFrame) {
		this.mainFrame = mainFrame;
	}

	/** Creates a new popup menu for the selected operator. */
	public void addToOperatorPopupMenu(JPopupMenu menu, Action renameAction, Action... furtherActions) {
		final Operator op = getFirstSelectedOperator();
		final boolean singleSelection = getSelectedOperators().size() == 1;

		if (op != null && !singleSelection) {
			if (!(op instanceof ProcessRootOperator) && op.getParent() != null) {
				// enable / disable operator
				menu.add(TOGGLE_ACTIVATION_ITEM.createMultipleActivationItem());
			}
		}

		if (op != null && singleSelection) {
			if (mainFrame.getProcessPanel().getProcessRenderer().getModel().getDisplayedChain() != op) {
				menu.add(INFO_OPERATOR_ACTION);
				menu.add(TOGGLE_ACTIVATION_ITEM.createMenuItem());
				if (renameAction != null) {
					menu.add(renameAction);
				} else {
					menu.add(RENAME_OPERATOR_ACTION);
				}
				if (!op.getErrorList().isEmpty()) {
					menu.add(SHOW_PROBLEM_ACTION);
				}
				menu.addSeparator();
				if (op instanceof OperatorChain && ((OperatorChain) op).getAllInnerOperators().size() > 0) {
					menu.add(OperatorMenu.REPLACE_OPERATORCHAIN_MENU);
				} else {
					menu.add(OperatorMenu.REPLACE_OPERATOR_MENU);
				}
			}
		}

		// add new operator and building block menu
		if (mainFrame.getProcessPanel().getProcessRenderer().getModel().getDisplayedChain() == op) {
			menu.add(OperatorMenu.NEW_OPERATOR_MENU);
		}

		// populate menu with registered operator actions (if any)
		synchronized (factories) {
			for (OperatorActionFactory factory : factories) {
				List<ResourceEntry> entries = factory.create(context);
				if (!entries.isEmpty()) {
					menu.addSeparator();
				}
				for (ResourceEntry entry : entries) {
					if (entry.isMenu()) {
						menu.add(entry.getMenu());
					} else {
						menu.add(entry.getAction());
					}
				}
			}
		}

		menu.addSeparator();
		boolean enableCutCopy = mainFrame.getProcessPanel().getProcessRenderer().getModel().getDisplayedChain() != op;
		OperatorTransferHandler.installMenuItems(menu, enableCutCopy);

		// add further actions here
		if (furtherActions.length > 0) {
			menu.addSeparator();
			for (Action a : furtherActions) {
				if (a == null) {
					continue;
				}

				if (a instanceof ToggleAction) {
					menu.add(((ToggleAction) a).createMenuItem());
				} else {
					menu.add(a);
				}
			}
		}

		if (op != null && !(op instanceof ProcessRootOperator) && singleSelection) {
			menu.addSeparator();
			for (int i = 0; i < TOGGLE_BREAKPOINT.length; i++) {
				JMenuItem item = TOGGLE_BREAKPOINT[i].createMenuItem();
				menu.add(item);
			}
		}

	}

	public Operator getFirstSelectedOperator() {
		if (selection != null && !selection.isEmpty()) {
			return selection.get(0);
		} else {
			return null;
		}
	}

	public List<Operator> getSelectedOperators() {
		return selection;
	}

	/**
	 * Enables and disables all actions according to the current state (process running, operator
	 * selected...
	 */
	public void enableActions() {
		synchronized (process) {
			SwingTools.invokeLater(new Runnable() {

				@Override
				public void run() {
					enableActionsNow();
				}
			});
		}
		updateCheckboxStates();
	}

	private void enableActionsNow() {
		boolean[] currentStates = new boolean[ConditionalAction.NUMBER_OF_CONDITIONS];
		Operator op = getFirstSelectedOperator();
		if (op != null) {
			currentStates[ConditionalAction.OPERATOR_SELECTED] = true;
			if (op instanceof OperatorChain) {
				currentStates[ConditionalAction.OPERATOR_CHAIN_SELECTED] = true;
			}
			if (op.getParent() == null) {
				currentStates[ConditionalAction.ROOT_SELECTED] = true;
			} else {
				currentStates[ConditionalAction.PARENT_ENABLED] = op.getParent().isEnabled();
				if (op.getExecutionUnit().getNumberOfOperators() > 1) {
					currentStates[ConditionalAction.SIBLINGS_EXIST] = true;
				}
			}
		}

		int processState = process.getProcessState();
		currentStates[ConditionalAction.PROCESS_STOPPED] = processState == Process.PROCESS_STATE_STOPPED;
		currentStates[ConditionalAction.PROCESS_PAUSED] = processState == Process.PROCESS_STATE_PAUSED;
		currentStates[ConditionalAction.PROCESS_RUNNING] = processState == Process.PROCESS_STATE_RUNNING;
		currentStates[ConditionalAction.EDIT_IN_PROGRESS] = EditBlockingProgressThread.isEditing();
		currentStates[ConditionalAction.PROCESS_SAVED] = process.hasSaveDestination();
		currentStates[ConditionalAction.PROCESS_RENDERER_IS_VISIBLE] = mainFrame.getProcessPanel().getProcessRenderer()
				.isShowing();
		currentStates[ConditionalAction.PROCESS_RENDERER_HAS_UNDO_STEPS] = mainFrame.hasUndoSteps();
		currentStates[ConditionalAction.PROCESS_RENDERER_HAS_REDO_STEPS] = mainFrame.hasRedoSteps();
		ConditionalAction.updateAll(currentStates);
		updateCheckboxStates();

	}

	/** The currently selected operator will be deleted. */
	public void delete() {
		Operator parent = null;
		for (Operator selectedOperator : new LinkedList<Operator>(getSelectedOperators())) {
			if (parent == null) {
				parent = selectedOperator.getParent();
			}
			if (selectedOperator instanceof ProcessRootOperator) {
				return;
			}
			selectedOperator.remove();
		}
		mainFrame.selectOperator(parent);
	}

	/**
	 * The given operators will be inserted at the last position of the currently selected operator
	 * chain.
	 */
	public void insert(List<Operator> newOperators) {
		Object selectedNode = getSelectedOperator();
		if (selectedNode == null) {
			SwingTools.showVerySimpleErrorMessage("cannot_insert_operator");
			return;
		} else if (mainFrame.getProcessPanel().getProcessRenderer().getModel().getDisplayedChain() == selectedNode) {
			for (Operator newOperator : newOperators) {
				int index = mainFrame.getProcessPanel().getProcessRenderer().getProcessIndexUnder(
						mainFrame.getProcessPanel().getProcessRenderer().getModel().getCurrentMousePosition());
				if (index == -1) {
					index = 0;
				}
				((OperatorChain) selectedNode).getSubprocess(index).addOperator(newOperator);
			}
		} else {
			int i = 0;
			Operator selectedOperator = (Operator) selectedNode;
			ExecutionUnit process = selectedOperator.getExecutionUnit();
			int parentIndex = process.getOperators().indexOf(selectedOperator) + 1;
			for (Operator newOperator : newOperators) {
				process.addOperator(newOperator, parentIndex + i);
				i++;
			}
		}

		AutoWireThread.autoWireInBackground(newOperators, true);
		mainFrame.selectOperators(newOperators);
	}

	public Operator getSelectedOperator() {
		return getFirstSelectedOperator();
	}

	public Operator getRootOperator() {
		if (process != null) {
			return process.getRootOperator();
		}
		return null;
	}

	public Process getProcess() {
		return process;
	}

	@Override
	public void processChanged(Process process) {
		if (this.process != process) {
			if (this.process != null) {
				this.process.removeBreakpointListener(breakpointListener);
				this.process.getRootOperator().removeProcessListener(processListener);
			}
			this.process = process;
			enableActions();
			if (this.process != null) {
				this.process.addBreakpointListener(breakpointListener);
				this.process.getRootOperator().addProcessListener(processListener);
			}
		}
	}

	@Override
	public void processUpdated(Process process) {
		enableActions();
	}

	/**
	 * Registers an {@link OperatorActionFactory} to be included when compiling {@link Operator}
	 * specific {@link Action}s.
	 *
	 * @param factory
	 *            the factory
	 * @since 6.5
	 */
	public void register(OperatorActionFactory factory) {
		if (factory == null) {
			throw new IllegalArgumentException("factory must not be null");
		}
		synchronized (factories) {
			factories.add(factory);
		}
	}

	@Override
	public void setSelection(List<Operator> selection) {
		this.selection = selection;
		enableActions();
	}

	private void updateCheckboxStates() {
		Operator op = getSelectedOperator();
		if (op != null) {
			for (int pos = 0; pos < TOGGLE_BREAKPOINT.length; pos++) {
				TOGGLE_BREAKPOINT[pos].setSelected(op.hasBreakpoint(pos));
			}
			TOGGLE_ACTIVATION_ITEM.setSelected(op.isEnabled());
		}
	}
}
