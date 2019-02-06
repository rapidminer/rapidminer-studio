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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;


/**
 * This class is used for drawing a handler icon.
 * 
 * @author Ingo Mierswa
 */
public class ToolbarHandlerIcon implements Icon {

	private int xBumps;

	private int yBumps;

	private Color topColor;

	private Color shadowColor;

	private static List<ToolbarHandlerBuffer> buffers = new LinkedList<ToolbarHandlerBuffer>();

	private ToolbarHandlerBuffer buffer;

	public ToolbarHandlerIcon(Dimension bumpArea) {
		this(bumpArea.width, bumpArea.height);
	}

	public ToolbarHandlerIcon(int width, int height) {
		this(width, height, RapidLookAndFeel.getPrimaryControlHighlight(), RapidLookAndFeel.getPrimaryControlDarkShadow());
	}

	public ToolbarHandlerIcon(int width, int height, Color newTopColor, Color newShadowColor) {
		setBumpArea(width, height);
		setBumpColors(newTopColor, newShadowColor);
	}

	private ToolbarHandlerBuffer getBuffer(GraphicsConfiguration gc, Color aTopColor, Color aShadowColor) {
		if ((this.buffer != null) && this.buffer.hasSameConfiguration(gc, aTopColor, aShadowColor)) {
			return this.buffer;
		}
		ToolbarHandlerBuffer result = null;

		for (ToolbarHandlerBuffer toolbarHandlerBuffer : buffers) {
			if (toolbarHandlerBuffer.hasSameConfiguration(gc, aTopColor, aShadowColor)) {
				result = toolbarHandlerBuffer;
				break;
			}
		}

		if (result == null) {
			result = new ToolbarHandlerBuffer(gc, this.topColor, this.shadowColor);
			buffers.add(result);
		}
		return result;
	}

	public void setBumpArea(Dimension bumpArea) {
		setBumpArea(bumpArea.width, bumpArea.height);
	}

	public void setBumpArea(int width, int height) {
		this.xBumps = width / 2;
		this.yBumps = height / 2;
	}

	public void setBumpColors(Color newTopColor, Color newShadowColor) {
		this.topColor = newTopColor;
		this.shadowColor = newShadowColor;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		GraphicsConfiguration gc = (g instanceof Graphics2D) ? (GraphicsConfiguration) ((Graphics2D) g)
				.getDeviceConfiguration() : null;

		this.buffer = getBuffer(gc, this.topColor, this.shadowColor);

		int bufferWidth = this.buffer.getImageSize().width;
		int bufferHeight = this.buffer.getImageSize().height;
		int iconWidth = getIconWidth();
		int iconHeight = getIconHeight();
		int x2 = x + iconWidth;
		int y2 = y + iconHeight;
		int savex = x;

		while (y < y2) {
			int h = Math.min(y2 - y, bufferHeight);
			for (x = savex; x < x2; x += bufferWidth) {
				int w = Math.min(x2 - x, bufferWidth);
				g.drawImage(this.buffer.getImage(), x, y, x + w, y + h, 0, 0, w, h, null);
			}
			y += bufferHeight;
		}
	}

	@Override
	public int getIconWidth() {
		return this.xBumps * 2;
	}

	@Override
	public int getIconHeight() {
		return this.yBumps * 2;
	}
}
