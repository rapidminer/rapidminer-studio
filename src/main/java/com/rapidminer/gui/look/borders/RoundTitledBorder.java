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
package com.rapidminer.gui.look.borders;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.border.Border;

import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawer;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.FontTools;


/**
 * Orange border with round corners, a circled number, and a key.
 *
 * @author Simon Fischer
 */
public class RoundTitledBorder implements Border {

	private static final Font FONT = FontTools.getFont(Font.DIALOG, Font.BOLD, 21);

	private int number;
	private String key;
	private Paint paint;
	private boolean drawRoundFrame;

	public RoundTitledBorder(int number, String key) {
		this(number, key, SwingTools.RAPIDMINER_ORANGE, true);
	}

	public RoundTitledBorder(int number, String key, boolean drawRoundFrame) {
		this(number, key, SwingTools.RAPIDMINER_ORANGE, drawRoundFrame);
	}

	private RoundTitledBorder(int number, String key, Paint paint, boolean drawRoundFrame) {
		super();
		this.number = number;
		this.key = key;
		this.paint = paint;
		this.drawRoundFrame = drawRoundFrame;
	}

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setRenderingHints(ProcessDrawer.HI_QUALITY_HINTS);
		g2d.setStroke(new BasicStroke(2f));

		// clear edges, otherwise they will be in the color of the component background
		if (drawRoundFrame && !c.getBackground().equals(c.getParent().getBackground())) {
			Shape frame = new Rectangle2D.Float(x + 2, y + 2, width - 4, height - 4);
			g2d.setPaint(c.getParent().getBackground());
			g2d.draw(frame);
		}

		g2d.setPaint(paint);
		g2d.setFont(FONT);

		if (drawRoundFrame) {
			Shape roundFrame = new RoundRectangle2D.Float(x + 2, y + 2, width - 4, height - 4, 10, 10);
			g2d.draw(roundFrame);
		}

		if (number > 0) {
			Shape circle = new Ellipse2D.Float(20, 20, 34, 34);
			g2d.fill(circle);
			g2d.setPaint(Color.WHITE);
			g2d.drawString(String.valueOf(number), 29, 44);
		}

		if (key != null) {
			g2d.setPaint(paint);
			g2d.drawString(key, 60, 44);
		}

		g2d.dispose();
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(number != 0 || key != null ? 64 : 10, 10, 10, 10);
	}

	@Override
	public boolean isBorderOpaque() {
		return false;
	}

	public void setTitle(String key) {
		this.key = key;
	}
}
