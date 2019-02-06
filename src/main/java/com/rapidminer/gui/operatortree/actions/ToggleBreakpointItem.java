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

import com.rapidminer.BreakpointListener;
import com.rapidminer.gui.actions.Actions;
import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessRootOperator;

import java.awt.event.ActionEvent;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class ToggleBreakpointItem extends ToggleAction {

	private static final long serialVersionUID = 1727841552148351670L;

	private final Actions actions;

	private final int position;

	{
		setCondition(OPERATOR_SELECTED, MANDATORY);
		setCondition(ROOT_SELECTED, DISALLOWED);
		setCondition(PROCESS_RENDERER_IS_VISIBLE, MANDATORY);
	}

	public ToggleBreakpointItem(final Actions actions, final int position) {
		super(true, "breakpoint_" + BreakpointListener.BREAKPOINT_POS_NAME[position]);

		this.actions = actions;
		this.position = position;
	}

	@Override
	public void actionToggled(ActionEvent e) {
		Operator op = this.actions.getFirstSelectedOperator();
		// don't allow breakpoints for ProcessRootOperator
		if (op != null && !(op instanceof ProcessRootOperator)) {
			op.setBreakpoint(position, !op.hasBreakpoint(position));
		}
		// compatibility clause to revoke breakpoints set before the fix above
		if (op != null && op instanceof ProcessRootOperator && op.hasBreakpoint(position)) {
			op.setBreakpoint(position, false);
		}

		// TODO: toggle (rather set to common state!) all operators would be nice
		// for (Operator op : this.actions.getSelectedOperators()) {
		// op.setBreakpoint(position, !op.hasBreakpoint(position));
		// }
	}
}
