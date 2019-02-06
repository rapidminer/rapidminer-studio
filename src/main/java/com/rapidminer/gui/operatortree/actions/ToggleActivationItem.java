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
package com.rapidminer.gui.operatortree.actions;

import java.awt.event.ActionEvent;
import java.util.Collection;
import javax.swing.JMenuItem;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.Actions;
import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.ParameterService;


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

	@SuppressWarnings("deprecation")
	@Override
	public void actionToggled(ActionEvent e) {
		if (actions.getSelectedOperators().isEmpty()) {
			return;
		}
		boolean targetState = getTargetState();

		for (Operator op : actions.getSelectedOperators()) {
			op.setEnabled(targetState);
		}
		if (!targetState) {
			final String disableBehavior = ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_DISABLE_OPERATOR_CONNECTION_BEHAVIOR);
			if ("bridged".equals(disableBehavior)) {
				for (Operator op : actions.getSelectedOperators()) {
					ActionUtil.doPassthroughPorts(op);
				}
			} else if ("removed".equals(disableBehavior)) {
				for (Operator op : actions.getSelectedOperators()) {
					op.getInputPorts().disconnectAll();
					op.getOutputPorts().disconnectAll();
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
			public void loggedActionPerformed(ActionEvent e) {
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
