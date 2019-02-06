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

import com.rapidminer.gui.new_plotter.configuration.ValueSource;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.gui.treenodes.ValueSourceTreeNode;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class AbstractTreeSelectionDependentPanel extends AbstractConfigurationPanel implements
		TreeSelectionListener {

	private static final long serialVersionUID = 1L;

	private ValueSource currentValueSource = null;

	private ValueSourceTreeNode valueSourceNode = null;

	public AbstractTreeSelectionDependentPanel(JTree plotConfigurationTree, PlotInstance plotInstance) {
		super(plotInstance);
		plotConfigurationTree.addTreeSelectionListener(this);
	}

	/**
	 * @return the currentValueSource
	 */
	public ValueSource getSelectedValueSource() {
		return currentValueSource;
	}

	public ValueSourceTreeNode getSelectedValueSourceTreeNode() {
		return valueSourceNode;
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		TreePath newLeadSelectionPath = e.getNewLeadSelectionPath();
		if (newLeadSelectionPath == null) {
			return;
		}
		Object lastPathComponent = newLeadSelectionPath.getLastPathComponent();
		if (lastPathComponent instanceof ValueSourceTreeNode) {

			valueSourceNode = (ValueSourceTreeNode) lastPathComponent;
			// get the selected PVC
			ValueSource selectedValueSource = valueSourceNode.getUserObject();

			if (selectedValueSource == currentValueSource) {
				return;
			}

			// change current PlotValueConfig
			currentValueSource = selectedValueSource;

			adaptGUI();
		} else {
			currentValueSource = null;
			valueSourceNode = null;
		}

	}
}
