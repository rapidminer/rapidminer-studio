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
package com.rapidminer.gui.security;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.Action;
import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.actions.ManagePasswordsAction;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.GlobalAuthenticator;


/**
 * The PasswordManger is a small dialog to manage all the passwords that were saved for different
 * url's. You can show your passwords and delete corresponding entries. A possibility to change the
 * username and password is also included.
 *
 * @author Miguel Buescher
 *
 */
public class PasswordManager extends ButtonDialog {

	public static final Action OPEN_WINDOW = new ManagePasswordsAction();

	private static final long serialVersionUID = 1L;
	private CredentialsTableModel credentialsModel;
	private Wallet clone;

	public PasswordManager() {

		super(ApplicationFrame.getApplicationFrame(), "password_manager", ModalityType.MODELESS, new Object[0]);
		this.clone = new Wallet(Wallet.getInstance());

		credentialsModel = new CredentialsTableModel(clone);
		final JTable table = new JTable(credentialsModel);
		table.setAutoCreateRowSorter(true);
		((DefaultRowSorter<?, ?>) table.getRowSorter()).setMaxSortKeys(1);
		JScrollPane scrollPane = new ExtendedJScrollPane(table);
		scrollPane.setBorder(null);
		JPanel main = new JPanel(new BorderLayout());
		main.add(scrollPane, BorderLayout.CENTER);

		ResourceAction removePasswordAction = new ResourceAction("password_manager_remove_row") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				int[] selectedTableRows = table.getSelectedRows();
				ArrayList<Integer> modelRows = new ArrayList<>(selectedTableRows.length);
				for (int i = 0; i <= selectedTableRows.length - 1; i++) {
					modelRows.add(table.getRowSorter().convertRowIndexToModel(selectedTableRows[i]));
				}
				Collections.sort(modelRows);
				for (int i = modelRows.size() - 1; i >= 0; i--) {
					credentialsModel.removeRow(modelRows.get(i));
				}
			}
		};

		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.add(makeButtonPanel(new JButton(removePasswordAction), makeOkButton("password_manager_save"),
				makeCancelButton()), BorderLayout.EAST);
		layoutDefault(main, buttonPanel, LARGE);
	}

	@Override
	protected void ok() {
		Wallet.setInstance(clone);
		clone.saveCache();
		GlobalAuthenticator.refreshProxyAuthenticators();
		super.ok();
	}
}
