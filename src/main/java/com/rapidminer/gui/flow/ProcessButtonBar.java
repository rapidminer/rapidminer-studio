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
package com.rapidminer.gui.flow;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.tools.ParentButtonPanel;
import com.rapidminer.operator.Operator;


/**
 *
 * @author Simon Fischer
 */
public class ProcessButtonBar extends ParentButtonPanel<Operator> {

	private static final long serialVersionUID = -2196273913282600609L;

	private ProcessParentButtonModel model = new ProcessParentButtonModel(null);

	public ProcessButtonBar(final MainFrame mainFrame) {
		setModel(model);
		addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Operator selectedNode = getSelectedNode();
				mainFrame.selectAndShowOperator(selectedNode, false);
			}
		});
	}

	public void setProcess(Process process) {
		model.setProcess(process);
	}
}
