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

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.operator.io.AbstractDataReader;
import com.rapidminer.operator.io.AbstractDataReader.AttributeColumn;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;


/**
 * An editor to declare meta data information (attributes, value types, roles, ...) in a importing
 * wizard
 *
 * @author Tobias Malbrecht
 * @author Sebastian Loh (22.04.2010)
 */
public class MetaDataDeclarationEditor extends JPanel {

	private static final long serialVersionUID = -520323914589387512L;

	/**
	 * upper table, actually a simulation of an multi-row header for the previewTable
	 */
	private MetaDataTable metadataTable;

	/** the preview table which contains a preview of the data */
	private PreviewTable previewTable;

	/**
	 * Listener to recognize if a column margin of the
	 * {@link MetaDataDeclarationEditor#metadataTable} changed. Not very nice to declare it globally
	 * but doesn't work otherwise.
	 */
	TableColumnModelListener metadataColumnListener;

	private AbstractDataReader reader = null;

	private UpdateQueue updateQueue = new UpdateQueue("ImportWizardDataRefresher");

	/**
	 * The font color of an error cell.
	 */
	private static final Color BLUE = new Color(52, 86, 164); // new Color(217, 69, 69);

	/** the background color of error cells */
	private static final Color YELLOW = new Color(245, 223, 171); // new Color(255, 234, 128);

	private final Color backGroundGray;

	/** the row number of the role selection */
	private static final int ROLE_ROW = 3;

	/** the row number of the column selection editor (check boxes) */
	private static final int IS_SELECTED_ROW = 2;

	/** the row number of the rol type selection */
	private static final int VALUE_TYPE_ROW = 1;

	private static final int ATTRIBUTE_NAME_ROW = 0;

	// private boolean lock = false;

	/**
	 *
	 */
	public MetaDataDeclarationEditor(AbstractDataReader reader, final boolean showMetaDataEditor) {
		super(new BorderLayout());

		backGroundGray = this.getBackground();

		this.reader = reader;

		metadataTable = new MetaDataTable();

		previewTable = new PreviewTable();

		ExtendedJScrollPane metadataPane = new ExtendedJScrollPane(metadataTable);

		ExtendedJScrollPane dataPane = new ExtendedJScrollPane(previewTable) {

			private static final long serialVersionUID = 1L;

			// Only show column header if there is no MetaDataTable
			@Override
			public void setColumnHeaderView(Component view) {
				if (!showMetaDataEditor) {
					super.setColumnHeaderView(view);
				}
			} // alternative work around: dataPane.setColumnHeader(null);
		};

		metadataColumnListener = new TableColumnModelListener() {

			@Override
			public void columnSelectionChanged(ListSelectionEvent e) {}

			@Override
			public void columnRemoved(TableColumnModelEvent e) {}

			@Override
			public void columnMoved(TableColumnModelEvent e) {}

			@Override
			public void columnMarginChanged(ChangeEvent e) {
				assert previewTable.getColumnCount() == metadataTable.getColumnCount();
				for (int i = 0; i < previewTable.getColumnCount(); i++) {
					int columnwidth = metadataTable.getColumnModel().getColumn(i).getWidth();
					int oldwidth = previewTable.getColumnModel().getColumn(i).getPreferredWidth();
					if (oldwidth == columnwidth) {
						continue;
					}
					previewTable.getColumnModel().getColumn(i).setPreferredWidth(columnwidth);
				}
				previewTable.doLayout();
				previewTable.repaint();
			}

			@Override
			public void columnAdded(TableColumnModelEvent e) {}
		};

		if (showMetaDataEditor) {

			metadataPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

			// synchronize the the scroll bars:
			metadataPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			JScrollBar bar = metadataPane.getVerticalScrollBar();
			JScrollBar dummyBar = new JScrollBar() {

				private static final long serialVersionUID = 1L;

				@Override
				public void paint(Graphics g) {
					// Color c = getParent().getBackground();
					g.setColor(backGroundGray);
					g.fillRect(0, 0, this.getSize().width, this.getSize().height);
				}
			};

			dummyBar.setPreferredSize(bar.getPreferredSize());
			metadataPane.setVerticalScrollBar(dummyBar);

			final JScrollBar bar1 = metadataPane.getHorizontalScrollBar();
			JScrollBar bar2 = dataPane.getHorizontalScrollBar();
			bar2.addAdjustmentListener(new AdjustmentListener() {

				@Override
				public void adjustmentValueChanged(AdjustmentEvent e) {
					bar1.setValue(e.getValue());
				}
			});

			/*
			 * Synchronize the column margins if they are changed:
			 */

			// propagate metadataTable margin changes to previewTable:
			metadataTable.getColumnModel().addColumnModelListener(metadataColumnListener);

			metadataPane.setPreferredSize(new Dimension(400, 103)); // ugly
			this.add(metadataPane, BorderLayout.NORTH);
		}

		this.add(dataPane, BorderLayout.CENTER);
		this.doLayout();

		updateQueue.start();
	}

