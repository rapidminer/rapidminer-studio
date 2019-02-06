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
package com.rapidminer.gui.properties;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JScrollPane;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeEnumeration;


/**
 * A Dialog displaying an {@link EnumerationPropertyTable}. This can be used to add new values to
 * the parameter enumeration or change current values. Removal of values is also supported.
 *
 * @see com.rapidminer.gui.properties.EnumerationPropertyTable
 * @author Ingo Mierswa, Simon Fischer, Tobias Malbrecht
 */
public class EnumerationPropertyDialog extends PropertyDialog {

	private static final long serialVersionUID = 1876607848416333390L;

	private boolean ok = false;

	// private final EnumerationPropertyTable enumerationPropertyTable;
	private final ListPropertyTable2 enumerationPropertyTable;

	private final List<String> parameterList;

	public EnumerationPropertyDialog(final ParameterTypeEnumeration type, List<String> parameterList, Operator operator) {
		super(type, "list");
		this.parameterList = parameterList;
		enumerationPropertyTable = new ListPropertyTable2(type, parameterList, operator);
		JScrollPane scrollPane = new ExtendedJScrollPane(enumerationPropertyTable);
		scrollPane.setBorder(null);
		layoutDefault(scrollPane, NORMAL, new JButton(new ResourceAction("list.add_row") {

			private static final long serialVersionUID = 2765131572516935488L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				enumerationPropertyTable.addRow();
			}
		}), new JButton(new ResourceAction("list.remove_row") {

			private static final long serialVersionUID = 538193403731059601L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				enumerationPropertyTable.removeSelected();
			}
		}), makeOkButton(), makeCancelButton());
	}

	@Override
	protected void ok() {
		ok = true;
		enumerationPropertyTable.stopEditing();
		enumerationPropertyTable.storeParameterEnumeration(parameterList);
		dispose();
	}

	@Override
	protected void cancel() {
		ok = false;
		dispose();
	}

	@Override
	public boolean isOk() {
		return ok;
	}
}
