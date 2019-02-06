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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;


/**
 * A table model wrapped around a {@link Wallet} used by the {@link PasswordManager} to edit user
 * credentials.
 *
 * @author Miguel Buescher
 *
 */
public class CredentialsTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	/** Bad luck if your future password consist of 14 stars */
	private static final String HIDDEN_PASSWORD = "**************";
	// column information
	private static final int COLUMN_USER_INDEX = 2;
	private static final int COLUMN_PASSWORD_INDEX = 3;
	private static final int COLUMN_COUNT = 4;

	private Wallet wallet;
	private List<String> listOfWalletKeys = new LinkedList<>();
	private Map<String, Boolean> modifiedPasswords = new HashMap<>();

	public CredentialsTableModel(Wallet wallet) {
		this.wallet = wallet;
	}

	@Override
	public int getColumnCount() {
		return COLUMN_COUNT;
		}

	@Override
	public int getRowCount() {
		return getWallet().size();
	}

	@Override
	@SuppressWarnings("deprecation")
	public Object getValueAt(int rowIndex, int columnIndex) {
		this.listOfWalletKeys = getWallet().getKeys();
		if (rowIndex >= listOfWalletKeys.size()) {
			return null;
		}
		// uses list of keys directly which may or may not contain ID attribute, therefore this call
		// is correct
		UserCredential userCredential = getWallet().getEntry(listOfWalletKeys.get(rowIndex));
		switch (columnIndex) {
			case 0:
				return getWallet().extractIdFromKey(listOfWalletKeys.get(rowIndex));
			case 1:
				return userCredential.getURL();
			case COLUMN_USER_INDEX:
				return userCredential.getUsername();
			case COLUMN_PASSWORD_INDEX:
				return HIDDEN_PASSWORD;
			default:
				throw new RuntimeException("No such column: " + columnIndex); // cannot happen
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == 0 || columnIndex == 1) {
			return false;
		}
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setValueAt(Object value, int row, int col) {
		this.listOfWalletKeys = getWallet().getKeys();
		// uses list of keys directly which may or may not contain ID attribute, therefore this call
		// is correct
		UserCredential userCredential = getWallet().getEntry(listOfWalletKeys.get(row));
		if (col == COLUMN_USER_INDEX) {
			userCredential.setUser((String) value);
		}
		if (col == COLUMN_PASSWORD_INDEX && !HIDDEN_PASSWORD.equals(((String) value).trim())) {
			modifiedPasswords.put(listOfWalletKeys.get(row), true);
			userCredential.setPassword(((String) value).toCharArray());
		}

		wallet.saveCache();
		fireTableCellUpdated(row, col);
	}

	@Override
	public String getColumnName(int column) {
		switch (column) {
			case 0:
				return "ID";
			case 1:
				return "URL";
			case COLUMN_USER_INDEX:
				return "Username";
			case COLUMN_PASSWORD_INDEX:
				return "Password";
			default:
				throw new RuntimeException("No such column: " + column); // cannot happen
		}
	}

	@SuppressWarnings("deprecation")
	public void removeRow(int index) {
		// uses list of keys directly which may or may not contain ID attribute, therefore this call
		// is correct
		getWallet().removeEntry(listOfWalletKeys.get(index));
		fireTableDataChanged();
	}

	@Override
	public Class<?> getColumnClass(int c) {
		if (getValueAt(0, c) == null) {
			return String.class;
		}
		return getValueAt(0, c).getClass();
	}

	private Wallet getWallet() {
		return wallet;
	}
}