	public void setData(List<Object[]> data) {
		previewTable.setData(data);
		metadataTable.updateTableStructure();
	}

	/*
	 * =========================
	 *
	 * BEGIN PRIVATE CLASSES:
	 *
	 * ValueTypeTable ValueTypeModle ValueTypeCellEditor CheckBoxCellEditor
	 *
	 * DataTable DataModle DataCellEditor
	 *
	 * ========================
	 */

	private class MetaDataTable extends ExtendedJTable implements TableModelListener {

		private static final long serialVersionUID = 1L;

		private final MetaDataModel fixedHeaderModel = new MetaDataModel();

		private ValueTypeCellEditor valueTypeCellEditor = null;
		private ValueTypeCellEditor valueTypeCellRenderer = null;

		private ColumnSelectionCellEditor checkBoxCellEditor = null;
		private ColumnSelectionCellEditor checkBoxCellRenderer = null;

		private RoleSelectionCellEditor roleCellEditor = null;
		private RoleSelectionCellEditor roleCellRenderer = null;

		// is used to select or deselect all columns
		private ColumnSelectionCellEditor globalCheckBoxCellEditor = new ColumnSelectionCellEditor();

		private MetaDataTable() {
			super(null, false, false, false, false, false);

			valueTypeCellEditor = new ValueTypeCellEditor();
			valueTypeCellRenderer = new ValueTypeCellEditor();
			checkBoxCellEditor = new ColumnSelectionCellEditor();
			checkBoxCellRenderer = new ColumnSelectionCellEditor();
			roleCellEditor = new RoleSelectionCellEditor();
			roleCellRenderer = new RoleSelectionCellEditor();

			setModel(fixedHeaderModel);

			setDefaultRenderer(String.class, new DefaultTableCellRenderer() {

				private static final long serialVersionUID = 1L;

				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
						boolean hasFocus, int row, int column) {
					Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					// RowNo. column
					if (column == 0) {
						// comp =
						// table.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(table,
						// value, isSelected, hasFocus, row, column);
						if (row == IS_SELECTED_ROW) {
							return globalCheckBoxCellEditor.getTableCellEditorComponent(table, value, isSelected, row,
									column);
						}
						comp.setBackground(backGroundGray);
						return comp;
					} else {
						column--;  // decrease to not count static row column when accessing data
									// reader
						if (row == ATTRIBUTE_NAME_ROW) {
							comp.setForeground(Color.BLACK);
							if (!reader.hasParseErrorInColumn(column)) {
								comp.setBackground(Color.WHITE);
							} else {
								comp.setBackground(YELLOW);
							}

							if (!reader.getAttributeColumn(column).isActivated()) {
								comp.setForeground(Color.LIGHT_GRAY);
							}
							return comp;
						}
						if (row == VALUE_TYPE_ROW) {
							return valueTypeCellRenderer.getTableCellEditorComponent(table, value, isSelected, row,
									column + 1);
						}
						if (row == IS_SELECTED_ROW) {
							return checkBoxCellRenderer.getTableCellEditorComponent(table, value, isSelected, row,
									column + 1);
						}
						if (row == ROLE_ROW) {
							return roleCellRenderer.getTableCellEditorComponent(table, value, isSelected, row, column + 1);
						}
						return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column + 1);
					}
				}
			});

			setColumnSelectionAllowed(false);
			setRowSelectionAllowed(false);
			setCellSelectionEnabled(false);

