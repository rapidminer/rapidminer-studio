 *
 * @author Miguel Buescher
 *
 */
public class CredentialsTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private boolean showPasswords;
	private Wallet wallet;
	private LinkedList<String> listOfWalletKeys = new LinkedList<String>();

	public CredentialsTableModel(Wallet wallet) {
		this.wallet = wallet;
	}

	@Override
	public int getColumnCount() {
		if (isShowPasswords()) {
			return 4;
		}
		return 3;
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
			case 2:
				return userCredential.getUsername();
			case 3:
				return new String(userCredential.getPassword());
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
		if (col == 2) {
			userCredential.setUser((String) value);
		}
		if (col == 3) {
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
			case 2:
				return "Username";
			case 3:
				return "Password";
			default:
				throw new RuntimeException("No such column: " + column); // cannot happen
		}
	}

	public void setShowPasswords(boolean showPasswords) {
		this.showPasswords = showPasswords;
		fireTableStructureChanged();
	}

	public boolean isShowPasswords() {
		return showPasswords;
	}

	@SuppressWarnings("deprecation")
	public void removeRow(int index) {
		// uses list of keys directly which may or may not contain ID attribute, therefore this call
		// is correct
		getWallet().removeEntry(listOfWalletKeys.get(index));
		fireTableDataChanged();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class getColumnClass(int c) {
		if (getValueAt(0, c) == null) {
			return String.class;
		}
		return getValueAt(0, c).getClass();
	}

	private Wallet getWallet() {
		return wallet;
	}
}
