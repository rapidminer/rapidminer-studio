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
package com.rapidminer.gui.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.EditBlockingProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;


/**
 * Wires the current process (only visible parts).
 *
 * @author Simon Fischer, Tobias Malbrecht
 *
 */
public class AutoWireAction extends ResourceAction {

	private static final long serialVersionUID = -4597160351305617508L;


	public AutoWireAction() {
		super(true, "wire");
		setCondition(OPERATOR_SELECTED, MANDATORY);
	}

	/**
	 * @deprecated use {@link #AutoWireAction()} instead
	 */
	@Deprecated
	public AutoWireAction(MainFrame mainFrame) {
		this();
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		new EditBlockingProgressThread("auto_wiring") {

			@Override
			public void execute() {
				Operator op = RapidMinerGUI.getMainFrame().getFirstSelectedOperator();
				if (op == null) {
					return;
				}
				OperatorChain chain;
				if (op instanceof OperatorChain) {
					chain = (OperatorChain) op;
				} else {
					chain = op.getParent();
				}
				if (chain != null) {
					getProgressListener().setTotal(chain.getSubprocesses().size() + 1);
					int i = 1;
					for (ExecutionUnit unit : chain.getSubprocesses()) {
						getProgressListener().setCompleted(i++);
						unit.autoWire(CompatibilityLevel.VERSION_5, true, false);
					}
					getProgressListener().complete();
				}
			}
		}.start();
	}
}
