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
package com.rapidminer.gui.viewer;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import com.rapidminer.ObjectVisualizer;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.operator.clustering.Cluster;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.clustering.HierarchicalClusterModel;
import com.rapidminer.operator.clustering.HierarchicalClusterNode;
import com.rapidminer.report.Renderable;
import com.rapidminer.tools.ObjectVisualizerService;


/**
 * Visualizes clusters as a bookmark like tree.
 *
 * @author Michael Wurst, Ingo Mierswa
 *
 */
public class ClusterTreeVisualization extends JTree implements TreeSelectionListener, Renderable {

	private static final long serialVersionUID = 3994390578811027103L;

	private Object clusterModel;

	private static class ClusterTreeLeaf {

		private final String title;

		private final Object id;

		public ClusterTreeLeaf(String title, Object id) {
			this.title = title;
			this.id = id;
		}

		@Override
		public String toString() {
			return title;
		}

		/** Returns the id. */
		public Object getId() {
			return id;
		}

	}

	public ClusterTreeVisualization(HierarchicalClusterModel cm) {
		DefaultTreeModel model = new DefaultTreeModel(generateTreeModel(cm.getRootNode()));
		setModel(model);
		addTreeSelectionListener(this);
		this.clusterModel = cm;
		setBackgroundWhite();
	}

	public ClusterTreeVisualization(ClusterModel cm) {
		DefaultTreeModel model = new DefaultTreeModel(generateFlatModel(cm));
		setModel(model);
		addTreeSelectionListener(this);
		this.clusterModel = cm;
		setBackgroundWhite();
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		TreePath[] paths = getSelectionPaths();
		// If only one item has been selected, then change the text in the
		// description area
		if (paths == null) {
			return;
		}
		if (paths.length == 1) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
			if (!node.getAllowsChildren()) {
				ClusterTreeLeaf leaf = (ClusterTreeLeaf) node.getUserObject();
				ObjectVisualizer viz = ObjectVisualizerService.getVisualizerForObject(clusterModel);
				viz.startVisualization(leaf.getId());
			}
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

	@Override
	public void prepareRendering() {
		expandAll();
	}

	@Override
	public void finishRendering() {}

	@Override
	public int getRenderHeight(int preferredHeight) {
		return Math.max(getPreferredSize().height, preferredHeight);
	}

	@Override
	public int getRenderWidth(int preferredWidth) {
		return Math.max(getPreferredSize().width, preferredWidth);
	}

	@Override
	public void render(Graphics graphics, int width, int height) {
		setSize(width, height);
		paint(graphics);
	}

	/**
	 * Ensure white background for this tree.
	 */
	private void setBackgroundWhite() {
		setBackground(Colors.WHITE);
		setCellRenderer(new DefaultTreeCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public Color getBackgroundNonSelectionColor() {
				return Colors.WHITE;
			}
		});
	}

	private DefaultMutableTreeNode generateFlatModel(ClusterModel cm) {
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
		rootNode.setAllowsChildren(true);
		for (int i = 0; i < cm.getNumberOfClusters(); i++) {
			Cluster cl = cm.getCluster(i);
			DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(cl);
			newNode.setAllowsChildren(true);
			rootNode.add(newNode);
			for (Object exampleId : cl.getExampleIds()) {
				newNode.add(createLeaf(exampleId));
			}
		}
		return rootNode;
	}

	private DefaultMutableTreeNode generateTreeModel(HierarchicalClusterNode cl) {
		DefaultMutableTreeNode result = new DefaultMutableTreeNode(cl);
		result.setAllowsChildren(true);

		// Add sub clusters
		for (HierarchicalClusterNode subNode : cl.getSubNodes()) {
			result.add(generateTreeModel(subNode));
		}

		// Add objects
		for (Object exampleId : cl.getExampleIdsInSubtree()) {
			result.add(createLeaf(exampleId));
		}
		return result;
	}

	private MutableTreeNode createLeaf(Object id) {
		ObjectVisualizer viz = ObjectVisualizerService.getVisualizerForObject(clusterModel);
		String title = viz.getTitle(id);
		if (title == null) {
			if (id instanceof String) {
				title = (String) id;
			} else {
				title = ((Integer) id).toString();
			}
		}
		DefaultMutableTreeNode newLeaf = new DefaultMutableTreeNode(new ClusterTreeLeaf(title, id));
		newLeaf.setAllowsChildren(false);
		return newLeaf;
	}
}
