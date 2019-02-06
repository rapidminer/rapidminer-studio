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
package com.rapidminer.gui.look.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.tools.ExtendedJTableSorterModel;


/**
 * The UI for table headers.
 *
 * @author Ingo Mierswa
 */
public class TableHeaderUI extends BasicTableHeaderUI {

	private static final Border HEADER_BORDER = BorderFactory.createEmptyBorder(0, 10, 0, 10);

	private static final int HEADER_HEIGHT = 31;

	private TableCellRenderer originalRenderer;

	private TableHeaderRenderer mainRenderer;

	private int highlightedColumn = -1;

	private int pressedColumn = -1;

	public static ComponentUI createUI(JComponent h) {
		return new TableHeaderUI();
	}

	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
		this.originalRenderer = this.header.getDefaultRenderer();
		if (this.originalRenderer instanceof UIResource) {
			this.mainRenderer = new TableHeaderRenderer();
			this.mainRenderer.setHorizontalAlignment(SwingConstants.CENTER);
			this.header.setDefaultRenderer(this.mainRenderer);
		}
	}

	@Override
	public void uninstallUI(JComponent c) {
		if (this.header.getDefaultRenderer() instanceof TableHeaderRenderer) {
			this.header.setDefaultRenderer(this.originalRenderer);
			this.mainRenderer = null;
		}
		super.uninstallUI(c);
	}

	@Override
	public void installDefaults() {
		super.installDefaults();

		// some tables need a special header background so check if it was set
		Object bgObject = header.getClientProperty(RapidLookTools.PROPERTY_TABLE_HEADER_BACKGROUND);
		if (bgObject != null && bgObject instanceof Color) {
			header.setBackground((Color) bgObject);
		}
	}

	private void updateRolloverColumn(Point p) {
		if (this.header.getDraggedColumn() == null && this.header.contains(p)) {
			int col = this.header.columnAtPoint(p);
			if (col != this.highlightedColumn) {
				this.highlightedColumn = col;
				this.header.repaint();
			}
		}
	}

	@Override
	protected MouseInputListener createMouseInputListener() {
		return new MouseInputHandler() {

			@Override
			public void mouseMoved(MouseEvent e) {
				super.mouseMoved(e);
				updateRolloverColumn(e.getPoint());
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				super.mouseEntered(e);
				updateRolloverColumn(e.getPoint());
			}

			@Override
			public void mouseExited(MouseEvent e) {
				super.mouseExited(e);
				TableHeaderUI.this.highlightedColumn = -1;
				TableHeaderUI.this.header.repaint();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				if (TableHeaderUI.this.header.contains(e.getPoint())) {
					if (TableHeaderUI.this.header.getDraggedColumn() != null) {
						TableHeaderUI.this.pressedColumn = TableHeaderUI.this.header.columnAtPoint(e.getPoint());
					} else {
						if (TableHeaderUI.this.header.getDraggedColumn() != null) {
							TableHeaderUI.this.pressedColumn = TableHeaderUI.this.header.getColumnModel().getColumnIndex(
									TableHeaderUI.this.header.getDraggedColumn());
						}
					}
				}
				if (TableHeaderUI.this.header.getReorderingAllowed()) {
					TableHeaderUI.this.highlightedColumn = -1;
				}
				TableHeaderUI.this.header.repaint();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				super.mouseDragged(e);
				if (TableHeaderUI.this.header.contains(e.getPoint())) {
					TableHeaderUI.this.pressedColumn = -1;
				}
				updateRolloverColumn(e.getPoint());
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				if (TableHeaderUI.this.header.contains(e.getPoint())) {
					TableHeaderUI.this.pressedColumn = -1;
				}
				updateRolloverColumn(e.getPoint());
				TableHeaderUI.this.header.repaint();
			}
		};
	}

	@Override
	public Dimension getPreferredSize(JComponent c) {
		return new Dimension((int) super.getPreferredSize(c).getWidth(), Math.max((int) super.getPreferredSize(c)
				.getHeight(), HEADER_HEIGHT));
	}

	private class TableHeaderRenderer extends DefaultTableCellRenderer implements UIResource {

		private static final long serialVersionUID = -7300727448162015796L;

		private boolean rollOver;

		private boolean isPressed;

		private boolean isLeftmost;

		private boolean isRightmost;

		private int curCol = 0;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (table != null) {
				JTableHeader header = table.getTableHeader();
				if (header != null) {
					setFont(header.getFont());
				}
			}

			this.rollOver = column == TableHeaderUI.this.highlightedColumn;
			if (header != null) {
				curCol = column;
				this.isLeftmost = column == 0;
				this.isRightmost = column == header.getColumnModel().getColumnCount() - 1;
			}

			if (header != null && header.getDraggedColumn() != null) {
				this.isPressed = viewIndexForColumn(header.getDraggedColumn()) == column
						|| column == TableHeaderUI.this.pressedColumn;
			} else {
				this.isPressed = false;
			}
			setText(value == null ? "" : value.toString());
			setHorizontalAlignment(SwingConstants.LEFT);
			setHorizontalTextPosition(SwingConstants.LEADING);
			setBorder(HEADER_BORDER);
			return this;
		}

		@Override
		public Icon getIcon() {
			int modelCol = header.getTable().convertColumnIndexToModel(curCol);
			TableModel model = header.getTable().getModel();
			if (model instanceof ExtendedJTableSorterModel) {
				ExtendedJTableSorterModel sortModel = (ExtendedJTableSorterModel) model;
				switch (sortModel.getSortingStatus(modelCol)) {
					case ExtendedJTableSorterModel.ASCENDING:
						return UIManager.getIcon("Table.ascendingSortIcon");
					case ExtendedJTableSorterModel.DESCENDING:
						return UIManager.getIcon("Table.descendingSortIcon");
					case ExtendedJTableSorterModel.NOT_SORTED:
					default:
						return null;
				}
			} else {
				SortKey sortKey = getSortKey(header.getTable().getRowSorter(), modelCol);
				SortOrder sortOrder = sortKey != null ? sortKey.getSortOrder() : SortOrder.UNSORTED;
				switch (sortOrder) {
					case ASCENDING:
						return UIManager.getIcon("Table.ascendingSortIcon");
					case DESCENDING:
						return UIManager.getIcon("Table.descendingSortIcon");
					case UNSORTED:
					default:
						return null;
				}
			}
		}

		@Override
		public Insets getInsets() {
			return new Insets(2, 4, 2, 3);
		}

		@Override
		public void paint(Graphics g) {
			int h = this.getHeight();
			int w = this.getWidth();

			Graphics2D g2 = (Graphics2D) g;
			if (this.isPressed) {
				g2.setColor(Colors.TABLE_HEADER_BACKGROUND_PRESSED);
			} else {
				if (this.rollOver) {
					g2.setColor(Colors.TABLE_HEADER_BACKGROUND_FOCUS);
				} else {
					Paint gp = new GradientPaint(0, 0, Colors.TABLE_HEADER_BACKGROUND_GRADIENT_START, 0, h,
							Colors.TABLE_HEADER_BACKGROUND_GRADIENT_END);
					g2.setPaint(gp);
				}
			}

			g2.fill(createHeaderShape(0, 0, w, h, isLeftmost, isRightmost));
			g2.setColor(Colors.TABLE_HEADER_BORDER);
			g2.draw(createHeaderShape(0, 0, w, h, isLeftmost, isRightmost));

			super.paint(g);
		}

		/**
		 * Get the view column index of the given table column
		 *
		 * @param aColumn
		 * @return
		 */
		private int viewIndexForColumn(TableColumn aColumn) {
			TableColumnModel cm = TableHeaderUI.this.header.getColumnModel();
			for (int column = 0; column < cm.getColumnCount(); column++) {
				if (cm.getColumn(column) == aColumn) {
					return column;
				}
			}
			return -1;
		}

		/**
		 * Tries to return the sort key for the given column.
		 *
		 * @param sorter
		 * @param column
		 * @return the sort key or {@code null}
		 */
		private SortKey getSortKey(RowSorter<? extends TableModel> sorter, int column) {
			if (sorter == null) {
				return null;
			}

			for (Object sortObj : sorter.getSortKeys()) {
				SortKey key = (SortKey) sortObj;
				if (key.getColumn() == column) {
					return key;
				}
			}
			return null;
		}

	}

	/**
	 * Creates the shape for a table header.
	 *
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @param isLeftmost
	 * @param isRightmost
	 * @return
	 */
	public static Path2D createHeaderShape(int x, int y, int w, int h, boolean isLeftmost, boolean isRightmost) {
		double rTop = RapidLookAndFeel.CORNER_DEFAULT_RADIUS * 0.33;
		Path2D path = new Path2D.Double();
		h -= 1;

		// middle columns are easy, just a rectangle without left border
		if (!isLeftmost && !isRightmost) {
			w -= 1;
			path.append(new Line2D.Double(x, y, x + w, y), true);
			path.append(new Line2D.Double(x + w, y, x + w, y + h), true);
			path.append(new Line2D.Double(x + w, y + h, x, y + h), true);
			return path;
		}

		// special case of single column
		if (isLeftmost && isRightmost) {
			w -= 1;
			path.append(new Line2D.Double(x, y + h - 1, x, y + rTop), true);
			QuadCurve2D curve = new QuadCurve2D.Double(x, y + rTop, x, y, x + rTop, y);
			path.append(curve, true);
			path.append(new Line2D.Double(x + rTop, y, x + w - rTop, y), true);
			curve = new QuadCurve2D.Double(x + w - rTop, y, x + w, y, x + w, y + rTop);
			path.append(curve, true);
			path.append(new Line2D.Double(x + w, y + rTop, x + w, y + h), true);
			path.append(new Line2D.Double(x + w, y + h, x, y + h), true);
			return path;
		}

		if (isLeftmost) {
			w -= 1;
			path.append(new Line2D.Double(x, y + h - 1, x, y + rTop), true);
			QuadCurve2D curve = new QuadCurve2D.Double(x, y + rTop, x, y, x + rTop, y);
			path.append(curve, true);
			path.append(new Line2D.Double(x + rTop, y, x + w, y), true);
			path.append(new Line2D.Double(x + w, y, x + w, y + h), true);
			path.append(new Line2D.Double(x + w, y + h, x, y + h), true);
		} else {
			w -= 1;
			path.append(new Line2D.Double(x, y, x + w - rTop, y), true);
			QuadCurve2D curve = new QuadCurve2D.Double(x + w - rTop, y, x + w, y, x + w, y + rTop);
			path.append(curve, true);
			path.append(new Line2D.Double(x + w, y + rTop, x + w, y + h), true);
			path.append(new Line2D.Double(x + w, y + h, x, y + h), true);
		}

		return path;
	}
}
