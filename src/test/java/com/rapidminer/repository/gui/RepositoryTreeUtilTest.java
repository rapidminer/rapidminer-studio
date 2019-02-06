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
package com.rapidminer.repository.gui;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests for RepositoryTreeUtil
 *
 * @author Jonas Wilms-Pfau
 * @since 8.1.2
 */
public class RepositoryTreeUtilTest {

	private static JTree tree = new JTree();

	@BeforeClass
	public static void populateTree() {
		/**
		 * <pre>
		 *     a
		 *    | |
		 *    b b2
		 *    |
		 *    c
		 *
		 * a = 0
		 * b = 1
		 * b2 = 2
		 * c = 3
		 * </pre>
		 */
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("a");
		DefaultMutableTreeNode child1_0 = new DefaultMutableTreeNode("b");
		DefaultMutableTreeNode child1_1 = new DefaultMutableTreeNode("b2");
		root.add(child1_0);
		root.add(child1_1);
		DefaultMutableTreeNode child1_0_2_0 = new DefaultMutableTreeNode("c");
		child1_0.add(child1_0_2_0);

		((DefaultTreeModel) tree.getModel()).setRoot(root);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	}

	@Test
	public void retainRootSelections() {
		tree.setSelectionRows(new int[]{1, 2, 3});
		RepositoryTreeUtil util = new RepositoryTreeUtil();
		util.retainRootSelections(tree);
		util.restoreSelectionPaths(tree);
		Assert.assertArrayEquals(new int[]{1, 2}, tree.getSelectionRows());
	}

	@Test
	public void retainRootSelection() {
		tree.setSelectionRows(new int[]{0, 1, 2, 3});
		RepositoryTreeUtil util = new RepositoryTreeUtil();
		util.retainRootSelections(tree);
		util.restoreSelectionPaths(tree);
		Assert.assertArrayEquals(new int[]{0}, tree.getSelectionRows());
	}

	@Test
	public void retainRootSelectionDiscontiguous () {
		tree.setSelectionRows(new int[]{0, 3});
		RepositoryTreeUtil util = new RepositoryTreeUtil();
		util.retainRootSelections(tree);
		util.restoreSelectionPaths(tree);
		Assert.assertArrayEquals(new int[]{0}, tree.getSelectionRows());
	}
}