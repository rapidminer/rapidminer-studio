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

import com.rapidminer.gui.tools.ListHoverHelper;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.template.Template;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;


/**
 * A fancy cell renderer for rendering title and short description of a {@link Template}, adding a
 * nice frame.
 * 
 * @author Simon Fischer
 * 
 */
public class TemplateCellRenderer extends DefaultListCellRenderer {

	private static final long serialVersionUID = 1L;

	private boolean selected = false;

	private boolean hovered = false;

	public static final int LABEL_WIDTH = 380;

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		label.setVerticalTextPosition(SwingConstants.TOP);
		label.setIconTextGap(18);
		label.setVerticalAlignment(SwingConstants.TOP);
		this.selected = isSelected;
		Template template = (Template) value;
		label.setIcon(template.getIcon());
		if (LABEL_WIDTH > 0) {
			label.setText("<html><div style=\"width:" + (LABEL_WIDTH - 120)
					+ "px\"><div style=\"font-size:14px;font-weight:bold;\">" + template.getTitle()
					+ "</div><div style=\"color:#888888;\">" + template.getShortDescription() + "</div></div></html>");
		}
		label.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
		label.setBackground(Color.WHITE);
		label.setForeground(Color.BLACK);

		hovered = ListHoverHelper.index(list) == index;

		return label;
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (selected) {
			int fillPadding = 10;
			((Graphics2D) g).setPaint(new GradientPaint(new Point2D.Float(fillPadding, 0), Color.WHITE, new Point2D.Float(
					getWidth() / 2, 0), SwingTools.LIGHTEST_BLUE, true));
			g.fillRect(fillPadding, 0, getWidth() - fillPadding * 2, getHeight());

			((Graphics2D) g).setPaint(new GradientPaint(new Point2D.Float(0, 0), Color.WHITE, new Point2D.Float(
					getWidth() / 2, 0), Color.BLACK, true));
			((Graphics2D) g).setStroke(new BasicStroke(2.5f));
			g.drawLine(0, 2, getWidth(), 2);
			g.drawLine(0, getHeight() - 2, getWidth(), getHeight() - 2);
		} else if (hovered) {
			((Graphics2D) g).setPaint(new GradientPaint(new Point2D.Float(0, 0), Color.WHITE, new Point2D.Float(
					getWidth() / 2, 0), SwingTools.RAPIDMINER_ORANGE, true));
			((Graphics2D) g).setStroke(new BasicStroke(2.5f));
			g.drawLine(0, 2, getWidth(), 2);
			g.drawLine(0, getHeight() - 2, getWidth(), getHeight() - 2);
		}
		super.paintComponent(g);
	}
}