			this.getModel().fireTableStructureChanged();
		}

		@Override
		public MetaDataModel getModel() {
			return fixedHeaderModel;
		}

		public void updateTableStructure() {
			this.getModel().fireTableStructureChanged();

			this.packColumn();
			// this.pack();
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					// fire the column margin changed event to sync
					// the column margins

					metadataColumnListener.columnMarginChanged(null);
				}
			});
		}

		@Override
		public JPopupMenu createPopupMenu() {
			JPopupMenu popUp = super.createPopupMenu();
			// JMenuItem deselect = new JMenuItem(new ResourceAction("wizard.deselect_all") {
			//
			// @Override
			// public void loggedActionPerformed(ActionEvent e) {
			// // TODO Auto-generated method stub
			//
			// }
			// });

			// TODO implement selectAll and deselectALL Columns MenuItems here
			// popUp.add(deselect);
			return popUp;
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			if (column == 0) { // RowNo.
				if (row == IS_SELECTED_ROW) {
					return globalCheckBoxCellEditor;
				}
				return super.getCellEditor();
			}
			column--;
			if (row == VALUE_TYPE_ROW) {
				return valueTypeCellEditor;
			}
			if (row == IS_SELECTED_ROW) {
				return checkBoxCellEditor;
			}
			if (row == ROLE_ROW) {
				return roleCellEditor;
			}
			// ATTRIBUTE_NAME_ROW
			return super.getCellEditor(row, column);
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			if (column == 0) { // RowNo.
				if (row == IS_SELECTED_ROW) {
					return true;
				}
				return false;
			}
			if (row <= Math.max(Math.max(Math.max(ATTRIBUTE_NAME_ROW, VALUE_TYPE_ROW), IS_SELECTED_ROW), ROLE_ROW)) {
				return true;
			}
			return false;
		}
	}

	/**
	 * @author Sebastian Loh (23.04.2010)
	 *
	 *         The table model for the fixed table that contains only the table header (attribute
	 *         names) and the attributes value type.
	 *
	 */
	private class MetaDataModel extends AbstractTableModel {

		private static final long serialVersionUID = -8096935282615030186L;

		@Override
		public int getColumnCount() {
			// return previewTable.getColumnCount();
			return reader.getColumnCount() + 1;
		}

		@Override
		public int getRowCount() {
			return Math.max(Math.max(VALUE_TYPE_ROW, IS_SELECTED_ROW), ROLE_ROW) + 1;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) { // RowNo.
				if (rowIndex == IS_SELECTED_ROW) {
					// return true for global column selection editor if at least one column is
					// selected.
					for (AttributeColumn col : reader.getAllAttributeColumns()) {
						if (col.isActivated()) {
							return true;
						}
					}
					return false;
				}
				return "";
			}
			columnIndex--;
			if (rowIndex == ATTRIBUTE_NAME_ROW) {
				return reader.getAttributeColumn(columnIndex).getName();
			}
			if (rowIndex == VALUE_TYPE_ROW) {
				return Ontology.VALUE_TYPE_NAMES[reader.getAttributeColumn(columnIndex).getValueType()];
			}
			if (rowIndex == IS_SELECTED_ROW) {
				return reader.getAttributeColumn(columnIndex).isActivated();
			}
			if (rowIndex == ROLE_ROW) {
				return reader.getAttributeColumn(columnIndex).getRole();
			}
			return null;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) { // RowNo.
				return "Row No.";
			}
			columnIndex--;
			// here the table header is implicitly set to the attributes name
			return Tools.getExcelColumnName(columnIndex);
			// return reader.getAttributeColumn(columnIndex).getName();
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			if (column == 0) { // RowNo.
				if (row == IS_SELECTED_ROW) {
					for (AttributeColumn col : reader.getAllAttributeColumns()) {
						col.activateColumn((Boolean) value);
					}
					repaint();
				}
				return;
			}
			column--;
			if (row == ATTRIBUTE_NAME_ROW) {
				reader.setAttributeNamesDefinedByUser(true);
				reader.getAttributeColumn(column).setName((String) value);
			}
			if (row == VALUE_TYPE_ROW) {
				// update only if its not the same value
				if (reader.getAttributeColumn(column).getValueType() != Ontology.ATTRIBUTE_VALUE_TYPE.mapName(value
						.toString())) {
					reader.getAttributeColumn(column).setValueType(Ontology.ATTRIBUTE_VALUE_TYPE.mapName(value.toString()));
				}
			}
			if (row == IS_SELECTED_ROW) {
				reader.getAttributeColumn(column).activateColumn((Boolean) value);
			}
			if (row == ROLE_ROW) {
				String role = (String) value;
				if (role.equals(AttributeColumn.REGULAR)) {
					reader.getAttributeColumn(column).setRole(role);
				} else {
					for (AttributeColumn attColumn : reader.getAllAttributeColumns()) {
						if (attColumn.getRole().equals(role)) {
							attColumn.setRole(AttributeColumn.REGULAR);
						}
					}
					reader.getAttributeColumn(column).setRole(role);
					fireTableDataChanged();
				}
			}
			repaint();
		}

		// private void updatePreview() {
		// updateQueue.execute(new Runnable() {
		// @Override
		// public void run() {
		// // long running task
		//
		// SwingUtilities.invokeLater(new Runnable() {
		// @Override
		// public void run() {
		// List<Object[]> result;
		// try {
		// result = reader.getPreviewAsList();
		//
		// MetaDataDeclarationEditor.this.previewTable.setData(result);
		// // fire the column margin changed event to sync
		// // the column margins
		// metadataColumnListener.columnMarginChanged(null);
		// metadataTable.repaint();
		// } catch (OperatorException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// });
		// }
		// });
		// }
	}

	private class ValueTypeCellEditor extends DefaultCellEditor {

		private static final long serialVersionUID = 7954919612214223430L;

		// DropDown menus in the second row to select the value type
		@SuppressWarnings("unchecked")
		public ValueTypeCellEditor() {
			super(new JComboBox<String>());
			ComboBoxModel<String> model = new DefaultComboBoxModel<String>() {

				private static final long serialVersionUID = 914764579359633239L;

				private String[] valueTypes;
				{
					int[] types = { Ontology.BINOMINAL, Ontology.NOMINAL, Ontology.INTEGER, Ontology.REAL,
							Ontology.DATE_TIME, Ontology.DATE, Ontology.TIME };
					valueTypes = new String[types.length];
					for (int i = 0; i < types.length; i++) {
						valueTypes[i] = Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(types[i]);
					}
				}

				@Override
				public String getElementAt(int index) {
					return valueTypes[index];
				}

				@Override
				public int getSize() {
					return valueTypes.length;
				}
			};
			((JComboBox<?>) super.getComponent()).setEnabled(true);
			((JComboBox<String>) super.getComponent()).setModel(model);

		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			Component comp = super.getTableCellEditorComponent(table, value, isSelected, row, column);
			if (!reader.getAttributeColumn(column - 1).isActivated()) {
				comp.setForeground(Color.LIGHT_GRAY);
			} else if (reader.hasParseErrorInColumn(column - 1)) {
				comp.setForeground(BLUE);
			} else {
				comp.setForeground(Color.BLACK);
			}
			return comp;
		}
	}

	private class ColumnSelectionCellEditor extends DefaultCellEditor implements TableCellEditor {

		private static final long serialVersionUID = 1L;

		private ColumnSelectionCellEditor() {
			super(new JCheckBox());
			((JCheckBox) (this.getComponent())).setSelected(true);
		}

		@Override
		public Component getTableCellEditorComponent(JTable arg0, Object arg1, boolean arg2, int row, int column) {
			((JCheckBox) (this.getComponent())).setSelected((Boolean) metadataTable.getValueAt(row, column));
			return ((this.getComponent()));
		}

		@Override
		public Object getCellEditorValue() {
			return ((JCheckBox) (this.getComponent())).isSelected();
		}
	}

	private class RoleSelectionCellEditor extends DefaultCellEditor {

		private static final long serialVersionUID = 6077812831224991517L;

		public RoleSelectionCellEditor() {
			super(new JComboBox<>(AbstractDataReader.ROLE_NAMES.toArray()));
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			Component comp = super.getTableCellEditorComponent(table, value, isSelected, row, column);
			if (!reader.getAttributeColumn(column - 1).isActivated()) {
				comp.setForeground(Color.LIGHT_GRAY);
			} else {
				comp.setForeground(Color.BLACK);
			}
			return comp;
		}
	}

	private class PreviewTable extends ExtendedJTable {

		private static final long serialVersionUID = 1L;

		private PreviewModel dataModel = new PreviewModel();

		private PreviewTable() {
			super(null, false, false, false, false, false);
			setModel(dataModel);

			// new cell renderer which paints the text gray if the column is not
			// activated for the import. It also alternates the background color
			// and
			// shows error cells
			setDefaultRenderer(String.class, new DefaultTableCellRenderer() {

				private static final long serialVersionUID = 1L;

				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
						boolean hasFocus, int row, int col) {
					Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
					if (col == 0) {
						if (reader.hasParseErrorInRow(Integer.parseInt((String) dataModel.getValueAt(row, 0)) - 1)) {
							comp.setForeground(BLUE);
							comp.setBackground(YELLOW);
						} else {
							comp.setForeground(Color.BLACK);
							comp.setBackground(backGroundGray);
						}
						return comp;
					}
					col--;
					if (reader.getAttributeColumn(col).isActivated()) {
						if (reader.hasParseError(col, Integer.parseInt((String) dataModel.getValueAt(row, 0)) - 1)) {
							// if (reader.isErrorTolerant()) {
							// comp.setForeground(Color.BLUE);
							// comp.setBackground(SwingTools.LIGHT_YELLOW);
							// } else{
							comp.setForeground(BLUE);
							comp.setBackground(YELLOW);
							// }
						} else {
							comp.setForeground(Color.BLACK);
							if (row % 2 == 0) {
								comp.setBackground(Color.WHITE);
							} else if (reader.getAttributeColumn(col).getRole().equals(AttributeColumn.REGULAR)) {
								comp.setBackground(SwingTools.LIGHTEST_BLUE);
							} else {
								comp.setBackground(SwingTools.LIGHTEST_YELLOW); // appears
								// as
								// light
								// red!
							}
						}

					} else {
						comp.setForeground(Color.LIGHT_GRAY);
						if (row % 2 == 0) {
							comp.setBackground(Color.WHITE);
						} else {
							comp.setBackground(SwingTools.LIGHTEST_BLUE);
						}
					}
					return comp;
				}

			});

			// columns are selectable
			setColumnSelectionAllowed(true);
			// rows are selectable
			setRowSelectionAllowed(true);
			// single cells are not selectable
			setCellSelectionEnabled(false);
		}

		public void setData(List<Object[]> data) {
			dataModel.setData(data);
		}

		// @Override
		// public TableCellRenderer getCellRenderer(int row, int column){
		// TableCellRenderer cellRenderer = super.getCellRenderer(row, col);
		// cellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
		// column)
		// return ;
		// }

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			TableCellEditor cellEditor = super.getCellEditor(row, column);
			return cellEditor;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		// fit to column margin and other actions does not work due the
		// synchronization of the two tables
		@Override
		public JPopupMenu createPopupMenu() {
			// return super.createPopupMenu();
			return new JPopupMenu();

		}
	}

	private class PreviewModel extends AbstractTableModel {

		private static final long serialVersionUID = -8096935282615030186L;

		private ArrayList<Object[]> data = null;

		private void setData(List<Object[]> data) {
			this.data = new ArrayList<Object[]>(data);
			this.fireTableStructureChanged();
		}

		@Override
		public int getColumnCount() {
			return reader.getColumnCount() + 1;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) {
				return "Row No.";
			}
			return reader.getAttributeColumn(columnIndex - 1).getName();
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		@Override
		public int getRowCount() {
			return data != null ? (data.size()) : 0;
		}

		@Override
		public Object getValueAt(int row, int column) {
			Object[] values = data.get(row);

			if (column == 0) {
				return values[column].toString();
			}

			if (column >= values.length) {
				return "";
			}
			int attributeType = reader.getAttributeColumn(column - 1).getValueType();

			try {
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributeType, Ontology.DATE)) {
					return Tools.formatDate((Date) values[column]);
				}
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributeType, Ontology.TIME)) {
					return Tools.formatTime((Date) values[column]);
				}
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributeType, Ontology.DATE_TIME)) {
					return Tools.formatDateTime((Date) values[column]);
				}
				return Tools.formatDateTime((Date) values[column]);
			} catch (ClassCastException e) {
				// do nothing, just return default value
			}
			// default value
			return values[column].toString();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		updateQueue.shutdown();
		super.finalize();
	}
}
