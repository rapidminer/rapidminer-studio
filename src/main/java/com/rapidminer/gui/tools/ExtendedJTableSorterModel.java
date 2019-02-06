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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;


/**
 * TableSorter is a decorator for TableModels; adding sorting functionality to a supplied
 * TableModel. TableSorter does not store or copy the data in its TableModel; instead it maintains a
 * map from the row indexes of the view to the row indexes of the model. As requests are made of the
 * sorter (like getValueAt(row, col)) they are passed to the underlying model after the row numbers
 * have been translated via the internal mapping array. This way, the TableSorter appears to hold
 * another copy of the table with the rows in a different order.
 * <p/>
 * TableSorter registers itself as a listener to the underlying model, just as the JTable itself
 * would. Events recieved from the model are examined, sometimes manipulated (typically widened),
 * and then passed on to the TableSorter's listeners (typically the JTable). If a change to the
 * model has invalidated the order of TableSorter's rows, a note of this is made and the sorter will
 * resort the rows the next time a value is requested.
 * <p/>
 * When the tableHeader property is set, either by using the setTableHeader() method or the two
 * argument constructor, the table header may be used as a complete UI for TableSorter. The default
 * renderer of the tableHeader is decorated with a renderer that indicates the sorting status of
 * each column. In addition, a mouse listener is installed with the following behavior:
 * <ul>
 * <li>
 * Mouse-click: Clears the sorting status of all other columns and advances the sorting status of
 * that column through three values: {NOT_SORTED, ASCENDING, DESCENDING} (then back to NOT_SORTED
 * again).
 * <li>
 * SHIFT-mouse-click: Clears the sorting status of all other columns and cycles the sorting status
 * of the column through the same three values, in the opposite order: {NOT_SORTED, DESCENDING,
 * ASCENDING}.
 * <li>
 * CONTROL-mouse-click and CONTROL-SHIFT-mouse-click: as above except that the changes to the column
 * do not cancel the statuses of columns that are already sortoing - giving a way to initiate a
 * compound sort.
 * </ul>
 * <p/>
 * This is a long overdue rewrite of a class of the same name that first appeared in the swing table
 * demos in 1997.
 *
 * @author Philip Milne
 * @author Brendon McLean
 * @author Dan van Enckevort
 * @author Parwinder Sekhon
 * @author Ingo Mierswa
 *
 *         (only for Java 5 generics parts and some other small enhancements)
 *
 */
public class ExtendedJTableSorterModel extends AbstractTableModel {

	private static final long serialVersionUID = -4206702130247556242L;

	protected TableModel tableModel;

	public static final int DESCENDING = -1;
	public static final int NOT_SORTED = 0;
	public static final int ASCENDING = 1;

	private static Directive EMPTY_DIRECTIVE = new Directive(-1, NOT_SORTED);

	private static class ComparableComparator<T extends Comparable<T>> implements Comparator<T>, Serializable {

		private static final long serialVersionUID = -5769752125995885597L;

		@Override
		public int compare(T o1, T o2) {
			return o1.compareTo(o2);
		}
	}

	private static class LexicalComparator<T> implements Comparator<T>, Serializable {

		private static final long serialVersionUID = 8510299047139484690L;

		@Override
		public int compare(T o1, T o2) {
			return o1.toString().compareTo(o2.toString());
		}
	}

	private static final Comparator<? extends Comparable<?>> COMPARABLE_COMPARATOR = new ComparableComparator<>();

	private static final Comparator<?> LEXICAL_COMPARATOR = new LexicalComparator<>();

	private transient Row[] viewToModel;
	private int[] modelToView;

	private JTableHeader tableHeader;
	private transient MouseListener mouseListener = new MouseHandler();
	private transient TableModelListener tableModelListener = new TableModelHandler();
	private Map<Class<?>, Comparator<?>> columnComparators = new HashMap<Class<?>, Comparator<?>>();
	private List<Directive> sortingColumns = new ArrayList<Directive>();

	public ExtendedJTableSorterModel() {
		super();
	}

	public ExtendedJTableSorterModel(TableModel tableModel) {
		this();
		setTableModel(tableModel);
	}

	public ExtendedJTableSorterModel(TableModel tableModel, JTableHeader tableHeader) {
		this();
		setTableHeader(tableHeader);
		setTableModel(tableModel);
	}

	protected Object readResolve() {
		this.mouseListener = new MouseHandler();
		this.tableModelListener = new TableModelHandler();
		return this;
	}

	private void clearSortingState() {
		viewToModel = null;
		modelToView = null;
	}

