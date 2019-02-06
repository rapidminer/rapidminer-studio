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
package com.rapidminer.gui.look;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicArrowButton;


/**
 * The button used at the end of scrollbars.
 *
 *
 * @author Ingo Mierswa, Marco Boeck
 */
public class GenericArrowButton extends BasicArrowButton implements UIResource {

	private static final long serialVersionUID = 8079721815873790893L;

	private static final Stroke ARROW_STROKE = new BasicStroke(1.5f);

	private static final int OFFSET = 4;

	public GenericArrowButton(int direction, int w, int h) {
		super(direction);

		this.setSize(w, h);
		this.setOpaque(false);
		switch (direction) {
			case NORTH:
			case SOUTH:
			case EAST:
			case WEST:
				this.direction = direction;
				break;
			default:
				throw new IllegalArgumentException("invalid direction");
		}

		setRequestFocusEnabled(false);
		setForeground(UIManager.getColor("ScrollBar.foreground"));
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(17, 17);
	}

	@Override
	public Dimension getMinimumSize() {
		return getPreferredSize();
	}

	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}

	@Override
	public boolean isFocusable() {
		return false;
	}

	@Override
	public void paint(Graphics g) {
		int w = getWidth();
		int h = getHeight();

		if (isOpaque()) {
			g.setColor(Colors.WHITE);
			g.fillRect(0, 0, w, h);
		}

		g.setColor(Colors.SCROLLBAR_ARROW_BACKGROUND);
		g.fillRect(0, 0, w, h);

		boolean isPressed = getModel().isPressed();
		boolean isRollover = getModel().isRollover();

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		switch (this.direction) {
			case NORTH:
				paintNorthArrow(g2, w, h, isPressed, isRollover);
				break;
			case SOUTH:
				paintSouthArrow(g2, w, h, isPressed, isRollover);
				break;
			case EAST:
				paintEastArrow(g2, w, h, isPressed, isRollover);
				break;
			case WEST:
				paintWestArrow(g2, w, h, isPressed, isRollover);
				break;
		}
	}

	private void paintWestArrow(Graphics2D g2, int w, int h, boolean isPressed, boolean isRollover) {
		g2.setColor(Colors.SCROLLBAR_ARROW_BORDER);
		g2.drawLine(0, 0, w - 1, 0);

		if (isPressed) {
			g2.setColor(Colors.SCROLLBAR_ARROW_PRESSED);
		} else if (isRollover) {
			g2.setColor(Colors.SCROLLBAR_ARROW_ROLLOVER);
		} else {
			g2.setColor(Colors.SCROLLBAR_ARROW);
		}

		g2.setStroke(ARROW_STROKE);
		g2.drawLine(w - 6, OFFSET, 6, h / 2);
		g2.drawLine(6, h / 2, w - 6, h - OFFSET);
	}

	private void paintEastArrow(Graphics2D g2, int w, int h, boolean isPressed, boolean isRollover) {
		g2.setColor(Colors.SCROLLBAR_ARROW_BORDER);
		g2.drawLine(0, 0, w - 1, 0);

		if (isPressed) {
			g2.setColor(Colors.SCROLLBAR_ARROW_PRESSED);
		} else if (isRollover) {
			g2.setColor(Colors.SCROLLBAR_ARROW_ROLLOVER);
		} else {
			g2.setColor(Colors.SCROLLBAR_ARROW);
		}

		g2.setStroke(ARROW_STROKE);
		g2.drawLine(6, OFFSET, w - 6, h / 2);
		g2.drawLine(w - 6, h / 2, 6, h - OFFSET);
	}

	private void paintSouthArrow(Graphics2D g2, int w, int h, boolean isPressed, boolean isRollover) {
		g2.setColor(Colors.SCROLLBAR_ARROW_BORDER);
		g2.drawLine(0, 0, 0, h - 1);

		if (isPressed) {
			g2.setColor(Colors.SCROLLBAR_ARROW_PRESSED);
		} else if (isRollover) {
			g2.setColor(Colors.SCROLLBAR_ARROW_ROLLOVER);
		} else {
			g2.setColor(Colors.SCROLLBAR_ARROW);
		}

		g2.setStroke(ARROW_STROKE);
		g2.drawLine(OFFSET, 6, w / 2, h - 6);
		g2.drawLine(w / 2, h - 6, w - OFFSET, 6);
	}

	private void paintNorthArrow(Graphics2D g2, int w, int h, boolean isPressed, boolean isRollover) {
		g2.setColor(Colors.SCROLLBAR_ARROW_BORDER);
		g2.drawLine(0, 0, 0, h - 1);

		if (isPressed) {
			g2.setColor(Colors.SCROLLBAR_ARROW_PRESSED);
		} else if (isRollover) {
			g2.setColor(Colors.SCROLLBAR_ARROW_ROLLOVER);
		} else {
			g2.setColor(Colors.SCROLLBAR_ARROW);
		}

		g2.setStroke(ARROW_STROKE);
		g2.drawLine(OFFSET, h - 6, w / 2, 6);
		g2.drawLine(w / 2, 6, w - OFFSET, h - 6);
	}
}
