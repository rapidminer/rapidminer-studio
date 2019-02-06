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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSliderUI;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookAndFeel;


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


		private Rectangle getThumbBounds() {
			return SliderUI.this.thumbRect;
		}

		private JSlider getSlider() {
			return SliderUI.this.slider;
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
	public void paintThumb(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.translate(this.thumbRect.x, this.thumbRect.y);
		g2.setColor(Colors.SLIDER_THUMB_BACKGROUND);

		if (this.slider.getOrientation() == SwingConstants.HORIZONTAL) {
			g2.fillRoundRect(0, 2, 11, 14, 3, 3);

			if (this.thumbIsPressed) {
				g2.setColor(Colors.SLIDER_THUMB_BORDER_FOCUS);
			} else {
				g2.setColor(Colors.SLIDER_THUMB_BORDER);
			}
			g2.drawRoundRect(0, 2, 10, 14, 3, 3);

			g2.drawLine(2, 5, 2, 13);
			g2.drawLine(4, 5, 4, 13);
			g2.drawLine(6, 5, 6, 13);
			g2.drawLine(8, 5, 8, 13);

			// paint the current value next to thumb
			int curVal = this.slider.getValue();
			String valString = String.valueOf(curVal);
			Rectangle2D strBounds = g2.getFontMetrics().getStringBounds(valString, g2);
			float x = (float) -(strBounds.getWidth() / 2) + 5;
			float y = (float) (thumbRect.height + strBounds.getHeight());

			double overflowX = thumbRect.x + x + strBounds.getWidth() - trackRect.getMaxX();
			if (overflowX > 0) {
				x = (float) (x - overflowX);
			}

			if (slider.getPaintLabels()) {
				g2.setColor(Colors.TEXT_FOREGROUND);
				g2.drawString(valString, x, y);
			}
		} else {
			g2.fillRoundRect(2, 0, 14, 11, 3, 3);

			if (this.thumbIsPressed) {
				g2.setColor(Colors.SLIDER_THUMB_BORDER_FOCUS);
			} else {
				g2.setColor(Colors.SLIDER_THUMB_BORDER);
			}
			g2.drawRoundRect(2, 0, 14, 10, 3, 3);

			g2.drawLine(5, 2, 13, 2);
			g2.drawLine(5, 4, 13, 4);
			g2.drawLine(5, 6, 13, 6);
			g2.drawLine(5, 8, 13, 8);

			// paint the current value next to thumb
			int curVal = this.slider.getValue();
			String valString = String.valueOf(curVal);
			Rectangle2D strBounds = g2.getFontMetrics().getStringBounds(valString, g2);
			float x = thumbRect.width + 10;
			float y = (float) (0 + strBounds.getHeight() / 2) + 2;

			if (slider.getPaintLabels()) {
				g2.setColor(Colors.TEXT_FOREGROUND);
				g2.drawString(valString, x, y);
			}
		}

		g2.translate(-this.thumbRect.x, -this.thumbRect.y);
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		recalculateIfInsetsChanged();
		recalculateIfOrientationChanged();
		Rectangle clip = g.getClipBounds();

		if (!clip.intersects(trackRect) && slider.getPaintTrack()) {
			calculateGeometry();
		}

		if (slider.getPaintTrack() && clip.intersects(trackRect)) {
			paintTrack(g);
		}
		if (slider.hasFocus() && clip.intersects(focusRect)) {
			paintFocus(g);
		}

		// the ticks are now inside the track so they have to be painted each thumb movement
		paintTicks(g);
		// thumb is always painted due to value below thumb
		paintThumb(g);
	}

	@Override
	public void paintTrack(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;

		// make sure slider has same background as container
		if (slider.getParent() != null) {
			g2.setColor(slider.getParent().getBackground());
			g2.fillRect(0, 0, slider.getWidth(), slider.getHeight());
		}

		if (this.slider.getOrientation() == SwingConstants.HORIZONTAL) {
			int trackTop = (int) this.trackRect.getY() + 2;
			int w = this.slider.getWidth();
			int length = w - 6;

			// draw background bar
			if (this.slider.isEnabled()) {
				g2.setColor(Colors.SLIDER_TRACK_BACKGROUND);
			} else {
				g2.setColor(Colors.SLIDER_TRACK_BACKGROUND_DISABLED);
			}
			g2.fillRoundRect(3, trackTop + 1, length, 5, RapidLookAndFeel.CORNER_DEFAULT_RADIUS / 2,
					RapidLookAndFeel.CORNER_DEFAULT_RADIUS / 2);

			// draw fill bar
			g2.setColor(Colors.SLIDER_TRACK_FOREGROUND);
			g2.fill(new Rectangle2D.Double(4, trackTop + 2, this.thumbRect.x + 2, 3));

			// draw border
			g2.setColor(Colors.SLIDER_TRACK_BORDER);
			g2.drawRoundRect(2, trackTop, length, 6, RapidLookAndFeel.CORNER_DEFAULT_RADIUS / 2,
					RapidLookAndFeel.CORNER_DEFAULT_RADIUS / 2);
		} else {
			int trackLeft = (int) this.trackRect.getX() + 2;
			int h = this.slider.getHeight();
			int height = h - 6;

			// draw background bar
			if (this.slider.isEnabled()) {
				g2.setColor(Colors.SLIDER_TRACK_BACKGROUND);
			} else {
				g2.setColor(Colors.SLIDER_TRACK_BACKGROUND_DISABLED);
			}
			g2.fillRoundRect(trackLeft + 1, 3, 5, height, RapidLookAndFeel.CORNER_DEFAULT_RADIUS / 2,
					RapidLookAndFeel.CORNER_DEFAULT_RADIUS / 2);

			// draw fill bar
			g2.setColor(Colors.SLIDER_TRACK_FOREGROUND);
			g2.fill(new Rectangle2D.Double(trackLeft + 2, this.thumbRect.y - 2, 3, h - this.thumbRect.y - 4));

			// draw border
			g2.setColor(Colors.SLIDER_TRACK_BORDER);
			g2.drawRoundRect(trackLeft, 2, 6, height, RapidLookAndFeel.CORNER_DEFAULT_RADIUS / 2,
					RapidLookAndFeel.CORNER_DEFAULT_RADIUS / 2);
		}
	}

	@Override
	public void paintFocus(Graphics g) {
		// intentionally left empty
	}

	@Override
	protected void calculateThumbSize() {
		Dimension size;
		if (this.slider.getOrientation() == SwingConstants.HORIZONTAL) {
			size = new Dimension(13, 18);
		} else {
			size = new Dimension(18, 13);
		}
		this.thumbRect.setSize(size.width, size.height);
	}

	@Override
	protected void calculateThumbLocation() {
		super.calculateThumbLocation();
		if (this.slider.getOrientation() == SwingConstants.HORIZONTAL) {
			thumbRect.y -= 4;
		} else {
			thumbRect.x -= 4;
		}
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
		// we don't want to paint them at all
	}

	@Override
	protected void paintMajorTickForHorizSlider(Graphics g, Rectangle tickBounds, int x) {
		if (Math.abs(trackRect.x - x) < 10 || Math.abs(trackRect.x + trackRect.width - x) < 10) {
			return;
		}

		Graphics2D g2 = (Graphics2D) g;

		int curVal = this.slider.getModel().getValue();
		double percentage = (double) (curVal - slider.getModel().getMinimum()) / (slider.getModel().getMaximum() - slider.getModel().getMinimum());

		boolean isBackground = false;
		if (trackRect.width * percentage > x) {
			g2.setColor(Colors.SLIDER_TRACK_BACKGROUND);
		} else {
			isBackground = true;
			g2.setColor(Colors.SLIDER_TRACK_BORDER);
		}

		int trackTop = (int) this.trackRect.getY() + 3;

		g2.translate(0, -tickBounds.y);
		g2.fillRect(x, trackTop, isBackground ? 1 : 2, 5);
		g2.translate(0, tickBounds.y);

	}

	@Override
	protected void calculateTrackBuffer() {
		trackBuffer = 5;
	}

	@Override
	protected void paintMinorTickForVertSlider(Graphics g, Rectangle tickBounds, int y) {
		// we don't want to paint them at all
	}

	@Override
	protected void paintMajorTickForVertSlider(Graphics g, Rectangle tickBounds, int y) {
		if (Math.abs(trackRect.y - y) < 10 || Math.abs(trackRect.y + trackRect.height - y) < 10) {
			return;
		}

		Graphics2D g2 = (Graphics2D) g;

		int curVal = this.slider.getModel().getValue();
		double percentage = (double) curVal / (slider.getModel().getMaximum() - slider.getModel().getExtent());

		if (trackRect.height * percentage > trackRect.height - y) {
			g2.setColor(Colors.SLIDER_TRACK_BACKGROUND);
		} else {
			g2.setColor(Colors.SLIDER_TRACK_BORDER);
		}

		int trackLeft = (int) this.trackRect.getX() + 3;

		g2.translate(-tickBounds.x, 0);
		g2.fillRect(trackLeft, y, 5, 2);
		g2.translate(tickBounds.x, 0);

	}
}
