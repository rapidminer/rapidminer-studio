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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

import com.rapidminer.tools.FontTools;


/**
 * This panel can be used to display some text.
 *
 * @author Ingo Mierswa
 */
public class TextPanel extends JPanel {

	private static final long serialVersionUID = -5728947680003081065L;

	public static final int ALIGNMENT_LEFT = 0;

	public static final int ALIGNMENT_RIGHT = 1;

	public static final int ALIGNMENT_TOP = 0;

	public static final int ALIGNMENT_BOTTOM = 1;

	protected static final Font TITLE_FONT = FontTools.getFont(Font.SANS_SERIF, java.awt.Font.BOLD, 12);

	protected static final Font TEXT_FONT = FontTools.getFont(Font.SANS_SERIF, java.awt.Font.PLAIN, 11);

	protected static final int LINE_HEIGHT = 16;

	protected static final int MARGIN = 24;

	protected static final int TITLE_MARGIN = 5;

	private String title;

	private String[] textLines;

	private int height;

	private int width;

	private int xAlignment = ALIGNMENT_RIGHT;

	private int yAlignment = ALIGNMENT_TOP;

	private boolean resized = false;

	public TextPanel(String title, String[] textLines, int xAlignment, int yAlignment) {
		this.title = title;
		this.xAlignment = xAlignment;
		this.yAlignment = yAlignment;
		setText(textLines);
		setOpaque(false);
	}

	public void setText(String[] textLines) {
		this.textLines = textLines;
		this.height = LINE_HEIGHT + TITLE_MARGIN + textLines.length * LINE_HEIGHT + MARGIN;
		resized = false;
	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics;

		this.width = 1;

		g.setColor(SwingTools.BROWN_FONT_COLOR);
		g.setFont(TITLE_FONT);
		int yPos = getTextStartY();
		drawString(g, title, yPos);
		Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(title, g);
		this.width = (int) Math.max(this.width, stringBounds.getWidth());

		yPos += TITLE_MARGIN;
		g.setFont(TEXT_FONT);
		for (String line : textLines) {
			yPos += LINE_HEIGHT;
			drawString(g, line, yPos);
			stringBounds = g.getFontMetrics().getStringBounds(line, g);
			this.width = (int) Math.max(this.width, stringBounds.getWidth());
		}

		this.width += 2 * MARGIN;
		Dimension dimension = new Dimension(this.width, this.height);
		setPreferredSize(dimension);

		// paintChildren(graphics);

		// necessary for activating scroll bars if necessary
		if (!resized) {
			revalidate();
			repaint();
			resized = true;
		}
	}

	private void drawString(Graphics2D g, String text, int height) {
		switch (xAlignment) {
			case ALIGNMENT_LEFT:
				float xPos = MARGIN;
				float yPos = height;
				g.drawString(text, xPos, yPos);
				break;
			case ALIGNMENT_RIGHT:
				Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(text, g);
				xPos = (float) (getWidth() - MARGIN - stringBounds.getWidth());
				yPos = height;
				g.drawString(text, xPos, yPos);
				break;
		}
	}

	protected int getTextStartY() {
		int yPos = MARGIN;
		switch (yAlignment) {
			case ALIGNMENT_TOP:
				yPos = MARGIN;
				break;
			case ALIGNMENT_BOTTOM:
				yPos = getHeight() - textLines.length * LINE_HEIGHT - MARGIN;
				break;
		}
		return yPos;
	}
}
