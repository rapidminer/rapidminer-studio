/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.gui.processeditor;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.dnd.DragListener;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.GroupTree;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;


/**
 * This container contains all available operators in a grouped view (tree). From here the current
 * group can be selected which displays the operators of the group in an operator list on the left
 * side. From here, new operators can be dragged into the operator tree.
 * 
 * @author Ingo Mierswa
 */
public class NewOperatorEditor extends JPanel implements TreeSelectionListener, Dockable {

	private static final long serialVersionUID = -8910332473638172252L;

	private final NewOperatorGroupTree newOperatorGroupTree;

	// private final OperatorList operatorList;

	public NewOperatorEditor() {
		this(null);
	}

	/**
	 * The drag listener will be registered at the operator tree and will receive drag start events
	 * if a drag has started and drag stopped events if dragging has stopped again
	 */
	public NewOperatorEditor(DragListener dragListener) {
		super(new BorderLayout());
		// will cause the tree half to keep fixed size during resizing
		setBorder(null);

		this.newOperatorGroupTree = new NewOperatorGroupTree(this);
		this.newOperatorGroupTree.getTree().addTreeSelectionListener(this);
		add(newOperatorGroupTree, BorderLayout.CENTER);

		if (dragListener != null) {
			newOperatorGroupTree.getOperatorTreeTransferhandler().addDragListener(dragListener);
		}
		// this.operatorList = new OperatorList();
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		TreePath path = e.getNewLeadSelectionPath();
		if (path != null) {
			if (path.getLastPathComponent() instanceof GroupTree) {
				setOperatorList((GroupTree) path.getLastPathComponent());
			}
		}
	}

	public void setOperatorList(GroupTree selectedTree) {
		Vector<OperatorDescription> descriptions = new Vector<OperatorDescription>();
		if (selectedTree != null) {
			Iterator<OperatorDescription> i = selectedTree.getOperatorDescriptions().iterator();
			while (i.hasNext()) {
				descriptions.add(i.next());
			}
			Collections.sort(descriptions);
		}
	}

	public static final String NEW_OPERATOR_DOCK_KEY = "new_operator";
	private final DockKey DOCK_KEY = new ResourceDockKey(NEW_OPERATOR_DOCK_KEY);

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	public boolean shouldAutoConnectNewOperatorsInputs() {
		return newOperatorGroupTree.shouldAutoConnectNewOperatorsInputs();
	}

	public boolean shouldAutoConnectNewOperatorsOutputs() {
		return newOperatorGroupTree.shouldAutoConnectNewOperatorsOutputs();
	}
}
