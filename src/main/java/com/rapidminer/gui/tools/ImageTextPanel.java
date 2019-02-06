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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;


/**
 * This panel can be used to display an image and draw some text across it.
 * 
 * @author Ingo Mierswa
 */
public class ImageTextPanel extends TextPanel {

	private static final long serialVersionUID = -5728947680003081065L;

	public static final int TEXT_START_Y = -1;

	private transient Image image = null;

	private boolean rescaleImage = false;

	private int imageX = 0;

	private int imageY = TEXT_START_Y;

	public ImageTextPanel(Image image, String title, String[] textLines, int xAlignment, int yAlignment) {
		this(image, title, textLines, xAlignment, yAlignment, false);
	}

	public ImageTextPanel(Image image, String title, String[] textLines, int xAlignment, int yAlignment, boolean rescaleImage) {
		this(image, title, textLines, xAlignment, yAlignment, rescaleImage, 0, TEXT_START_Y);
	}

	public ImageTextPanel(Image image, String title, String[] textLines, int xAlignment, int yAlignment,
			boolean rescaleImage, int imageX, int imageY) {
		super(title, textLines, xAlignment, yAlignment);
		this.image = image;
		this.rescaleImage = rescaleImage;
		this.imageX = imageX;
		this.imageY = imageY;
	}

	@Override
	public void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics;
		if (image != null) {
			int actualImageY = this.imageY;
			if (this.imageY == TEXT_START_Y) {
				actualImageY = getTextStartY();
			}
			if (this.rescaleImage) {
				g.drawImage(image, imageX, actualImageY, getWidth() - imageX, getHeight() - actualImageY, this);
			} else {
				g.drawImage(image, imageX, actualImageY, this);
			}
		} else {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());
		}
		super.paintComponent(graphics);
	}
}
