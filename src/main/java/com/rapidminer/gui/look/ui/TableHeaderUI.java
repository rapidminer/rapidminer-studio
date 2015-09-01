/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.look.ui;

import com.rapidminer.gui.look.RapidLookTools;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;


/**
 * The UI for table headers.
 * 
 * @author Ingo Mierswa
 */
public class TableHeaderUI extends BasicTableHeaderUI {

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
	}

	private void updateRolloverColumn(Point p) {
		if ((this.header.getDraggedColumn() == null) && this.header.contains(p)) {
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
				.getHeight(), 20));
	}

	private class TableHeaderRenderer extends DefaultTableCellRenderer implements UIResource {

		private static final long serialVersionUID = -7300727448162015796L;

		private boolean rollOver;

		private boolean isPressed;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (table != null) {
				JTableHeader header = table.getTableHeader();
				if (header != null) {
					setFont(header.getFont());
				}
			}

			this.rollOver = (column == TableHeaderUI.this.highlightedColumn);

			if (TableHeaderUI.this.header != null && TableHeaderUI.this.header.getDraggedColumn() != null) {
				this.isPressed = (viewIndexForColumn(TableHeaderUI.this.header.getDraggedColumn()) == column)
						|| (column == TableHeaderUI.this.pressedColumn);
			} else {
				this.isPressed = false;
			}
			setText((value == null) ? "" : value.toString());
			setBorder(null);
			return this;
		}

		@Override
		public Insets getInsets() {
			return new Insets(2, 4, 2, 4);
		}

		private int viewIndexForColumn(TableColumn aColumn) {
			TableColumnModel cm = TableHeaderUI.this.header.getColumnModel();
			for (int column = 0; column < cm.getColumnCount(); column++) {
				if (cm.getColumn(column) == aColumn) {
					return column;
				}
			}
			return -1;
		}

		@Override
		public void paint(Graphics g) {
			int h = this.getHeight();
			int w = this.getWidth();

			if (this.isPressed) {
				g.setColor(RapidLookTools.getColors().getTableHeaderColors()[0]);
				g.drawLine(0, 0, w - 1, 0);
				g.setColor(RapidLookTools.getColors().getTableHeaderColors()[1]);
				g.drawLine(0, 1, w - 1, 1);
				Graphics2D g2 = (Graphics2D) g;
				g2.setPaint(new GradientPaint(0, 2, RapidLookTools.getColors().getTableHeaderColors()[2], 0, h - 1,
						RapidLookTools.getColors().getTableHeaderColors()[3]));
				g2.fillRect(0, 2, w, h - 1);
				g.setColor(RapidLookTools.getColors().getTableHeaderColors()[0]);
				g.drawLine(0, h - 1, w - 1, h - 1);
			} else {
				if (this.rollOver) {
					g.setColor(RapidLookTools.getColors().getTableHeaderColors()[4]);
					g.drawLine(0, 0, w - 1, 0);

					g.setColor(RapidLookTools.getColors().getTableHeaderColors()[5]);
					g.drawLine(0, h - 2, w - 1, h - 2);
				} else {
					g.setColor(RapidLookTools.getColors().getTableHeaderColors()[6]);
					g.drawLine(0, 0, w - 1, 0);
					g.setColor(RapidLookTools.getColors().getTableHeaderColors()[7]);
					g.drawLine(0, h - 2, w - 1, h - 2);
				}

				Graphics2D g2 = (Graphics2D) g;
				g2.setPaint(new GradientPaint(0, 1, RapidLookTools.getColors().getTableHeaderColors()[8], 0, h - 5,
						RapidLookTools.getColors().getTableHeaderColors()[9]));
				g2.fillRect(0, 1, w, h - 5);
				g.setColor(RapidLookTools.getColors().getTableHeaderColors()[10]);
				g.drawLine(0, h - 5, w - 1, h - 5);
				g.setColor(RapidLookTools.getColors().getTableHeaderColors()[11]);
				g.drawLine(0, h - 4, w - 1, h - 4);
				g.setColor(RapidLookTools.getColors().getTableHeaderColors()[12]);
				g.drawLine(0, h - 3, w - 1, h - 3);
				g.setColor(RapidLookTools.getColors().getTableHeaderColors()[13]);
				g.drawLine(0, h - 1, w - 1, h - 1);
			}

			super.paint(g);
		}
	}
}
