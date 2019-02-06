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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.operatortree.actions.CollapseAllAction;
import com.rapidminer.gui.operatortree.actions.ExpandAllAction;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.PrintingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;


/**
 * Displays the process definition as a JTree. No longer has editing capabilities.
 *
 * Since version 5.0 this view was mainly replaced by the process flow view. See
 * {@link ProcessRendererView}.
 *
 * @see com.rapidminer.gui.operatortree.ProcessTreeModel
 * @author Ingo Mierswa
 */
public class OperatorTree extends JTree implements TreeSelectionListener, TreeExpansionListener, MouseListener,
ProcessEditor {

	private static final long serialVersionUID = 1L;

	public transient final Action EXPAND_ALL_ACTION = new ExpandAllAction(this, IconSize.SMALL);

	public transient final Action COLLAPSE_ALL_ACTION = new CollapseAllAction(this, IconSize.SMALL);

	/** The main frame. Used for conditional action updates and property table settings. */
	private final MainFrame mainFrame;

	/** The tree model of the operator tree. */
	private transient ProcessTreeModel treeModel;

	private volatile boolean preventEvent = false;

	/** Creates a new operator tree. */
	public OperatorTree(MainFrame mainFrame) {
		super();
		this.mainFrame = mainFrame;

		setCellRenderer(new OperatorTreeCellRenderer());
		addTreeSelectionListener(this);
		addTreeExpansionListener(this);
		addMouseListener(this);

		// forces the tree to ask the nodes for the correct row heights
		// must also be invoked after LaF changes...
		setRowHeight(0);

		getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	}

	/** Returns the currently selected operator, i.e. the last node in the current selection path. */
	public List<Operator> getSelectedOperators() {
		TreePath[] paths = getSelectionPaths();
		if (paths == null) {
			return null;
		} else {
			List<Operator> selection = new LinkedList<Operator>();
			for (TreePath path : paths) {
				Object selected = path.getLastPathComponent();
				if (selected instanceof Operator) {
					selection.add((Operator) selected);
				} else if (selected instanceof ExecutionUnit) {
					selection.add(((ExecutionUnit) selected).getEnclosingOperator());
				}
			}
			return selection;
		}
	}

	/** Expands the complete tree. */
	public void expandAll() {
		int row = 0;
		while (row < getRowCount()) {
			expandRow(row);
			row++;
		}
	}

	/** Collapses the complete tree. */
	public void collapseAll() {
		int row = getRowCount() - 1;
		while (row >= 0) {
			collapseRow(row);
			row--;
		}
	}

	/**
	 * This method will be invoked after a user selection of an operator in the tree. Causes a
	 * property table update and an update of the conditional action container.
	 */
	@Override
	public void valueChanged(TreeSelectionEvent e) {
		if (preventEvent) {
			return;
		}

		if (mainFrame != null) {
			List<Operator> selectedOperators = getSelectedOperators();
			if (selectedOperators != null && !selectedOperators.isEmpty()) {
				mainFrame.selectOperators(selectedOperators);
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseClicked(MouseEvent e) {
		evaluatePopup(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {
		evaluatePopup(e);
	}

	@Override
	public void treeCollapsed(TreeExpansionEvent event) {
		Object last = event.getPath().getLastPathComponent();
		if (last instanceof Operator) {
			((Operator) last).setExpanded(false);
		} else if (last instanceof ExecutionUnit) {
			((ExecutionUnit) last).setExpanded(false);
		}
	}

	@Override
	public void treeExpanded(TreeExpansionEvent event) {
		Object last = event.getPath().getLastPathComponent();
		if (last instanceof Operator) {
			((Operator) last).setExpanded(true);
		} else if (last instanceof ExecutionUnit) {
			((ExecutionUnit) last).setExpanded(true);
		}
	}

	@Override
	public void processChanged(Process process) {
		this.treeModel = new ProcessTreeModel(process.getRootOperator());
		setModel(treeModel);
		setRootVisible(true);
		applyExpansionState(process.getRootOperator());
	}

	@Override
	public void processUpdated(Process process) {}

	@Override
	public void setSelection(List<Operator> selection) {
		TreePath[] paths = new TreePath[selection.size()];
		int i = 0;
		for (Operator op : selection) {
			paths[i++] = treeModel.getPathTo(op);
		}

		preventEvent = true;
		setSelectionPaths(paths);
		preventEvent = false;
	}

	private void applyExpansionState(Operator operator) {
		if (operator.isExpanded()) {
			expandPath(treeModel.getPathTo(operator));
			if (operator instanceof OperatorChain) {
				OperatorChain chain = (OperatorChain) operator;
				if (chain.getNumberOfSubprocesses() == 1) { // subprocesses hidden
					for (Operator op : chain.getSubprocess(0).getOperators()) {
						applyExpansionState(op);
					}
				} else {
					for (ExecutionUnit unit : chain.getSubprocesses()) {
						if (unit.isExpanded()) {
							if (unit.isExpanded()) {
								expandPath(treeModel.getPathTo(unit));
								for (Operator op : unit.getOperators()) {
									applyExpansionState(op);
								}
							} else {
								collapsePath(treeModel.getPathTo(unit));
							}
						}
					}
				}
			}
		} else {
			collapsePath(treeModel.getPathTo(operator));
		}
	}

	/**
	 * Checks if the given mouse event is a popup trigger and creates a new popup menu if necessary.
	 */
	private void evaluatePopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			createOperatorPopupMenu().show(this, e.getX(), e.getY());
			e.consume();
		}
	}

	/** Creates a new popup menu for the selected operator. */
	private JPopupMenu createOperatorPopupMenu() {
		JPopupMenu menu = new JPopupMenu();
		menu.add(EXPAND_ALL_ACTION);
		menu.add(COLLAPSE_ALL_ACTION);
		menu.addSeparator();
		String name = "Tree";
		if (mainFrame.getProcess().getProcessLocation() != null) {
			name = mainFrame.getProcess().getProcessLocation().getShortName();
		}
		menu.add(PrintingTools.makeExportPrintMenu(this, name));

		return menu;
	}
}
