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
package com.rapidminer.gui.operatormenu;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.tools.OperatorService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * An operator menu which can be used to replace the currently selected operator by one of the same
 * type. Simple operators can be by other simple operators or operator chains, operator chains can
 * only be replaced by other chains. This operator menu is available in the context menu of an
 * operator in tree view.
 * 
 * @author Ingo Mierswa, Simon Fischer, Tobias Malbrecht
 */
public class ReplaceOperatorMenu extends OperatorMenu {

	private static final long serialVersionUID = -663404687013352042L;

	protected ReplaceOperatorMenu(boolean onlyChains) {
		super("replace_operator", onlyChains);
	}

	@Override
	public void performAction(OperatorDescription description) {
		try {
			Operator operator = OperatorService.createOperator(description);
			replace(operator);
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("cannot_instantiate", e, description.getName());
		}
	}

	/** The currently selected operator will be replaced by the given operator. */
	private void replace(Operator operator) {
		List<Operator> selection = RapidMinerGUI.getMainFrame().getSelectedOperators();
		if (selection.isEmpty()) {
			return;
		}
		Operator selectedOperator = selection.get(0);
		ExecutionUnit parent = selectedOperator.getExecutionUnit();
		if (parent == null) {
			return;
		}

		// remember source and sink connections so we can reconnect them later.
		Map<String, InputPort> inputPortMap = new HashMap<String, InputPort>();
		Map<String, OutputPort> outputPortMap = new HashMap<String, OutputPort>();
		for (OutputPort source : selectedOperator.getOutputPorts().getAllPorts()) {
			if (source.isConnected()) {
				inputPortMap.put(source.getName(), source.getDestination());
				source.lock();
				source.getDestination().lock();
			}
		}
		for (InputPort sink : selectedOperator.getInputPorts().getAllPorts()) {
			if (sink.isConnected()) {
				outputPortMap.put(sink.getName(), sink.getSource());
				sink.lock();
				sink.getSource().lock();
			}
		}
		selectedOperator.getOutputPorts().disconnectAll();
		selectedOperator.getInputPorts().disconnectAll();

		int failedReconnects = 0;

		// copy children if possible
		if ((selectedOperator instanceof OperatorChain) && (operator instanceof OperatorChain)) {
			OperatorChain oldChain = (OperatorChain) selectedOperator;
			OperatorChain newChain = (OperatorChain) operator;
			int numCommonSubprocesses = Math.min(oldChain.getNumberOfSubprocesses(), newChain.getNumberOfSubprocesses());
			for (int i = 0; i < numCommonSubprocesses; i++) {
				ExecutionUnit oldSubprocess = oldChain.getSubprocess(i);
				ExecutionUnit newSubprocess = newChain.getSubprocess(i);
				failedReconnects += newSubprocess.stealOperatorsFrom(oldSubprocess);
			}
		}
		int oldPos = parent.getOperators().indexOf(selectedOperator);
		selectedOperator.remove();
		parent.addOperator(operator, oldPos);

		// Rewire sources and sinks
		for (Map.Entry<String, InputPort> entry : inputPortMap.entrySet()) {
			OutputPort mySource = operator.getOutputPorts().getPortByName(entry.getKey());
			if (mySource != null) {
				mySource.connectTo(entry.getValue());
				mySource.unlock();
				entry.getValue().unlock();
			} else {
				failedReconnects++;
			}
		}

		for (Map.Entry<String, OutputPort> entry : outputPortMap.entrySet()) {
			InputPort mySink = operator.getInputPorts().getPortByName(entry.getKey());
			if (mySink != null) {
				entry.getValue().connectTo(mySink);
				entry.getValue().unlock();
				mySink.unlock();
			} else {
				failedReconnects++;
			}
		}

		RapidMinerGUI.getMainFrame().selectOperator(operator.getParent());

		if (failedReconnects > 0) {
			SwingTools.showVerySimpleErrorMessage("op_replaced_failed_connections_restored", failedReconnects);
		}
		
	}
}
