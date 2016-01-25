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
package com.rapidminer.gui.operatortree.actions;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.Actions;
import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;
import com.rapidminer.tools.ParameterService;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JMenuItem;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class ToggleActivationItem extends ToggleAction {

	private static final long serialVersionUID = -7020868989225535479L;

	private final Actions actions;

	{
		setCondition(ROOT_SELECTED, DISALLOWED);
		setCondition(OPERATOR_SELECTED, MANDATORY);
	}

	public ToggleActivationItem(final Actions actions) {
		super(true, "enable_operator");
		this.actions = actions;
	}

	@Override
	public void actionToggled(ActionEvent e) {
		if (actions.getSelectedOperators().isEmpty()) {
			return;
		}
		boolean targetState = getTargetState();

		for (Operator op : actions.getSelectedOperators()) {
			op.setEnabled(targetState);
		}
		if (targetState == false
				&& !"false".equals(ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_DISCONNECT_ON_DISABLE))) {
			for (Operator op : actions.getSelectedOperators()) {
				List<Port> toUnlock = new LinkedList<Port>();
				try {
					// disconnect and pass through
					List<OutputPort> sources = new LinkedList<OutputPort>();
					for (InputPort in : op.getInputPorts().getAllPorts()) {
						if (in.isConnected() && in.getSource().getPorts().getOwner().getOperator().isEnabled()) {
							sources.add(in.getSource());
							toUnlock.add(in.getSource());
							in.getSource().lock();
						}
					}
					for (OutputPort in : sources) {
						in.disconnect();
					}
					// op.getInputPorts().disconnectAll();

					List<InputPort> destinations = new LinkedList<InputPort>();
					for (OutputPort out : op.getOutputPorts().getAllPorts()) {
						if (out.isConnected() && out.getDestination().getPorts().getOwner().getOperator().isEnabled()) {
							destinations.add(out.getDestination());
							toUnlock.add(out.getDestination());
							out.getDestination().lock();
						}
					}
					for (InputPort in : destinations) {
						in.getSource().disconnect();
					}
					// op.getOutputPorts().disconnectAll();

					for (OutputPort source : sources) {
						Iterator<InputPort> i = destinations.iterator();
						while (i.hasNext()) {
							InputPort dest = i.next();
							if (source.getMetaData() != null
									&& dest.isInputCompatible(source.getMetaData(), CompatibilityLevel.PRE_VERSION_5)) {
								source.connectTo(dest);
								i.remove();
								break;
							}
						}
					}
				} finally {
					for (Port port : toUnlock) {
						port.unlock();
					}
				}
			}
		}
	}

	public JMenuItem createMultipleActivationItem() {
		boolean targetState = getTargetState();
		String actionKey = targetState ? "enable_operator_multiple" : "disable_operator_multiple";
		return new JMenuItem(new ResourceAction(actionKey, actions.getSelectedOperators().size()) {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				actionToggled(e);
			}
		});
	}

	private boolean getTargetState() {
		Collection<Operator> ops = actions.getSelectedOperators();
		if (ops == null || ops.isEmpty()) {
			return false;
		}
		for (Operator op : ops) {
			if (op.isEnabled()) {
				return false;
			}
		}
		return true;
	}
}
