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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;

import javax.swing.JButton;

import com.rapidminer.gui.look.Colors;


/**
 * A spinner button.
 *
 * @author Ingo Mierswa
 */
public class SpinnerButton extends JButton {

	private static final long serialVersionUID = 7401548433114573444L;

	private boolean up = false;

	private Dimension dimension;

	public SpinnerButton(String type) {
		if (type.equals("up")) {
			this.up = true;
		}
		setBorder(null);
		setContentAreaFilled(false);
	}

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (this.up) {
			paintUpButton(g2);
		} else {
			paintDownButton(g2);
		}
		drawArrow(g2);
	}

	@Override
	protected void paintBorder(Graphics g) {}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(16, 6);
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(16, 6);
	}

	@Override
	public Dimension getMaximumSize() {
		return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	private void paintDownButton(Graphics2D g2) {
		int w = (int) getSize().getWidth();
		int h = (int) getSize().getHeight();

		if (getModel().isEnabled()) {
			if (getModel().isPressed()) {
				g2.setColor(Colors.SPINNER_BUTTON_PRESSED);
			} else if (getModel().isRollover()) {
				g2.setColor(Colors.SPINNER_BUTTON_ROLLOVER);
			} else {
				g2.setColor(Colors.SPINNER_BUTTON_BACKGROUND);
			}
		} else {
			g2.setColor(Colors.SPINNER_BUTTON_DISABLED);
		}

		g2.fillRect(1, 0, w - 1, h);
	}

	private void paintUpButton(Graphics2D g2) {
		this.dimension = getSize();
		int h = (int) this.dimension.getHeight();
		int w = (int) this.dimension.getWidth();

		if (getModel().isEnabled()) {
			if (getModel().isPressed()) {
				g2.setColor(Colors.SPINNER_BUTTON_PRESSED);
			} else if (getModel().isRollover()) {
				g2.setColor(Colors.SPINNER_BUTTON_ROLLOVER);
			} else {
				g2.setColor(Colors.SPINNER_BUTTON_BACKGROUND);
			}
		} else {
			g2.setColor(Colors.SPINNER_BUTTON_DISABLED);
		}

		g2.fillRect(1, 0, w, h);
	}

	private void drawArrow(Graphics2D g2) {
		this.dimension = getSize();

		int baseY = (int) (this.dimension.getHeight() / 2 - 2);
		if (!this.up) {
			baseY++;
		}

		g2.translate(5, baseY);
		g2.setColor(Colors.SPINNER_ARROW);

		if (this.up) {
			Polygon triangle = new Polygon(new int[] { 0, 6, 3 }, new int[] { 4, 4, 0 }, 3);
			g2.fillPolygon(triangle);
		} else {
			Polygon triangle = new Polygon(new int[] { 0, 6, 3 }, new int[] { 0, 0, 4 }, 3);
			g2.fillPolygon(triangle);
		}
		g2.translate(-6, -baseY);
	}
}
