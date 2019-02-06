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

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.I18N;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 * @author Tobias Malbrecht
 */
public class OperatorTreePanel extends JPanel implements Dockable, ProcessEditor, PrintableComponent {

	private static final long serialVersionUID = 1L;

	private final OperatorTree operatorTree;

	public OperatorTreePanel(final MainFrame mainFrame) {
		operatorTree = new OperatorTree(mainFrame);

		setLayout(new BorderLayout());

		JScrollPane scrollPane = new ExtendedJScrollPane(operatorTree);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
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
	public Component getExportComponent() {
		return this;
	}

	@Override
	public String getExportIconName() {
		return I18N.getGUIMessage("gui.dockkey.operator_tree.icon");
	}

	@Override
	public String getExportName() {
		return I18N.getGUIMessage("gui.dockkey.operator_tree.name");
	}

	@Override
	public String getIdentifier() {
		Process process = RapidMinerGUI.getMainFrame().getProcess();
		if (process != null) {
			ProcessLocation processLocation = process.getProcessLocation();
			if (processLocation != null) {
				return processLocation.toString();
			}
		}
		return null;
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
