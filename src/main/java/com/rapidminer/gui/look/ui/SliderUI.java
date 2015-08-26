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
import com.rapidminer.gui.tools.SwingTools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSliderUI;


/**
 * The UI for sliders.
 * 
 * @author Ingo Mierswa
 */
public class SliderUI extends BasicSliderUI {

	private class ThumbListener extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			if (getThumbBounds().contains(e.getX(), e.getY())) {
				SliderUI.this.thumbIsPressed = true;
				getSlider().repaint();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			SliderUI.this.thumbIsPressed = false;
			getSlider().repaint();
		}
	}

	private boolean thumbIsPressed = false;

	private MouseListener thumbPressedListener;

	public static ComponentUI createUI(JComponent jcomponent) {
		return new SliderUI((JSlider) jcomponent);
	}

	public SliderUI(JSlider jSlider) {
		super(jSlider);
	}

	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
	}

	@Override
	public void paintThumb(Graphics g) {
		g.translate(this.thumbRect.x, this.thumbRect.y);
		Image sliderImage = RapidLookTools.getColors().getSliderImage().getImage();
		if (sliderImage == null) {
			sliderImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
			Graphics2D gr = ((BufferedImage) sliderImage).createGraphics();
			gr.setColor(SwingTools.LIGHT_BLUE);
			gr.fillOval(0, 0, 10, 10);
			gr.setColor(Color.BLACK);
			gr.drawOval(0, 0, 10, 10);
		}
		if (this.slider.getOrientation() == SwingConstants.HORIZONTAL) { // horizonal
			if (this.slider.isEnabled()) {
				if (this.thumbIsPressed) {
					g.drawImage(sliderImage, 0, 0, null);
				} else {
					g.drawImage(sliderImage, 0, 0, null);
				}
			} else {
				g.drawImage(sliderImage, 0, 0, null);
			}
		} else { // vertical
			if (this.slider.isEnabled()) {
				if (this.thumbIsPressed) {
					g.drawImage(sliderImage, 0, 0, null);
				} else {
					g.drawImage(sliderImage, 0, 0, null);
				}
			} else {
				g.drawImage(sliderImage, 0, 0, null);
			}
		}
		g.translate(-this.thumbRect.x, -this.thumbRect.y);
	}

	@Override
	public void paintTrack(Graphics g) {
		if (this.slider.getOrientation() == SwingConstants.HORIZONTAL) {
			int trackTop = (int) this.trackRect.getY() + 2;
			int w = this.slider.getWidth();

			g.setColor(new ColorUIResource(170, 170, 170));
			g.drawRect(2, trackTop, w - 6, 6);

			if (this.slider.isEnabled()) {
				g.setColor(new ColorUIResource(220, 220, 220));
				g.drawLine(3, trackTop + 1, w - 5, trackTop + 1);
				g.setColor(new ColorUIResource(230, 230, 230));
				g.drawLine(3, trackTop + 2, w - 5, trackTop + 2);
				g.setColor(new ColorUIResource(240, 240, 240));
				g.drawLine(3, trackTop + 3, w - 5, trackTop + 3);
				g.setColor(new ColorUIResource(245, 245, 245));
				g.drawLine(3, trackTop + 4, w - 5, trackTop + 4);
				g.setColor(new ColorUIResource(250, 250, 250));
				g.drawLine(3, trackTop + 5, w - 5, trackTop + 5);
			} else {
				g.setColor(new ColorUIResource(240, 240, 240));
				g.drawLine(3, trackTop + 1, w - 5, trackTop + 1);
				g.drawLine(3, trackTop + 2, w - 5, trackTop + 2);
				g.drawLine(3, trackTop + 3, w - 5, trackTop + 3);
				g.drawLine(3, trackTop + 4, w - 5, trackTop + 4);
				g.drawLine(3, trackTop + 5, w - 5, trackTop + 5);
			}

			g.setColor(new ColorUIResource(210, 210, 210));
			g.drawLine(2, trackTop, 3, trackTop);
			g.setColor(new ColorUIResource(210, 210, 210));
			g.drawLine(2, trackTop + 6, 3, trackTop + 6);
			g.setColor(new ColorUIResource(190, 190, 190));
			g.drawLine(2, trackTop + 1, 2, trackTop + 1);
			g.setColor(new ColorUIResource(190, 190, 190));
			g.drawLine(2, trackTop + 5, 2, trackTop + 5);
			g.setColor(new ColorUIResource(210, 210, 210));
			g.drawLine(w - 4, trackTop, w - 5, trackTop);
			g.setColor(new ColorUIResource(210, 210, 210));
			g.drawLine(w - 4, trackTop + 6, w - 5, trackTop + 6);
			g.setColor(new ColorUIResource(190, 190, 190));
			g.drawLine(w - 4, trackTop + 1, w - 4, trackTop + 1);
			g.setColor(new ColorUIResource(190, 190, 190));
			g.drawLine(w - 4, trackTop + 5, w - 4, trackTop + 5);
		} else {
			int trackLeft = (int) this.trackRect.getX() + 2;
			int h = this.slider.getHeight();

			g.setColor(new ColorUIResource(170, 170, 170));
			g.drawRect(trackLeft, 2, 6, h - 6);

			if (this.slider.isEnabled()) {
				g.setColor(new ColorUIResource(220, 220, 220));
				g.drawLine(trackLeft + 1, 3, trackLeft + 1, h - 5);
				g.setColor(new ColorUIResource(230, 230, 230));
				g.drawLine(trackLeft + 2, 3, trackLeft + 2, h - 5);
				g.setColor(new ColorUIResource(240, 240, 240));
				g.drawLine(trackLeft + 3, 3, trackLeft + 3, h - 5);
				g.setColor(new ColorUIResource(245, 245, 245));
				g.drawLine(trackLeft + 4, 3, trackLeft + 4, h - 5);
				g.setColor(new ColorUIResource(250, 250, 250));
				g.drawLine(trackLeft + 5, 3, trackLeft + 5, h - 5);
			} else {
				g.setColor(new ColorUIResource(240, 240, 240));
				g.drawLine(trackLeft + 1, 3, trackLeft + 1, h - 5);
				g.drawLine(trackLeft + 2, 3, trackLeft + 2, h - 5);
				g.drawLine(trackLeft + 3, 3, trackLeft + 3, h - 5);
				g.drawLine(trackLeft + 4, 3, trackLeft + 4, h - 5);
				g.drawLine(trackLeft + 5, 3, trackLeft + 5, h - 5);

			}

			g.setColor(new ColorUIResource(210, 210, 210));
			g.drawLine(trackLeft, 2, trackLeft, 3);
			g.setColor(new ColorUIResource(210, 210, 210));
			g.drawLine(trackLeft + 6, 2, trackLeft + 6, 3);
			g.setColor(new ColorUIResource(190, 190, 190));
			g.drawLine(trackLeft + 1, 2, trackLeft + 1, 2);
			g.setColor(new ColorUIResource(190, 190, 190));
			g.drawLine(trackLeft + 5, 2, trackLeft + 5, 2);
			g.setColor(new ColorUIResource(210, 210, 210));
			g.drawLine(trackLeft, h - 4, trackLeft, h - 5);
			g.setColor(new ColorUIResource(210, 210, 210));
			g.drawLine(trackLeft + 6, h - 4, trackLeft + 6, h - 5);
			g.setColor(new ColorUIResource(190, 190, 190));
			g.drawLine(trackLeft + 1, h - 4, trackLeft + 1, h - 4);
			g.setColor(new ColorUIResource(190, 190, 190));
			g.drawLine(trackLeft + 5, h - 4, trackLeft + 5, h - 4);
		}
	}

	@Override
	public void paintFocus(Graphics g) {}

	@Override
	public void paintLabels(Graphics g) {
		super.paintLabels(g);
	}

	@Override
	protected void calculateThumbSize() {
		Dimension size = new Dimension(14, 14);
		this.thumbRect.setSize(size.width, size.height);
	}

	private Rectangle getThumbBounds() {
		return this.thumbRect;
	}

	private JSlider getSlider() {
		return this.slider;
	}

	protected MouseListener createThumbPressedListener() {
		return new ThumbListener();
	}

	@Override
	protected void installListeners(JSlider slider) {
		super.installListeners(slider);
		if ((this.thumbPressedListener = createThumbPressedListener()) != null) {
			slider.addMouseListener(this.thumbPressedListener);
		}
	}

	@Override
	protected void uninstallListeners(JSlider slider) {
		if (this.thumbPressedListener != null) {
			slider.removeMouseListener(this.thumbPressedListener);
			this.thumbPressedListener = null;
		}
		super.uninstallListeners(slider);
	}

	@Override
	protected void paintMinorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
		if (this.slider.isEnabled()) {
			g.setColor((Color) UIManager.get("Slider.minorColor"));
		} else {
			g.setColor((Color) UIManager.get("Slider.minorDisabledColor"));
		}
		g.drawLine(x, 0, x, tickBounds.height / 2 - 1);
	}

	@Override
	protected void paintMajorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
		if (this.slider.isEnabled()) {
			g.setColor((Color) UIManager.get("Slider.majorColor"));
		} else {
			g.setColor((Color) UIManager.get("Slider.majorDisabledColor"));
		}
		g.drawLine(x, 0, x, tickBounds.height - 2);
	}

	@Override
	protected void paintMinorTickForVertSlider(Graphics g, Rectangle tickBounds, int y) {
		if (this.slider.isEnabled()) {
			g.setColor((Color) UIManager.get("Slider.minorColor"));
		} else {
			g.setColor((Color) UIManager.get("Slider.minorDisabledColor"));
		}
		g.drawLine(0, y, tickBounds.width / 2 - 1, y);
	}

	@Override
	protected void paintMajorTickForVertSlider(Graphics g, Rectangle tickBounds, int y) {
		if (this.slider.isEnabled()) {
			g.setColor((Color) UIManager.get("Slider.majorColor"));
		} else {
			g.setColor((Color) UIManager.get("Slider.majorDisabledColor"));
		}
		g.drawLine(0, y, tickBounds.width - 2, y);
	}
}
