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

import com.rapidminer.Process;
import com.rapidminer.gui.tools.components.ToolTipWindow;
import com.rapidminer.gui.tools.components.ToolTipWindow.TipProvider;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError;

import java.awt.Component;
import java.awt.Point;
import java.util.List;

import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;


/**
 * This is an view on a given process. In contrast to the {@link OperatorTree}, this is just a view
 * that does neither allow editing of the process nor interacts with the current RapidMiner state.
 * So this can be used on processes different than the currently opened one.
 * 
 * @author Sebastian Land
 */
public class ProcessTree extends JTree {

	private static final long serialVersionUID = -1015256958117451381L;

	public ProcessTree(Process process) {
		setModel(new ProcessTreeModel(process.getRootOperator()));
		setEditable(false);
		setCellRenderer(new OperatorTreeCellRenderer());
		setCellEditor(new OperatorTreeCellEditor(this));
		setShowsRootHandles(true);

		ToolTipManager.sharedInstance().registerComponent(this);

		// forces the tree to ask the nodes for the correct row heights
		// must also be invoked after LaF changes...
		setRowHeight(0);

		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

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
}
