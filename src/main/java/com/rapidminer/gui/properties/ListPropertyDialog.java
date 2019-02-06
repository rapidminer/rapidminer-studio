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
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeList;


/**
 * A Dialog displaying a {@link ListPropertyTable}. This can be used to add new values to the
 * parameter list or change current values. Removal of values is also supported.
 *
 * @see com.rapidminer.gui.properties.ListPropertyTable
 * @author Ingo Mierswa, Simon Fischer, Tobias Malbrecht, Nils Woehler, Marius Helf
 */
public class ListPropertyDialog extends PropertyDialog {

	private static final long serialVersionUID = 1876607848416333390L;

	private boolean ok = false;

	private final ListPropertyTable2 listPropertyTable;

	private final List<String[]> parameterList;

	public ListPropertyDialog(final ParameterTypeList type, List<String[]> parameterList, Operator operator) {
		super(type, "list");
		this.parameterList = parameterList;
		listPropertyTable = new ListPropertyTable2(type, parameterList, operator);
		if (listPropertyTable.isEmpty()) {
			listPropertyTable.addRow();
		}
		JScrollPane scrollPane = new ExtendedJScrollPane(listPropertyTable);
		scrollPane.setBorder(null);
		layoutDefault(scrollPane, NORMAL, new JButton(new ResourceAction("list.add_row") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				listPropertyTable.addRow();
			}
		}), new JButton(new ResourceAction("list.remove_row") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				listPropertyTable.removeSelected();
			}
		}), makeOkButton("list_property_dialog_apply"), makeCancelButton());

		listPropertyTable.requestFocusForLastEditableCell();
	}

	@Override
	protected void ok() {
		ok = true;
		listPropertyTable.stopEditing();

		List<String[]> list = ((ListTableModel) listPropertyTable.getModel()).getParameterList();

		if (checkTableNames(list)) {
			listPropertyTable.storeParameterList(parameterList);
			dispose();
		} else {

			ConfirmDialog dialog = new ConfirmDialog(this, "missing_attribute_names", ConfirmDialog.OK_CANCEL_OPTION, false) {

				private static final long serialVersionUID = 1L;

				@Override
				protected JButton makeOkButton() {
					JButton okButton = new JButton(new ResourceAction("missing_attribute_names_continue") {

						private static final long serialVersionUID = -8187199234055845095L;

						@Override
						public void loggedActionPerformed(ActionEvent e) {
							returnOption = OK_OPTION;
							ok();
						}
					});
					getRootPane().setDefaultButton(okButton);
					return okButton;
				}

				@Override
				protected JButton makeCancelButton() {
					ResourceAction cancelAction = new ResourceAction("missing_attribute_names_dismiss") {

						private static final long serialVersionUID = -8387199234055845095L;

						@Override
						public void loggedActionPerformed(ActionEvent e) {
							returnOption = CANCEL_OPTION;
							cancel();
						}
					};
					JButton cancelButton = new JButton(cancelAction);
					getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
							KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
					getRootPane().getActionMap().put("CANCEL", cancelAction);

					return cancelButton;
				}
			};
			dialog.setVisible(true);
			int answer = dialog.getReturnOption();
			if (answer == ConfirmDialog.CANCEL_OPTION) {
				listPropertyTable.storeParameterList(parameterList);
				dispose();
			} else {
				// do nothing and just close dialog window
			}

		}
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

	private boolean checkTableNames(List<String[]> parameterList) {

		for (String[] names : parameterList) {
			// check if there is no attribute name given but an expression
			if (names[0].isEmpty() & !names[1].isEmpty()) {
				return false;
			}

		}

		return true;
	}
}
