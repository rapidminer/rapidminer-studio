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
package com.rapidminer.gui.renderer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.IOObject;
import com.rapidminer.report.Reportable;
import com.rapidminer.report.Tableable;


/**
 *
 * @author Simon Fischer
 *
 */
public class AnnotationsRenderer extends AbstractRenderer {

	private static final class AnnotationsTableModel extends AbstractTableModel {

		private final IOObject ioobject;
		private static final long serialVersionUID = 1L;

		private AnnotationsTableModel(IOObject ioobject) {
			this.ioobject = ioobject;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int columnIndex) {
			switch (columnIndex) {
				case 0:
					return "Key";
				case 1:
					return "Annotation";
				default:
					return "";
			}
		}

		@Override
		public int getRowCount() {
			return ioobject.getAnnotations().size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			final Annotations annotations = ioobject.getAnnotations();
			switch (columnIndex) {
				case 0:
					return annotations.getKeys().get(rowIndex);
				case 1:
					return annotations.getAnnotation(annotations.getKeys().get(rowIndex));
				default:
					return null;
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 1;
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			final Annotations annotations = ioobject.getAnnotations();
			switch (columnIndex) {
				case 0:
					throw new RuntimeException("Key collumn is immutable.");
				case 1:
					annotations.setAnnotation(annotations.getKeys().get(rowIndex), aValue.toString());
					break;
				default:
			}
		}

		private void addRow(String key) {
			ioobject.getAnnotations().setAnnotation(key, "");
			final int newSize = ioobject.getAnnotations().size();
			fireTableRowsInserted(newSize, newSize);
		}

		public void deleteRow(int row) {
			if (row >= 0) {
				ioobject.getAnnotations().removeAnnotation(ioobject.getAnnotations().getKeys().get(row));
				fireTableRowsDeleted(row, row);
			}
		}
	}

	@Override
	public String getName() {
		return "Annotations";
	}

	@Override
	public Reportable createReportable(Object renderable, IOContainer ioContainer, int desiredWidth, int desiredHeight) {
		final Annotations annotations = ((IOObject) renderable).getAnnotations();
		return new Tableable() {

			@Override
			public void finishReporting() {}

			@Override
			public String getCell(int row, int column) {
				switch (column) {
					case 0:
						return annotations.getKeys().get(row);
					case 1:
						return annotations.getAnnotation(annotations.getKeys().get(row));
					default:
						return null;
				}
			}

			@Override
			public String getColumnName(int index) {
				switch (index) {
					case 0:
						return "Key";
					case 1:
						return "Annotation";
					default:
						return null;
				}
			}

			@Override
			public int getColumnNumber() {
				return 2;
			}

			@Override
			public int getRowNumber() {
				return annotations.size();
			}

			@Override
			public boolean isFirstColumnHeader() {
				return false;
			}

			@Override
			public boolean isFirstLineHeader() {
				return true;
			}

			@Override
			public void prepareReporting() {}

		};
	}

	@Override
	public Component getVisualizationComponent(Object renderable, IOContainer ioContainer) {
		if (!(renderable instanceof IOObject)) {
			throw new RuntimeException("Can only display IOObjects");
		}
		final IOObject ioobject = (IOObject) renderable;

		JPanel component = new JPanel(new BorderLayout());
		component.setBackground(Colors.WHITE);
		final AnnotationsTableModel model = new AnnotationsTableModel(ioobject);
		final ExtendedJTable table = new ExtendedJTable(model, true);
		table.setRowHeight(PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);
		table.setRowHighlighting(true);

		JScrollPane scrollPane = new ExtendedJScrollPane(table);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(42, 10, 15, 10));
		scrollPane.setBackground(Colors.WHITE);
		scrollPane.getViewport().setBackground(Colors.WHITE);
		component.add(scrollPane, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttons.setOpaque(true);
		buttons.setBackground(Colors.WHITE);
		buttons.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 10));
		buttons.add(new JButton(new ResourceAction("add_annotation") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				String key = (String) SwingTools.showInputDialog("select_annotation", true, Annotations.ALL_KEYS_IOOBJECT,
						Annotations.ALL_KEYS_IOOBJECT[0]);
				if (key != null) {
					model.addRow(key);
				}
			}
		}));
		buttons.add(new JButton(new ResourceAction("delete_annotation") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				model.deleteRow(table.getSelectedRow());
			}
		}));
		component.add(buttons, BorderLayout.SOUTH);
		return component;
	}

}
