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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport;

import com.rapidminer.example.Attributes;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.wizards.WizardStep;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;

import java.awt.Component;
import java.util.LinkedList;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;


/**
 * @author Tobias Malbrecht
 */
public abstract class AttributeSelectionWizardStep extends WizardStep {

	private static final String[] ROLE_NAMES = new String[Attributes.KNOWN_ATTRIBUTE_TYPES.length];
	{
		// TODO clarify role names in Attributes.KNOWN_ATTRIBUTE_TYPES (regular vs. attribute)
		for (int i = 0; i < ROLE_NAMES.length; i++) {
			if (Attributes.KNOWN_ATTRIBUTE_TYPES[i].equals("attribute")) {
				ROLE_NAMES[i] = "regular";
			} else {
				ROLE_NAMES[i] = Attributes.KNOWN_ATTRIBUTE_TYPES[i];
			}
		}
	}

	private AttributeMetaData[] attributes;

	class RoleSelectionTableModel extends AbstractTableModel {

		private static final long serialVersionUID = -8756273955084001031L;

		public void setMetaData(final ExampleSetMetaData metaData) {
			attributes = new AttributeMetaData[metaData.getAllAttributes().size()];
			int i = 0;
			for (AttributeMetaData amd : metaData.getAllAttributes()) {
				attributes[i++] = amd;
			}
			fireTableStructureChanged();
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int columnIndex) {
			switch (columnIndex) {
				case 0:
					return "Name";
				case 1:
					return "Role";
			}
			return null;
		}

		@Override
		public int getRowCount() {
			return attributes != null ? attributes.length : 0;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					return attributes[rowIndex].getName();
				case 1:
					return attributes[rowIndex].isSpecial() ? attributes[rowIndex].getRole() : "regular";
			}
			return null;
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					// TODO integrate global renaming scheme
					String name = (String) value;
					if (name != null && !name.isEmpty()) {
						boolean existent = false;
						for (int i = 0; i < attributes.length; i++) {
							if (i != rowIndex && attributes[i].getName().equals(name)) {
								existent = true;
								break;
							}
						}
						attributes[rowIndex].setName(name + (existent ? ("_" + (rowIndex + 1)) : ""));
					}
					break;
				case 1:
					String role = (String) value;
					if ("regular".equals(role)) {
						attributes[rowIndex].setRegular();
					} else {
						for (int i = 0; i < attributes.length; i++) {
							if (attributes[i].isSpecial() && attributes[i].getRole().equals(role)) {
								attributes[i].setRegular();
							}
						}
						attributes[rowIndex].setRole(role);
					}
					break;
				default:
					return;
			}
			fireTableDataChanged();
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					return true;
				case 1:
					return true;
				default:
					return false;
			}
		}
	}

	class RoleSelectionTable extends JTable {

		private static final long serialVersionUID = -4636146637929280203L;

		private LinkedList<RoleSelectionEditor> editors = new LinkedList<RoleSelectionEditor>();

		class RoleSelectionEditor extends DefaultCellEditor implements TableCellRenderer {

			private static final long serialVersionUID = 6077812831224991517L;

			public RoleSelectionEditor(JComboBox<?> comboBox) {
				super(comboBox);
			}

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				return this.getTableCellEditorComponent(table, value, isSelected, row, column);
			}
		}

		public RoleSelectionTable() {
			super(new RoleSelectionTableModel());
			setRowHeight(28);
		}

		public void setMetaData(ExampleSetMetaData metaData) {
			((RoleSelectionTableModel) getModel()).setMetaData(metaData);
			updateEditorsAndRenderers();
		}

		public void updateEditorsAndRenderers() {
			if (editors != null) {
				editors.clear();
				int numberOfRows = getModel().getRowCount();
				for (int i = 0; i < numberOfRows; i++) {
					JComboBox<String> comboBox = new JComboBox<>(ROLE_NAMES);
					comboBox.setEditable(true);
					RoleSelectionEditor editor = new RoleSelectionEditor(comboBox);
					editors.add(editor);
				}
			}
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			switch (column) {
				case 1:
					return editors.get(row);
			}
			return super.getCellEditor(row, column);
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (column == 0) {
				return super.getCellRenderer(row, column);
			}
			return editors.get(row);
		}

		@Override
		public void tableChanged(TableModelEvent e) {
			updateEditorsAndRenderers();
			super.tableChanged(e);
		}
	};

	private RoleSelectionTable table = new RoleSelectionTable();

	public AttributeSelectionWizardStep(String key) {
		super(key);
	}

	public void setMetaData(ExampleSetMetaData metaData) {
		table.setMetaData(metaData);
	}

	@Override
	protected JComponent getComponent() {
		ExtendedJScrollPane pane = new ExtendedJScrollPane(table);
		pane.setBorder(ButtonDialog.createBorder());
		return pane;
	}
}
