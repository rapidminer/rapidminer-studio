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

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JButton;


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

	private void paintDownButton(Graphics g) {
		int w = (int) getSize().getWidth();
		int h = (int) getSize().getHeight();

		if (getModel().isEnabled()) {
			if (getModel().isPressed()) {
				g.setColor(RapidLookTools.getColors().getSpinnerColors()[0]);
				g.drawLine(1, 0, w - 1, 0);
				g.fillRect(1, 1, w - 1, h - 3);
				g.drawLine(1, h - 4, w - 1, h - 4);
				g.drawLine(1, h - 3, w - 2, h - 3);
				g.drawLine(1, h - 2, w - 3, h - 2);
				g.drawLine(1, h - 1, w - 5, h - 1);
				g.drawLine(1, 0, w - 1, 0);
				g.drawLine(w - 1, 1, w - 1, h - 3);
				g.drawLine(w - 2, 1, w - 2, h - 4);
				g.drawLine(w - 3, 1, w - 3, h - 4);

				g.setColor(RapidLookTools.getColors().getSpinnerColors()[1]);
				g.drawLine(0, 0, 0, h);
			} else {
				g.setColor(RapidLookTools.getColors().getSpinnerColors()[2]);
				g.drawLine(1, 0, w - 1, 0);
				g.fillRect(1, 1, w - 1, h - 3);

				g.setColor(RapidLookTools.getColors().getSpinnerColors()[3]);
				g.drawLine(1, h - 4, w - 1, h - 4);

				g.setColor(RapidLookTools.getColors().getSpinnerColors()[4]);
				g.drawLine(1, h - 3, w - 2, h - 3);

				g.setColor(RapidLookTools.getColors().getSpinnerColors()[5]);
				g.drawLine(1, h - 2, w - 3, h - 2);

				g.setColor(RapidLookTools.getColors().getSpinnerColors()[6]);
				g.drawLine(1, h - 1, w - 5, h - 1);

				g.setColor(RapidLookTools.getColors().getSpinnerColors()[3]);
				g.drawLine(1, 0, w - 1, 0);

				g.setColor(RapidLookTools.getColors().getSpinnerColors()[7]);
				g.drawLine(0, 0, 0, h);

				g.setColor(RapidLookTools.getColors().getSpinnerColors()[4]);
				g.drawLine(w - 1, 1, w - 1, h - 3);

				g.setColor(RapidLookTools.getColors().getSpinnerColors()[8]);
				g.drawLine(w - 2, 1, w - 2, h - 4);

				g.setColor(RapidLookTools.getColors().getSpinnerColors()[3]);
				g.drawLine(w - 3, 1, w - 3, h - 4);
			}
		} else {
			g.setColor(RapidLookTools.getColors().getSpinnerColors()[8]);
			g.drawLine(1, 0, w - 1, 0);
			g.fillRect(1, 1, w - 1, h - 3);
			g.drawLine(1, h - 4, w - 1, h - 4);
			g.drawLine(1, h - 3, w - 2, h - 3);
			g.drawLine(1, h - 2, w - 3, h - 2);
			g.drawLine(1, h - 1, w - 5, h - 1);
			g.drawLine(1, 0, w - 1, 0);
			g.drawLine(w - 1, 1, w - 1, h - 3);
			g.drawLine(w - 2, 1, w - 2, h - 4);
			g.drawLine(w - 3, 1, w - 3, h - 4);

			g.setColor(RapidLookTools.getColors().getSpinnerColors()[7]);
			g.drawLine(0, 0, 0, h);
		}
	}

	private void paintUpButton(Graphics g) {
		this.dimension = getSize();
		int h = (int) this.dimension.getHeight();
		int w = (int) this.dimension.getWidth();

		if (getModel().isEnabled()) {
			if (getModel().isPressed()) {
				g.setColor(RapidLookTools.getColors().getSpinnerColors()[0]);
				g.drawLine(1, 0, w - 5, 0);
				g.drawLine(1, 1, w - 3, 1);
				g.drawLine(1, 2, w - 2, 2);
				g.fillRect(1, 3, w - 1, h - 3);
				g.drawLine(1, h - 4, w - 1, h - 4);
				g.drawLine(1, h - 3, w - 1, h - 3);
				g.drawLine(1, h - 2, w - 1, h - 2);
				g.drawLine(1, h - 1, w - 1, h - 1);
				g.drawLine(1, h - 1, w - 1, h - 1);
				g.drawLine(w - 1, 3, w - 1, h - 3);
				g.drawLine(w - 2, 2, w - 2, h - 4);
				g.drawLine(w - 3, 1, w - 3, h - 4);

				g.setColor(RapidLookTools.getColors().getSpinnerColors()[1]);
				g.drawLine(0, 0, 0, h);
			} else {
				g.setColor(RapidLookTools.getColors().getSpinnerColors()[3]);
				g.drawLine(1, 0, w - 5, 0);

				g.setColor(RapidLookTools.getColors().getSpinnerColors()[2]);
				g.drawLine(1, 1, w - 3, 1);
				g.drawLine(1, 2, w - 2, 2);
				g.fillRect(1, 3, w - 1, h - 3);

				g.setColor(RapidLookTools.getColors().getSpinnerColors()[3]);
				g.drawLine(1, h - 4, w - 1, h - 4);
				g.setColor(RapidLookTools.getColors().getSpinnerColors()[4]);
				g.drawLine(1, h - 3, w - 1, h - 3);
				g.setColor(RapidLookTools.getColors().getSpinnerColors()[5]);
				g.drawLine(1, h - 2, w - 1, h - 2);
				g.setColor(RapidLookTools.getColors().getSpinnerColors()[6]);
				g.drawLine(1, h - 1, w - 1, h - 1);
				g.setColor(RapidLookTools.getColors().getSpinnerColors()[7]);
				g.drawLine(1, h - 1, w - 1, h - 1);
				g.drawLine(0, 0, 0, h);
				g.setColor(RapidLookTools.getColors().getSpinnerColors()[4]);
				g.drawLine(w - 1, 3, w - 1, h - 3);
				g.setColor(RapidLookTools.getColors().getSpinnerColors()[8]);
				g.drawLine(w - 2, 2, w - 2, h - 4);
				g.setColor(RapidLookTools.getColors().getSpinnerColors()[3]);
				g.drawLine(w - 3, 1, w - 3, h - 4);
			}
		} else {
			g.setColor(RapidLookTools.getColors().getSpinnerColors()[8]);
			g.drawLine(1, 0, w - 5, 0);
			g.drawLine(1, 1, w - 3, 1);
			g.drawLine(1, 2, w - 2, 2);
			g.fillRect(1, 3, w - 1, h - 3);
			g.drawLine(1, h - 4, w - 1, h - 4);
			g.drawLine(1, h - 3, w - 1, h - 3);
			g.drawLine(1, h - 2, w - 1, h - 2);
			g.drawLine(1, h - 1, w - 1, h - 1);
			g.drawLine(1, h - 1, w - 1, h - 1);
			g.drawLine(w - 1, 3, w - 1, h - 3);
			g.drawLine(w - 2, 2, w - 2, h - 4);
			g.drawLine(w - 3, 1, w - 3, h - 4);

			g.setColor(RapidLookTools.getColors().getSpinnerColors()[1]);
			g.drawLine(0, 0, 0, h);
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		if (this.up) {
			paintUpButton(g);
		} else {
			paintDownButton(g);
		}
		drawArrow(g);
	}

	protected void drawArrow(Graphics g) {
		this.dimension = getSize();

		if (getModel().isEnabled()) {
			g.setColor(RapidLookTools.getColors().getSpinnerColors()[9]);
		} else {
			g.setColor(RapidLookTools.getColors().getSpinnerColors()[10]);
		}

		int baseY = (int) ((this.dimension.getHeight() / 2) - 2);
		if (!this.up) {
			baseY++;
		}

		g.translate(5, baseY);
		if (this.up) {
			g.drawLine(2, 0, 3, 0);
			g.drawLine(1, 1, 4, 1);
			g.drawLine(0, 2, 1, 2);
			g.drawLine(4, 2, 5, 2);
		} else {
			g.drawLine(2, 2, 3, 2);
			g.drawLine(1, 1, 4, 1);
			g.drawLine(0, 0, 1, 0);
			g.drawLine(4, 0, 5, 0);
		}
		g.translate(-6, -baseY);
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
}