	public TableModel getTableModel() {
		return tableModel;
	}

	public void setTableModel(TableModel tableModel) {
		if (this.tableModel != null) {
			this.tableModel.removeTableModelListener(tableModelListener);
		}

		this.tableModel = tableModel;
		if (this.tableModel != null) {
			this.tableModel.addTableModelListener(tableModelListener);
		}

		clearSortingState();
		fireTableStructureChanged();
	}

	public JTableHeader getTableHeader() {
		return tableHeader;
	}

	public void setTableHeader(JTableHeader tableHeader) {
		if (this.tableHeader != null) {
			this.tableHeader.removeMouseListener(mouseListener);
		}
		this.tableHeader = tableHeader;
		if (this.tableHeader != null) {
			this.tableHeader.addMouseListener(mouseListener);
		}
	}

	public boolean isSorting() {
		return sortingColumns.size() != 0;
	}

	private Directive getDirective(int column) {
		for (int i = 0; i < sortingColumns.size(); i++) {
			Directive directive = sortingColumns.get(i);
			if (directive.column == column) {
				return directive;
			}
		}
		return EMPTY_DIRECTIVE;
	}

	public int getSortingStatus(int column) {
		return getDirective(column).direction;
	}

	public void sortingStatusChanged() {
		clearSortingState();
		fireTableDataChanged();
		if (tableHeader != null) {
			tableHeader.repaint();
		}
	}

	public void setSortingStatus(int column, int status) {
		Directive directive = getDirective(column);
		if (directive != EMPTY_DIRECTIVE) {
			sortingColumns.remove(directive);
		}
		if (status != NOT_SORTED) {
			sortingColumns.add(new Directive(column, status));
		}
		sortingStatusChanged();
	}

	public void cancelSorting() {
		sortingColumns.clear();
		sortingStatusChanged();
	}

	public void setColumnComparator(Class<?> type, Comparator<?> comparator) {
		if (comparator == null) {
			columnComparators.remove(type);
		} else {
			columnComparators.put(type, comparator);
		}
	}

	@SuppressWarnings("rawtypes")
	protected Comparator getComparator(int column) {
		Class<?> columnType = tableModel.getColumnClass(column);
		Comparator<?> comparator = columnComparators.get(columnType);
		if (comparator != null) {
			return comparator;
		}
		if (Comparable.class.isAssignableFrom(columnType)) {
			return COMPARABLE_COMPARATOR;
		} else {
			return LEXICAL_COMPARATOR;
		}
	}

	private Row[] getViewToModel() {
		if (viewToModel == null) {
			int tableModelRowCount = tableModel.getRowCount();
			viewToModel = new Row[tableModelRowCount];
			for (int row = 0; row < tableModelRowCount; row++) {
				viewToModel[row] = new Row(row);
			}

			if (isSorting()) {
				Arrays.sort(viewToModel);
			}
		}
		return viewToModel;
	}

	public int modelIndex(int viewIndex) {
		if (viewIndex >= 0 && viewIndex < getViewToModel().length) {
			return getViewToModel()[viewIndex].modelIndex;
		} else {
			return viewIndex;
		}
	}

	private int[] getModelToView() {
		if (modelToView == null) {
			int n = getViewToModel().length;
			modelToView = new int[n];
			for (int i = 0; i < n; i++) {
				modelToView[modelIndex(i)] = i;
			}
		}
		return modelToView;
	}

	// TableModel interface methods

	@Override
	public int getRowCount() {
		return tableModel == null ? 0 : tableModel.getRowCount();
	}

	@Override
	public int getColumnCount() {
		return tableModel == null ? 0 : tableModel.getColumnCount();
	}

	@Override
	public String getColumnName(int column) {
		return tableModel.getColumnName(column);
	}

	@Override
	public Class<?> getColumnClass(int column) {
		return tableModel.getColumnClass(column);
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return tableModel.isCellEditable(modelIndex(row), column);
	}

	@Override
	public Object getValueAt(int row, int column) {
		return tableModel.getValueAt(modelIndex(row), column);
	}

	@Override
	public void setValueAt(Object aValue, int row, int column) {
		tableModel.setValueAt(aValue, modelIndex(row), column);
	}

	// Helper classes

	private class Row implements Comparable<Row> {

		private int modelIndex;

		public Row(int index) {
			this.modelIndex = index;
		}

