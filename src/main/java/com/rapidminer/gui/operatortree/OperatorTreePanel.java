/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.ViewToolBar;
import com.rapidminer.operator.Operator;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * @author Tobias Malbrecht
 */
public class OperatorTreePanel extends JPanel implements Dockable, ProcessEditor {

	private static final long serialVersionUID = -6121229143892782298L;

	private final OperatorTree operatorTree;

	public OperatorTreePanel(final MainFrame mainFrame) {
		operatorTree = new OperatorTree(mainFrame);

		ViewToolBar toolBar = new ViewToolBar();
		toolBar.add(mainFrame.REWIRE_RECURSIVELY);
		JToggleButton toggleAllBreakpointsButton = mainFrame.getActions().TOGGLE_ALL_BREAKPOINTS.createToggleButton();
		toggleAllBreakpointsButton.setText(null);
		toolBar.add(toggleAllBreakpointsButton);

		toolBar.add(operatorTree.EXPAND_ALL_ACTION, ViewToolBar.RIGHT);
		toolBar.add(operatorTree.COLLAPSE_ALL_ACTION, ViewToolBar.RIGHT);

		setLayout(new BorderLayout());
		add(toolBar, BorderLayout.NORTH);

		JScrollPane scrollPane = new ExtendedJScrollPane(operatorTree);
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);
	}

	private final DockKey DOCK_KEY = new ResourceDockKey("operator_tree");

	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	private Component component;

	@Override
	public Component getComponent() {
		if (component == null) {
			component = this;
		}
		return component;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	@Override
	public void processChanged(Process process) {
		operatorTree.processChanged(process);
	}

	@Override
	public void processUpdated(Process process) {
		operatorTree.processUpdated(process);
	}

	@Override
	public void setSelection(List<Operator> selection) {
		operatorTree.setSelection(selection);
	}

	public OperatorTree getOperatorTree() {
		return operatorTree;
	}
}
