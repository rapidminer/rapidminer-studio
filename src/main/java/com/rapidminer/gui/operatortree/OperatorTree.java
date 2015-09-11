/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.operatortree;

import com.rapidminer.BreakpointListener;
import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.dnd.OperatorTreeTransferHandler;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.operatortree.actions.CollapseAllAction;
import com.rapidminer.gui.operatortree.actions.ExpandAllAction;
import com.rapidminer.gui.operatortree.actions.LockTreeStructureAction;
import com.rapidminer.gui.operatortree.actions.RenameOperatorAction;
import com.rapidminer.gui.operatortree.actions.ToggleShowDisabledItem;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.PrintingTools;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.components.ToolTipWindow;
import com.rapidminer.gui.tools.components.ToolTipWindow.TipProvider;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ProcessSetupError;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;


/**
 * Displays the process definition as a JTree. This is the one of the process views of the
 * RapidMiner GUI and can be used to edit processes. New operators can be added by selecting a new
 * operator from the context menu of the currently selected operator. This editor also supports cut
 * and paste and drag and drop.
 * 
 * Since version 5.0 this view was mainly replaced by the process flow view. See
 * {@link ProcessRendererView}.
 * 
 * @see com.rapidminer.gui.operatortree.ProcessTreeModel
 * @author Ingo Mierswa
 */
