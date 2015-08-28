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
package com.rapidminer.template.gui;

import com.rapidminer.example.AttributeRole;
import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.Ontology;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;


/**
 * Cell renderer with alternating colors for header and body. To be used with a table with no grid
 * and other lines and some padding added by setting an appropriate row height. Colors used are
 * {@link Color#WHITE} and {@link #ALTERNATING_ROW_COLOR}.
 * 
 * @author Simon Fischer
 * 
 */
public class DataCellRenderer extends DefaultTableCellRenderer {

	private static final int PADDING = 6;
	private static final Color ALTERNATING_ROW_COLOR = new Color(230, 230, 230);
	private static final NumberFormat NUMBER_FORMAT = new DecimalFormat("0.###");
	private static final long serialVersionUID = 1L;

	private final boolean isHeader;
	private Font headerFont;
	private Font bodyFont;

	private ExampleSetTableModel tableModel;
	private boolean rightAlignNumbers = false;

	public DataCellRenderer(ExampleSetTableModel model, boolean isHeader) {
		super();
		this.isHeader = isHeader;
		this.bodyFont = getFont();
		this.headerFont = getFont().deriveFont(Font.BOLD);
		setModel(model);
	}

	protected void setModel(ExampleSetTableModel model) {
		this.tableModel = model;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		AttributeRole attRole = tableModel.getAttributesByIndex().get(column);
		if (value instanceof Number) {
			if (attRole.getAttribute().getValueType() == Ontology.INTEGER) {
				cell.setText(String.valueOf(Math.round(((Number) value).doubleValue())));
			} else {
				cell.setText(NUMBER_FORMAT.format(((Number) value).doubleValue()));
			}
		}
		boolean isSpecial = attRole.isSpecial();
		if (isHeader) {
			if (isSpecial) {
				String roleName = attRole.getSpecialName();
				Color color = AttributeGuiTools.getColorForAttributeRole(roleName);
				cell.setBackground(SwingTools.darkenColor(color));
			} else {
				cell.setBackground(Color.WHITE);
			}
			cell.setForeground(Color.BLACK);
			cell.setFont(headerFont);
			cell.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK),
					BorderFactory.createEmptyBorder(0, 6, 0, 0)));
		} else {
			if (isSpecial) {
				String roleName = attRole.getSpecialName();
				Color color = AttributeGuiTools.getColorForAttributeRole(roleName);
				cell.setBackground((row % 2 == 1) ? SwingTools.darkenColor(color) : color);
			} else {
				cell.setBackground((row % 2 == 0) ? Color.WHITE : ALTERNATING_ROW_COLOR);
			}
			cell.setForeground(Color.DARK_GRAY);
			cell.setFont(bodyFont);
			cell.setBorder(BorderFactory.createEmptyBorder(0, PADDING, 0, 0));
		}
		if (isRightAlignNumbers() && (value instanceof Number)) {
			cell.setHorizontalAlignment(SwingConstants.RIGHT);
		} else {
			cell.setHorizontalAlignment(SwingConstants.LEFT);
		}
		return cell;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension dim = super.getPreferredSize();
		return new Dimension((int) dim.getWidth() + 5, (int) dim.getHeight() + 5);
	}

	@Override
	public Dimension getMinimumSize() {
		Dimension dim = super.getMinimumSize();
		return new Dimension((int) dim.getWidth() + 5, (int) dim.getHeight() + 5);
	}

	@Override
	public Dimension getMaximumSize() {
		Dimension dim = super.getMaximumSize();
		return new Dimension((int) dim.getWidth() + 5, (int) dim.getHeight() + 5);
	}

	public boolean isRightAlignNumbers() {
		return rightAlignNumbers;
	}

	public void setRightAlignNumbers(boolean rightAlignNumbers) {
		this.rightAlignNumbers = rightAlignNumbers;
	}
}
