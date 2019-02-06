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
package com.rapidminer.gui.processeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultRowSorter;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import com.rapidminer.Process;
import com.rapidminer.ProcessContext;
import com.rapidminer.gui.properties.celleditors.value.RepositoryLocationValueCellEditor;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.ViewToolBar;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.parameter.ParameterTypeRepositoryLocation;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;


/**
 * This provides a panel for entering a repository location.
 *
 * @author Simon Fischer
 */
class RepositoryLocationsEditor<T extends Ports<?>> extends JPanel {

	private final class RepositoryLocationTable extends JTable {

		private static final long serialVersionUID = -8662024609772376145L;
		private final LinkedList<RepositoryLocationValueCellEditor> editors = new LinkedList<RepositoryLocationValueCellEditor>();
		{
			this.setRowHeight(26);
			this.setBackground(Color.WHITE);
		}

		private RepositoryLocationTable(TableModel dm) {
			super(dm);
		}

		@Override
		public TableCellEditor getCellEditor(int row, int column) {
			if (column == 0) {
				return super.getCellEditor(row, column);
			} else {
				return editors.get(row);
			}
		}

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			if (column == 0 || row >= editors.size()) {
				return super.getCellRenderer(row, column);
			}
			return editors.get(row);
		}

		@Override
		public void tableChanged(TableModelEvent e) {
			updateEditorsAndRenderers();
			super.tableChanged(e);
		}

		private void updateEditorsAndRenderers() {
			if (editors != null) {
				editors.clear();
				int numberOfRows = table.getModel().getRowCount();
				for (int i = 0; i < numberOfRows; i++) {
					if (!model.isInput()) {
						editors.add(createEditor(i, true));
					} else {
						editors.add(createEditor(i));
					}

				}
			}
		}
	}

	private class RepositoryLocationTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private final boolean input;

		public boolean isInput() {
			return input;
		}

		private ProcessContext context;

		private Observer<ProcessContext> contextObserver = new Observer<ProcessContext>() {

			@Override
			public void update(Observable<ProcessContext> observable, ProcessContext arg) {
				fireTableStructureChanged();
			}
		};

		private RepositoryLocationTableModel(boolean input) {
			this.input = input;
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public int getRowCount() {
			if (context == null) {
				return 0;
			} else {
				return getLocations().size();
			}
		}

		private List<String> getLocations() {
			return input ? context.getInputRepositoryLocations() : context.getOutputRepositoryLocations();
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					return false;
				case 1:
					return true;
			}
			return false;
		}

		@Override
		public String getColumnName(int col) {
			switch (col) {
				case 0:
					return "Name";
				case 1:
					return "Location";
				default:
					throw new IndexOutOfBoundsException(col + " > 1");
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					String value = prefix + " " + (rowIndex + 1);
					if (rowIndex < ports.getNumberOfPorts()) {
						Port correspondingPort = ports.getPortByIndex(rowIndex);
						if (correspondingPort.isConnected()) {
							if (correspondingPort instanceof OutputPort) {
								return value + " (" + ((OutputPort) correspondingPort).getDestination() + ")";
							} else {
								return value + " (" + ((InputPort) correspondingPort).getSource() + ")";
							}
						} else {
							return value + " (disconnected)";
						}
					}
					return value;
				case 1:
					return getLocations().get(rowIndex);
				default:
					throw new IndexOutOfBoundsException(columnIndex + " > 1");
			}
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					break;
				case 1:
					if (value == null) {
						value = "";
					}
					if (input) {
						context.setInputRepositoryLocation(rowIndex, (String) value);
					} else {
						context.setOutputRepositoryLocation(rowIndex, (String) value);
					}
					break;
				default:
					throw new IndexOutOfBoundsException(columnIndex + " > 1");
			}
		}

		public void remove(int rowIndex) {
			if (input) {
				context.removeInputLocation(rowIndex);
			} else {
				context.removeOutputLocation(rowIndex);
			}
			fireTableRowsDeleted(rowIndex, rowIndex);
		}

		public void add() {
			add("");
		}

		public void add(String location) {
			if (input) {
				context.addInputLocation(location);
			} else {
				context.addOutputLocation(location);
			}
			fireTableRowsInserted(getLocations().size() - 1, getLocations().size() - 1);
		}

		public void setContext(ProcessContext context2) {
			if (this.context != null) {
				this.context.removeObserver(contextObserver);
			}
			this.context = context2;
			if (this.context != null) {
				this.context.addObserver(contextObserver, true);
			}
			fireTableStructureChanged();
		}

	};

	private static final long serialVersionUID = 1L;

	private final String prefix;

	private final RepositoryLocationTableModel model;

	private final JTable table;

	private transient Observer<Port> portObserver = new Observer<Port>() {

		@Override
		public void update(Observable<Port> observable, Port arg) {
			adaptModelToPorts();
		}
	};
	private T ports;

	private com.rapidminer.Process process;

	RepositoryLocationsEditor(boolean input, String i18nKey, String prefix) {
		this.prefix = prefix;
		this.model = new RepositoryLocationTableModel(input);
		this.table = new RepositoryLocationTable(model);
		this.table.setAutoCreateRowSorter(true);
		((DefaultRowSorter<?, ?>) this.table.getRowSorter()).setMaxSortKeys(1);
		setLayout(new BorderLayout());
		JScrollPane tablePane = new ExtendedJScrollPane(table);
		tablePane.setBorder(null);
		tablePane.getViewport().setBackground(Color.WHITE);
		add(tablePane, BorderLayout.CENTER);
		JLabel label = new ResourceLabel(i18nKey);
		label.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		ViewToolBar toolBar = new ViewToolBar();
		toolBar.add(label);
		toolBar.add(new ResourceAction(true, i18nKey + ".add_row") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				model.add();
			}
		}, ViewToolBar.RIGHT);
		toolBar.add(new ResourceAction(true, i18nKey + ".delete_row") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				int[] selected = table.getSelectedRows();
				for (int i = selected.length - 1; i >= 0; i--) {
					model.remove(selected[i]);
				}
			}
		}, ViewToolBar.RIGHT);
		toolBar.setBorder(null);
		add(toolBar, BorderLayout.NORTH);
	}

	private RepositoryLocationValueCellEditor createEditor(int index) {
		return createEditor(index, false);
	}

	private RepositoryLocationValueCellEditor createEditor(int index, boolean onlyWriteableLocations) {
		RepositoryLocationValueCellEditor editor = new RepositoryLocationValueCellEditor(
				new ParameterTypeRepositoryLocation(prefix + " " + (index + 1), prefix + " " + (index + 1), true, false,
						false, true, false, onlyWriteableLocations));
		editor.setOperator(process.getRootOperator());
		return editor;
	}

	void setData(ProcessContext context, Process process, T ports) {
		this.process = process;
		if (this.ports != null) {
			this.ports.removeObserver(portObserver);
		}

		this.ports = ports;
		if (this.ports != null) {
			this.ports.addObserver(portObserver, true);
		}

		model.setContext(context);
		adaptModelToPorts();
	}

	private void adaptModelToPorts() {
		while (model.getRowCount() < ports.getNumberOfPorts()) {
			model.add("");
		}
		for (int i = model.getRowCount() - 1; i >= ports.getNumberOfPorts(); i--) {
			String loc = model.getLocations().get(i);
			if (loc == null || loc.isEmpty()) {
				model.remove(i);
			}
		}
	}

}
