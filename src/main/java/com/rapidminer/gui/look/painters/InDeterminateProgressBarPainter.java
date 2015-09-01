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
 * This is a cached painter for the indeterminated progress bars.
 * 
 * @author Ingo Mierswa
 */
public class InDeterminateProgressBarPainter extends AbstractCachedPainter {

	public static final InDeterminateProgressBarPainter SINGLETON = new InDeterminateProgressBarPainter(7);

	InDeterminateProgressBarPainter(int count) {
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
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][0]);
			g.drawLine(x, y + 1, x, y + h - 2);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][1]);
			g.drawLine(x + 1, y + 1, x + 1, y + h - 2);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][2]);
			g.drawLine(x + 1, y, x + 1, y + h - 1);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][3]);
			g.drawLine(x + 2, y, x + 2, y + h - 1);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][4]);
			g.drawLine(x + 2, y + 1, x + 2, y + h - 2);

			if (w > 0) {
				g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][5]);
				g.drawLine(x + 3, y, x + w - 4, y);
				g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][6]);
				g.fillRect(x + 3, y + 1, w - 6, h - 2);
				g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][7]);
				g.drawLine(x + 3, y + h - 1, x + w - 4, y + h - 1);
			}

			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][8]);
			g.drawLine(x + w - 3, y, x + w - 3, y + h - 1);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][9]);
			g.drawLine(x + w - 3, y + 1, x + w - 3, y + h - 2);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][10]);
			g.drawLine(x + w - 2, y, x + w - 2, y + h - 1);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][11]);
			g.drawLine(x + w - 2, y + 1, x + w - 2, y + h - 2);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][12]);
			g.drawLine(x + w - 1, y + 1, x + w - 1, y + h - 2);
		} else {
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][0]);
			g.drawLine(x + 1, y, x + w - 2, y);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][2]);
			g.drawLine(x, y + 1, x + w - 1, y + 1);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][1]);
			g.drawLine(x + 1, y + 1, x + w - 2, y + 1);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][3]);
			g.drawLine(x, y + 2, x + w - 1, y + 2);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][4]);
			g.drawLine(x + 1, y + 2, x + w - 2, y + 2);

			if (h > 0) {
				g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][7]);
				g.drawLine(x, y + 3, x, y + h - 4);
				g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][6]);
				g.fillRect(x + 1, y + 3, w - 2, h - 6);
				g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][5]);
				g.drawLine(x + w - 1, y + 3, x + w - 1, y + h - 4);
			}

			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][8]);
			g.drawLine(x, y + h - 3, x + w - 1, y + h - 3);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][9]);
			g.drawLine(x + 1, y + h - 3, x + w - 2, y + h - 3);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][10]);
			g.drawLine(x, y + h - 2, x + w - 1, y + h - 2);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][11]);
			g.drawLine(x + 1, y + h - 2, x + w - 2, y + h - 2);
			g.setColor(RapidLookTools.getColors().getProgressBarColors()[1][12]);
			g.drawLine(x + 1, y + h - 1, x + w - 2, y + h - 1);
		}
	}

	@Override
	protected void paintImage(Component c, Graphics g, int x, int y, int imageW, int imageH, Image image, Object[] args) {
		g.translate(x, y);
		g.drawImage(image, 0, 0, null);
		g.translate(-x, -y);
	}
}
