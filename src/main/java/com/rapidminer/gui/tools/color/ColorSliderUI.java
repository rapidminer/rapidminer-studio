/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.tools.color;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.tools.FontTools;


/**
 * The abstract UI class for a color slider.
 *
 * @author Marco Boeck
 * @since 9.2.0
 */
public abstract class ColorSliderUI extends ComponentUI {


	protected static final Font FONT = FontTools.getFont("Open Sans", Font.BOLD, 9);
	protected static final BasicStroke STROKE_HOVERED = new BasicStroke(2);
	protected static final BasicStroke STROKE_NORMAL = new BasicStroke(1);
	protected static final BasicStroke STROKE_PREVIEW = new BasicStroke(1.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{3f, 4f}, 0f);
	protected static final BasicStroke STROKE_PLUS_SYMBOL = new BasicStroke(1.5f);
	protected static final Color COLOR_INVISIBLE = new Color(0, 0, 0, 0);

	protected DecimalFormat format;
	protected ColorSlider colorSlider;

	/** flag if we currently need to draw a preview indicator at {@link #currentMouseX} */
	protected boolean drawPreview;
	/** flag if we draw values under color point indicators */
	protected boolean drawValues = true;
	protected int currentMouseX;

	private MouseListener mouseListener;
	private MouseMotionListener mouseMotionListener;


	public ColorSliderUI() {
		this.format = new DecimalFormat("0.0#");
		this.mouseListener = new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent e) {
				drawPreview = true;
				colorSlider.repaint();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				drawPreview = false;
				colorSlider.repaint();
			}
		};
		this.mouseMotionListener = new MouseAdapter() {

			@Override
			public void mouseMoved(MouseEvent e) {
				currentMouseX = e.getX();
			}
		};
	}

	@Override
	public void installUI(JComponent c) {
		colorSlider = (ColorSlider) c;
		colorSlider.addMouseListener(mouseListener);
		colorSlider.addMouseMotionListener(mouseMotionListener);
	}

	@Override
	public void uninstallUI(JComponent c) {
		colorSlider.removeMouseListener(mouseListener);
		colorSlider.removeMouseMotionListener(mouseMotionListener);
		colorSlider = null;
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		int width = colorSlider.getWidth();
		int height = colorSlider.getHeight();

		Graphics2D g2 = (Graphics2D) g.create();
		g2.setColor(COLOR_INVISIBLE);
		g2.fillRect(0, 0, width, height);
		g2.dispose();

		drawColors((Graphics2D) g.create());
		drawPoints((Graphics2D) g.create());
	}

	/**
	 * Draws the color bar itself.
	 *
	 * @param g2
	 * 		the graphics2D context
	 */
	protected abstract void drawColors(Graphics2D g2);

	/**
	 * Draws the color points.
	 *
	 * @param g2
	 * 		the graphics2D context
	 */
	protected void drawPoints(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		List<ColorPoint> colorPoints = colorSlider.getColorPoints();
		for (ColorPoint colorPoint : colorPoints) {
			if (colorPoint == colorSlider.getHoveredPoint()) {
				continue;
			}
			drawPoint((Graphics2D) g2.create(), colorPoint, false);
		}
		// draw hovered point last (so it is above the others)
		if (colorSlider.getHoveredPoint() != null) {
			drawPoint((Graphics2D) g2.create(), colorSlider.getHoveredPoint(), false);
		} else if (drawPreview && colorSlider.canAddPoint() && getFullHeightShapeForBar().contains(currentMouseX, 0)) {
			// draw a preview point on current mouse location if x coordinate is inside color bar
			drawPoint((Graphics2D) g2.create(), new ColorPoint(colorSlider.getRelativeXForAbsoluteX(currentMouseX), Color.WHITE), true);
		}

		g2.dispose();
	}

	/**
	 * Draws the given color point.
	 *
	 * @param g2
	 * 		the graphics2D context
	 * @param colorPoint
	 * 		the point to draw
	 * @param preview
	 * 		if {@code true}, the point will be drawn as a preview; if {@code false}, a real point will be drawn
	 */
	protected void drawPoint(Graphics2D g2, ColorPoint colorPoint, boolean preview) {
		g2.setFont(FONT);
		Polygon shape = colorSlider.getShapeForColorPoint(colorPoint, false);
		int centerX = (int) (colorPoint.getPoint() * colorSlider.getBarWidth() + ColorSlider.X_OFFSET);
		String s = format.format(colorPoint.getPoint());
		int stringWidth = g2.getFontMetrics().stringWidth(s);
		int stringX = centerX - (stringWidth / 2);
		int stringY = colorSlider.getHeight() - 4;
		g2.setColor(colorPoint.getColor());
		g2.fill(shape);
		if (preview) {
			g2.setColor(Colors.PANEL_BORDER);
			g2.setStroke(STROKE_PREVIEW);
			g2.draw(shape);

			g2.setStroke(STROKE_PLUS_SYMBOL);
			g2.drawLine((int) (shape.getBounds().getCenterX() + 4), (int) (shape.getBounds().getY() - 6), (int) (shape.getBounds().getMaxX() + 6), (int) (shape.getBounds().getY() - 6));
			g2.drawLine((int) (shape.getBounds().getMaxX() + 2), (int) (shape.getBounds().getY() - 10), (int) (shape.getBounds().getMaxX() + 2), (int) (shape.getBounds().getY() - 2));

			if (drawValues) {
				g2.drawString(s, stringX, stringY);
			}
		} else {
			if (colorPoint == colorSlider.getHoveredPoint()) {
				g2.setStroke(STROKE_HOVERED);
				g2.setColor(Colors.PANEL_BORDER);
				g2.draw(shape);
				if (drawValues) {
					g2.setColor(Color.DARK_GRAY);
					g2.drawString(s, stringX, stringY);
				}
			} else {
				g2.setStroke(STROKE_NORMAL);
				g2.setColor(Colors.PANEL_BORDER);
				g2.draw(shape);
			}
		}

		g2.dispose();
	}

	/**
	 * Creates the bar polygon but with 100% height.
	 *
	 * @return the polygon, never {@code null}
	 */
	protected Polygon getFullHeightShapeForBar() {
		int x = ColorSlider.X_OFFSET;
		int y = 0;
		int width = colorSlider.getBarWidth();
		int[] xPoints = new int[] { x, x, x + width, x + width};
		int[] YPoints = new int[] { y + colorSlider.getHeight(), y, y, y + colorSlider.getHeight()};
		return new Polygon(xPoints, YPoints, 4);
	}

}