		@Override
		@SuppressWarnings("unchecked")
		public int compareTo(Row o) {
			int row1 = modelIndex;
			int row2 = o.modelIndex;

			for (Directive directive : sortingColumns) {
				int column = directive.column;
				Object o1 = tableModel.getValueAt(row1, column);
				Object o2 = tableModel.getValueAt(row2, column);

				int comparison = 0;
				// Define null less than everything, except null.
				if (o1 == null && o2 == null) {
					comparison = 0;
				} else if ("?".equals(o1) && "?".equals(o2)) {
					comparison = 0;
				} else if (o1 == null || "?".equals(o1)) {
					comparison = -1;
				} else if (o2 == null || "?".equals(o2)) {
					comparison = 1;
				} else {
					comparison = getComparator(column).compare(o1, o2);
				}
				if (comparison != 0) {
					return directive.direction == DESCENDING ? -comparison : comparison;
				}
			}
			return 0;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Row)) {
				return false;
			} else {
				return this.modelIndex == ((Row) o).modelIndex;
			}
		}

		@Override
		public int hashCode() {
			return Integer.valueOf(modelIndex).hashCode();
		}
	}

	private class TableModelHandler implements TableModelListener {

		@Override
		public void tableChanged(TableModelEvent e) {
			// If we're not sorting by anything, just pass the event along.
			if (!isSorting()) {
				clearSortingState();
				fireTableChanged(e);
				return;
			}

			// If the table structure has changed, cancel the sorting; the
			// sorting columns may have been either moved or deleted from
			// the model.
			if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
				cancelSorting();
				fireTableChanged(e);
				return;
			}

			// We can map a cell event through to the view without widening
			// when the following conditions apply:
			//
			// a) all the changes are on one row (e.getFirstRow() == e.getLastRow()) and,
			// b) all the changes are in one column (column != TableModelEvent.ALL_COLUMNS) and,
			// c) we are not sorting on that column (getSortingStatus(column) == NOT_SORTED) and,
			// d) a reverse lookup will not trigger a sort (modelToView != null)
			//
			// Note: INSERT and DELETE events fail this test as they have column == ALL_COLUMNS.
			//
			// The last check, for (modelToView != null) is to see if modelToView
			// is already allocated. If we don't do this check; sorting can become
			// a performance bottleneck for applications where cells
			// change rapidly in different parts of the table. If cells
			// change alternately in the sorting column and then outside of
			// it this class can end up re-sorting on alternate cell updates -
			// which can be a performance problem for large tables. The last
			// clause avoids this problem.
			int column = e.getColumn();
			if (e.getFirstRow() == e.getLastRow() && column != TableModelEvent.ALL_COLUMNS
					&& getSortingStatus(column) == NOT_SORTED && modelToView != null) {
				int viewIndex = getModelToView()[e.getFirstRow()];
				fireTableChanged(new TableModelEvent(ExtendedJTableSorterModel.this, viewIndex, viewIndex, column,
						e.getType()));
				return;
			}

			// Something has happened to the data that may have invalidated the row order.
			clearSortingState();
			fireTableDataChanged();
			return;
		}
	}

	private class MouseHandler extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			JTableHeader h = (JTableHeader) e.getSource();
			TableColumnModel columnModel = h.getColumnModel();
			int viewColumn = getSortingColumnIndex(h, e.getPoint());
			if (viewColumn != -1) {
				int column = columnModel.getColumn(viewColumn).getModelIndex();
				if (column != -1) {
					int status = getSortingStatus(column);
					if (!SwingTools.isControlOrMetaDown(e)) {
						cancelSorting();
					}
					// Cycle the sorting states through {NOT_SORTED, ASCENDING, DESCENDING} or
					// {NOT_SORTED, DESCENDING, ASCENDING} depending on whether shift is
					// pressed.
					status = status + (e.isShiftDown() ? -1 : 1);
					status = (status + 4) % 3 - 1; // signed mod, returning {-1, 0, 1}
					setSortingStatus(column, status);
				}
				e.consume();
			}
		}

		// copied from BasicTableHeader.MouseInputHandler.getResizingColumn
		private int getSortingColumnIndex(JTableHeader header, Point p) {
			return getSortingColumnIndex(header, p, header.columnAtPoint(p));
		}

		// copied from BasicTableHeader.MouseInputHandler.getResizingColumn
		private int getSortingColumnIndex(JTableHeader header, Point p, int column) {
			if (column == -1) {
				return -1;
			}

			Rectangle r = header.getHeaderRect(column);
			r.grow(-4, 0);

			if (!r.contains(p)) {
				return -1;
			} else {
				return column;
			}
		}
	}

	private static class Directive {

		private int column;
		private int direction;

		public Directive(int column, int direction) {
			this.column = column;
			this.direction = direction;
		}
	}
}
