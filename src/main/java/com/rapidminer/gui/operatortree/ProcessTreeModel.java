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
package com.rapidminer.gui.operatortree;

import java.util.List;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.rapidminer.ProcessSetupListener;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.tools.LogService;


/**
 * A model view of a process that displays processes as individual nodes. Rewrite of the original
 * OperatorTreeModel.
 *
 * Nodes of this model can be {@link Operator}s and {@link ExecutionUnit}s. The latter are hidden
 * from the model if an {@link OperatorChain} contains only one {@link ExecutionUnit}.
 *
 * @author Simon Fischer
 *
 */
public class ProcessTreeModel implements TreeModel {

	private final EventListenerList listenerList = new EventListenerList();

	/** Defines whether or not disabled operators are shown by this model. */
	private boolean showDisabled = true;

	/** The root of the operator tree. */
	private final Operator root;

	private final ProcessSetupListener delegatingListener = new ProcessSetupListener() {

		@Override
		public void operatorAdded(Operator operator) {
			fireTreeNodesInserted(operator);
		}

		@Override
		public void operatorChanged(Operator operator) {
			if (operator.getProcess().getProcessState() != com.rapidminer.Process.PROCESS_STATE_RUNNING) {
				fireTreeNodesChanged(operator);
			}
		}

		@Override
		public void operatorRemoved(Operator operator, int oldIndex, int oldIndexAmongEnabled) {
			fireTreeNodesRemoved(operator, showDisabled ? oldIndex : oldIndexAmongEnabled);
		}

		@Override
		public void executionOrderChanged(ExecutionUnit unit) {
			fireTreeStructureChanged(unit);
		}
	};

	public ProcessTreeModel(Operator root) {
		this.root = root;
		root.getProcess().addProcessSetupListener(delegatingListener);
	}

	/**
	 * Returns either the list of enabled schildren of the given process or the list of all
	 * children, depending on {@link #showDisabled}.
	 */
	private List<Operator> getChildren(ExecutionUnit process) {
		if (showDisabled) {
			return process.getOperators();
		} else {
			return process.getEnabledOperators();
		}
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		listenerList.add(TreeModelListener.class, l);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		listenerList.remove(TreeModelListener.class, l);
	}

	@Override
	public Object getChild(Object parent, int index) {
		if (index == -1) {
			throw new IllegalArgumentException("Index -1 not allowed.");
		}
		if (parent instanceof OperatorChain) {
			OperatorChain chain = (OperatorChain) parent;
			if (chain.getNumberOfSubprocesses() == 1) {
				return getChildren(chain.getSubprocess(0)).get(index);
			} else {
				return ((OperatorChain) parent).getSubprocess(index);
			}
		} else if (parent instanceof ExecutionUnit) {
			return getChildren((ExecutionUnit) parent).get(index);
		} else {
			throw new IllegalArgumentException("Illegal tree node: " + parent);
		}
	}

