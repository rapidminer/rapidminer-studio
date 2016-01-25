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
package com.rapidminer.gui.security;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;


/**
 * The PasswordManger is a small dialog to manage all the passwords that were saved for different
 * url's. You can show your passwords and delete corresponding entries. A possibility to change the
 * username and password is also included.
 *
 * @author Miguel Buescher
 *
 */
public class PasswordManager extends ButtonDialog {

	public static final Action OPEN_WINDOW = new ResourceAction("password_manager") {

		{
			setCondition(EDIT_IN_PROGRESS, DONT_CARE);
		}

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			new PasswordManager().setVisible(true);
		}
	};

	private static final long serialVersionUID = 1L;
	private JButton showPasswordsButton;
	private CredentialsTableModel credentialsModel;
	private Wallet clone;

	public PasswordManager() {

		super(ApplicationFrame.getApplicationFrame(), "password_manager", ModalityType.MODELESS, new Object[] {});
		this.clone = Wallet.getInstance().clone();

		credentialsModel = new CredentialsTableModel(clone);
		final JTable table = new JTable(credentialsModel);
		table.setAutoCreateRowSorter(true);
		((DefaultRowSorter<?, ?>) table.getRowSorter()).setMaxSortKeys(1);
		JScrollPane scrollPane = new ExtendedJScrollPane(table);
		scrollPane.setBorder(null);
		JPanel main = new JPanel(new BorderLayout());
		final JPanel showpasswordPanel = new JPanel(new BorderLayout());
		main.add(scrollPane, BorderLayout.CENTER);

		ResourceAction showPasswordsAction = new ResourceAction("password_manager_showpasswords") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				updateButton();
			}
		};

		ResourceAction removePasswordAction = new ResourceAction("password_manager_remove_row") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				int[] rows = table.getSelectedRows();
				for (int i = 0; i <= rows.length - 1; i++) {
					credentialsModel.removeRow(rows[i]);
				}
			}
		};

		JPanel buttonPanel = new JPanel(new BorderLayout());
		showPasswordsButton = new JButton(showPasswordsAction);
		showpasswordPanel.add(makeButtonPanel(showPasswordsButton));
		buttonPanel.add(showpasswordPanel, BorderLayout.WEST);
		buttonPanel
		.add(makeButtonPanel(new JButton(removePasswordAction), makeOkButton("password_manager_save"),
				makeCancelButton()), BorderLayout.EAST);
		layoutDefault(main, buttonPanel, LARGE);
	}

	@Override
	protected void ok() {
		Wallet.setInstance(clone);
		clone.saveCache();
		super.ok();
	}

	private void updateButton() {
		credentialsModel.setShowPasswords(!credentialsModel.isShowPasswords());
		if (!credentialsModel.isShowPasswords()) {
			// The Show Password Button
			ResourceAction showPasswords = new ResourceAction("password_manager_showpasswords") {

				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					updateButton();
				}
			};
			showPasswordsButton.setAction(showPasswords);
		} else {
			// The Hide Password Button
			ResourceAction hidePasswords = new ResourceAction("password_manager_hidepasswords") {

				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					updateButton();
				}
			};
			showPasswordsButton.setAction(hidePasswords);
		}
	}
}
