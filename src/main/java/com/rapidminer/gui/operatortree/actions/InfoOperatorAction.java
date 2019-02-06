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

import com.rapidminer.gui.dialog.OperatorInfoScreen;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;

import java.awt.event.ActionEvent;


/**
 * Start the corresponding action.
 * 
 * @author Ingo Mierswa
 */
public abstract class InfoOperatorAction extends ResourceAction {

	private static final long serialVersionUID = 1764142570608930118L;

	public InfoOperatorAction() {
		super(true, "operator_info");
		setCondition(OPERATOR_SELECTED, MANDATORY);
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		Operator selectedOperator = getOperator();
		if (selectedOperator != null) {
			OperatorInfoScreen infoScreen = new OperatorInfoScreen(selectedOperator);
			infoScreen.setVisible(true);
		}
	}

	protected abstract Operator getOperator();
}
