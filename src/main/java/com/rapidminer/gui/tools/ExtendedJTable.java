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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;
import java.util.Date;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.actions.EqualColumnWidthsAction;
import com.rapidminer.gui.tools.actions.FitAllColumnWidthsAction;
import com.rapidminer.gui.tools.actions.FitColumnWidthAction;
import com.rapidminer.gui.tools.actions.RestoreOriginalColumnOrderAction;
import com.rapidminer.gui.tools.actions.SelectColumnAction;
import com.rapidminer.gui.tools.actions.SelectRowAction;
import com.rapidminer.gui.tools.actions.SortByColumnAction;
import com.rapidminer.gui.tools.actions.SortColumnsAccordingToNameAction;
import com.rapidminer.gui.tools.components.ToolTipWindow;
import com.rapidminer.gui.tools.components.ToolTipWindow.TipProvider;
import com.rapidminer.report.Tableable;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.container.Pair;


/**
 * <p>
 * This class extends a JTable in a way that editing is handled like it is expected, i.e. editing is
 * properly stopped during focus losts, resizing, or column movement. The current value is then set
 * to the model. The only way to abort the value change is by pressing the escape key.
 * </p>
 *
 * <p>
 * The extended table is sortable per default. Developers should note that this feature might lead
 * to problems if the columns contain different class types and different editors. In this case one
 * of the constructors should be used which set the sortable flag to false.
 * </p>
 *
 * @author Ingo Mierswa, Marco Boeck
 */
public class ExtendedJTable extends JTable implements Tableable, MouseListener {

	private static final long serialVersionUID = 4840252601155251257L;

	private static final int DEFAULT_MAX_ROWS_FOR_SORTING = 100000;

	private static final int DEFAULT_COLUMN_WIDTH = 100;

	/** Limit the tooltip length that the html parsing does not take forever */
	private static final int MAX_TOOLTIP_LENGTH = 1000;

	public static final int NO_DATE_FORMAT = -1;
	public static final int DATE_FORMAT = 0;
	public static final int TIME_FORMAT = 1;
	public static final int DATE_TIME_FORMAT = 2;

	private final Action ROW_ACTION = new SelectRowAction(this, IconSize.SMALL);
	private final Action COLUMN_ACTION = new SelectColumnAction(this, IconSize.SMALL);
	private final Action FIT_COLUMN_ACTION = new FitColumnWidthAction(this, IconSize.SMALL);
	private final Action FIT_ALL_COLUMNS_ACTION = new FitAllColumnWidthsAction(this, IconSize.SMALL);
	private final Action EQUAL_WIDTHS_ACTION = new EqualColumnWidthsAction(this, IconSize.SMALL);
	private final Action SORTING_DESCENDING_ACTION = new SortByColumnAction(this, ExtendedJTableSorterModel.DESCENDING,
			IconSize.SMALL);
	private final Action SORTING_ASCENDING_ACTION = new SortByColumnAction(this, ExtendedJTableSorterModel.ASCENDING,
			IconSize.SMALL);
	private final Action SORT_COLUMNS_BY_NAME_ACTION = new SortColumnsAccordingToNameAction(this, IconSize.SMALL);
	private final Action RESTORE_COLUMN_ORDER_ACTION = new RestoreOriginalColumnOrderAction(this, IconSize.SMALL);

	private boolean sortable = true;

	private transient CellColorProvider cellColorProvider = new CellColorProviderAlternating();

	private boolean useColoredCellRenderer = true;

	private transient ColoredTableCellRenderer renderer = new ColoredTableCellRenderer();

	private ExtendedJTableSorterModel tableSorter = null;

	private ExtendedJScrollPane scrollPaneParent = null;

	private ExtendedJTablePacker packer = null;

	private boolean fixFirstColumn = false;

	private String[] originalOrder = null;

	private boolean showPopopUpMenu = true;

	private boolean[] cutOnLineBreaks;

