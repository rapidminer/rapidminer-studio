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
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import com.rapidminer.tools.Tools;


/**
 * The default table cell renderer for all viewer tables. Provides the correct border and colors.
 * Numbers will be formatted with the generic number of fraction digits. It is possible to restrict
 * the maximum length of strings shown. This might speed up rendering for very large strings, which
 * of course can't be shown completely anyway.
 *
 * @author Ingo Mierswa, Marco Boeck
 */
public class ColoredTableCellRenderer implements TableCellRenderer {

	private static final Border CELL_BORDER = BorderFactory.createEmptyBorder(0, 10, 0, 5);

	/** use default Java one for best performance */
	private final DefaultTableCellRenderer defaultCellRenderer = new DefaultTableCellRenderer();

	private int maximalTextLength = Integer.MAX_VALUE;
	private boolean cutOnFirstLineBreak = false;

	private int dateFormat = ExtendedJTable.NO_DATE_FORMAT;

	public void setColor(Color color) {
		defaultCellRenderer.setBackground(color);
	}

	public void setDateFormat(int dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * Use this method to set the maximal text length. Enter Integer.MAX_VALUE to disable text
	 * cutting (default)
	 */
	public void setMaximalTextLength(int maxLength) {
		this.maximalTextLength = maxLength;
	}

	/**
	 * This enables or disables the cutting on the first linebreak of a string. Normally only one
	 * line is shown, so this might be useful to speed up text rendering.
	 */
	public void setCutOnFirstLineBreak(boolean enable) {
		this.cutOnFirstLineBreak = enable;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
			int column) {

		// prepare text
		String text = null;
		if (value instanceof Number) {
			Number number = (Number) value;
			double numberValue = number.doubleValue();
			text = Tools.formatIntegerIfPossible(numberValue);
		} else {
			if (value != null) {
				if (value instanceof Date) {
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
					// cutting on line
					if (cutOnFirstLineBreak) {
						int indexOfLineBreak = text.indexOf("\n");
						if (indexOfLineBreak > 0) {
							text = text.substring(0, indexOfLineBreak);
						}
					}
					// cutting on maximal length
					if (text.length() > maximalTextLength) {
						text = text.substring(0, maximalTextLength);
					}

				}
			} else {
				text = "?";
			}
		}

		// use java renderer to draw text
		JLabel l = (JLabel) defaultCellRenderer.getTableCellRendererComponent(table, text, isSelected, hasFocus, row,
				column);
		l.setBorder(CELL_BORDER);

		// highlight hovered row?
		if (table instanceof ExtendedJTable) {
			ExtendedJTable t = (ExtendedJTable) table;
			if (t.isRowHighlighting()) {
				if (t.isRowHighlighted(row)) {
					t.setLastHighlightedRow(row);
					l.setBackground(SwingTools.darkenColor(l.getBackground(), 0.95f));
				}
			}
		}

		return l;
	}
}
