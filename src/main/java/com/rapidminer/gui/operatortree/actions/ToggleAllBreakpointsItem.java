/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import com.rapidminer.BreakpointListener;
import com.rapidminer.gui.actions.Actions;
import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;

import java.awt.event.ActionEvent;


/**
 * Start the corresponding action.
 * 
 * @author Tobias Malbrecht
 */
public class ToggleAllBreakpointsItem extends ToggleAction {

	private static final long serialVersionUID = 1727841552148351670L;

	private final Actions actions;

	public ToggleAllBreakpointsItem(final Actions actions) {
		super(true, "toggle_all_breakpoints");
		this.actions = actions;
	}

	@Override
	public void actionToggled(ActionEvent e) {
		Operator rootOp = actions.getRootOperator();
		if (rootOp != null) {
			for (Operator op : ((OperatorChain) rootOp).getAllInnerOperators()) {
				op.setBreakpoint(BreakpointListener.BREAKPOINT_BEFORE, false);
				op.setBreakpoint(BreakpointListener.BREAKPOINT_AFTER, isSelected());
			}
		}
	}
}
