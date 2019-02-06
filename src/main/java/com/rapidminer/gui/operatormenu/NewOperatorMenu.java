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

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;

import java.util.Collections;


/**
 * An operator menu which can be used to add a new operator to the currently selected operator. This
 * operator menu is available in the context menu of an operator in tree view.
 * 
 * @author Ingo Mierswa, Simon Fischer, Tobias Malbrecht
 */
public class NewOperatorMenu extends OperatorMenu {

	private static final long serialVersionUID = 7654028997343227244L;

	protected NewOperatorMenu() {
		super("new_operator", false);
	}

	@Override
	public void performAction(OperatorDescription description) {
		try {
			Operator operator = OperatorService.createOperator(description);
			RapidMinerGUI.getMainFrame().getActions().insert(Collections.singletonList(operator));
			ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_NEW_OPERATOR_MENU, "inserted", operator.getOperatorDescription().getKey());
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage("cannot_instantiate", e, description.getName());
		}
	}

}