	private int[] maximalTextLengths;

	private boolean rowHighlightingEnabled;
	private int rowHighlight = -1;
	private int lastColoredHighlightedRow = -1;
	private boolean checkHighlight = false;

	public ExtendedJTable() {
		this(null, true);
	}

	public ExtendedJTable(final boolean sortable) {
		this(null, sortable);
	}

	public ExtendedJTable(final TableModel model, final boolean sortable) {
		this(model, sortable, true);
	}

	public ExtendedJTable(final TableModel model, final boolean sortable, final boolean columnMovable) {
		this(model, sortable, columnMovable, true);
	}

	public ExtendedJTable(final boolean sortable, final boolean columnMovable, final boolean autoResize) {
		this(null, sortable, columnMovable, autoResize);
	}

	public ExtendedJTable(final TableModel model, final boolean sortable, final boolean columnMovable,
						  final boolean autoResize) {
		this(model, sortable, columnMovable, autoResize, true, false);
	}

	public ExtendedJTable(final TableModel model, final boolean sortable, final boolean columnMovable,
						  final boolean autoResize, final boolean useColoredCellRenderer, final boolean fixFirstColumn) {
		super();
		this.sortable = sortable;
		this.useColoredCellRenderer = useColoredCellRenderer;
		this.fixFirstColumn = fixFirstColumn;

		// allow all kinds of selection (e.g. for copy and paste)
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setColumnSelectionAllowed(true);
		setRowSelectionAllowed(true);

		setRowHeight(Math.max(getRowHeight(), 25));
		getTableHeader().setReorderingAllowed(columnMovable);

		// necessary in order to fix changes after focus was lost
		putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

		// auto resize?
		if (!autoResize) {
			setAutoResizeMode(AUTO_RESIZE_OFF);
		} else {
			setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		}

		if (model != null) {
			setModel(model);
		}

		// add listener for automatically resizing the table for double clicking the header border
		getTableHeader().addMouseListener(new ExtendedJTableColumnFitMouseListener());

		addMouseListener(this);

		// handles the highlighting of the currently hovered row
		addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent e) {
				if (!rowHighlightingEnabled) {
					return;
				}

				if (checkHighlight) {
					rowHighlight = rowAtPoint(e.getPoint());
					if (rowHighlight != lastColoredHighlightedRow) {
						repaint();
					}
				} else {
					rowHighlight = -1;
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (!rowHighlightingEnabled) {
					return;
				}

				// row highlight feels weird while dragging
				rowHighlight = -1;
			}

		});

		setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, Colors.TABLE_CELL_BORDER));
	}

	/** Registers a new {@link ToolTipWindow} on this table. */
	public void installToolTip() {
		// adding a new extended tool tip window
		new ToolTipWindow(new TableToolTipProvider(), this);
		setToolTipText(null);
	}

	protected Object readResolve() {
		this.renderer = new ColoredTableCellRenderer();
		return this;
	}

	protected ExtendedJTableSorterModel getTableSorter() {
		return this.tableSorter;
	}

	/**
	 * Subclasses might overwrite this method which by default simply returns NO_DATE. The returned
	 * format should be one out of NO_DATE_FORMAT, DATE_FORMAT, TIME_FORMAT, or DATE_TIME_FORMAT.
	 * This information will be used for the cell renderer.
	 */
	public int getDateFormat(final int row, final int column) {
		return NO_DATE_FORMAT;
	}

	/**
	 * The given color provider will be used for the cell renderer. The default method
	 * implementation returns {@link SwingTools#LIGHTEST_BLUE} and white for alternating rows. If no
	 * colors should be used at all, set the cell color provider to null or to the default white
	 * color provider {@link CellColorProviderWhite}.
	 */
	public void setCellColorProvider(final CellColorProvider cellColorProvider) {
		this.cellColorProvider = cellColorProvider;
	}

	/**
	 * The returned color provider will be used for the cell renderer. The default method
	 * implementation returns {@link SwingTools#LIGHTEST_BLUE} and white for alternating rows. If no
	 * colors should be used at all, set the cell color provider to null or to the default white
	 * color provider {@link CellColorProviderWhite}.
	 */
	public CellColorProvider getCellColorProvider() {
		return this.cellColorProvider;
	}

	public void setColoredTableCellRenderer(ColoredTableCellRenderer renderer) {
		this.renderer = renderer;
	}

	public void setSortable(final boolean sortable) {
		this.sortable = sortable;
	}

	public boolean isSortable() {
		return sortable;
	}

	public void setShowPopupMenu(final boolean showPopupMenu) {
		this.showPopopUpMenu = showPopupMenu;
	}

	public void setFixFirstColumnForRearranging(final boolean fixFirstColumn) {
		this.fixFirstColumn = fixFirstColumn;
	}

	public void setMaximalTextLength(final int maximalTextLength) {
		Arrays.fill(maximalTextLengths, maximalTextLength);
	}

	/**
	 * Sets whether row highlighting (aka darken the row the mouse is currently over) is active or
	 * not. By default it is active. Can only be activated if {@link #useColoredCellRenderer} is
	 * {@code true}.
	 *
	 * @param enabled
	 */
	public void setRowHighlighting(boolean enabled) {
		if (!useColoredCellRenderer) {
			return;
		}

		rowHighlightingEnabled = enabled;
	}

	/**
	 * If row highlighting is enabled (see {@link #setRowHighlighting(boolean)}, returns whether the
	 * given row is the currently highlighted row.
	 *
	 * @param row
	 * 		the row to check
	 * @return {@code true} if it is currently highlighted; {@code false} otherwise
	 */
	public boolean isRowHighlighted(int row) {
		if (!rowHighlightingEnabled) {
			return false;
		}

		return row == rowHighlight;
	}

	/**
	 * Sets the last highlighted row.
	 *
	 * @param row
	 * 		the last highlighted row
	 */
	public void setLastHighlightedRow(int row) {
		if (!rowHighlightingEnabled) {
			return;
		}

		lastColoredHighlightedRow = row;
	}

	/**
	 * Returns whether row highlighting (see {@link #setRowHighlighting(boolean)} is enabled.
	 *
	 * @return
	 */
	public boolean isRowHighlighting() {
		return rowHighlightingEnabled;
	}

	public void setMaximalTextLength(final int maximalTextLength, final int column) {
		maximalTextLengths[column] = maximalTextLength;
	}

	public void setCutOnLineBreak(final boolean enable) {
		Arrays.fill(cutOnLineBreaks, enable);
	}

	public void setCutOnLineBreak(final boolean enable, final int column) {
		cutOnLineBreaks[column] = enable;
	}

	@Override
	public void setModel(final TableModel model) {
		boolean shouldSort = this.sortable && checkIfSortable(model);

		if (shouldSort) {
			this.tableSorter = new ExtendedJTableSorterModel(model);
			this.tableSorter.setTableHeader(getTableHeader());
			super.setModel(this.tableSorter);
		} else {
			super.setModel(model);
			this.tableSorter = null;
		}

		originalOrder = new String[model.getColumnCount()];
		for (int c = 0; c < model.getColumnCount(); c++) {
			originalOrder[c] = model.getColumnName(c);
		}

		// initializing arrays for cell renderer settings
		cutOnLineBreaks = new boolean[model.getColumnCount()];
		maximalTextLengths = new int[model.getColumnCount()];
		Arrays.fill(maximalTextLengths, Integer.MAX_VALUE);

		model.addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(final TableModelEvent e) {
				int oldLength = cutOnLineBreaks.length;
				if (oldLength != model.getColumnCount()) {
					cutOnLineBreaks = Arrays.copyOf(cutOnLineBreaks, model.getColumnCount());
					maximalTextLengths = Arrays.copyOf(maximalTextLengths, model.getColumnCount());
					if (oldLength < cutOnLineBreaks.length) {
						Arrays.fill(cutOnLineBreaks, oldLength, cutOnLineBreaks.length, false);
						Arrays.fill(maximalTextLengths, oldLength, cutOnLineBreaks.length, Integer.MAX_VALUE);
					}
				}
			}
		});
	}

	public void setSortingStatus(final int status, final boolean cancelSorting) {
		if (getModel() instanceof ExtendedJTableSorterModel) {
			ExtendedJTableSorterModel sorterModel = (ExtendedJTableSorterModel) getModel();

			JTableHeader h = getTableHeader();
			TableColumnModel columnModel = h.getColumnModel();
			int viewColumn = getSelectedColumn();
			if (viewColumn != -1) {
				int column = columnModel.getColumn(viewColumn).getModelIndex();
				if (column != -1) {
					if (sorterModel.isSorting()) {
						if (cancelSorting) {
							sorterModel.cancelSorting();
						}
					}
					sorterModel.setSortingStatus(column, status);
				}
			}
		}
	}

	public void pack() {
		packer = new ExtendedJTablePacker(true);
		if (isShowing()) {
			packer.pack(this);
			packer = null;
		}
	}

	@Override
	public void addNotify() {
		super.addNotify();
		if (packer != null) {
			packer.pack(this);
			packer = null;
		}
	}

	public void unpack() {
		JTableHeader header = getTableHeader();
		if (header != null) {
			for (int c = 0; c < getColumnCount(); c++) {
				TableColumn tableColumn = header.getColumnModel().getColumn(c);
				header.setResizingColumn(tableColumn); // this line is very important

				int width = DEFAULT_COLUMN_WIDTH;
				if (getWidth() / width > getColumnCount()) {
					width = getWidth() / getColumnCount();
				}
				tableColumn.setWidth(width);
			}
		}
	}

	public void packColumn() {
		JTableHeader header = getTableHeader();
		if (header != null) {
			int col = getSelectedColumn();
			if (col >= 0) {
				TableColumn tableColumn = header.getColumnModel().getColumn(col);

				if (tableColumn != null) {
					int width = (int) header.getDefaultRenderer()
							.getTableCellRendererComponent(this, tableColumn.getIdentifier(), false, false, -1, col)
							.getPreferredSize().getWidth();

					int firstRow = 0;
					int lastRow = getRowCount();

					ExtendedJScrollPane scrollPane = getExtendedScrollPane();
					if (scrollPane != null) {
						JViewport viewport = scrollPane.getViewport();
						Rectangle viewRect = viewport.getViewRect();
						if (viewport.getHeight() < getHeight()) {
							firstRow = rowAtPoint(new Point(0, viewRect.y));
							firstRow = Math.max(0, firstRow);
							lastRow = rowAtPoint(new Point(0, viewRect.y + viewRect.height - 1));
							lastRow = Math.min(lastRow, getRowCount());
						}
					}

					for (int row = firstRow; row < lastRow; row++) {
						int preferedWidth = (int) getCellRenderer(row, col)
								.getTableCellRendererComponent(this, getValueAt(row, col), false, false, row, col)
								.getPreferredSize().getWidth();
						width = Math.max(width, preferedWidth);
					}

					header.setResizingColumn(tableColumn); // this line is very important

					tableColumn.setWidth(width + getIntercellSpacing().width);
				}
			}
		}
	}

	public void sortColumnsAccordingToNames() {
		int offset = 0;
		if (fixFirstColumn) {
			offset = 1;
		}
		for (int i = offset; i < getColumnCount(); i++) {
			int minIndex = -1;
			String minName = null;
			for (int j = i; j < getColumnCount(); j++) {
				String currentName = getColumnName(j);
				if (minName == null || currentName.compareTo(minName) < 0) {
					minName = currentName;
					minIndex = j;
				}
			}
			moveColumn(minIndex, i);
		}
	}

	public void restoreOriginalColumnOrder() {
		for (int i = 0; i < originalOrder.length; i++) {
			String nextColumn = originalOrder[i];
			for (int j = i; j < getColumnCount(); j++) {
				String candidateName = getColumnName(j);
				if (nextColumn.equals(candidateName)) {
					moveColumn(j, i);
					break;
				}
			}
		}
	}

	@Override
	public Dimension getIntercellSpacing() {
		Dimension dimension = super.getIntercellSpacing();
		dimension.width = dimension.width + 6;
		return dimension;
	}

	private boolean checkIfSortable(final TableModel model) {
		int maxSortableRows = DEFAULT_MAX_ROWS_FOR_SORTING;
		String maxString = ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_RAPIDMINER_GUI_MAX_SORTABLE_ROWS);
		if (maxString != null) {
			try {
				maxSortableRows = Integer.parseInt(maxString);
			} catch (NumberFormatException e) {
				// do nothing
			}
		}

		if (model.getRowCount() > maxSortableRows) {
			return false;
		} else {
			return true;
		}
	}

	/** Necessary to properly stopping the editing when a column is moved (dragged). */
	@Override
	public void columnMoved(final TableColumnModelEvent e) {
		if (isEditing()) {
			cellEditor.stopCellEditing();
		}
		super.columnMoved(e);
	}

	/** Necessary to properly stopping the editing when a column is resized. */
	@Override
	public void columnMarginChanged(final ChangeEvent e) {
		if (isEditing()) {
			cellEditor.stopCellEditing();
		}
		super.columnMarginChanged(e);
	}

	public boolean shouldUseColoredCellRenderer() {
		return this.useColoredCellRenderer;
	}

	@Override
	public TableCellRenderer getCellRenderer(final int row, final int col) {
		if (useColoredCellRenderer) {
			Color color = null;
			CellColorProvider usedColorProvider = getCellColorProvider();
			if (usedColorProvider != null) {
				color = usedColorProvider.getCellColor(row, col);
			}

			if (color != null) {
				renderer.setColor(color);
			}

			renderer.setDateFormat(getDateFormat(row, convertColumnIndexToModel(col)));

			if (col < maximalTextLengths.length) {
				renderer.setMaximalTextLength(maximalTextLengths[col]);
			}
			if (col < cutOnLineBreaks.length) {
				renderer.setCutOnFirstLineBreak(cutOnLineBreaks[col]);
			}
			return renderer;
		} else {
			return super.getCellRenderer(row, col);
		}
	}

	/** This method ensures that the correct tool tip for the current table cell is delivered. */
	@Override
	public String getToolTipText(final MouseEvent e) {
		Point p = e.getPoint();
		int colIndex = columnAtPoint(p);
		int rowIndex = rowAtPoint(p);

		return getToolTipText(colIndex, rowIndex);
	}

	protected String getToolTipText(final int colIndex, final int rowIndex) {
		int realColumnIndex = convertColumnIndexToModel(colIndex);
		String text = null;
		if (rowIndex >= 0 && rowIndex < getRowCount() && realColumnIndex >= 0
				&& realColumnIndex < getModel().getColumnCount()) {
			Object value = getModel().getValueAt(rowIndex, realColumnIndex);
			if (value instanceof Number) {
				// display
				Number number = (Number) value;
				double numberValue = number.doubleValue();
				long longValue = Math.round(numberValue);
				if (Math.abs(longValue - numberValue) <= Double.MIN_VALUE) {
					// for all intents and purposes we consider these integer
					text = String.valueOf(longValue);
				} else {
					// real values here
					text = String.valueOf(numberValue);
				}
			} else {
				if (value != null) {
					if (value instanceof Date) {
						int dateFormat = getDateFormat(rowIndex, realColumnIndex);
						switch (dateFormat) {
							case ExtendedJTable.DATE_FORMAT:
								text = Tools.formatDate((Date) value);
								break;
							case ExtendedJTable.TIME_FORMAT:
								text = Tools.formatTime((Date) value);
								break;
							case ExtendedJTable.DATE_TIME_FORMAT:
								text = Tools.formatDateTime((Date) value);
								break;
							default:
								text = value.toString();
								break;
						}
					} else {
						text = value.toString();
					}
				} else {
					text = "?";
				}
			}
		}
		if (text != null && !text.equals("")) {
			text = SwingTools.getShortenedDisplayName(text, MAX_TOOLTIP_LENGTH);
			return SwingTools.transformToolTipText(text, true);
		} else {
			return super.getToolTipText();
		}
	}

	/**
	 * {@link Tableable} Method
	 */
	@Override
	public String getCell(int row, final int column) {
		String text = null;
		if (getTableHeader() != null) {
			if (row == 0) {
				// titel row
				return getTableHeader().getColumnModel().getColumn(column).getHeaderValue().toString();
			} else {
				row--;
			}
		}
		// data area
		Object value = getModel().getValueAt(row, column);
		if (value instanceof Number) {
			Number number = (Number) value;
			double numberValue = number.doubleValue();
			text = Tools.formatIntegerIfPossible(numberValue);
		} else {
			if (value != null) {
				text = value.toString();
			} else {
				text = "?";
			}
		}
		return text;
	}

	/**
	 * {@link Tableable} Method
	 */
	@Override
	public int getColumnNumber() {
		return getColumnCount();
	}

	/**
	 * {@link Tableable} Method
	 */
	@Override
	public int getRowNumber() {
		if (getTableHeader() != null) {
			return getRowCount() + 1;
		} else {
			return getRowCount();
		}
	}

	/**
	 * {@link Tableable} Method
	 */
	@Override
	public void prepareReporting() {}

	/**
	 * {@link Tableable} Method
	 */
	@Override
	public void finishReporting() {}

	/**
	 * {@link Tableable} Method
	 */
	@Override
	public boolean isFirstLineHeader() {
		return false;
	}

	/**
	 * {@link Tableable} Method
	 */
	@Override
	public boolean isFirstColumnHeader() {
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		// in case of the auto resize for all columns, either make the table fill the available
		// space or grow the needed space
		// this fixes the issue that otherwise it will never exceed the viewport width regardless of
		// column count
		if (autoResizeMode == AUTO_RESIZE_ALL_COLUMNS) {
			return getPreferredSize().width < getParent().getWidth();
		}
		return super.getScrollableTracksViewportWidth();
	}

	/**
	 * Converts the index of the row in the view to the corresponding row in the original model.
	 * They might difer if the table is sorted.
	 *
	 * @param rowIndex
	 * 		The index of the row in the view.
	 * @return The index of the row in the original model.
	 */
	public int getModelIndex(final int rowIndex) {
		if (tableSorter != null) {
			return tableSorter.modelIndex(rowIndex);
		}
		return rowIndex;
	}

	public void setExtendedScrollPane(final ExtendedJScrollPane scrollPane) {
		this.scrollPaneParent = scrollPane;
	}

	public ExtendedJScrollPane getExtendedScrollPane() {
		return this.scrollPaneParent;
	}

	public void selectCompleteRow() {
		addColumnSelectionInterval(0, getColumnCount() - 1);
	}

	public void selectCompleteColumn() {
		addRowSelectionInterval(0, getRowCount() - 1);
	}

	@Override
	public void mouseEntered(final MouseEvent e) {
		if (!rowHighlightingEnabled) {
			return;
		}

		checkHighlight = true;
		repaint();
	}

	@Override
	public void mouseExited(final MouseEvent e) {
		if (!rowHighlightingEnabled) {
			return;
		}

		checkHighlight = false;
		rowHighlight = -1;
		repaint();
	}

	@Override
	public void mouseClicked(final MouseEvent e) {
		mouseReleased(e);
	}

	@Override
	public void mousePressed(final MouseEvent e) {
		mouseReleased(e);
	}

	@Override
	public void mouseReleased(final MouseEvent e) {
		if (showPopopUpMenu) {
			if (e.isPopupTrigger()) {
				Point p = e.getPoint();
				int row = rowAtPoint(p);
				int c = columnAtPoint(p);

				// don't do anything when outside of table
				if (row < 0 || c < 0) {
					return;
				}
				// only set cell selection if clicked cell is outside current selection
				if (row < getSelectedRow() || row > getSelectedRow() + getSelectedRowCount() - 1 || c < getSelectedColumn()
						|| c > getSelectedColumn() + getSelectedColumnCount() - 1) {
					if (row < getRowCount() && c < getColumnCount()) {
						// needed because sometimes row could be outside [0, getRowCount()-1]
						setRowSelectionInterval(row, row);
						setColumnSelectionInterval(c, c);
					}
				}

				JPopupMenu menu = createPopupMenu();

				showPopupMenu(menu, e.getPoint());
			}
		}
	}

	protected void showPopupMenu(final JPopupMenu menu, final Point location) {
		menu.show(this, (int) location.getX(), (int) location.getY());
	}

	public JPopupMenu createPopupMenu() {
		JPopupMenu menu = new JPopupMenu();
		populatePopupMenu(menu);
		return menu;
	}

	public void populatePopupMenu(final JPopupMenu menu) {
		menu.add(ROW_ACTION);
		menu.add(COLUMN_ACTION);

		if (getTableHeader() != null) {
			menu.addSeparator();
			menu.add(FIT_COLUMN_ACTION);
			menu.add(FIT_ALL_COLUMNS_ACTION);
			menu.add(EQUAL_WIDTHS_ACTION);
		}

		if (isSortable()) {
			menu.addSeparator();
			menu.add(SORTING_ASCENDING_ACTION);
			menu.add(SORTING_DESCENDING_ACTION);
		}

		if (getTableHeader() != null) {
			if (getTableHeader().getReorderingAllowed()) {
				menu.addSeparator();
				menu.add(SORT_COLUMNS_BY_NAME_ACTION);
				menu.add(RESTORE_COLUMN_ORDER_ACTION);
			}
		}
	}

	private class TableToolTipProvider implements TipProvider {

		@Override
		public Component getCustomComponent(final Object id) {
			return null;
		}

		@Override
		public Object getIdUnder(final Point point) {
			Pair<Integer, Integer> cellId = new Pair<>(columnAtPoint(point), rowAtPoint(point));
			return cellId;
		}

		@SuppressWarnings("unchecked")
		@Override
		public String getTip(final Object id) {
			Pair<Integer, Integer> cellId = (Pair<Integer, Integer>) id;
			return getToolTipText(cellId.getFirst(), cellId.getSecond());
		}
	}

	@Override
	public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
		Rectangle rect = super.getCellRect(row, column, includeSpacing);
		if (rect.y < 0) {
			// If y < 0 the amount of rows times rowheight exceeds Integer.MAX_VALUE and becomes negative.
			// This workaround will show a huge table, secured by factor 10 because else initially the table might be blank
			rect.y = Integer.MAX_VALUE - (rect.height * 10);
		}
		return rect;
	}

	/**
	 * Check if there are more rows than those that can be displayed due to swing's height limitation that can not be
	 * larger than Integer.MAX_VALUE.
	 *
	 * @return {@code true} if the table is completely displayable and no rows are missing, else it would be useful to display the information about invisible rows.
	 * @since 9.1
	 */
	public boolean canShowAllRows() {
		return (long) getRowHeight() * getRowCount() < Integer.MAX_VALUE;
	}
}
