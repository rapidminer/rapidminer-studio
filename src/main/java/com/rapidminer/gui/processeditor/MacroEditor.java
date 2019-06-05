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
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.rapidminer.ProcessContext;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.ViewToolBar;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.container.Pair;


/**
 *
 * @author Simon Fischer
 */
public class MacroEditor extends JPanel {

	private static final long serialVersionUID = 1L;

	private class MacroTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public int getRowCount() {
			if (context == null) {
				return 0;
			} else {
				return context.getMacros().size();
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		@Override
		public String getColumnName(int col) {
			switch (col) {
				case 0:
					return "Macro";
				case 1:
					return "Value";
				default:
					throw new IndexOutOfBoundsException(col + " > 1");
			}
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					return context.getMacros().get(rowIndex).getFirst();
				case 1:
					return context.getMacros().get(rowIndex).getSecond();
				default:
					throw new IndexOutOfBoundsException(columnIndex + " > 1");
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
				case 1:
					// changed to a new method which fires updates so RM notices a process context
					// change and visualizes it immediately
					context.updateMacroValue(rowIndex, columnIndex, aValue.toString());
					break;
				default:
					throw new IndexOutOfBoundsException(columnIndex + " > 1");
			}
		}

		private void fireAdd() {
			fireTableRowsInserted(context.getMacros().size() - 1, context.getMacros().size() - 1);
		}

		private void fireRemoved(int row) {
			fireTableRowsDeleted(row, row);
		}

		private void reset() {
			fireTableStructureChanged();
		}
	};

	private final Action ADD_MACRO_ACTION;

	private final Action REMOVE_MACRO_ACTION;

	private ProcessContext context;

	private final MacroTableModel macroModel = new MacroTableModel();

	private final JTable macroTable = new JTable(macroModel);
	{
		macroTable.setRowHeight(26);
		macroTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		macroTable.setAutoCreateRowSorter(true);
		((DefaultRowSorter<?, ?>) macroTable.getRowSorter()).setMaxSortKeys(1);
	}

	public MacroEditor(boolean embedded) {
		setLayout(new BorderLayout());
		ADD_MACRO_ACTION = new ResourceAction(embedded, "macros.add_macro") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				addMacro();
			}
		};
		REMOVE_MACRO_ACTION = new ResourceAction(embedded, "macros.delete_macro") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				removeMacros();
			}
		};

		if (embedded) {
			ViewToolBar toolBar = new ViewToolBar();
			JLabel label = new ResourceLabel("macros");
			label.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
			toolBar.add(label);
			toolBar.add(ADD_MACRO_ACTION, ViewToolBar.RIGHT);
			toolBar.add(REMOVE_MACRO_ACTION, ViewToolBar.RIGHT);
			toolBar.setBorder(null);
			add(toolBar, BorderLayout.NORTH);
		}

		JScrollPane tablePane = new ExtendedJScrollPane(macroTable);
		tablePane.getViewport().setBackground(Color.WHITE);
		tablePane.setBorder(null);
		add(tablePane, BorderLayout.CENTER);
	}

	private void addMacro() {
		int previousMacroCount = context.getMacros().size();
		context.addMacro(new Pair<>("", ""));

		// if an empty macro already existed, nothing will change, so we don't want to fire an update here
		if (context.getMacros().size() > previousMacroCount) {
			macroModel.fireAdd();
		}
	}

	private void removeMacros() {
		int[] selected = macroTable.getSelectedRows();
		for (int i = selected.length - 1; i >= 0; i--) {
			context.removeMacro(selected[i]);
			macroModel.fireRemoved(selected[i]);
		}
	}

	public void setContext(ProcessContext context) {
		this.context = context;
		macroModel.reset();
	}

	public static void showMacroEditorDialog(final ProcessContext context) {
		ButtonDialog dialog = new ButtonDialog(ApplicationFrame.getApplicationFrame(), "define_macros",
				ModalityType.APPLICATION_MODAL, new Object[] {}) {

			private static final long serialVersionUID = 2874661432345426452L;

			{
				MacroEditor editor = new MacroEditor(false);
				editor.setBorder(createBorder());
				JButton addMacroButton = new JButton(editor.ADD_MACRO_ACTION);
				JButton removeMacroButton = new JButton(editor.REMOVE_MACRO_ACTION);
				layoutDefault(editor, NORMAL, addMacroButton, removeMacroButton, makeOkButton());
			}

			@Override
			protected void ok() {
				super.ok();
			}
		};
		dialog.setVisible(true);
	}
}
