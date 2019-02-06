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
package com.rapidminer.gui.tools;

import java.awt.Component;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import com.rapidminer.gui.look.Colors;


/**
 * A {@link RowNumberTable} allows to display row numbers directly next to a {@link JTable} by using
 * it as a view port header for a {@link JScrollPane}. </br>
 * </br>
 * Example:
 *
 * <pre>
 * JTable contentTable = new JTable();
 * JScrollPane scrollPane = new JScrollPane(contentTable);
 * scrollPane.setRowHeaderView(new RowNumberTable(contentTable));
 * </pre>
 *
 * @author Nils Woehler
 * @since 7.0.0
 *
 */
public class RowNumberTable extends JTable {

	private static final long serialVersionUID = 1L;

	private static class RowNumberRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		private final Font derivedFont = RowNumberRenderer.this.getFont().deriveFont(Font.BOLD);

		public RowNumberRenderer() {
			setHorizontalAlignment(JLabel.CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			JLabel cellRenderer = (JLabel) super.getTableCellRendererComponent(table, value, false, hasFocus, row, column);
			cellRenderer.setFont(derivedFont);
			cellRenderer.setBackground(Colors.TABLE_HEADER_BACKGROUND_GRADIENT_START);
			cellRenderer.setBorder(BorderFactory.createLineBorder(Colors.TABLE_HEADER_BORDER));
			return cellRenderer;
		}
	}

	private final PropertyChangeListener listener = new PropertyChangeListener() {

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			// Keep the row table in sync with the main table
			if ("rowHeight".equals(evt.getPropertyName())) {
				repaint();
			}

			if ("model".equals(evt.getPropertyName())) {
				contentTable.getModel().addTableModelListener(new TableModelListener() {

					@Override
					public void tableChanged(TableModelEvent e) {
						revalidate();
					}
				});
				revalidate();
			}
		}
	};

	private final JTable contentTable;
	private int preferredWidth;

	/**
	 * Constructor to create a new {@link RowNumberTable} instance.
	 *
	 * @param contentTable
	 *            the table the {@link RowNumberTable} should be created for
	 */
	public RowNumberTable(final JTable contentTable) {
		this.contentTable = contentTable;
		contentTable.addPropertyChangeListener(listener);

		setFocusable(false);
		setAutoCreateColumnsFromModel(false);

		TableColumn column = new TableColumn();
		column.setCellRenderer(new RowNumberRenderer());
		addColumn(column);

		// calculate preferred width dynamically
		int rowCount = contentTable.getRowCount();
		preferredWidth = 25;
		if (rowCount > 0) {
			preferredWidth = 15 + 6 * String.valueOf(rowCount).length();
		}
		getColumnModel().getColumn(0).setPreferredWidth(preferredWidth);
		setPreferredScrollableViewportSize(getPreferredSize());
	}

	@Override
	public void addNotify() {
		super.addNotify();

		Component c = getParent();

		// Keep scrolling of the row table in sync with the main table.
		if (c instanceof JViewport) {
			JViewport viewport = (JViewport) c;
			viewport.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					// Keep the scrolling of the row table in sync with main table
					JViewport viewport = (JViewport) e.getSource();
					JScrollPane scrollPane = (JScrollPane) viewport.getParent();
					scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
				}
			});
		}
	}

	/**
	 * @return the preferred width for the row column which is calculated based on the row count
	 */
	public int getPreferredWidth() {
		return preferredWidth;
	}

	@Override
	public int getRowCount() {
		return contentTable.getRowCount();
	}

	@Override
	public int getRowHeight(int row) {
		int rowHeight = contentTable.getRowHeight(row);

		if (rowHeight != super.getRowHeight(row)) {
			super.setRowHeight(row, rowHeight);
		}

		return rowHeight;
	}

	@Override
	public Object getValueAt(int row, int column) {
		/*
		 * No model is being used for this table so just use the row number as the value of the
		 * cell.
		 */
		return Integer.toString(row + 1);
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	@Override
	public boolean getCellSelectionEnabled() {
		return false;
	}

	@Override
	public boolean getRowSelectionAllowed() {
		return false;
	}

	@Override
	public boolean getColumnSelectionAllowed() {
		return false;
	}

	@Override
	public void setValueAt(Object value, int row, int column) {}

}
