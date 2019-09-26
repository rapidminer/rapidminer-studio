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
package com.rapidminer.gui.operatormenu;

import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.ProcessTools;
import com.rapidminer.tools.container.Pair;


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
		MainFrame mainFrame = RapidMinerGUI.getMainFrame();
		List<Operator> selection = mainFrame.getSelectedOperators();
		if (selection.isEmpty()) {
			return;
		}
		Operator selectedOperator = selection.get(0);
		ExecutionUnit parent = selectedOperator.getExecutionUnit();
		if (parent == null) {
			return;
		}

		// remember source and sink connections so we can reconnect them later.
		Map<String, Port> inputPortMap = getConnectedPorts(selectedOperator.getOutputPorts(), OutputPort::getDestination);
		Map<String, Port> outputPortMap = getConnectedPorts(selectedOperator.getInputPorts(), InputPort::getSource);

		// copy parameters if possible
		Parameters oldParameters = selectedOperator.getParameters();
		Parameters newParameters = operator.getParameters();
		for (String key : oldParameters.getDefinedKeys()) {
			ParameterType newType = newParameters.getParameterType(key);
			// copy if parameter types match
			if (newType != null && oldParameters.getParameterType(key) != null && oldParameters.getParameterType(key).getClass() == newType.getClass()) {
				newParameters.setParameter(key, oldParameters.getParameterOrNull(key));
			}
		}

		int failedReconnects = 0;

		// copy children if possible
		if (selectedOperator instanceof OperatorChain && operator instanceof OperatorChain) {
			OperatorChain oldChain = (OperatorChain) selectedOperator;
			OperatorChain newChain = (OperatorChain) operator;
			int numCommonSubprocesses = Math.min(oldChain.getNumberOfSubprocesses(), newChain.getNumberOfSubprocesses());
			for (int i = 0; i < numCommonSubprocesses; i++) {
				failedReconnects += newChain.getSubprocess(i).stealOperatorsFrom(oldChain.getSubprocess(i));
			}
		}
		int oldPos = parent.getOperators().indexOf(selectedOperator);
		Process process = selectedOperator.getProcess();
		selectedOperator.remove();

		if (process != null) {
			// find actual new name within process
			String newName = ProcessTools.getNewName(process.getAllOperatorNames(), operator.getName());
			// inform parameters of update
			process.notifyReplacing(selectedOperator.getName(), selectedOperator, newName, operator);
		}

		parent.addOperator(operator, oldPos);

		// Rewire sources and sinks
		failedReconnects += rewirePorts(inputPortMap, operator.getOutputPorts());
		failedReconnects += rewirePorts(outputPortMap, operator.getInputPorts());

		// copy operator rectangle from old operator to the new one to make the swap in place
		ProcessRendererModel processModel = mainFrame.getProcessPanel().getProcessRenderer().getModel();
		Rectangle2D rect = processModel.getOperatorRect(selectedOperator);
		rect = new Rectangle2D.Double(rect.getX(), rect.getY(), rect.getWidth(),
				ProcessDrawUtils.calcHeighForOperator(operator));
		processModel.setOperatorRect(operator, rect);
		mainFrame.selectAndShowOperator(operator, true);

		if (failedReconnects > 0) {
			SwingTools.showVerySimpleErrorMessage("op_replaced_failed_connections_restored", failedReconnects);
		}
	}

	/**
	 * Gets a map of all connected ports to its connected counter part. The key is the name of the port found by the
	 * given {@link Ports} instance, the value is the actual {@link Port} object connected to it. The port name corresponds
	 * to a port belonging to the operator to be replaced.
	 *
	 * @since 9.3
	 */
	private <P extends Port> LinkedHashMap<String, Port> getConnectedPorts(Ports<P> ports, Function<P, Port> opposite) {
		return ports.getAllPorts().stream().filter(Port::isConnected).map(p -> getDisconnectedLockedPair(p, opposite.apply(p)))
				.collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (a, b) -> b, LinkedHashMap::new));
	}

	/**
	 * Gets a pair of connected {@link Port Ports}. Locks both ports, disconnects them and returns the a {@link Pair}
	 * with the first port's name and the second port.
	 *
	 * @see Port#lock()
	 * @since 9.3
	 */
	private Pair<String, Port> getDisconnectedLockedPair(Port port, Port other) {
		port.lock();
		other.lock();
		if (port instanceof OutputPort) {
			((OutputPort) port).disconnect();
		} else if (other instanceof OutputPort) {
			((OutputPort) other).disconnect();
		}
		return new Pair<>(port.getName(), other);
	}

	/**
	 * Connects all port pairs specified in the map using the {@link Ports port finder} to resolve ports by name. Returns the number
	 * of failed connections.
	 *
	 * @see Ports#getPortByName(String)
	 * @since 9.3
	 */
	private int rewirePorts(Map<String, ? extends Port> connectedPorts, Ports<?> ports) {
		int sum = 0;
		for (Entry<String, ? extends Port> e : connectedPorts.entrySet()) {
			Port p = ports.getPortByName(e.getKey());
			if (p == null) {
				sum++;
				continue;
			}
			Port q = e.getValue();
			if (p instanceof OutputPort) {
				((OutputPort) p).connectTo((InputPort) q);
			} else {
				((OutputPort) q).connectTo((InputPort) p);
			}
			p.unlock();
			q.unlock();
		}
		return sum;
	}
}
