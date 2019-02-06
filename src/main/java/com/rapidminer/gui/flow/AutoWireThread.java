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

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.EditBlockingProgressThread;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;

import java.util.LinkedList;
import java.util.List;


/**
 * 
 * @author Simon Fischer
 * 
 */
public class AutoWireThread extends EditBlockingProgressThread {

	private List<Operator> newOperators;

	private AutoWireThread(List<Operator> newOperators) {
		super("auto_wiring");
		this.newOperators = newOperators;
	}

	@Override
	public void execute() {
		getProgressListener().setTotal(newOperators.size() + 1);
		for (int i = 0; i < newOperators.size(); i++) {
			Operator newOp = newOperators.get(i);
			getProgressListener().setCompleted(i + 1);
			newOp.getExecutionUnit().autoWireSingle(newOp, CompatibilityLevel.VERSION_5,
					RapidMinerGUI.getMainFrame().getNewOperatorEditor().shouldAutoConnectNewOperatorsInputs(),
					RapidMinerGUI.getMainFrame().getNewOperatorEditor().shouldAutoConnectNewOperatorsOutputs());
		}
		getProgressListener().complete();
	}

	public static void autoWireInBackground(List<Operator> newOperators, boolean firstMustBeWired) {
		if (!firstMustBeWired) {
			newOperators = new LinkedList<Operator>(newOperators);
			newOperators.remove(0);
		}
		if (RapidMinerGUI.getMainFrame().getNewOperatorEditor().shouldAutoConnectNewOperatorsInputs()
				|| RapidMinerGUI.getMainFrame().getNewOperatorEditor().shouldAutoConnectNewOperatorsOutputs()) {
			new AutoWireThread(newOperators).start();
		}
	}
}
