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
package com.rapidminer.gui.new_plotter.gui;

import javax.swing.DropMode;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.gui.dnd.DragListener;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.gui.cellrenderer.PlotConfigurationTreeCellRenderer;
import com.rapidminer.gui.new_plotter.gui.dnd.DataTableColumnListTransferHandler;
import com.rapidminer.gui.new_plotter.gui.dnd.PlotConfigurationTreeTransferHandler;
import com.rapidminer.gui.new_plotter.gui.treenodes.PlotConfigurationTreeNode;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class PlotConfigurationTree extends JTree {

	private static final long serialVersionUID = 1L;

	public PlotConfigurationTree(PlotConfiguration plotConfiguration, DataTable dataTable,
			DataTableColumnListTransferHandler aTH) {
		super();
		expandAll();

		// forces the tree to ask the nodes for the correct row heights
		// must also be invoked after LaF changes...
		setRowHeight(0);

		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		setExpandsSelectedPaths(true);

		// DnD support
		setDragEnabled(true);
		setDropMode(DropMode.INSERT);

		// Rendering
		setShowsRootHandles(false);
		setBackground(Colors.WHITE);
		setCellRenderer(new PlotConfigurationTreeCellRenderer(aTH));
		putClientProperty("JTree.lineStyle", "Horizontal");

		createNewTreeModel(plotConfiguration);
	}

	/**
	 * Expands all paths in the tree.
	 *
	 * @see JTree#expandPath(TreePath)
	 */
	public void expandAll() {
		cancelEditing();
		final TreeModel tm = getModel();
		final Object root = tm.getRoot();

		/* nothing to expand, if no root */
		if (root != null) {
			expandAllPaths(new TreePath(root), tm);
		}
	}

	/**
	 * Opens all paths in the given node and all nodes below that.
	 *
	 * @param path
	 *            the tree path to the node to expand
	 * @see JTree#expandPath(TreePath)
	 */
	public void expandAllPaths(TreePath path) {
		cancelEditing();
		expandAllPaths(path, getModel());
	}

	/**
	 * Opens all paths in the given node and all nodes below that.
	 *
	 * @param path
	 *            the tree path to the node to expand
	 * @param treeModel
	 *            the tree model
	 * @see JTree#expandPath(TreePath)
	 */
	protected void expandAllPaths(TreePath path, TreeModel treeModel) {
		expandPath(path);
		final Object node = path.getLastPathComponent();
		final int n = treeModel.getChildCount(node);
		for (int index = 0; index < n; index++) {
			final Object child = treeModel.getChild(node, index);
			expandAllPaths(path.pathByAddingChild(child));
		}
	}

	@Override
	public void setTransferHandler(TransferHandler newHandler) {
		if (newHandler instanceof PlotConfigurationTreeTransferHandler) {
			DragListener cellRenderer = (DragListener) getCellRenderer();
			PlotConfigurationTreeTransferHandler plotConfigurationTreeTransferHandler = (PlotConfigurationTreeTransferHandler) newHandler;
			if (cellRenderer != null) {
				plotConfigurationTreeTransferHandler.removeDragListener(cellRenderer);
			}
			plotConfigurationTreeTransferHandler.addDragListener(cellRenderer);
		}
		super.setTransferHandler(newHandler);
	}

	public void createNewTreeModel(PlotConfiguration plotConfiguration) {

		PlotConfigurationTreeNode rootNode = new PlotConfigurationTreeNode(plotConfiguration);
		PlotConfigurationTreeModel treeModel = new PlotConfigurationTreeModel(rootNode, plotConfiguration, this);

		setModel(treeModel);

		treeModel.addTreeModelListener(new TreeModelListener() {

			@Override
			public void treeStructureChanged(TreeModelEvent e) {
				repaint();
				expandAll();
				TreePath selectionPath = getSelectionPath();
				if (selectionPath != null) {
					scrollPathToVisible(selectionPath);
				}
			}

			@Override
			public void treeNodesRemoved(TreeModelEvent e) {
				repaint();
				expandAll();
				TreePath selectionPath = getSelectionPath();
				if (selectionPath != null) {
					scrollPathToVisible(selectionPath);
				}
			}

			@Override
			public void treeNodesInserted(TreeModelEvent e) {
				repaint();
				expandAll();
				TreePath selectionPath = getSelectionPath();
				if (selectionPath != null) {
					scrollPathToVisible(selectionPath);
				}
			}

			@Override
			public void treeNodesChanged(TreeModelEvent e) {
				repaint();
				expandAll();
				TreePath selectionPath = getSelectionPath();
				if (selectionPath != null) {
					scrollPathToVisible(selectionPath);
				}
			}
		});

	}
}
