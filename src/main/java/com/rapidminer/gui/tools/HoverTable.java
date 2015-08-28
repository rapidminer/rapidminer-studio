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
package com.rapidminer.gui.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;


/**
 * An extended {@link JTable} which supports hovering.
 * 
 * @author Marcel Michel
 */
public class HoverTable extends JTable {

	private static final long serialVersionUID = 1L;

	public static final Color DEFAULT_EVEN_ROW_COLOR = Color.WHITE;
	public static final Color DEFAULT_ODD_ROW_COLOR = UIManager.getColor("Panel.background");
	public static final Color DEFAULT_HIGHLIGHTING_COLOR = new Color(225, 225, 225);

	private Color oddRowColor;
	private Color evenRowColor;
	private Color highlightingColor;

	private int currentRowIndex = 0;
	private int currentColIndex = 0;

	private boolean highlightEnabled;

	private class HoveringMouseAdapter extends MouseMotionAdapter {

		@Override
		public void mouseMoved(MouseEvent e) {
			JTable table = (JTable) e.getSource();
			currentRowIndex = table.rowAtPoint(e.getPoint());
			currentColIndex = table.columnAtPoint(e.getPoint());
			table.repaint();
		}
	}

	private class EnterExitMouseAdapter extends MouseAdapter {

		@Override
		public void mouseExited(MouseEvent e) {
			if (!SwingTools.isMouseEventExitedToChildComponents(HoverTable.this, e)) {
				highlightEnabled(false);
				HoverTable.this.repaint();
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			highlightEnabled(true);
			HoverTable.this.repaint();
		}
	}

	private class HoveringCellRenderer extends JLabel implements TableCellRenderer {

		private static final long serialVersionUID = 1L;

		public HoveringCellRenderer() {
			setOpaque(true);
			setHorizontalAlignment(SwingConstants.CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (row == currentRowIndex && highlightEnabled) {
				this.setBackground(highlightingColor);
			} else if (row % 2 == 0) {
				this.setBackground(evenRowColor);
			} else {
				this.setBackground(oddRowColor);
			}
			this.setText(String.valueOf(value));
			return this;
		}
	}

	public HoverTable(TableModel model) {
		this(model, DEFAULT_ODD_ROW_COLOR, DEFAULT_EVEN_ROW_COLOR, DEFAULT_HIGHLIGHTING_COLOR);
	}

	public HoverTable(TableModel model, Color oddRowColor, Color evenRowColor, Color highlightingColor) {
		super(model);
		setOpaque(false);
		setDefaultRenderer(Object.class, new HoveringCellRenderer());
		setShowGrid(false);
		setHighlightingColor(highlightingColor);
		setOddRowColor(oddRowColor);
		setEvenRowColor(evenRowColor);
		addMouseMotionListener(new HoveringMouseAdapter());
		addMouseListener(new EnterExitMouseAdapter());
	}

	private void highlightEnabled(boolean value) {
		highlightEnabled = value;
	}

	public void setHighlightingColor(Color color) {
		highlightingColor = color;
	}

	public void setOddRowColor(Color color) {
		oddRowColor = color;
	}

	public void setEvenRowColor(Color color) {
		evenRowColor = color;
	}

	public int getCurrentRowIndex() {
		return currentRowIndex;
	}

	public int getCurrentColumnIndex() {
		return currentColIndex;
	}
}