	@Override
	public int getChildCount(Object parent) {
		if (parent instanceof OperatorChain) {
			OperatorChain chain = (OperatorChain) parent;
			if (chain.getNumberOfSubprocesses() == 1) {
				return getChildren(chain.getSubprocess(0)).size();
			} else {
				return ((OperatorChain) parent).getNumberOfSubprocesses();
			}
		} else if (parent instanceof ExecutionUnit) {
			return getChildren((ExecutionUnit) parent).size();
		} else {
			return 0;
		}
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if (parent instanceof OperatorChain) {
			OperatorChain chain = (OperatorChain) parent;
			if (chain.getNumberOfSubprocesses() == 1) {
				return getChildren(chain.getSubprocess(0)).indexOf(child);
			} else {
				return ((OperatorChain) parent).getSubprocesses().indexOf(child);
			}
		} else if (parent instanceof ExecutionUnit) {
			return getChildren((ExecutionUnit) parent).indexOf(child);
		} else {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.operatortree.ProcessTreeModel.child_is_no_child",
					new Object[] { child, parent });

			return -1;
		}
	}

	@Override
	public Object getRoot() {
		return root;
	}

	@Override
	public boolean isLeaf(Object node) {
		if (node instanceof OperatorChain) {
			if (((OperatorChain) node).getNumberOfSubprocesses() == 0) {
				return true;
			}
			if (((OperatorChain) node).getNumberOfSubprocesses() == 1) {
				return ((OperatorChain) node).getSubprocess(0).getNumberOfOperators() == 0;
			}
			return false;
		} else if (node instanceof ExecutionUnit) {
			return ((ExecutionUnit) node).getNumberOfOperators() == 0;
		} else {
			return true;
		}
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		Object leaf = path.getLastPathComponent();
		if (leaf instanceof Operator) {
			Operator op = (Operator) leaf;
			String desiredName = ((String) newValue).trim();
			if (desiredName.length() > 0) {
				if (desiredName.indexOf('.') >= 0) {
					JOptionPane.showMessageDialog(RapidMinerGUI.getMainFrame(),
							"Renaming not possible: operator names are now allowed to contain the character '.'",
							"Renaming failed", JOptionPane.WARNING_MESSAGE);
				} else {
					op.rename(desiredName);
				}
			}
		}
	}

	/** Creates TreePath leading to the specified ExecutionUnit. */
	TreePath getPathTo(ExecutionUnit process) {
		return getPathTo(process.getEnclosingOperator()).pathByAddingChild(process);
	}

	/** Creates TreePath leading to the specified operator. */
	public TreePath getPathTo(Operator operator) {
		if (operator.getParent() == null) {
			return new TreePath(operator);
		} else {
			TreePath pathToParent;
			if (operator.getParent().getNumberOfSubprocesses() == 1) {
				pathToParent = getPathTo(operator.getParent());
			} else {
				pathToParent = getPathTo(operator.getExecutionUnit());
			}
			return pathToParent.pathByAddingChild(operator);
		}
	}

	/** Creates an event that points to changes in the given operator. */
	private TreeModelEvent makeChangeEvent(Operator operator) {
		ExecutionUnit parent = operator.getExecutionUnit();
		if (parent != null) {
			// NOTE: In the tree model, the parent may be an ExecutionUnit or an OperatorChain (if
			// it has only one subprocess)
			// In both cases, the index of the operator is the same, so we don't have to treat these
			// cases separately.
			TreePath path = getPathTo(operator).getParentPath();
			int index = getChildren(operator.getExecutionUnit()).indexOf(operator);
			return new TreeModelEvent(this, path, new int[] { index }, new Object[] { operator });
		} else {
			return new TreeModelEvent(this, (TreePath) null, null, null);
		}
	}

	private void fireTreeNodesChanged(Operator operator) {
		TreeModelEvent e = makeChangeEvent(operator);
		if (e.getChildIndices() != null && e.getChildIndices()[0] != -1) { // otherwise the
			// operator is in
			// the state of
			// being removed and
			// has
			// triggered an
			// update while
			// dying.
			for (TreeModelListener l : listenerList.getListeners(TreeModelListener.class)) {
				try {
					l.treeNodesChanged(e);
				} catch (Exception ex) {
					//ignore
				}
			}
		}
	}

	private void fireTreeNodesInserted(Operator operator) {
		TreeModelEvent e = makeChangeEvent(operator);
		for (TreeModelListener l : listenerList.getListeners(TreeModelListener.class)) {
			try {
				l.treeNodesInserted(e);
			} catch (Exception ex) {
				//ignore
			}
		}
	}

	private void fireTreeNodesRemoved(Operator operator, int oldIndex) {
		TreePath path = getPathTo(operator).getParentPath();
		TreeModelEvent e = new TreeModelEvent(this, path, new int[] { oldIndex }, new Object[] { operator });
		for (TreeModelListener l : listenerList.getListeners(TreeModelListener.class)) {
			try {
				l.treeNodesRemoved(e);
			} catch (Exception ex) {
				//ignore
			}
		}
	}

	private void fireTreeStructureChanged(ExecutionUnit unit) {
		TreePath path = getPathTo(unit).getParentPath();
		TreeModelEvent e = new TreeModelEvent(this, path);
		for (TreeModelListener l : listenerList.getListeners(TreeModelListener.class)) {
			try {
				l.treeStructureChanged(e);
			} catch (Exception ex) {
				//ignore
			}
		}
	}

}
