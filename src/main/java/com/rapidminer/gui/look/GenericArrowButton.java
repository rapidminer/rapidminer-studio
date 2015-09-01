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
package com.rapidminer.gui.look;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicArrowButton;


/**
 * The button used at the end of scrollbars.
 * 
 * 
 * @author Ingo Mierswa
 */
public class GenericArrowButton extends BasicArrowButton implements UIResource {

	private static final long serialVersionUID = 8079721815873790893L;

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
			g.setColor((Color) UIManager.get("ScrollBar.background"));
			g.fillRect(0, 0, w, h);
		}

		boolean isPressed = getModel().isPressed();

		switch (this.direction) {
			case NORTH:
				paintNorthArrow(g, w, h, isPressed);
				break;

			case SOUTH:
				paintSouthArrow(g, w, h, isPressed);
				break;

			case EAST:
				paintEastArrow(g, w, h, isPressed);
				break;

			case WEST:
				paintWestArrow(g, w, h, isPressed);
				break;
		}
	}

	private void paintWestArrow(Graphics g, int w, int h, boolean isPressed) {
		int baseX = 5;
		int baseY = 3;

		if (isPressed) {
			baseX--;

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][0]);
			g.drawLine(0, 4, 0, h - 5);
			g.drawLine(1, 2, 1, h - 3);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(1, 3, 1, h - 4);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][0]);
			g.drawLine(2, 1, 2, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(2, 2, 2, h - 3);
			g.drawLine(3, 1, 3, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][2]);
			g.drawLine(4, 0, w - 1, 0);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][3]);
			g.drawLine(5, 0, w - 1, 0);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][4]);
			g.drawLine(4, 1, w - 2, 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][2]);
			g.drawLine(4, h - 1, w - 1, h - 1);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][3]);
			g.drawLine(5, h - 1, w - 1, h - 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][4]);
			g.drawLine(4, h - 2, w - 2, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.fillRect(4, 2, w - 5, h - 4);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][5]);
			g.drawLine(w - 1, 1, w - 1, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(w - 2, 2, w - 2, h - 3);

		} else {
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][0]);
			g.drawLine(0, 4, 0, h - 5);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][1]);
			g.drawLine(1, 2, 1, h - 3);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][2]);
			g.drawLine(1, 3, 1, h - 4);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][3]);
			g.drawLine(1, 4, 1, h - 5);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][4]);
			g.drawLine(2, 1, 2, h - 2);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][5]);
			g.drawLine(2, 2, 2, h - 3);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][6]);
			g.drawLine(3, 1, 3, h - 2);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][7]);
			g.drawLine(3, 2, 3, h - 3);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][8]);
			g.drawLine(4, 0, w - 1, 0);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][9]);
			g.drawLine(5, 0, w - 1, 0);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][10]);
			g.drawLine(4, 1, w - 2, 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][8]);
			g.drawLine(4, h - 1, w - 1, h - 1);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][9]);
			g.drawLine(5, h - 1, w - 1, h - 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][10]);
			g.drawLine(4, h - 2, w - 2, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][11]);
			g.fillRect(4, 2, w - 6, h - 4);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][12]);
			g.drawLine(w - 1, 1, w - 1, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][13]);
			g.drawLine(w - 2, 2, w - 2, h - 3);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][14]);
			g.drawLine(w - 3, 3, w - 3, h - 4);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][15]);
			g.drawLine(w - 4, 4, w - 4, h - 5);
		}

		int yCenter = h / 2 - 9;
		g.translate(0, yCenter);

		g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][16]);
		g.drawLine(baseX, baseY + 5, baseX + 4, baseY + 1);
		g.drawLine(baseX, baseY + 5, baseX + 4, baseY + 9);

		g.setColor(Color.white);
		g.drawLine(baseX + 1, baseY + 5, baseX + 5, baseY + 1);
		g.drawLine(baseX + 1, baseY + 5, baseX + 5, baseY + 9);

		g.drawLine(baseX + 2, baseY + 5, baseX + 4, baseY + 3);
		g.drawLine(baseX + 2, baseY + 5, baseX + 4, baseY + 7);

		g.drawLine(baseX + 3, baseY + 5, baseX + 3, baseY + 5);

		g.translate(0, -yCenter);

	}

	private void paintEastArrow(Graphics g, int w, int h, boolean isPressed) {
		int baseX = 5;
		int baseY = 3;

		if (isPressed) {
			baseX++;

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][0]);
			g.drawLine(w - 1, 4, w - 1, h - 5);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][0]);
			g.drawLine(w - 2, 2, w - 2, h - 3);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(w - 2, 3, w - 2, h - 4);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][0]);
			g.drawLine(w - 3, 1, w - 3, h - 2);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(w - 3, 2, w - 3, h - 3);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(w - 4, 1, w - 4, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][2]);
			g.drawLine(0, 0, w - 5, 0);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][3]);
			g.drawLine(0, 0, w - 6, 0);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][4]);
			g.drawLine(1, 1, w - 5, 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][2]);
			g.drawLine(0, h - 1, w - 5, h - 1);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][3]);
			g.drawLine(0, h - 1, w - 6, h - 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][4]);
			g.drawLine(1, h - 2, w - 5, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.fillRect(1, 2, w - 5, h - 4);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][5]);
			g.drawLine(0, 1, 0, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(1, 2, 1, h - 3);
		} else {
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][0]);
			g.drawLine(w - 1, 4, w - 1, h - 5);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][1]);
			g.drawLine(w - 2, 2, w - 2, h - 3);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][2]);
			g.drawLine(w - 2, 3, w - 2, h - 4);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][3]);
			g.drawLine(w - 2, 4, w - 2, h - 5);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][4]);
			g.drawLine(w - 3, 1, w - 3, h - 2);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][5]);
			g.drawLine(w - 3, 2, w - 3, h - 3);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][6]);
			g.drawLine(w - 4, 1, w - 4, h - 2);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][7]);
			g.drawLine(w - 4, 2, w - 4, h - 3);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][8]);
			g.drawLine(0, 0, w - 5, 0);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][9]);
			g.drawLine(0, 0, w - 6, 0);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][10]);
			g.drawLine(1, 1, w - 5, 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][8]);
			g.drawLine(0, h - 1, w - 5, h - 1);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][9]);
			g.drawLine(0, h - 1, w - 6, h - 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][10]);
			g.drawLine(1, h - 2, w - 5, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][11]);
			g.fillRect(1, 2, w - 5, h - 4);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][12]);
			g.drawLine(0, 1, 0, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][13]);
			g.drawLine(1, 2, 1, h - 3);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][14]);
			g.drawLine(2, 3, 2, h - 4);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][15]);
			g.drawLine(3, 4, 3, h - 5);
		}

		int yCenter = h / 2 - 9;
		g.translate(0, yCenter);

		g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][16]);
		g.drawLine(baseX + 6, baseY + 5, baseX + 2, baseY + 1);
		g.drawLine(baseX + 6, baseY + 5, baseX + 2, baseY + 9);

		g.setColor(Color.white);
		g.drawLine(baseX + 5, baseY + 5, baseX + 1, baseY + 1);
		g.drawLine(baseX + 5, baseY + 5, baseX + 1, baseY + 9);

		g.drawLine(baseX + 4, baseY + 5, baseX + 2, baseY + 3);
		g.drawLine(baseX + 4, baseY + 5, baseX + 2, baseY + 7);

		g.drawLine(baseX + 3, baseY + 5, baseX + 3, baseY + 5);

		g.translate(0, -yCenter);
	}

	private void paintSouthArrow(Graphics g, int w, int h, boolean isPressed) {
		int baseX = 4;
		int baseY = 5;

		if (isPressed) {
			baseY++;

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][7]);
			g.drawLine(4, h - 1, w - 5, h - 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][0]);
			g.drawLine(2, h - 2, w - 3, h - 2);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(3, h - 2, w - 4, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][0]);
			g.drawLine(1, h - 3, w - 2, h - 3);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(2, h - 3, w - 3, h - 3);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(1, h - 4, w - 2, h - 4);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][2]);
			g.drawLine(0, 0, 0, h - 5);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][3]);
			g.drawLine(0, 0, 0, h - 6);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(1, 1, 1, h - 5);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][2]);
			g.drawLine(w - 1, 0, w - 1, h - 5);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][3]);
			g.drawLine(w - 1, 0, w - 1, h - 6);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][4]);
			g.drawLine(w - 2, 1, w - 2, h - 5);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.fillRect(2, 2, w - 4, h - 6);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][5]);
			g.drawLine(1, 0, w - 2, 0);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(2, 1, w - 3, 1);
		} else {
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][0]);
			g.drawLine(4, h - 1, w - 5, h - 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][1]);
			g.drawLine(2, h - 2, w - 3, h - 2);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][2]);
			g.drawLine(3, h - 2, w - 4, h - 2);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][3]);
			g.drawLine(4, h - 2, w - 5, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][4]);
			g.drawLine(1, h - 3, w - 2, h - 3);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][5]);
			g.drawLine(2, h - 3, w - 3, h - 3);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][6]);
			g.drawLine(1, h - 4, w - 2, h - 4);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][7]);
			g.drawLine(2, h - 4, w - 3, h - 4);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][8]);
			g.drawLine(0, 0, 0, h - 5);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][9]);
			g.drawLine(0, 0, 0, h - 6);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][10]);
			g.drawLine(1, 1, 1, h - 5);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][8]);
			g.drawLine(w - 1, 0, w - 1, h - 5);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][9]);
			g.drawLine(w - 1, 0, w - 1, h - 6);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][10]);
			g.drawLine(w - 2, 1, w - 2, h - 5);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][11]);
			g.fillRect(2, 2, w - 4, h - 6);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][12]);
			g.drawLine(1, 0, w - 2, 0);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][13]);
			g.drawLine(2, 1, w - 3, 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][14]);
			g.drawLine(3, 2, w - 4, 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][15]);
			g.drawLine(4, 3, w - 5, 3);
		}

		int xCenter = w / 2 - 9;
		g.translate(xCenter, 0);

		g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][16]);
		g.drawLine(baseX + 5, baseY + 6, baseX + 1, baseY + 2);
		g.drawLine(baseX + 5, baseY + 6, baseX + 9, baseY + 2);

		g.setColor(Color.white);
		g.drawLine(baseX + 5, baseY + 5, baseX + 1, baseY + 1);
		g.drawLine(baseX + 5, baseY + 5, baseX + 9, baseY + 1);

		g.drawLine(baseX + 5, baseY + 4, baseX + 3, baseY + 2);
		g.drawLine(baseX + 5, baseY + 4, baseX + 7, baseY + 2);

		g.drawLine(baseX + 5, baseY + 3, baseX + 5, baseY + 3);

		g.translate(-xCenter, 0);
	}

	private void paintNorthArrow(Graphics g, int w, int h, boolean isPressed) {
		int baseX = 4;
		int baseY = 5;

		if (isPressed) {
			baseY--;

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][7]);
			g.drawLine(4, 0, w - 5, 0);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][6]);
			g.drawLine(2, 1, w - 3, 1);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(3, 1, w - 4, 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][6]);
			g.drawLine(1, 2, w - 2, 2);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(2, 2, w - 3, 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(1, 3, w - 2, 3);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][2]);
			g.drawLine(0, 4, 0, h - 1);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][3]);
			g.drawLine(0, 5, 0, h - 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][4]);
			g.drawLine(1, 4, 1, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][2]);
			g.drawLine(w - 1, 4, w - 1, h - 1);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][3]);
			g.drawLine(w - 1, 5, w - 1, h - 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][4]);
			g.drawLine(w - 2, 4, w - 2, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.fillRect(2, 4, w - 4, h - 6);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][5]);
			g.drawLine(1, h - 1, w - 2, h - 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[0][1]);
			g.drawLine(2, h - 2, w - 3, h - 2);
		} else {
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][0]);
			g.drawLine(4, 0, w - 5, 0);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][1]);
			g.drawLine(2, 1, w - 3, 1);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][2]);
			g.drawLine(3, 1, w - 4, 1);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][3]);
			g.drawLine(4, 1, w - 5, 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][4]);
			g.drawLine(1, 2, w - 2, 2);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][5]);
			g.drawLine(2, 2, w - 3, 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][6]);
			g.drawLine(1, 3, w - 2, 3);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][7]);
			g.drawLine(2, 3, w - 3, 3);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][8]);
			g.drawLine(0, 4, 0, h - 1);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][9]);
			g.drawLine(0, 5, 0, h - 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][10]);
			g.drawLine(1, 4, 1, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][8]);
			g.drawLine(w - 1, 4, w - 1, h - 1);
			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][9]);
			g.drawLine(w - 1, 5, w - 1, h - 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][10]);
			g.drawLine(w - 2, 4, w - 2, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][11]);
			g.fillRect(2, 4, w - 4, h - 6);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][12]);
			g.drawLine(1, h - 1, w - 2, h - 1);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][13]);
			g.drawLine(2, h - 2, w - 3, h - 2);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][14]);
			g.drawLine(3, h - 3, w - 4, h - 3);

			g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][15]);
			g.drawLine(4, h - 4, w - 5, h - 4);
		}

		int xCenter = w / 2 - 9;
		g.translate(xCenter, 0);

		g.setColor(RapidLookTools.getColors().getArrowButtonColors()[1][16]);
		g.drawLine(baseX + 1, baseY + 4, baseX + 5, baseY);
		g.drawLine(baseX + 9, baseY + 4, baseX + 5, baseY);

		g.setColor(Color.white);
		g.drawLine(baseX + 5, baseY + 1, baseX + 1, baseY + 5);
		g.drawLine(baseX + 5, baseY + 1, baseX + 9, baseY + 5);

		g.drawLine(baseX + 5, baseY + 2, baseX + 3, baseY + 4);
		g.drawLine(baseX + 5, baseY + 2, baseX + 7, baseY + 4);

		g.drawLine(baseX + 5, baseY + 3, baseX + 5, baseY + 3);

		g.translate(-xCenter, 0);
	}
}
