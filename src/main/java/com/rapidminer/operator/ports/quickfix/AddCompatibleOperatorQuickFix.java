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
package com.rapidminer.operator.ports.quickfix;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.dialog.NewOperatorDialog;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.CompatibilityLevel;


/**
 * @author Simon Fischer
 */
public class AddCompatibleOperatorQuickFix extends AbstractQuickFix {

	private final InputPort inputPort;
	private final Class<? extends IOObject> neededClass;

	public AddCompatibleOperatorQuickFix(InputPort inputPort, Class<? extends IOObject> clazz) {
		super(MAX_RATING - 1, false, "add_compatible", clazz.getSimpleName());
		this.inputPort = inputPort;
		this.neededClass = clazz;
	}

	@Override
	public void apply() {
		try {
			Operator oldOperator = inputPort.getPorts().getOwner().getOperator();
			Operator newOperator = NewOperatorDialog.selectMatchingOperator(RapidMinerGUI.getMainFrame().getActions(), null,
					neededClass, null, null);

			if (newOperator != null) {
				ExecutionUnit unit = inputPort.getPorts().getOwner().getConnectionContext();
				int index = unit.getIndexOfOperator(oldOperator);
				if (index == -1) {
					unit.addOperator(newOperator);
				} else {
					unit.addOperator(newOperator, unit.getIndexOfOperator(oldOperator));
				}
				if (RapidMinerGUI.getMainFrame().VALIDATE_AUTOMATICALLY_ACTION.isSelected()) {
					unit.autoWireSingle(newOperator, CompatibilityLevel.VERSION_5, true, true);
				}
			}
		} catch (OperatorCreationException e) {
		}
	}

}
