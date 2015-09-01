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
package com.rapidminer.gui.look.painters;

import com.rapidminer.gui.look.RapidLookTools;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;


/**
 * This is a cached painter for the determinated progress bars.
 * 
 * @author Ingo Mierswa
 */
public class DeterminateProgressBarPainter extends AbstractCachedPainter {

	public static final DeterminateProgressBarPainter SINGLETON = new DeterminateProgressBarPainter(7);

	DeterminateProgressBarPainter(int count) {
		super(count);
	}

	public synchronized void paint(Component c, Graphics g, int x, int y, int w, int h) {
		paint(c, g, x, y, w, h, new Object[] {});
	}

	@Override
	protected void paintToImage(Component c, Graphics g, int w, int h, Object[] args) {
		boolean vertical = ((Boolean) args[0]).booleanValue();
		int x = 0, y = 0;
		if (vertical) {
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][0]);
			g.drawLine(x, y + 1, x, y + 7);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][1]);
			g.drawLine(x + 1, y + 1, x + 1, y + 7);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][2]);
			g.drawLine(x + 1, y, x + 1, y + 8);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][3]);
			g.drawLine(x + 2, y, x + 2, y + 8);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][4]);
			g.drawLine(x + 2, y + 1, x + 2, y + 7);

			if (w > 0) {
				g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][5]);
				g.drawLine(x + 3, y, x + w - 4, y);
				g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][6]);
				g.fillRect(x + 3, y + 1, w - 6, 7);
				g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][7]);
				g.drawLine(x + 3, y + 8, x + w - 4, y + 8);
			}

			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][8]);
			g.drawLine(x + w - 3, y, x + w - 3, y + 8);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][9]);
			g.drawLine(x + w - 3, y + 1, x + w - 3, y + 7);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][10]);
			g.drawLine(x + w - 2, y, x + w - 2, y + 8);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][11]);
			g.drawLine(x + w - 2, y + 1, x + w - 2, y + 7);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][12]);
			g.drawLine(x + w - 1, y + 1, x + w - 1, y + 7);
		} else {
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][0]);
			g.drawLine(x + 1, y, x + 7, y);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][2]);
			g.drawLine(x, y + 1, x + 8, y + 1);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][1]);
			g.drawLine(x + 1, y + 1, x + 7, y + 1);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][2]);
			g.drawLine(x, y + 2, x + 8, y + 2);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][3]);
			g.drawLine(x + 1, y + 2, x + 7, y + 2);

			if (h > 0) {
				g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][7]);
				g.drawLine(x, y + 3, x, y + h - 4);
				g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][6]);
				g.fillRect(x + 1, y + 3, 7, h - 6);
				g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][5]);
				g.drawLine(x + 8, y + 3, x + 8, y + h - 4);
			}

			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][8]);
			g.drawLine(x, y + h - 3, x + 8, y + h - 3);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][9]);
			g.drawLine(x + 1, y + h - 3, x + 7, y + h - 3);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][10]);
			g.drawLine(x, y + h - 2, x + 8, y + h - 2);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][11]);
			g.drawLine(x + 1, y + h - 2, x + 7, y + h - 2);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[0][12]);
			g.drawLine(x + 1, y + h - 1, x + 7, y + h - 1);
		}
	}

	@Override
	protected void paintImage(Component c, Graphics g, int x, int y, int imageW, int imageH, Image image, Object[] args) {
		g.translate(x, y);
		g.drawImage(image, 0, 0, null);
		g.translate(-x, -y);
	}
}
