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

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.GenericArrowButton;


/**
 * The UI for scroll bars.
 *
 * @author Ingo Mierswa, Marco Boeck
 */
public class ScrollBarUI extends BasicScrollBarUI {

	private static final int SCROLLBAR_WIDTH = 16;

	private class ScrollBarThumbListener extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			if (getThumbBounds().contains(e.getX(), e.getY())) {
				ScrollBarUI.this.thumbIsPressed = true;
				getScrollBar().repaint();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			ScrollBarUI.this.thumbIsPressed = false;
			getScrollBar().repaint();
		}
	}

	private JButton decreaseButton;

	private JButton increaseButton;

	private boolean thumbIsPressed;

	private MouseListener thumbPressedListener;

	public static ComponentUI createUI(JComponent c) {
		return new ScrollBarUI();
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();
	}

	@Override
	public void uninstallUI(JComponent c) {
		super.uninstallUI(c);
	}

	protected MouseListener createThumbPressedListener() {
		return new ScrollBarThumbListener();
	}

	@Override
	protected void installListeners() {
		super.installListeners();
		if ((this.thumbPressedListener = createThumbPressedListener()) != null) {
			this.scrollbar.addMouseListener(this.thumbPressedListener);
		}
	}

	@Override
	protected void uninstallListeners() {
		if (this.thumbPressedListener != null) {
			this.scrollbar.removeMouseListener(this.thumbPressedListener);
			this.thumbPressedListener = null;
		}
		super.uninstallListeners();
	}

	@Override
	public Dimension getPreferredSize(JComponent c) {
		if (this.scrollbar.getOrientation() == Adjustable.VERTICAL) {
			return new Dimension(SCROLLBAR_WIDTH, 53 + 10);
		} else {
			return new Dimension(100, SCROLLBAR_WIDTH);
		}
	}

	@Override
	public Dimension getMinimumSize(JComponent c) {
		if (this.scrollbar.getOrientation() == Adjustable.VERTICAL) {
			return new Dimension(SCROLLBAR_WIDTH, 40);
		} else {
			return new Dimension(40, SCROLLBAR_WIDTH);
		}
	}

	@Override
	protected void configureScrollBarColors() {
		super.configureScrollBarColors();
		this.thumbColor = UIManager.getColor("ScrollBar.thumb");
		this.thumbHighlightColor = UIManager.getColor("ScrollBar.thumbHighlight");
	}

	@Override
	protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
		int x = (int) trackBounds.getX();
		int y = (int) trackBounds.getY();
		int w = (int) trackBounds.getWidth();
		int h = (int) trackBounds.getHeight();

		g.setColor(Colors.SCROLLBAR_TRACK_BACKGROUND);
		g.fillRect(x - 1, y - 1, w + 2, h + 2);

		g.setColor(Colors.SCROLLBAR_TRACK_BORDER);
		if (this.scrollbar.getOrientation() == Adjustable.HORIZONTAL) {
			g.drawLine(x, y, x + w, y);
		} else {
			g.drawLine(x, y, x, y + h);
		}
	}

	@Override
	protected JButton createDecreaseButton(int orientation) {
		if (this.scrollbar.getOrientation() == Adjustable.HORIZONTAL) {
			this.decreaseButton = new GenericArrowButton(orientation, this.scrollbar.getHeight(), SCROLLBAR_WIDTH - 1);
		} else {
			this.decreaseButton = new GenericArrowButton(orientation, SCROLLBAR_WIDTH - 1, this.scrollbar.getWidth());
		}
		return this.decreaseButton;
	}

	@Override
	protected JButton createIncreaseButton(int orientation) {
		if (this.scrollbar.getOrientation() == Adjustable.HORIZONTAL) {
			this.increaseButton = new GenericArrowButton(orientation, this.scrollbar.getHeight(), SCROLLBAR_WIDTH - 1);
		} else {
			this.increaseButton = new GenericArrowButton(orientation, SCROLLBAR_WIDTH - 1, this.scrollbar.getWidth());
		}
		return this.increaseButton;
	}

	@Override
	protected void paintIncreaseHighlight(Graphics g) {
		g.setColor(Color.green);
	}

	@Override
	protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
		int x = (int) thumbBounds.getX();
		int y = (int) thumbBounds.getY();
		int w = (int) thumbBounds.getWidth();
		int h = (int) thumbBounds.getHeight();

		if (c.isEnabled() && w > 0 && h > 0) {
			if (this.scrollbar.getOrientation() == Adjustable.HORIZONTAL) {
				h -= 1;
				y++;
				drawHorizThumb(g, x, y, w, h);
			} else {
				w -= 1;
				x++;
				drawVertThumb(g, x, y, w, h);
			}
		}
	}

	private void drawHorizThumb(Graphics g, int x, int y, int w, int h) {
		if (isThumbRollover() && !thumbIsPressed) {
			g.setColor(Colors.SCROLLBAR_THUMB_BACKGROUND_ROLLOVER);
		} else if (thumbIsPressed) {
			g.setColor(Colors.SCROLLBAR_THUMB_BACKGROUND_PRESSED);
		} else {
			g.setColor(Colors.SCROLLBAR_THUMB_BACKGROUND);
		}
		g.fillRect(x, y, w, h);

		if (w < 30) {
			return;
		}
		int xMiddle = x + w / 2;
		int offset = 1;
		g.setColor(Colors.SCROLLBAR_THUMB_FOREGROUND);
		g.drawLine(xMiddle - offset * 3, y + 2, xMiddle - offset * 3, y + h - 3);
		g.drawLine(xMiddle - offset, y + 2, xMiddle - offset, y + h - 3);
		g.drawLine(xMiddle + offset, y + 2, xMiddle + offset, y + h - 3);
		g.drawLine(xMiddle + offset * 3, y + 2, xMiddle + offset * 3, y + h - 3);
	}

	private void drawVertThumb(Graphics g, int x, int y, int w, int h) {
		if (isThumbRollover() && !thumbIsPressed) {
			g.setColor(Colors.SCROLLBAR_THUMB_BACKGROUND_ROLLOVER);
		} else if (thumbIsPressed) {
			g.setColor(Colors.SCROLLBAR_THUMB_BACKGROUND_PRESSED);
		} else {
			g.setColor(Colors.SCROLLBAR_THUMB_BACKGROUND);
		}
		g.fillRect(x, y, w, h);

		if (h < 30) {
			return;
		}
		int yMiddle = y + h / 2;
		int offset = 1;
		g.setColor(Colors.SCROLLBAR_THUMB_FOREGROUND);
		g.drawLine(x + 2, yMiddle - offset * 3, x + w - 3, yMiddle - offset * 3);
		g.drawLine(x + 2, yMiddle - offset, x + w - 3, yMiddle - offset);
		g.drawLine(x + 2, yMiddle + offset, x + w - 3, yMiddle + offset);
		g.drawLine(x + 2, yMiddle + offset * 3, x + w - 3, yMiddle + offset * 3);
	}

	private JScrollBar getScrollBar() {
		return this.scrollbar;
	}
}