public class OperatorTree extends JTree implements TreeSelectionListener, TreeExpansionListener, MouseListener,
		ProcessEditor {

	private static final long serialVersionUID = -6934683725946634563L;

	// ======================================================================
	// Operator Menu Actions and Items
	// ======================================================================

	public final Action RENAME_OPERATOR_ACTION = new RenameOperatorAction(this, IconSize.SMALL);

	public final ToggleShowDisabledItem TOGGLE_SHOW_DISABLED = new ToggleShowDisabledItem(this, true);

	public transient final Action EXPAND_ALL_ACTION = new ExpandAllAction(this, IconSize.SMALL);

	public transient final Action COLLAPSE_ALL_ACTION = new CollapseAllAction(this, IconSize.SMALL);

	public transient final LockTreeStructureAction TOGGLE_STRUCTURE_LOCK_ACTION = new LockTreeStructureAction(this,
			IconSize.SMALL);

	/** The main frame. Used for conditional action updates and property table settings. */
	private final MainFrame mainFrame;

	/** The tree model of the operator tree. */
	private transient ProcessTreeModel treeModel;

	/**
	 * Indicates if the structure is locked. This means that the structure cannot be changed via
	 * drag and drop and only parameters can be changed.
	 */
	private boolean isStructureLocked = false;

	private final OperatorTreeTransferHandler transferHandler;

	// ======================================================================

	/** Creates a new operator tree. */
	public OperatorTree(MainFrame mainFrame) {
		super();
		this.mainFrame = mainFrame;

		((ResourceAction) mainFrame.getActions().TOGGLE_BREAKPOINT[BreakpointListener.BREAKPOINT_AFTER]).addToActionMap(
				this, WHEN_FOCUSED);
		((ResourceAction) mainFrame.getActions().TOGGLE_ACTIVATION_ITEM).addToActionMap(this, WHEN_FOCUSED);
		// getActionMap().put("toggleActivation",
		// mainFrame.getActions().TOGGLE_ACTIVATION_ITEM.getAction());
		// getActionMap().put("cutAction", mainFrame.getActions().CUT_ACTION);
		// getActionMap().put("copyAction", mainFrame.getActions().COPY_ACTION);
		// getActionMap().put("pasteAction", mainFrame.getActions().PASTE_ACTION);
		// the next three lines are necessary to overwrite the default behavior
		// for these key strokes
		// getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_X,
		// InputEvent.CTRL_MASK), "cutAction");
		// getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_C,
		// InputEvent.CTRL_MASK), "copyAction");
		// getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_V,
		// InputEvent.CTRL_MASK), "pasteAction");
		// getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_E,
		// InputEvent.CTRL_MASK), "toggleActivation");

		setCellRenderer(new OperatorTreeCellRenderer());
		setCellEditor(new OperatorTreeCellEditor(this));
		setEditable(true);
		setShowsRootHandles(true);
		addTreeSelectionListener(this);
		addTreeExpansionListener(this);
		addMouseListener(this);
		ToolTipManager.sharedInstance().registerComponent(this);
		// setToggleClickCount(5);

		// forces the tree to ask the nodes for the correct row heights
		// must also be invoked after LaF changes...
		setRowHeight(0);

		// DnD support
		setDragEnabled(true);
		transferHandler = new OperatorTreeTransferHandler(this);
		setTransferHandler(transferHandler);

		getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

		new ToolTipWindow(new TipProvider() {

			@Override
			public String getTip(Object o) {
				Operator op;
				if (o instanceof Operator) {
					op = (Operator) o;
				} else if (o instanceof ExecutionUnit) {
					op = ((ExecutionUnit) o).getEnclosingOperator();
				} else {
					return null;
				}
				StringBuilder b = new StringBuilder();
				b.append("<h3>").append(op.getOperatorDescription().getName()).append("</h3><p>");
				b.append(op.getOperatorDescription().getLongDescriptionHTML()).append("</p>");

				List<ProcessSetupError> errorList = op.getErrorList();
				if (!errorList.isEmpty()) {
					b.append("<h4>Errors:</h4><ul>");
					for (ProcessSetupError error : errorList) {
						b.append("<li>").append(error.getMessage()).append("</li>");
					}
					b.append("</ul>");
				}
				return b.toString();
			}

			@Override
			public Object getIdUnder(Point point) {
				TreePath path = getPathForLocation((int) point.getX(), (int) point.getY());
				if (path != null) {
					return path.getLastPathComponent();
				} else {
					return null;
				}
			}

			@Override
			public Component getCustomComponent(Object id) {
				return null;
			}
		}, this);
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

	public Object getSelectedNode() {
		TreePath path = getSelectionPath();
		if (path == null) {
			return null;
		} else {
			return path.getLastPathComponent();
		}
	}

	/**
	 * Returns true if the tree structure is currently locked for drag and drop and false otherwise.
	 */
	public boolean isStructureLocked() {
		return isStructureLocked;
	}

	/** Sets the current lock status for the drag and drop locking. */
	public void setStructureLocked(boolean locked) {
		this.isStructureLocked = locked;
		TOGGLE_STRUCTURE_LOCK_ACTION.updateIcon();
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

	/** Renames the currently selected operator. */
	public void renameOperator() {
		TreePath path = getSelectionPath();
		if (path != null) {
			// returns immediately... no refresh possible after this method
			startEditingAtPath(path);
		}
	}

	/** Toggles if disabled operators should be shown. */
	public void toggleShowDisabledOperators() {
		treeModel.setShowDisabledOperators(!treeModel.showDisabledOperators());
	}

	/**
	 * This method will be invoked after a user selection of an operator in the tree. Causes a
	 * property table update and an update of the conditional action container.
	 */
	@Override
	public void valueChanged(TreeSelectionEvent e) {
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
		int selRow = getRowForLocation(e.getX(), e.getY());
		TreePath selPath = getPathForLocation(e.getX(), e.getY());
		if (selRow != -1) {
			if (e.getClickCount() == 2) {
				evaluateDoubleClick(selRow, selPath);
				e.consume();
			}
		}
		evaluatePopup(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// if (selPath != null) {
		// setSelectionPath(selPath);
		// }
		// evaluatePopup(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TreePath selPath = getPathForLocation(e.getX(), e.getY());
		// if (selPath != null) {
		// setSelectionPath(selPath);
		// }
		evaluatePopup(e);
	}

	/** Removes existing breakpoints or add a new breakpoint after the currently selected operator. */
	private void evaluateDoubleClick(int row, TreePath path) {
		// setSelectionPath(path);
		for (Operator op : getSelectedOperators()) {
			if (op.hasBreakpoint()) {
				op.setBreakpoint(BreakpointListener.BREAKPOINT_BEFORE, false);
				op.setBreakpoint(BreakpointListener.BREAKPOINT_AFTER, false);
			} else {
				op.setBreakpoint(BreakpointListener.BREAKPOINT_AFTER, true);
			}
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
		mainFrame.getActions().addToOperatorPopupMenu(menu, RENAME_OPERATOR_ACTION);
		menu.addSeparator();
		// menu.add(TOGGLE_SHOW_DISABLED);
		menu.add(EXPAND_ALL_ACTION);
		menu.add(COLLAPSE_ALL_ACTION);
		// menu.add(TOGGLE_STRUCTURE_LOCK_ACTION);
		menu.addSeparator();
		String name = "Tree";
		if (mainFrame.getProcess().getProcessLocation() != null) {
			name = mainFrame.getProcess().getProcessLocation().getShortName();
		}
		menu.add(PrintingTools.makeExportPrintMenu(this, name));

		return menu;
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
		setSelectionPaths(paths);
	}

	protected OperatorTreeTransferHandler getOperatorTreeTransferHandler() {
		return transferHandler;
	}
}
