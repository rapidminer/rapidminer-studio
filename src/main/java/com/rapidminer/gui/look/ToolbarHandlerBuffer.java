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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;


/**
 * The buffer used for drawing toolbar handlers.
 * 
 * @author Ingo Mierswa
 */
public class ToolbarHandlerBuffer {

	private static final int IMAGE_SIZE = 64;

	private static Dimension imageSize = new Dimension(IMAGE_SIZE, IMAGE_SIZE);

	private transient Image image;

	private Color topColor;

	private Color shadowColor;

	private GraphicsConfiguration gc;

	public ToolbarHandlerBuffer(GraphicsConfiguration gc, Color aTopColor, Color aShadowColor) {
		this.gc = gc;
		this.topColor = aTopColor;
		this.shadowColor = aShadowColor;
		createImage();
		fillBumpBuffer();
	}

	public boolean hasSameConfiguration(GraphicsConfiguration gc, Color aTopColor, Color aShadowColor) {
		if (this.gc != null) {
			if (!this.gc.equals(gc)) {
				return false;
			}
		} else if (gc != null) {
			return false;
		}
		return this.topColor.equals(aTopColor) && this.shadowColor.equals(aShadowColor);
	}

	public Image getImage() {
		return this.image;
	}

	public Dimension getImageSize() {
		return imageSize;
	}

	private void fillBumpBuffer() {
		Graphics g = this.image.getGraphics();

		g.setColor(this.topColor);
		for (int x = 0; x < IMAGE_SIZE; x += 2) {
			for (int y = 0; y < IMAGE_SIZE; y += 4) {
				g.drawLine(x, y, x, y);
			}
		}

		g.setColor(this.shadowColor);
		for (int x = 0; x < IMAGE_SIZE; x += 2) {
			for (int y = 0; y < IMAGE_SIZE; y += 4) {
				g.drawLine(x + 1, y + 1, x + 1, y + 1);
			}
		}
		g.dispose();
	}

	private void createImage() {
		if (this.gc != null) {
			this.image = this.gc.createCompatibleImage(IMAGE_SIZE, IMAGE_SIZE, Transparency.BITMASK);
		} else {
			int cmap[] = { this.topColor.getRGB(), this.shadowColor.getRGB() };
			IndexColorModel icm = new IndexColorModel(8, 3, cmap, 0, false, -1, DataBuffer.TYPE_BYTE);
			this.image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_BYTE_INDEXED, icm);
		}
	}
}
